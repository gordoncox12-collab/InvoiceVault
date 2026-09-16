package za.co.invoicevault.data

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale
import java.util.UUID
import kotlin.math.roundToLong

fun newId(): String = UUID.randomUUID().toString()

enum class InvoiceStatus { DRAFT, SENT, PAID, OVERDUE }

enum class DiscountType { NONE, PERCENT, AMOUNT }

enum class ThemeMode { SYSTEM, LIGHT, DARK }

enum class AccentPalette { CAPE_TEAL, TABLE_MOUNTAIN, PROTEA, GOLD, FOREST, SUNSET }

enum class TemplateLayout { CLASSIC, MODERN, COMPACT }

enum class FileKind { INVOICE_PDF, RECEIPT, EXCEL, IMAGE, NOTE, OTHER }

@Entity(tableName = "businesses")
data class Business(
    @PrimaryKey val id: String = newId(),
    val name: String,
    val tradingName: String = "",
    val registrationNumber: String = "",
    val vatNumber: String = "",
    val email: String = "",
    val phone: String = "",
    val addressLine1: String = "",
    val addressLine2: String = "",
    val city: String = "Cape Town",
    val province: String = "Western Cape",
    val postalCode: String = "",
    val country: String = "South Africa",
    val bankName: String = "",
    val accountName: String = "",
    val accountNumber: String = "",
    val branchCode: String = "",
    val logoPath: String = "",
    val currencyCode: String = "ZAR",
    val defaultVatRate: Double = 15.0,
    val invoicePrefix: String = "INV",
    val nextInvoiceNumber: Int = 1,
    val defaultTemplateId: String = "",
    val createdAt: Long = System.currentTimeMillis()
) {
    val displayName: String get() = tradingName.ifBlank { name }
    val fullAddress: String
        get() = listOf(addressLine1, addressLine2, "$city $postalCode", province, country)
            .filter { it.isNotBlank() }
            .joinToString("\n")
}

@Entity(
    tableName = "customers",
    foreignKeys = [ForeignKey(
        entity = Business::class,
        parentColumns = ["id"],
        childColumns = ["businessId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("businessId")]
)
data class Customer(
    @PrimaryKey val id: String = newId(),
    val businessId: String,
    val name: String,
    val email: String = "",
    val phone: String = "",
    val address: String = "",
    val vatNumber: String = "",
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "invoices",
    foreignKeys = [
        ForeignKey(entity = Business::class, parentColumns = ["id"], childColumns = ["businessId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = Customer::class, parentColumns = ["id"], childColumns = ["customerId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("businessId"), Index("customerId"), Index(value = ["businessId", "number"], unique = true)]
)
data class Invoice(
    @PrimaryKey val id: String = newId(),
    val businessId: String,
    val customerId: String,
    val number: String,
    val issuedOn: Long = System.currentTimeMillis(),
    val dueOn: Long = System.currentTimeMillis() + 14L * 24 * 60 * 60 * 1000,
    val status: InvoiceStatus = InvoiceStatus.DRAFT,
    val notes: String = "",
    val discountType: DiscountType = DiscountType.NONE,
    val discountValue: Double = 0.0,
    val vatRate: Double = 15.0,
    val currencyCode: String = "ZAR",
    val signaturePath: String = "",
    val templateId: String = "",
    val pdfPath: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "invoice_lines",
    foreignKeys = [ForeignKey(
        entity = Invoice::class,
        parentColumns = ["id"],
        childColumns = ["invoiceId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("invoiceId")]
)
data class InvoiceLine(
    @PrimaryKey val id: String = newId(),
    val invoiceId: String,
    val description: String,
    val quantity: Double = 1.0,
    val unitPrice: Double = 0.0,
    val position: Int = 0
) {
    val lineTotal: Double get() = (quantity * unitPrice).roundMoney()
}

@Entity(
    tableName = "receipts",
    foreignKeys = [
        ForeignKey(entity = Business::class, parentColumns = ["id"], childColumns = ["businessId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = Customer::class, parentColumns = ["id"], childColumns = ["customerId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("businessId"), Index("customerId")]
)
data class Receipt(
    @PrimaryKey val id: String = newId(),
    val businessId: String,
    val customerId: String,
    val invoiceId: String = "",
    val number: String,
    val receivedOn: Long = System.currentTimeMillis(),
    val amount: Double,
    val method: String = "EFT",
    val notes: String = "",
    val imagePath: String = "",
    val pdfPath: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "notes",
    foreignKeys = [
        ForeignKey(entity = Business::class, parentColumns = ["id"], childColumns = ["businessId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = Customer::class, parentColumns = ["id"], childColumns = ["customerId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("businessId"), Index("customerId")]
)
data class CustomerNote(
    @PrimaryKey val id: String = newId(),
    val businessId: String,
    val customerId: String,
    val title: String,
    val body: String,
    val filePath: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "vault_files",
    foreignKeys = [
        ForeignKey(entity = Business::class, parentColumns = ["id"], childColumns = ["businessId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = Customer::class, parentColumns = ["id"], childColumns = ["customerId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("businessId"), Index("customerId")]
)
data class VaultFile(
    @PrimaryKey val id: String = newId(),
    val businessId: String,
    val customerId: String,
    val kind: FileKind,
    val displayName: String,
    val path: String,
    val mimeType: String = "application/octet-stream",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "templates")
data class InvoiceTemplate(
    @PrimaryKey val id: String = newId(),
    val businessId: String,
    val name: String,
    val layout: TemplateLayout = TemplateLayout.MODERN,
    val primaryColor: Long = 0xFF0B3A4AL,
    val accentColor: Long = 0xFF1F8A70L,
    val logoPath: String = "",
    val showVat: Boolean = true,
    val footerText: String = "Thank you for your business.",
    val bannerPath: String = ""
)

@Entity(tableName = "prefs")
data class AppPrefs(
    @PrimaryKey val id: Int = 1,
    val seeded: Boolean = false,
    val activeBusinessId: String = "",
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val accent: AccentPalette = AccentPalette.CAPE_TEAL
)

data class InvoiceWithDetails(
    @Embedded val invoice: Invoice,
    @Relation(parentColumn = "customerId", entityColumn = "id")
    val customer: Customer,
    @Relation(parentColumn = "id", entityColumn = "invoiceId")
    val lines: List<InvoiceLine>
)

data class MoneyBreakdown(
    val subtotal: Double,
    val discount: Double,
    val taxable: Double,
    val vat: Double,
    val total: Double
)

fun List<InvoiceLine>.breakdown(invoice: Invoice): MoneyBreakdown {
    val subtotal = sumOf { it.lineTotal }.roundMoney()
    val discount = when (invoice.discountType) {
        DiscountType.NONE -> 0.0
        DiscountType.PERCENT -> (subtotal * invoice.discountValue / 100.0).roundMoney()
        DiscountType.AMOUNT -> invoice.discountValue.roundMoney()
    }.coerceAtMost(subtotal)
    val taxable = (subtotal - discount).roundMoney()
    val vat = (taxable * invoice.vatRate / 100.0).roundMoney()
    return MoneyBreakdown(subtotal, discount, taxable, vat, (taxable + vat).roundMoney())
}

fun Double.roundMoney(): Double = (this * 100.0).roundToLong() / 100.0

fun formatMoney(amount: Double, currencyCode: String = "ZAR"): String {
    val locale = Locale("en", "ZA")
    val nf = NumberFormat.getCurrencyInstance(locale)
    return try {
        nf.currency = Currency.getInstance(currencyCode)
        nf.format(amount)
    } catch (_: Exception) {
        "R ${"%.2f".format(locale, amount)}"
    }
}

fun formatDate(millis: Long): String {
    val fmt = java.text.SimpleDateFormat("dd MMM yyyy", Locale("en", "ZA"))
    return fmt.format(java.util.Date(millis))
}

fun parseDate(text: String, fallback: Long = System.currentTimeMillis()): Long {
    val patterns = listOf("yyyy-MM-dd", "dd/MM/yyyy", "dd MMM yyyy", "yyyy/MM/dd")
    for (p in patterns) {
        try {
            val fmt = java.text.SimpleDateFormat(p, Locale("en", "ZA"))
            fmt.isLenient = false
            return fmt.parse(text)?.time ?: continue
        } catch (_: Exception) {
        }
    }
    return fallback
}

fun isoDate(millis: Long): String {
    val fmt = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.US)
    return fmt.format(java.util.Date(millis))
}
