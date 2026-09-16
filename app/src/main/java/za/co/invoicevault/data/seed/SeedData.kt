package za.co.invoicevault.data.seed

import za.co.invoicevault.data.db.BusinessEntity
import za.co.invoicevault.data.db.CustomerEntity
import za.co.invoicevault.data.db.FolderFileEntity
import za.co.invoicevault.data.db.InvoiceEntity
import za.co.invoicevault.data.db.InvoiceLineEntity
import za.co.invoicevault.data.db.InvoiceVaultDatabase
import za.co.invoicevault.data.db.TemplateEntity
import za.co.invoicevault.data.files.VaultFileStore
import za.co.invoicevault.domain.DiscountType
import za.co.invoicevault.domain.FolderCategory
import za.co.invoicevault.domain.InvoiceStatus
import za.co.invoicevault.domain.TemplateStyle
import java.util.concurrent.TimeUnit

class SeedData(
    private val db: InvoiceVaultDatabase,
    private val files: VaultFileStore
) {
    suspend fun seedIfEmpty() {
        if (db.businesses().count() > 0) return
        val now = System.currentTimeMillis()
        val day = TimeUnit.DAYS.toMillis(1)

        val classicId = db.templates().insert(
            TemplateEntity(
                name = "Classic Cape",
                style = TemplateStyle.CLASSIC,
                primaryColor = 0xFF123524L,
                accentColor = 0xFFC4A35AL,
                footerText = "Banking details on this invoice. E&OE. Registered in South Africa."
            )
        )
        val modernId = db.templates().insert(
            TemplateEntity(
                name = "Waterfront Modern",
                style = TemplateStyle.MODERN,
                primaryColor = 0xFF0E7C7BL,
                accentColor = 0xFFC4A35AL,
                defaultNotes = "Please use the invoice number as payment reference."
            )
        )
        db.templates().insert(
            TemplateEntity(
                name = "Compact Fynbos",
                style = TemplateStyle.COMPACT,
                primaryColor = 0xFF2D6A4FL,
                accentColor = 0xFFE76F51L
            )
        )

        val toursId = db.businesses().insert(
            BusinessEntity(
                name = "Table Mountain Guided Walks (Pty) Ltd",
                tradingName = "Table Mountain Walks",
                registrationNumber = "2021/884421/07",
                vatNumber = "4560123456",
                email = "invoices@tablemountainwalks.co.za",
                phone = "+27 21 424 1100",
                addressLine1 = "12 Kloof Nek Road",
                city = "Cape Town",
                province = "Western Cape",
                postalCode = "8001",
                bankName = "Standard Bank",
                accountName = "Table Mountain Guided Walks",
                accountNumber = "071234567",
                branchCode = "051001",
                invoicePrefix = "TMW"
            )
        )
        val gardenId = db.businesses().insert(
            BusinessEntity(
                name = "Kirstenbosch Garden Care",
                tradingName = "Kirstenbosch Care",
                registrationNumber = "2018/220119/07",
                vatNumber = "4129988776",
                email = "accounts@kirstenboschcare.co.za",
                phone = "+27 21 799 8783",
                addressLine1 = "Rhodes Drive, Newlands",
                city = "Cape Town",
                province = "Western Cape",
                postalCode = "7700",
                bankName = "FNB",
                accountName = "Kirstenbosch Garden Care",
                accountNumber = "62844123987",
                branchCode = "250655",
                invoicePrefix = "KGC"
            )
        )
        val spiceId = db.businesses().insert(
            BusinessEntity(
                name = "Bo-Kaap Spice Kitchen",
                tradingName = "Bo-Kaap Kitchen",
                registrationNumber = "2019/331002/07",
                vatNumber = "4332211099",
                email = "hello@bokaapkitchen.co.za",
                phone = "+27 21 424 8855",
                addressLine1 = "77 Wale Street, Bo-Kaap",
                city = "Cape Town",
                province = "Western Cape",
                postalCode = "8001",
                bankName = "Nedbank",
                accountName = "Bo-Kaap Spice Kitchen",
                accountNumber = "11098765432",
                branchCode = "198765",
                invoicePrefix = "BKK"
            )
        )

        val vaId = db.customers().insert(
            CustomerEntity(
                businessId = toursId,
                name = "V&A Waterfront Management",
                contactPerson = "Thandi Nkosi",
                email = "events@waterfront.co.za",
                phone = "+27 21 408 7600",
                addressLine1 = "V&A Waterfront",
                city = "Cape Town",
                province = "Western Cape",
                postalCode = "8002",
                vatNumber = "4500112233"
            )
        )
        val seaPointId = db.customers().insert(
            CustomerEntity(
                businessId = toursId,
                name = "Sea Point Guesthouse",
                contactPerson = "Johan van Zyl",
                email = "stay@seapointguesthouse.co.za",
                phone = "+27 21 434 2211",
                addressLine1 = "18 Beach Road",
                city = "Sea Point",
                province = "Western Cape",
                postalCode = "8005"
            )
        )
        val campsId = db.customers().insert(
            CustomerEntity(
                businessId = gardenId,
                name = "Camps Bay Villa Rentals",
                contactPerson = "Lerato Mokoena",
                email = "ops@campsbayvillas.co.za",
                phone = "+27 21 438 0090",
                addressLine1 = "The Drive",
                city = "Camps Bay",
                province = "Western Cape",
                postalCode = "8005"
            )
        )
        val wineId = db.customers().insert(
            CustomerEntity(
                businessId = gardenId,
                name = "Stellenbosch Wine Route NPC",
                contactPerson = "Anika Botha",
                email = "info@wineroute.co.za",
                phone = "+27 21 886 4310",
                addressLine1 = "36 Market Street",
                city = "Stellenbosch",
                province = "Western Cape",
                postalCode = "7600",
                vatNumber = "4187766554"
            )
        )
        val coffeeId = db.customers().insert(
            CustomerEntity(
                businessId = spiceId,
                name = "Observatory Coffee Lab",
                contactPerson = "Sipho Dlamini",
                email = "sipho@obscoffeelab.co.za",
                phone = "+27 21 447 3300",
                addressLine1 = "91 Lower Main Road",
                city = "Observatory",
                province = "Western Cape",
                postalCode = "7925"
            )
        )
        val constantiaId = db.customers().insert(
            CustomerEntity(
                businessId = spiceId,
                name = "Constantia Valley Weddings",
                contactPerson = "Claire Naidoo",
                email = "claire@cvweddings.co.za",
                phone = "+27 21 794 5120",
                addressLine1 = "Spaanschemat River Road",
                city = "Constantia",
                province = "Western Cape",
                postalCode = "7806"
            )
        )

        listOf(
            toursId to vaId,
            toursId to seaPointId,
            gardenId to campsId,
            gardenId to wineId,
            spiceId to coffeeId,
            spiceId to constantiaId
        ).forEach { (b, c) -> files.ensureCustomerTree(b, c) }

        val inv1 = db.invoices().insert(
            InvoiceEntity(
                businessId = toursId,
                customerId = vaId,
                number = "TMW-2026-0001",
                status = InvoiceStatus.PAID,
                issueDate = now - 40 * day,
                dueDate = now - 26 * day,
                notes = "Sunrise hike for the waterfront staff retreat.",
                templateId = modernId,
                paidAt = now - 30 * day
            )
        )
        db.lines().insertAll(
            listOf(
                InvoiceLineEntity(invoiceId = inv1, position = 0, description = "Guided sunrise hike (12 guests)", quantity = 12.0, unitPrice = 650.0, unit = "pp"),
                InvoiceLineEntity(invoiceId = inv1, position = 1, description = "Cableway coordination & permits", quantity = 1.0, unitPrice = 1800.0, unit = "ea")
            )
        )
        db.businesses().update(db.businesses().byId(toursId)!!.copy(nextInvoiceSequence = 2))

        val inv2 = db.invoices().insert(
            InvoiceEntity(
                businessId = toursId,
                customerId = seaPointId,
                number = "TMW-2026-0002",
                status = InvoiceStatus.SENT,
                issueDate = now - 10 * day,
                dueDate = now + 4 * day,
                discountType = DiscountType.PERCENT,
                discountValue = 5.0,
                notes = "Repeat guest rate applied.",
                templateId = classicId
            )
        )
        db.lines().insertAll(
            listOf(
                InvoiceLineEntity(invoiceId = inv2, position = 0, description = "Lion's Head sunset walk", quantity = 6.0, unitPrice = 420.0, unit = "pp")
            )
        )
        db.businesses().update(db.businesses().byId(toursId)!!.copy(nextInvoiceSequence = 3))

        val inv3 = db.invoices().insert(
            InvoiceEntity(
                businessId = gardenId,
                customerId = wineId,
                number = "KGC-2026-0001",
                status = InvoiceStatus.OVERDUE,
                issueDate = now - 45 * day,
                dueDate = now - 15 * day,
                notes = "Seasonal planting at the tasting terrace.",
                templateId = classicId
            )
        )
        db.lines().insertAll(
            listOf(
                InvoiceLineEntity(invoiceId = inv3, position = 0, description = "Indigenous planting (fynbos beds)", quantity = 1.0, unitPrice = 18500.0, unit = "job"),
                InvoiceLineEntity(invoiceId = inv3, position = 1, description = "Irrigation repair", quantity = 8.0, unitPrice = 450.0, unit = "hr")
            )
        )
        db.businesses().update(db.businesses().byId(gardenId)!!.copy(nextInvoiceSequence = 2))

        val inv4 = db.invoices().insert(
            InvoiceEntity(
                businessId = spiceId,
                customerId = constantiaId,
                number = "BKK-2026-0001",
                status = InvoiceStatus.DRAFT,
                issueDate = now,
                dueDate = now + 14 * day,
                notes = "Menu tasting still to be confirmed.",
                templateId = modernId
            )
        )
        db.lines().insertAll(
            listOf(
                InvoiceLineEntity(invoiceId = inv4, position = 0, description = "Wedding catering — Cape Malay feast", quantity = 80.0, unitPrice = 285.0, unit = "pp"),
                InvoiceLineEntity(invoiceId = inv4, position = 1, description = "Koeksister & malva pudding station", quantity = 1.0, unitPrice = 2400.0, unit = "ea")
            )
        )
        db.businesses().update(db.businesses().byId(spiceId)!!.copy(nextInvoiceSequence = 2))

        suspend fun note(businessId: Long, customerId: Long, name: String, body: String) {
            val dir = files.categoryDir(businessId, customerId, FolderCategory.NOTES)
            val file = files.uniqueFile(dir, name)
            files.writeBytes(file, body.toByteArray())
            db.files().insert(
                FolderFileEntity(
                    businessId = businessId,
                    customerId = customerId,
                    category = FolderCategory.NOTES,
                    displayName = name,
                    filePath = file.absolutePath,
                    mimeType = "text/plain",
                    note = "Seeded on first launch"
                )
            )
        }
        note(toursId, vaId, "retreat-brief.txt", "Staff retreat: 12 walkers, meet at Lower Cable Station 05:30. Weather backup: Kirstenbosch contour.")
        note(spiceId, constantiaId, "menu-notes.txt", "Halal kitchen. Load-shedding backup: silent generator on terrace.")

        suspend fun csv(businessId: Long, customerId: Long, name: String, csv: String) {
            val dir = files.categoryDir(businessId, customerId, FolderCategory.EXCEL)
            val file = files.uniqueFile(dir, name)
            files.writeBytes(file, csv.toByteArray())
            db.files().insert(
                FolderFileEntity(
                    businessId = businessId,
                    customerId = customerId,
                    category = FolderCategory.EXCEL,
                    displayName = name,
                    filePath = file.absolutePath,
                    mimeType = "text/csv"
                )
            )
        }
        csv(
            gardenId,
            campsId,
            "villa-garden-hours.csv",
            "date,hours,rate,notes\n2026-08-12,6,450,hedge trim\n2026-08-19,4,450,irrigation"
        )
    }
}
