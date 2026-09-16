package za.co.invoicevault.data.repo

import android.net.Uri
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import za.co.invoicevault.data.db.BusinessEntity
import za.co.invoicevault.data.db.CustomerEntity
import za.co.invoicevault.data.db.FolderFileEntity
import za.co.invoicevault.data.db.InvoiceEntity
import za.co.invoicevault.data.db.InvoiceLineEntity
import za.co.invoicevault.data.db.InvoiceVaultDatabase
import za.co.invoicevault.data.db.InvoiceWithLines
import za.co.invoicevault.data.db.TemplateEntity
import za.co.invoicevault.data.excel.ColumnMapper
import za.co.invoicevault.data.excel.SheetTable
import za.co.invoicevault.data.excel.SpreadsheetService
import za.co.invoicevault.data.files.VaultFileStore
import za.co.invoicevault.data.pdf.InvoicePdfWriter
import za.co.invoicevault.data.prefs.SettingsStore
import za.co.invoicevault.data.seed.SeedData
import za.co.invoicevault.data.share.ShareHelper
import za.co.invoicevault.domain.FolderCategory
import za.co.invoicevault.domain.InvoiceNumbering
import za.co.invoicevault.domain.InvoiceStatus
import java.io.File
import java.util.Calendar

class VaultRepository(
    private val db: InvoiceVaultDatabase,
    private val files: VaultFileStore,
    private val pdf: InvoicePdfWriter,
    private val sheets: SpreadsheetService,
    val share: ShareHelper,
    val settings: SettingsStore
) {
    private val seed = SeedData(db, files)

    suspend fun bootstrap() {
        seed.seedIfEmpty()
        db.customers().let { dao ->
            db.businesses().all().forEach { business ->
                dao.forBusiness(business.id).forEach { customer ->
                    files.ensureCustomerTree(business.id, customer.id)
                }
            }
        }
        refreshOverdue()
    }

    fun businesses(): Flow<List<BusinessEntity>> = db.businesses().observeAll()
    fun templates(): Flow<List<TemplateEntity>> = db.templates().observeAll()
    fun customers(businessId: Long): Flow<List<CustomerEntity>> = db.customers().observeForBusiness(businessId)
    fun invoices(businessId: Long): Flow<List<InvoiceEntity>> = db.invoices().observeForBusiness(businessId)
    fun invoicesForCustomer(customerId: Long): Flow<List<InvoiceEntity>> = db.invoices().observeForCustomer(customerId)
    fun folderFiles(customerId: Long, category: FolderCategory): Flow<List<FolderFileEntity>> =
        db.files().observe(customerId, category)

    suspend fun business(id: Long) = db.businesses().byId(id)
    suspend fun customer(id: Long) = db.customers().byId(id)
    suspend fun invoice(id: Long) = db.invoices().withLines(id)
    suspend fun template(id: Long) = db.templates().byId(id)
    suspend fun allTemplates() = db.templates().all()

    suspend fun saveBusiness(entity: BusinessEntity): Long {
        return if (entity.id == 0L) db.businesses().insert(entity) else {
            db.businesses().update(entity)
            entity.id
        }
    }

    suspend fun deleteBusiness(entity: BusinessEntity) = db.businesses().delete(entity)

    suspend fun saveCustomer(entity: CustomerEntity): Long {
        val id = if (entity.id == 0L) db.customers().insert(entity) else {
            db.customers().update(entity)
            entity.id
        }
        files.ensureCustomerTree(entity.businessId, id)
        return id
    }

    suspend fun deleteCustomer(entity: CustomerEntity) = db.customers().delete(entity)

    suspend fun saveTemplate(entity: TemplateEntity): Long {
        return if (entity.id == 0L) db.templates().insert(entity) else {
            db.templates().update(entity)
            entity.id
        }
    }

    suspend fun deleteTemplate(entity: TemplateEntity) = db.templates().delete(entity)

    suspend fun allocateNumber(business: BusinessEntity): String {
        val year = Calendar.getInstance().get(Calendar.YEAR)
        val existing = db.invoices().numbersForBusiness(business.id)
        val seq = InvoiceNumbering.nextSequence(existing, business.invoicePrefix, year)
            .coerceAtLeast(business.nextInvoiceSequence)
        db.businesses().update(business.copy(nextInvoiceSequence = seq + 1))
        return InvoiceNumbering.format(business.invoicePrefix, year, seq)
    }

    suspend fun saveInvoice(invoice: InvoiceEntity, lines: List<InvoiceLineEntity>): Long {
        val id = if (invoice.id == 0L) db.invoices().insert(invoice) else {
            db.invoices().update(invoice.copy(updatedAt = System.currentTimeMillis()))
            invoice.id
        }
        db.lines().deleteForInvoice(id)
        db.lines().insertAll(lines.mapIndexed { index, line -> line.copy(id = 0, invoiceId = id, position = index) })
        val customer = db.customers().byId(invoice.customerId)
        if (customer != null) files.ensureCustomerTree(invoice.businessId, customer.id)
        return id
    }

    suspend fun deleteInvoice(invoice: InvoiceEntity) = db.invoices().delete(invoice)

    suspend fun setInvoiceStatus(id: Long, status: InvoiceStatus) {
        val paidAt = if (status == InvoiceStatus.PAID) System.currentTimeMillis() else null
        db.invoices().updateStatus(id, status, paidAt, System.currentTimeMillis())
    }

    suspend fun refreshOverdue() {
        val now = System.currentTimeMillis()
        db.invoices().all().forEach { invoice ->
            if (invoice.status == InvoiceStatus.SENT && invoice.dueDate < now) {
                db.invoices().updateStatus(invoice.id, InvoiceStatus.OVERDUE, invoice.paidAt, now)
            }
        }
    }

    suspend fun renderPdf(invoiceId: Long): File {
        val packed = requireNotNull(db.invoices().withLines(invoiceId))
        val business = requireNotNull(db.businesses().byId(packed.invoice.businessId))
        val customer = requireNotNull(db.customers().byId(packed.invoice.customerId))
        val template = packed.invoice.templateId?.let { db.templates().byId(it) }
            ?: db.templates().all().firstOrNull()
        val dir = files.categoryDir(business.id, customer.id, FolderCategory.INVOICES)
        val dest = files.uniqueFile(dir, "${packed.invoice.number}.pdf")
        pdf.write(dest, business, customer, packed, template)
        db.invoices().update(packed.invoice.copy(pdfPath = dest.absolutePath, updatedAt = System.currentTimeMillis()))
        val shareCopy = File(files.sharedCache(), dest.name)
        dest.copyTo(shareCopy, overwrite = true)
        db.files().insert(
            FolderFileEntity(
                businessId = business.id,
                customerId = customer.id,
                category = FolderCategory.INVOICES,
                displayName = dest.name,
                filePath = dest.absolutePath,
                mimeType = "application/pdf"
            )
        )
        return shareCopy
    }

    suspend fun importUriToFolder(
        businessId: Long,
        customerId: Long,
        category: FolderCategory,
        uri: Uri,
        displayName: String,
        mime: String
    ): FolderFileEntity {
        files.ensureCustomerTree(businessId, customerId)
        val dest = files.uniqueFile(files.categoryDir(businessId, customerId, category), displayName)
        files.copyFromUri(uri, dest)
        val entity = FolderFileEntity(
            businessId = businessId,
            customerId = customerId,
            category = category,
            displayName = dest.name,
            filePath = dest.absolutePath,
            mimeType = mime.ifBlank { "application/octet-stream" }
        )
        val id = db.files().insert(entity)
        return entity.copy(id = id)
    }

    suspend fun saveNote(businessId: Long, customerId: Long, title: String, body: String): FolderFileEntity {
        val dest = files.uniqueFile(files.categoryDir(businessId, customerId, FolderCategory.NOTES), "$title.txt")
        files.writeBytes(dest, body.toByteArray())
        val entity = FolderFileEntity(
            businessId = businessId,
            customerId = customerId,
            category = FolderCategory.NOTES,
            displayName = dest.name,
            filePath = dest.absolutePath,
            mimeType = "text/plain",
            note = body.take(200)
        )
        val id = db.files().insert(entity)
        return entity.copy(id = id)
    }

    suspend fun saveLocalImage(businessId: Long, customerId: Long, bytes: ByteArray, name: String): File {
        val dest = files.uniqueFile(files.categoryDir(businessId, customerId, FolderCategory.IMAGES), name)
        files.writeBytes(dest, bytes)
        db.files().insert(
            FolderFileEntity(
                businessId = businessId,
                customerId = customerId,
                category = FolderCategory.IMAGES,
                displayName = dest.name,
                filePath = dest.absolutePath,
                mimeType = "image/png"
            )
        )
        return dest
    }

    suspend fun deleteFolderFile(entity: FolderFileEntity) {
        File(entity.filePath).delete()
        db.files().delete(entity)
    }

    fun readSheetFromStream(fileName: String, bytes: ByteArray): SheetTable =
        sheets.read(fileName, bytes.inputStream())

    fun writeCustomersXlsx(customers: List<CustomerEntity>): ByteArray {
        val headers = listOf("name", "contactPerson", "email", "phone", "addressLine1", "city", "province", "postalCode", "vatNumber", "notes")
        val rows = customers.map {
            listOf(it.name, it.contactPerson, it.email, it.phone, it.addressLine1, it.city, it.province, it.postalCode, it.vatNumber, it.notes)
        }
        return sheets.writeXlsxBytes("Customers", headers, rows)
    }

    suspend fun captureSheet(
        businessId: Long,
        customerId: Long,
        fileName: String,
        bytes: ByteArray,
        mime: String
    ): File {
        files.ensureCustomerTree(businessId, customerId)
        val dest = files.uniqueFile(files.categoryDir(businessId, customerId, FolderCategory.EXCEL), fileName)
        files.writeBytes(dest, bytes)
        db.files().insert(
            FolderFileEntity(
                businessId = businessId,
                customerId = customerId,
                category = FolderCategory.EXCEL,
                displayName = dest.name,
                filePath = dest.absolutePath,
                mimeType = mime
            )
        )
        return dest
    }

    fun writeInvoicesCsv(invoices: List<InvoiceWithLines>): ByteArray {
        val headers = listOf("number", "status", "customerId", "issueDate", "dueDate", "description", "quantity", "unitPrice", "unit")
        val rows = invoices.flatMap { packed ->
            packed.lines.map { line ->
                listOf(
                    packed.invoice.number,
                    packed.invoice.status.name,
                    packed.invoice.customerId.toString(),
                    packed.invoice.issueDate.toString(),
                    packed.invoice.dueDate.toString(),
                    line.description,
                    line.quantity.toString(),
                    line.unitPrice.toString(),
                    line.unit
                )
            }
        }
        return sheets.writeCsv(headers, rows)
    }

    suspend fun importCustomers(businessId: Long, table: SheetTable, mapping: Map<String, String>): Int {
        var count = 0
        table.rows.forEach { row ->
            val mapped = ColumnMapper.mappedRow(table.headers, row, mapping)
            val name = mapped["name"].orEmpty()
            if (name.isBlank()) return@forEach
            saveCustomer(
                CustomerEntity(
                    businessId = businessId,
                    name = name,
                    contactPerson = mapped["contactPerson"].orEmpty(),
                    email = mapped["email"].orEmpty(),
                    phone = mapped["phone"].orEmpty(),
                    addressLine1 = mapped["addressLine1"].orEmpty(),
                    city = mapped["city"].orEmpty(),
                    province = mapped["province"].orEmpty(),
                    postalCode = mapped["postalCode"].orEmpty(),
                    vatNumber = mapped["vatNumber"].orEmpty(),
                    notes = mapped["notes"].orEmpty()
                )
            )
            count++
        }
        return count
    }

    suspend fun exportCustomersToFolder(businessId: Long, customerId: Long): File {
        val customers = db.customers().forBusiness(businessId)
        val bytes = writeCustomersXlsx(customers)
        val dest = files.uniqueFile(files.categoryDir(businessId, customerId, FolderCategory.EXCEL), "customers.xlsx")
        files.writeBytes(dest, bytes)
        db.files().insert(
            FolderFileEntity(
                businessId = businessId,
                customerId = customerId,
                category = FolderCategory.EXCEL,
                displayName = dest.name,
                filePath = dest.absolutePath,
                mimeType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            )
        )
        return dest
    }

    suspend fun currentBusiness(): BusinessEntity? {
        val all = db.businesses().all()
        val preferred = settings.settings.first().defaultBusinessId
        return all.firstOrNull { it.id == preferred } ?: all.firstOrNull()
    }
}
