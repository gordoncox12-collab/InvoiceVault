package za.co.invoicevault.data.db

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import za.co.invoicevault.domain.DiscountType
import za.co.invoicevault.domain.FolderCategory
import za.co.invoicevault.domain.InvoiceStatus
import za.co.invoicevault.domain.TemplateStyle

@Entity(tableName = "businesses")
data class BusinessEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val tradingName: String = "",
    val registrationNumber: String = "",
    val vatNumber: String = "",
    val email: String = "",
    val phone: String = "",
    val addressLine1: String = "",
    val addressLine2: String = "",
    val city: String = "",
    val province: String = "",
    val postalCode: String = "",
    val country: String = "South Africa",
    val bankName: String = "",
    val accountName: String = "",
    val accountNumber: String = "",
    val branchCode: String = "",
    val invoicePrefix: String = "INV",
    val nextInvoiceSequence: Int = 1,
    val defaultCurrency: String = "ZAR",
    val defaultVatRate: Double = 0.15,
    val logoPath: String? = null,
    val createdAt: Long = System.currentTimeMillis()
) {
    val displayName: String get() = tradingName.ifBlank { name }
}

@Entity(
    tableName = "customers",
    foreignKeys = [
        ForeignKey(
            entity = BusinessEntity::class,
            parentColumns = ["id"],
            childColumns = ["businessId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("businessId")]
)
data class CustomerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val businessId: Long,
    val name: String,
    val contactPerson: String = "",
    val email: String = "",
    val phone: String = "",
    val addressLine1: String = "",
    val city: String = "",
    val province: String = "",
    val postalCode: String = "",
    val country: String = "South Africa",
    val vatNumber: String = "",
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "invoices",
    foreignKeys = [
        ForeignKey(
            entity = BusinessEntity::class,
            parentColumns = ["id"],
            childColumns = ["businessId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = CustomerEntity::class,
            parentColumns = ["id"],
            childColumns = ["customerId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("businessId"), Index("customerId"), Index("number", unique = true)]
)
data class InvoiceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val businessId: Long,
    val customerId: Long,
    val number: String,
    val status: InvoiceStatus = InvoiceStatus.DRAFT,
    val issueDate: Long,
    val dueDate: Long,
    val currency: String = "ZAR",
    val vatRate: Double = 0.15,
    val discountType: DiscountType = DiscountType.NONE,
    val discountValue: Double = 0.0,
    val notes: String = "",
    val footer: String = "Thank you for your business. E&OE.",
    val signaturePath: String? = null,
    val imagePath: String? = null,
    val templateId: Long? = null,
    val pdfPath: String? = null,
    val paidAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "invoice_lines",
    foreignKeys = [
        ForeignKey(
            entity = InvoiceEntity::class,
            parentColumns = ["id"],
            childColumns = ["invoiceId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("invoiceId")]
)
data class InvoiceLineEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val invoiceId: Long,
    val position: Int,
    val description: String,
    val quantity: Double,
    val unitPrice: Double,
    val unit: String = "ea"
)

@Entity(tableName = "templates")
data class TemplateEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val style: TemplateStyle = TemplateStyle.MODERN,
    val primaryColor: Long = 0xFF123524L,
    val accentColor: Long = 0xFFC4A35AL,
    val logoPath: String? = null,
    val bannerImagePath: String? = null,
    val showLogo: Boolean = true,
    val footerText: String = "Banking details appear on each invoice. Prices include 15% VAT unless noted.",
    val defaultNotes: String = "Payment due within 14 days."
)

@Entity(
    tableName = "folder_files",
    foreignKeys = [
        ForeignKey(
            entity = BusinessEntity::class,
            parentColumns = ["id"],
            childColumns = ["businessId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = CustomerEntity::class,
            parentColumns = ["id"],
            childColumns = ["customerId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("businessId"), Index("customerId"), Index("category")]
)
data class FolderFileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val businessId: Long,
    val customerId: Long,
    val category: FolderCategory,
    val displayName: String,
    val filePath: String,
    val mimeType: String,
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

data class InvoiceWithLines(
    @Embedded val invoice: InvoiceEntity,
    @Relation(parentColumn = "id", entityColumn = "invoiceId")
    val lines: List<InvoiceLineEntity>
)
