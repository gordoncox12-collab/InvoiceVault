package za.co.invoicevault.ui.vm

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import za.co.invoicevault.InvoiceVaultApp
import za.co.invoicevault.data.db.CustomerEntity
import za.co.invoicevault.data.db.InvoiceEntity
import za.co.invoicevault.data.db.InvoiceLineEntity
import za.co.invoicevault.data.db.TemplateEntity
import za.co.invoicevault.domain.DiscountType
import za.co.invoicevault.domain.InvoiceStatus
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.concurrent.TimeUnit

data class InvoiceEditorState(
    val invoice: InvoiceEntity? = null,
    val lines: List<InvoiceLineEntity> = listOf(InvoiceLineEntity(invoiceId = 0, position = 0, description = "", quantity = 1.0, unitPrice = 0.0)),
    val customers: List<CustomerEntity> = emptyList(),
    val templates: List<TemplateEntity> = emptyList(),
    val message: String? = null,
    val pdfFile: File? = null,
    val loading: Boolean = true
)

class InvoiceEditorViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {
    private val repo = (application as InvoiceVaultApp).container.repository
    private val invoiceId = savedStateHandle.get<String>("invoiceId")?.toLongOrNull() ?: 0L
    private val businessIdArg = savedStateHandle.get<String>("businessId")?.toLongOrNull() ?: 0L
    private val customerIdArg = savedStateHandle.get<String>("customerId")?.toLongOrNull() ?: 0L

    private val _state = MutableStateFlow(InvoiceEditorState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch { load() }
    }

    private suspend fun load() {
        val businessId = if (invoiceId != 0L) {
            repo.invoice(invoiceId)?.invoice?.businessId ?: businessIdArg
        } else businessIdArg
        val business = repo.business(businessId) ?: repo.currentBusiness()
        if (business == null) {
            _state.value = _state.value.copy(loading = false, message = "Create a business first")
            return
        }
        val customerList = repo.customers(business.id).first()
        val templates = repo.allTemplates()
        if (invoiceId != 0L) {
            val packed = repo.invoice(invoiceId)
            _state.value = InvoiceEditorState(
                invoice = packed?.invoice,
                lines = packed?.lines?.ifEmpty { _state.value.lines } ?: _state.value.lines,
                customers = customerList,
                templates = templates,
                loading = false
            )
        } else {
            val now = System.currentTimeMillis()
            val number = repo.allocateNumber(business)
            val customerId = if (customerIdArg != 0L) customerIdArg else customerList.firstOrNull()?.id ?: 0L
            _state.value = InvoiceEditorState(
                invoice = InvoiceEntity(
                    businessId = business.id,
                    customerId = customerId,
                    number = number,
                    issueDate = now,
                    dueDate = now + TimeUnit.DAYS.toMillis(14),
                    currency = business.defaultCurrency,
                    vatRate = business.defaultVatRate,
                    templateId = templates.firstOrNull()?.id
                ),
                customers = customerList,
                templates = templates,
                loading = false
            )
        }
    }

    fun updateInvoice(transform: (InvoiceEntity) -> InvoiceEntity) {
        val current = _state.value.invoice ?: return
        _state.value = _state.value.copy(invoice = transform(current))
    }

    fun updateLines(lines: List<InvoiceLineEntity>) {
        _state.value = _state.value.copy(lines = lines)
    }

    fun addLine() {
        val lines = _state.value.lines
        updateLines(lines + InvoiceLineEntity(invoiceId = 0, position = lines.size, description = "", quantity = 1.0, unitPrice = 0.0))
    }

    fun removeLine(index: Int) {
        updateLines(_state.value.lines.filterIndexed { i, _ -> i != index }.ifEmpty { _state.value.lines })
    }

    fun save(onSaved: (Long) -> Unit) {
        viewModelScope.launch {
            val invoice = _state.value.invoice ?: return@launch
            if (invoice.customerId == 0L) {
                _state.value = _state.value.copy(message = "Choose a customer")
                return@launch
            }
            val id = repo.saveInvoice(invoice, _state.value.lines.filter { it.description.isNotBlank() })
            _state.value = _state.value.copy(invoice = invoice.copy(id = id), message = "Invoice ${invoice.number} saved")
            onSaved(id)
        }
    }

    fun setStatus(status: InvoiceStatus) {
        updateInvoice { it.copy(status = status) }
        val id = _state.value.invoice?.id ?: return
        if (id != 0L) viewModelScope.launch { repo.setInvoiceStatus(id, status) }
    }

    fun setDiscount(type: DiscountType, value: Double) = updateInvoice { it.copy(discountType = type, discountValue = value) }

    fun attachImage(path: String) = updateInvoice { it.copy(imagePath = path) }
    fun attachSignature(path: String) = updateInvoice { it.copy(signaturePath = path) }

    fun saveSignatureBitmap(bitmap: Bitmap) {
        viewModelScope.launch {
            val invoice = _state.value.invoice ?: return@launch
            val out = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            val file = repo.saveLocalImage(invoice.businessId, invoice.customerId, out.toByteArray(), "signature-${invoice.number}.png")
            attachSignature(file.absolutePath)
            if (invoice.id != 0L) repo.saveInvoice(invoice.copy(signaturePath = file.absolutePath), _state.value.lines)
            _state.value = _state.value.copy(message = "Signature captured")
        }
    }

    fun renderPdf() {
        viewModelScope.launch {
            val invoice = _state.value.invoice ?: return@launch
            val id = if (invoice.id == 0L) repo.saveInvoice(invoice, _state.value.lines) else invoice.id
            val file = repo.renderPdf(id)
            _state.value = _state.value.copy(pdfFile = file, invoice = invoice.copy(id = id), message = "PDF ready: ${file.name}")
        }
    }

    fun consumeMessage() {
        _state.value = _state.value.copy(message = null)
    }

    fun repository() = repo
}
