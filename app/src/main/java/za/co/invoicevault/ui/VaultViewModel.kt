package za.co.invoicevault.ui

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import za.co.invoicevault.AppContainer
import za.co.invoicevault.data.AccentPalette
import za.co.invoicevault.data.AppPrefs
import za.co.invoicevault.data.Business
import za.co.invoicevault.data.Customer
import za.co.invoicevault.data.CustomerNote
import za.co.invoicevault.data.DiscountType
import za.co.invoicevault.data.FileKind
import za.co.invoicevault.data.Invoice
import za.co.invoicevault.data.InvoiceLine
import za.co.invoicevault.data.InvoiceStatus
import za.co.invoicevault.data.InvoiceTemplate
import za.co.invoicevault.data.InvoiceWithDetails
import za.co.invoicevault.data.Receipt
import za.co.invoicevault.data.ThemeMode
import za.co.invoicevault.data.VaultFile
import za.co.invoicevault.data.VaultFolder
import za.co.invoicevault.data.breakdown
import za.co.invoicevault.data.formatDate
import za.co.invoicevault.data.formatMoney
import za.co.invoicevault.data.newId
import za.co.invoicevault.data.parseDate
import za.co.invoicevault.excel.ExcelService
import za.co.invoicevault.excel.SheetData
import za.co.invoicevault.excel.WorkbookData
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalCoroutinesApi::class)
class VaultViewModel(private val container: AppContainer) : ViewModel() {
    private val repo = container.repo
    val share = container.share
    val excel: ExcelService = container.excel

    val prefs: StateFlow<AppPrefs> = repo.prefs.stateIn(viewModelScope, SharingStarted.Eagerly, AppPrefs())
    val businesses: StateFlow<List<Business>> = repo.businesses.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val activeBusiness: StateFlow<Business?> = combine(businesses, prefs) { list, p ->
        list.firstOrNull { it.id == p.activeBusinessId } ?: list.firstOrNull()
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val customers: StateFlow<List<Customer>> = activeBusiness.flatMapLatest { biz ->
        if (biz == null) flowOf(emptyList()) else repo.customers(biz.id)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val invoices: StateFlow<List<InvoiceWithDetails>> = activeBusiness.flatMapLatest { biz ->
        if (biz == null) flowOf(emptyList()) else repo.invoices(biz.id)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val receipts: StateFlow<List<Receipt>> = activeBusiness.flatMapLatest { biz ->
        if (biz == null) flowOf(emptyList()) else repo.receipts(biz.id)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val templates: StateFlow<List<InvoiceTemplate>> = activeBusiness.flatMapLatest { biz ->
        if (biz == null) flowOf(emptyList()) else repo.templates(biz.id)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _message = MutableStateFlow<String?>(null)
    val message = _message.asStateFlow()

    private val _workbook = MutableStateFlow<WorkbookData?>(null)
    val workbook = _workbook.asStateFlow()
    private val _columnMap = MutableStateFlow<List<String>>(emptyList())
    val columnMap = _columnMap.asStateFlow()

    val outstanding = invoices.map { list ->
        list.filter { it.invoice.status == InvoiceStatus.SENT || it.invoice.status == InvoiceStatus.OVERDUE }
            .sumOf { it.lines.breakdown(it.invoice).total }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, 0.0)

    init {
        viewModelScope.launch {
            runCatching { repo.ensureSeeded() }
                .onFailure { _message.value = it.message ?: "Could not start local vault" }
        }
    }

    fun clearMessage() { _message.value = null }

    fun setTheme(mode: ThemeMode) = viewModelScope.launch {
        repo.savePrefs(prefs.value.copy(themeMode = mode))
    }

    fun setAccent(accent: AccentPalette) = viewModelScope.launch {
        repo.savePrefs(prefs.value.copy(accent = accent))
    }

    fun setActiveBusiness(id: String) = viewModelScope.launch {
        repo.savePrefs(prefs.value.copy(activeBusinessId = id))
    }

    fun saveBusiness(business: Business) = viewModelScope.launch {
        repo.saveBusiness(business)
        if (prefs.value.activeBusinessId.isBlank()) {
            repo.savePrefs(prefs.value.copy(activeBusinessId = business.id))
        }
        _message.value = "Business saved"
    }

    fun saveCustomer(customer: Customer) = viewModelScope.launch {
        repo.saveCustomer(customer)
        _message.value = "Customer saved"
    }

    fun deleteCustomer(customer: Customer) = viewModelScope.launch {
        repo.deleteCustomer(customer)
    }

    fun customer(id: String): StateFlow<Customer?> =
        customers.map { list -> list.firstOrNull { it.id == id } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(3_000), customers.value.firstOrNull { it.id == id })

    fun invoicesFor(customerId: String) = repo.invoicesForCustomer(customerId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(3_000), emptyList())

    fun receiptsFor(customerId: String) = repo.receiptsForCustomer(customerId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(3_000), emptyList())

    fun notesFor(customerId: String) = repo.notes(customerId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(3_000), emptyList())

    fun filesFor(customerId: String) = repo.vaultFiles(customerId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(3_000), emptyList())

    suspend fun loadInvoice(id: String): InvoiceWithDetails? = repo.getInvoice(id)

    fun saveInvoice(invoice: Invoice, lines: List<InvoiceLine>, andGenerate: Boolean = false, context: Context? = null) {
        viewModelScope.launch {
            repo.saveInvoice(invoice, lines)
            if (andGenerate && context != null) {
                generateAndShare(invoice.id, shareKind = ShareKind.NONE)
            } else {
                _message.value = "Invoice ${invoice.number} saved"
            }
        }
    }

    fun deleteInvoice(invoice: Invoice) = viewModelScope.launch { repo.deleteInvoice(invoice) }

    fun saveReceipt(receipt: Receipt, generatePdf: Boolean = false) = viewModelScope.launch {
        var stored = receipt
        if (generatePdf) {
            val biz = activeBusiness.value ?: return@launch
            val customer = repo.getCustomer(receipt.customerId) ?: return@launch
            val file = container.files.folder(biz.id, customer.id, VaultFolder.RECEIPTS)
                .resolve("${receipt.number}.pdf")
            val invoiceNo = receipt.invoiceId.takeIf { it.isNotBlank() }?.let { repo.getInvoice(it)?.invoice?.number }
            container.pdf.renderReceipt(file, biz, customer, receipt, invoiceNo)
            repo.registerFile(biz.id, customer.id, file, FileKind.RECEIPT, "application/pdf")
            stored = receipt.copy(pdfPath = file.absolutePath)
        }
        repo.saveReceipt(stored)
        _message.value = "Receipt ${stored.number} saved"
    }

    fun saveNote(note: CustomerNote) = viewModelScope.launch {
        repo.saveNote(note)
        _message.value = "Note saved to customer folder"
    }

    fun deleteNote(note: CustomerNote) = viewModelScope.launch { repo.deleteNote(note) }

    fun saveTemplate(template: InvoiceTemplate) = viewModelScope.launch {
        repo.saveTemplate(template)
        _message.value = "Template saved"
    }

    suspend fun allocateInvoiceNumber(): String {
        val biz = activeBusiness.value ?: return "INV-0001"
        val (updated, number) = repo.nextInvoiceNumber(biz)
        return number
    }

    fun importMedia(uri: Uri, customer: Customer, folder: VaultFolder, name: String, kind: FileKind, mime: String) {
        viewModelScope.launch {
            repo.importUriAsFile(uri, customer.businessId, customer.id, folder, name, kind, mime)
            _message.value = "Saved $name"
        }
    }

    fun saveBitmap(bitmap: Bitmap, businessId: String, customerId: String, folder: VaultFolder, name: String): File {
        val file = container.files.folder(businessId, customerId, folder).resolve(name)
        FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        return file
    }

    fun saveSignature(invoiceId: String, bitmap: Bitmap) {
        viewModelScope.launch {
            val details = repo.getInvoice(invoiceId) ?: return@launch
            val file = saveBitmap(
                bitmap,
                details.invoice.businessId,
                details.invoice.customerId,
                VaultFolder.INVOICES,
                "${details.invoice.number}-signature.png"
            )
            repo.saveInvoice(details.invoice.copy(signaturePath = file.absolutePath), details.lines)
            repo.registerFile(details.invoice.businessId, details.invoice.customerId, file, FileKind.IMAGE, "image/png")
            _message.value = "Signature stamped on ${details.invoice.number}"
        }
    }

    fun saveLogo(uri: Uri, business: Business) {
        viewModelScope.launch {
            val dummyCustomer = customers.value.firstOrNull()?.id ?: "branding"
            container.files.ensureCustomerTree(business.id, dummyCustomer)
            val file = container.files.copyFromUri(uri, business.id, dummyCustomer, VaultFolder.IMAGES, "logo.png")
            repo.saveBusiness(business.copy(logoPath = file.absolutePath))
            _message.value = "Logo updated"
        }
    }

    fun saveTemplateImage(uri: Uri, template: InvoiceTemplate, banner: Boolean) {
        viewModelScope.launch {
            val biz = template.businessId.ifBlank { activeBusiness.value?.id ?: return@launch }
            val folder = container.files.folder(biz, "templates", VaultFolder.IMAGES)
            val file = File(folder, if (banner) "banner-${template.id}.png" else "logo-${template.id}.png")
            container.app.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(file).use { input.copyTo(it) }
            }
            repo.saveTemplate(
                if (banner) template.copy(bannerPath = file.absolutePath)
                else template.copy(logoPath = file.absolutePath)
            )
            _message.value = "Image inserted into template"
        }
    }

    enum class ShareKind { NONE, CHOOSER, WHATSAPP, EMAIL }

    fun generateAndShare(invoiceId: String, shareKind: ShareKind) {
        viewModelScope.launch {
            val details = repo.getInvoice(invoiceId) ?: return@launch
            val biz = repo.getBusiness(details.invoice.businessId) ?: return@launch
            val templateId = details.invoice.templateId.ifBlank { biz.defaultTemplateId }
            val template = templateId.takeIf { it.isNotBlank() }?.let { repo.getTemplate(it) }
                ?: templates.value.firstOrNull()
            val file = container.files.folder(biz.id, details.customer.id, VaultFolder.INVOICES)
                .resolve("${details.invoice.number}.pdf")
            container.pdf.renderInvoice(file, biz, details.customer, details.invoice, details.lines, template)
            repo.saveInvoice(details.invoice.copy(pdfPath = file.absolutePath), details.lines)
            repo.registerFile(biz.id, details.customer.id, file, FileKind.INVOICE_PDF, "application/pdf")
            val totals = details.lines.breakdown(details.invoice)
            val text = "${details.invoice.number} from ${biz.displayName} · ${formatMoney(totals.total, details.invoice.currencyCode)}"
            when (shareKind) {
                ShareKind.NONE -> _message.value = "PDF saved to ${details.customer.name}'s invoices folder"
                ShareKind.CHOOSER -> share.shareFile(file, "application/pdf", details.invoice.number, text)
                ShareKind.WHATSAPP -> share.shareViaWhatsApp(file, text)
                ShareKind.EMAIL -> share.shareViaEmail(
                    file,
                    "${details.invoice.number} — ${biz.displayName}",
                    "Please find invoice ${details.invoice.number} attached.\n\n${totalLine(details)}",
                    details.customer.email.ifBlank { null }
                )
            }
        }
    }

    fun shareReceipt(receipt: Receipt, whatsApp: Boolean) {
        viewModelScope.launch {
            val path = receipt.pdfPath
            if (path.isBlank() || !File(path).exists()) {
                saveReceipt(receipt, generatePdf = true)
            }
            val file = File(receipt.pdfPath.ifBlank {
                container.files.folder(receipt.businessId, receipt.customerId, VaultFolder.RECEIPTS)
                    .resolve("${receipt.number}.pdf").absolutePath
            })
            if (!file.exists()) {
                _message.value = "Generate the receipt PDF first"
                return@launch
            }
            val text = "Receipt ${receipt.number} — ${formatMoney(receipt.amount)}"
            if (whatsApp) share.shareViaWhatsApp(file, text)
            else share.shareViaEmail(file, "Receipt ${receipt.number}", text, null)
        }
    }

    private fun totalLine(details: InvoiceWithDetails): String {
        val t = details.lines.breakdown(details.invoice)
        return "Total due ${formatMoney(t.total, details.invoice.currencyCode)} by ${formatDate(details.invoice.dueOn)}"
    }

    fun openWorkbook(uri: Uri) {
        viewModelScope.launch {
            runCatching { excel.readUri(uri) }
                .onSuccess { data ->
                    _workbook.value = data
                    val headers = data.firstSheet.headers
                    _columnMap.value = headers.map { guessColumn(it) }
                    _message.value = "Loaded ${data.firstSheet.body.size} rows from ${data.firstSheet.name}"
                }
                .onFailure { _message.value = it.message ?: "Could not read spreadsheet" }
        }
    }

    fun updateMapping(index: Int, value: String) {
        _columnMap.value = _columnMap.value.toMutableList().also {
            if (index in it.indices) it[index] = value
        }
    }

    fun importMappedRows() {
        viewModelScope.launch {
            val biz = activeBusiness.value ?: return@launch
            val sheet = _workbook.value?.firstSheet ?: return@launch
            val map = _columnMap.value
            var createdCustomers = 0
            var createdInvoices = 0
            val grouped = linkedMapOf<String, MutableList<List<String>>>()
            sheet.body.forEachIndexed { index, row ->
                val number = mapped(row, map, "Invoice number").ifBlank { "ROW-${index + 1}" }
                grouped.getOrPut(number) { mutableListOf() }.add(row)
            }
            grouped.forEach { (number, rows) ->
                val first = rows.first()
                val customerName = mapped(first, map, "Customer name").ifBlank { "Imported customer" }
                val existed = customers.value.any { it.businessId == biz.id && it.name.equals(customerName, true) }
                val customer = repo.findOrCreateCustomer(
                    biz.id,
                    customerName,
                    mapped(first, map, "Customer email"),
                    mapped(first, map, "Customer phone"),
                    mapped(first, map, "Customer address")
                )
                if (!existed) createdCustomers++
                val existingNumber = mapped(first, map, "Invoice number")
                val invoiceNumber = existingNumber.ifBlank {
                    repo.nextInvoiceNumber(repo.getBusiness(biz.id) ?: biz).second
                }
                val status = runCatching {
                    InvoiceStatus.valueOf(mapped(first, map, "Status").uppercase().ifBlank { "DRAFT" })
                }.getOrDefault(InvoiceStatus.DRAFT)
                val invoice = Invoice(
                    businessId = biz.id,
                    customerId = customer.id,
                    number = invoiceNumber,
                    issuedOn = parseDate(mapped(first, map, "Invoice date")),
                    dueOn = parseDate(mapped(first, map, "Due date"), parseDate(mapped(first, map, "Invoice date")) + 14L * 86400000),
                    status = status,
                    notes = mapped(first, map, "Notes"),
                    vatRate = mapped(first, map, "VAT rate").toDoubleOrNull() ?: biz.defaultVatRate,
                    discountType = if (mapped(first, map, "Discount").isNotBlank()) DiscountType.AMOUNT else DiscountType.NONE,
                    discountValue = mapped(first, map, "Discount").toDoubleOrNull() ?: 0.0,
                    currencyCode = biz.currencyCode,
                    templateId = biz.defaultTemplateId
                )
                val lines = rows.mapIndexed { i, row ->
                    val desc = mapped(row, map, "Line description").ifBlank { "Imported line ${i + 1}" }
                    val qty = mapped(row, map, "Quantity").toDoubleOrNull() ?: 1.0
                    val price = mapped(row, map, "Unit price").toDoubleOrNull()
                        ?: ((mapped(row, map, "Amount").toDoubleOrNull() ?: 0.0) / qty)
                    InvoiceLine(invoiceId = invoice.id, description = desc, quantity = qty, unitPrice = price, position = i)
                }
                repo.saveInvoice(invoice, lines)
                createdInvoices++
            }
            _message.value = "Imported $createdInvoices invoice(s), $createdCustomers new customer(s)"
        }
    }

    fun exportWorkbook(kind: String): File? {
        val biz = activeBusiness.value ?: return null
        val sheets = when (kind) {
            "customers" -> listOf(
                SheetData(
                    "Customers",
                    listOf(listOf("Name", "Email", "Phone", "Address", "VAT")) +
                        customers.value.map { listOf(it.name, it.email, it.phone, it.address, it.vatNumber) }
                )
            )
            "invoices" -> {
                val header = listOf("Invoice", "Customer", "Issued", "Due", "Status", "Description", "Qty", "Unit price", "Line total", "VAT %", "Notes")
                val rows = mutableListOf(header)
                invoices.value.forEach { inv ->
                    if (inv.lines.isEmpty()) {
                        rows += listOf(inv.invoice.number, inv.customer.name, formatDate(inv.invoice.issuedOn), formatDate(inv.invoice.dueOn), inv.invoice.status.name, "", "", "", "", inv.invoice.vatRate.toString(), inv.invoice.notes)
                    } else {
                        inv.lines.forEach { line ->
                            rows += listOf(
                                inv.invoice.number, inv.customer.name, formatDate(inv.invoice.issuedOn),
                                formatDate(inv.invoice.dueOn), inv.invoice.status.name, line.description,
                                line.quantity.toString(), line.unitPrice.toString(), line.lineTotal.toString(),
                                inv.invoice.vatRate.toString(), inv.invoice.notes
                            )
                        }
                    }
                }
                listOf(SheetData("Invoices", rows))
            }
            else -> listOf(
                SheetData(
                    "Customers",
                    listOf(listOf("Name", "Email", "Phone", "Address", "VAT")) +
                        customers.value.map { listOf(it.name, it.email, it.phone, it.address, it.vatNumber) }
                ),
                SheetData(
                    "Invoices",
                    listOf(listOf("Number", "Customer", "Status", "Issued", "Due", "Total")) +
                        invoices.value.map {
                            listOf(
                                it.invoice.number, it.customer.name, it.invoice.status.name,
                                formatDate(it.invoice.issuedOn), formatDate(it.invoice.dueOn),
                                it.lines.breakdown(it.invoice).total.toString()
                            )
                        }
                )
            )
        }
        val file = container.files.sharedCache("${kind}-${biz.invoicePrefix}.xlsx")
        file.writeBytes(excel.writeXlsx(sheets))
        val firstCustomer = customers.value.firstOrNull()
        if (firstCustomer != null) {
            viewModelScope.launch {
                val stored = container.files.copyFile(file, biz.id, firstCustomer.id, VaultFolder.EXCEL, file.name)
                repo.registerFile(biz.id, firstCustomer.id, stored, FileKind.EXCEL, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
            }
        }
        return file
    }

    fun exportCsv(): File {
        val file = container.files.sharedCache("invoices.csv")
        val sheet = SheetData(
            "Invoices",
            listOf(listOf("Number", "Customer", "Status", "Total")) +
                invoices.value.map {
                    listOf(it.invoice.number, it.customer.name, it.invoice.status.name, it.lines.breakdown(it.invoice).total.toString())
                }
        )
        file.writeBytes(excel.writeCsv(listOf(sheet)))
        return file
    }

    private fun mapped(row: List<String>, map: List<String>, field: String): String {
        val index = map.indexOf(field)
        if (index < 0 || index >= row.size) return ""
        return row[index].trim()
    }

    private fun guessColumn(header: String): String {
        val h = header.lowercase()
        return when {
            h.contains("email") -> "Customer email"
            h.contains("phone") || h.contains("mobile") || h.contains("cell") -> "Customer phone"
            h.contains("address") -> "Customer address"
            h.contains("vat") && h.contains("customer") -> "Customer VAT"
            h.contains("customer") || h.contains("client") || h == "name" -> "Customer name"
            h.contains("invoice") && (h.contains("no") || h.contains("num") || h.contains("#")) -> "Invoice number"
            h.contains("due") -> "Due date"
            h.contains("date") -> "Invoice date"
            h.contains("status") -> "Status"
            h.contains("qty") || h.contains("quantity") -> "Quantity"
            h.contains("unit") || h.contains("price") || h.contains("rate") -> "Unit price"
            h.contains("amount") || h.contains("total") -> "Amount"
            h.contains("desc") || h.contains("item") || h.contains("service") -> "Line description"
            h.contains("note") -> "Notes"
            h.contains("discount") -> "Discount"
            h.contains("vat") || h.contains("tax") -> "VAT rate"
            else -> "Ignore"
        }
    }

    companion object {
        fun factory(container: AppContainer) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = VaultViewModel(container) as T
        }
    }
}
