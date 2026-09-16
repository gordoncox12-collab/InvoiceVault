package za.co.invoicevault.data

import android.content.Context
import android.net.Uri
import androidx.room.Room
import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.File

class VaultRepository(
    context: Context,
    val files: VaultFileStore
) {
    val db: InvoiceVaultDatabase = Room.databaseBuilder(
        context,
        InvoiceVaultDatabase::class.java,
        "invoicevault.db"
    ).fallbackToDestructiveMigration().build()

    val businesses = db.businesses().observeAll()
    val prefs = db.prefs().observe().map { it ?: AppPrefs() }

    fun customers(businessId: String): Flow<List<Customer>> = db.customers().observeForBusiness(businessId)
    fun invoices(businessId: String): Flow<List<InvoiceWithDetails>> = db.invoices().observeForBusiness(businessId)
    fun invoicesForCustomer(customerId: String) = db.invoices().observeForCustomer(customerId)
    fun receiptsForCustomer(customerId: String) = db.receipts().observeForCustomer(customerId)
    fun receipts(businessId: String) = db.receipts().observeForBusiness(businessId)
    fun notes(customerId: String) = db.notes().observeForCustomer(customerId)
    fun vaultFiles(customerId: String) = db.files().observeForCustomer(customerId)
    fun templates(businessId: String) = db.templates().observeForBusiness(businessId)

    suspend fun ensureSeeded() {
        val current = db.prefs().get()
        if (current?.seeded == true) {
            db.invoices().markOverdue(System.currentTimeMillis())
            return
        }
        db.withTransaction {
            val business = SeedData.business()
            db.businesses().upsert(business)
            SeedData.templates().forEach { db.templates().upsert(it) }
            SeedData.customers().forEach {
                files.ensureCustomerTree(business.id, it.id)
                db.customers().upsert(it)
            }
            val (invoices, lines) = SeedData.invoicesAndLines()
            invoices.forEach { db.invoices().upsert(it) }
            db.invoices().upsertLines(lines)
            SeedData.receipts().forEach { db.receipts().upsert(it) }
            SeedData.notes().forEach { note ->
                val stored = files.writeBytes(
                    business.id,
                    note.customerId,
                    VaultFolder.NOTES,
                    "${note.title.replace(' ', '_')}.txt",
                    note.body.toByteArray()
                )
                db.notes().upsert(note.copy(filePath = stored.absolutePath))
            }
            db.prefs().upsert(
                AppPrefs(
                    seeded = true,
                    activeBusinessId = business.id,
                    themeMode = ThemeMode.SYSTEM,
                    accent = AccentPalette.CAPE_TEAL
                )
            )
        }
        db.invoices().markOverdue(System.currentTimeMillis())
    }

    suspend fun savePrefs(prefs: AppPrefs) = db.prefs().upsert(prefs)

    suspend fun saveBusiness(business: Business) = db.businesses().upsert(business)

    suspend fun deleteBusiness(business: Business) = db.businesses().delete(business)

    suspend fun saveCustomer(customer: Customer) {
        files.ensureCustomerTree(customer.businessId, customer.id)
        db.customers().upsert(customer)
    }

    suspend fun deleteCustomer(customer: Customer) = db.customers().delete(customer)

    suspend fun getCustomer(id: String) = db.customers().get(id)

    suspend fun getBusiness(id: String) = db.businesses().get(id)

    suspend fun getInvoice(id: String) = db.invoices().getWithDetails(id)

    suspend fun nextInvoiceNumber(business: Business): Pair<Business, String> {
        val number = "${business.invoicePrefix}-${business.nextInvoiceNumber.toString().padStart(4, '0')}"
        val updated = business.copy(nextInvoiceNumber = business.nextInvoiceNumber + 1)
        db.businesses().upsert(updated)
        return updated to number
    }

    suspend fun saveInvoice(invoice: Invoice, lines: List<InvoiceLine>) {
        db.withTransaction {
            db.invoices().upsert(invoice.copy(updatedAt = System.currentTimeMillis()))
            db.invoices().clearLines(invoice.id)
            db.invoices().upsertLines(lines.mapIndexed { index, line -> line.copy(invoiceId = invoice.id, position = index) })
        }
        files.ensureCustomerTree(invoice.businessId, invoice.customerId)
    }

    suspend fun deleteInvoice(invoice: Invoice) = db.invoices().delete(invoice)

    suspend fun saveReceipt(receipt: Receipt) = db.receipts().upsert(receipt)

    suspend fun deleteReceipt(receipt: Receipt) = db.receipts().delete(receipt)

    suspend fun saveNote(note: CustomerNote): CustomerNote {
        val stored = files.writeBytes(
            note.businessId,
            note.customerId,
            VaultFolder.NOTES,
            "${note.title.ifBlank { "note" }.replace(' ', '_')}.txt",
            note.body.toByteArray()
        )
        val updated = note.copy(filePath = stored.absolutePath, updatedAt = System.currentTimeMillis())
        db.notes().upsert(updated)
        return updated
    }

    suspend fun deleteNote(note: CustomerNote) = db.notes().delete(note)

    suspend fun saveTemplate(template: InvoiceTemplate) = db.templates().upsert(template)

    suspend fun deleteTemplate(template: InvoiceTemplate) = db.templates().delete(template)

    suspend fun getTemplate(id: String) = db.templates().get(id)

    suspend fun importUriAsFile(
        uri: Uri,
        businessId: String,
        customerId: String,
        folder: VaultFolder,
        name: String,
        kind: FileKind,
        mime: String
    ): VaultFile {
        val file = files.copyFromUri(uri, businessId, customerId, folder, name)
        val record = VaultFile(
            businessId = businessId,
            customerId = customerId,
            kind = kind,
            displayName = name,
            path = file.absolutePath,
            mimeType = mime
        )
        db.files().upsert(record)
        return record
    }

    suspend fun registerFile(
        businessId: String,
        customerId: String,
        file: File,
        kind: FileKind,
        mime: String
    ): VaultFile {
        val record = VaultFile(
            businessId = businessId,
            customerId = customerId,
            kind = kind,
            displayName = file.name,
            path = file.absolutePath,
            mimeType = mime
        )
        db.files().upsert(record)
        return record
    }

    suspend fun findOrCreateCustomer(businessId: String, name: String, email: String = "", phone: String = "", address: String = ""): Customer {
        val existing = db.customers().findByName(businessId, name.trim())
        if (existing != null) return existing
        val created = Customer(
            businessId = businessId,
            name = name.trim(),
            email = email,
            phone = phone,
            address = address
        )
        saveCustomer(created)
        return created
    }
}
