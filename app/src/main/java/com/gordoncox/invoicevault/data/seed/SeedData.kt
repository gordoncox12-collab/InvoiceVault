package com.gordoncox.invoicevault.data.seed

import com.gordoncox.invoicevault.data.db.AppDatabase
import com.gordoncox.invoicevault.data.entity.AccentPalette
import com.gordoncox.invoicevault.data.entity.AppSettingsEntity
import com.gordoncox.invoicevault.data.entity.BusinessEntity
import com.gordoncox.invoicevault.data.entity.CustomerEntity
import com.gordoncox.invoicevault.data.entity.FolderType
import com.gordoncox.invoicevault.data.entity.InvoiceEntity
import com.gordoncox.invoicevault.data.entity.InvoiceLineItemEntity
import com.gordoncox.invoicevault.data.entity.InvoiceStatus
import com.gordoncox.invoicevault.data.entity.InvoiceTemplateEntity
import com.gordoncox.invoicevault.data.entity.NoteEntity
import com.gordoncox.invoicevault.data.entity.TemplateLayout
import com.gordoncox.invoicevault.data.entity.ThemeMode
import com.gordoncox.invoicevault.data.entity.TransactionEntity
import com.gordoncox.invoicevault.data.entity.TransactionType
import com.gordoncox.invoicevault.data.excel.ExcelService
import com.gordoncox.invoicevault.data.files.LocalStorage
import com.gordoncox.invoicevault.util.Money
import com.gordoncox.invoicevault.util.Za
import java.io.File

object SeedData {
    suspend fun insertIfEmpty(db: AppDatabase, storage: LocalStorage, excel: ExcelService, filesDir: File) {
        if (db.businessDao().count() > 0) return
        val now = System.currentTimeMillis()
        val day = 86_400_000L

        val coxId = "biz-cox-electrical"
        val harbourId = "biz-harbour-books"

        val cox = BusinessEntity(
            id = coxId,
            name = "Cox Electrical & Solar",
            tradingName = "Cox Electrical",
            registrationNumber = "2016/442118/07",
            vatNumber = "4560123456",
            email = "gordon@coxelectrical.co.za",
            phone = "021 555 0148",
            addressLine1 = "14 Roodebloem Road",
            addressLine2 = null,
            city = "Cape Town",
            province = "Western Cape",
            postalCode = "7925",
            country = "South Africa",
            bankName = "Standard Bank",
            bankAccountName = "Cox Electrical & Solar",
            bankAccountNumber = "071234567",
            bankBranchCode = "051001",
            logoPath = null,
            defaultCurrency = "ZAR",
            defaultVatPercent = 15.0,
            invoicePrefix = "CX",
            nextInvoiceNumber = 8,
            createdAt = now - 400 * day,
            updatedAt = now,
        )
        val harbour = BusinessEntity(
            id = harbourId,
            name = "Harbour View Bookkeeping",
            tradingName = null,
            registrationNumber = "2019/118334/07",
            vatNumber = "4129988776",
            email = "gordon@harbourviewbooks.co.za",
            phone = "021 555 2201",
            addressLine1 = "2 Dock Road",
            addressLine2 = "V&A Waterfront",
            city = "Cape Town",
            province = "Western Cape",
            postalCode = "8001",
            country = "South Africa",
            bankName = "FNB",
            bankAccountName = "Harbour View Bookkeeping",
            bankAccountNumber = "62844123987",
            bankBranchCode = "250655",
            logoPath = null,
            defaultCurrency = "ZAR",
            defaultVatPercent = 15.0,
            invoicePrefix = "HV",
            nextInvoiceNumber = 3,
            createdAt = now - 200 * day,
            updatedAt = now,
        )
        db.businessDao().upsert(cox)
        db.businessDao().upsert(harbour)

        val customers = listOf(
            CustomerEntity("cust-atlantic", coxId, "Atlantic Seaboard Properties", "Naledi Jacobs", "naledi@asbprop.co.za", "021 434 8800", "302 Beach Road", "Sea Point", "Western Cape", "8005", "4230011122", "Gate code 4412. Park in basement B.", now - 300 * day, now),
            CustomerEntity("cust-kirstenbosch", coxId, "Kirstenbosch Hospitality Group", "Pieter van Wyk", "pieter@kbosch.hospitality", "021 799 8783", "Rhodes Drive", "Newlands", "Western Cape", "7735", "4015566778", "Deliveries via service entrance after 07:00.", now - 280 * day, now),
            CustomerEntity("cust-falsebay", coxId, "False Bay Marine Services", "Amina Davids", "amina@falsebaymarine.co.za", "021 788 4102", "12 Harbour Road", "Kalk Bay", "Western Cape", "7975", "4182233445", "Boat yard — hard hats required.", now - 210 * day, now),
            CustomerEntity("cust-stellenbosch", coxId, "Stellenbosch Wine Logistics", "Johan de Villiers", "johan@swlogistics.co.za", "021 883 2290", "44 Distillery Road", "Stellenbosch", "Western Cape", "7600", "4098877665", "Accounts contact: finance@swlogistics.co.za", now - 180 * day, now),
            CustomerEntity("cust-obs", coxId, "Observatory Coffee Roasters", "Thandi Mokoena", "thandi@obsroasters.co.za", "021 447 3311", "88 Lower Main Road", "Observatory", "Western Cape", "7925", null, "Prefer WhatsApp for site access.", now - 90 * day, now),
            CustomerEntity("cust-waterfront", harbourId, "Table Bay Yacht Club", "Sipho Nkosi", "office@tbyc.co.za", "021 421 1354", "Breakwater Boulevard", "Cape Town", "Western Cape", "8001", "4001122334", "Monthly retainers.", now - 120 * day, now),
        )
        customers.forEach {
            FolderType.entries.forEach { type -> storage.folder(it.businessId, it.id, type) }
            db.customerDao().upsert(it)
        }

        val classic = InvoiceTemplateEntity(
            id = "tpl-classic",
            businessId = coxId,
            name = "Classic Cape",
            isDefault = true,
            primaryColor = 0xFF0F6E56,
            accentColor = 0xFFC9A227,
            logoPath = null,
            layout = TemplateLayout.CLASSIC.name,
            showBankDetails = true,
            footerText = "Cox Electrical & Solar  ·  Woodstock, Cape Town  ·  VAT 4560123456",
            headerImagePath = null,
            extraImagePath = null,
            createdAt = now,
            updatedAt = now,
        )
        val modern = InvoiceTemplateEntity(
            id = "tpl-modern",
            businessId = coxId,
            name = "Modern Ocean",
            isDefault = false,
            primaryColor = 0xFF0B6E99,
            accentColor = 0xFF0F6E56,
            logoPath = null,
            layout = TemplateLayout.MODERN.name,
            showBankDetails = true,
            footerText = "E&OE. All workmanship guaranteed for 12 months.",
            headerImagePath = null,
            extraImagePath = null,
            createdAt = now,
            updatedAt = now,
        )
        val harbourTpl = InvoiceTemplateEntity(
            id = "tpl-harbour",
            businessId = harbourId,
            name = "Harbour compact",
            isDefault = true,
            primaryColor = 0xFF1E3A5F,
            accentColor = 0xFFC9A227,
            logoPath = null,
            layout = TemplateLayout.COMPACT.name,
            showBankDetails = true,
            footerText = "Harbour View Bookkeeping — V&A Waterfront",
            headerImagePath = null,
            extraImagePath = null,
            createdAt = now,
            updatedAt = now,
        )
        db.templateDao().upsert(classic)
        db.templateDao().upsert(modern)
        db.templateDao().upsert(harbourTpl)

        suspend fun invoice(
            id: String,
            customerId: String,
            number: String,
            status: InvoiceStatus,
            issueAgoDays: Long,
            dueInDays: Long,
            items: List<Triple<String, Double, Double>>,
            discountPercent: Double = 0.0,
            notes: String? = null,
            businessId: String = coxId,
            templateId: String = classic.id,
        ) {
            val issue = now - issueAgoDays * day
            val due = issue + dueInDays * day
            val lineEntities = items.mapIndexed { idx, triple ->
                InvoiceLineItemEntity(Za.newId(), id, idx, triple.first, triple.second, triple.third, true)
            }
            val totals = Money.totals(lineEntities.map { Money.lineTotal(it.quantity, it.unitPrice) }, 0.0, discountPercent, 15.0)
            val inv = InvoiceEntity(
                id = id,
                businessId = businessId,
                customerId = customerId,
                number = number,
                status = status.name,
                issueDate = issue,
                dueDate = due,
                currency = "ZAR",
                vatPercent = 15.0,
                discountAmount = 0.0,
                discountPercent = discountPercent,
                notes = notes,
                terms = "Payment due within 30 days. Interest may be charged on overdue accounts.",
                signaturePath = null,
                pdfPath = null,
                templateId = templateId,
                subtotal = totals.subtotal,
                vatAmount = totals.vat,
                total = totals.total,
                createdAt = issue,
                updatedAt = now,
            )
            db.invoiceDao().upsert(inv)
            lineEntities.forEach { db.invoiceDao().upsertItem(it) }
        }

        invoice(
            "inv-cx-2026-0001", "cust-atlantic", "CX-2026-0001", InvoiceStatus.PAID, 80, 30,
            listOf(
                Triple("Distribution board upgrade — 12-way", 1.0, 18500.0),
                Triple("Surge protection kit", 1.0, 2450.0),
                Triple("Labour — 2 electricians x 1 day", 2.0, 1850.0),
            ),
            notes = "Paid by EFT. Quote Q-441.",
        )
        invoice(
            "inv-cx-2026-0002", "cust-kirstenbosch", "CX-2026-0002", InvoiceStatus.SENT, 18, 30,
            listOf(
                Triple("LED downlighter retrofit (garden restaurant)", 42.0, 185.0),
                Triple("Driver replacement (outdoor IP65)", 8.0, 420.0),
                Triple("After-hours call-out", 1.0, 950.0),
            ),
            notes = "Work completed 28 Aug 2026.",
        )
        invoice(
            "inv-cx-2026-0003", "cust-falsebay", "CX-2026-0003", InvoiceStatus.OVERDUE, 50, 21,
            listOf(
                Triple("Three-phase socket install — workshop", 4.0, 1650.0),
                Triple("Cable 16mm² (per metre)", 40.0, 89.0),
                Triple("COC inspection", 1.0, 1250.0),
            ),
        )
        invoice(
            "inv-cx-2026-0004", "cust-stellenbosch", "CX-2026-0004", InvoiceStatus.DRAFT, 2, 30,
            listOf(
                Triple("8kW hybrid inverter supply & install", 1.0, 42800.0),
                Triple("5.1kWh battery module", 2.0, 18900.0),
                Triple("Roof-mount PV array 6.6kWp", 1.0, 56400.0),
                Triple("SSEG application assistance", 1.0, 2200.0),
            ),
            discountPercent = 5.0,
            notes = "Draft pending confirmation of battery count.",
            templateId = modern.id,
        )
        invoice(
            "inv-cx-2026-0005", "cust-obs", "CX-2026-0005", InvoiceStatus.SENT, 6, 14,
            listOf(
                Triple("Emergency lighting pack", 6.0, 780.0),
                Triple("Fault-finding — roasting plant", 1.0, 1450.0),
            ),
        )
        invoice(
            "inv-hv-2026-0001", "cust-waterfront", "HV-2026-0001", InvoiceStatus.PAID, 40, 30,
            listOf(
                Triple("Monthly bookkeeping — July 2026", 1.0, 4800.0),
                Triple("VAT201 preparation", 1.0, 950.0),
            ),
            businessId = harbourId,
            templateId = harbourTpl.id,
        )

        val paid = db.invoiceDao().get("inv-cx-2026-0001")!!
        db.transactionDao().upsert(
            TransactionEntity(
                id = "tx-1",
                businessId = coxId,
                customerId = "cust-atlantic",
                invoiceId = paid.id,
                type = TransactionType.PAYMENT.name,
                amount = paid.total,
                currency = "ZAR",
                occurredAt = now - 70 * day,
                method = "EFT",
                reference = "ASB-441",
                notes = "Full settlement CX-2026-0001",
                receiptPath = null,
                createdAt = now - 70 * day,
            ),
        )
        db.transactionDao().upsert(
            TransactionEntity(
                id = "tx-2",
                businessId = coxId,
                customerId = "cust-falsebay",
                invoiceId = null,
                type = TransactionType.EXPENSE.name,
                amount = 1860.0,
                currency = "ZAR",
                occurredAt = now - 48 * day,
                method = "Card",
                reference = "MAKRO-CT",
                notes = "Cable and glands for Kalk Bay job",
                receiptPath = null,
                createdAt = now - 48 * day,
            ),
        )
        val harbourPaid = db.invoiceDao().get("inv-hv-2026-0001")!!
        db.transactionDao().upsert(
            TransactionEntity(
                id = "tx-3",
                businessId = harbourId,
                customerId = "cust-waterfront",
                invoiceId = harbourPaid.id,
                type = TransactionType.PAYMENT.name,
                amount = harbourPaid.total,
                currency = "ZAR",
                occurredAt = now - 20 * day,
                method = "EFT",
                reference = "TBYC-JUL",
                notes = null,
                receiptPath = null,
                createdAt = now - 20 * day,
            ),
        )

        db.noteDao().upsert(
            NoteEntity(
                id = "note-1",
                businessId = coxId,
                customerId = "cust-atlantic",
                title = "Site access",
                body = "Basement parking bay 18. Reception has spare keys for the meter room. Load-shedding: they run a 20kVA generator — isolate before working on the DB.",
                createdAt = now - 40 * day,
                updatedAt = now - 10 * day,
            ),
        )
        db.noteDao().upsert(
            NoteEntity(
                id = "note-2",
                businessId = coxId,
                customerId = "cust-stellenbosch",
                title = "SSEG paperwork",
                body = "Awaiting CoCT SSEG approval before commissioning. Johan will send the existing single-line diagram.",
                createdAt = now - 3 * day,
                updatedAt = now - 3 * day,
            ),
        )

        val excelDir = storage.folder(coxId, "cust-stellenbosch", FolderType.EXCEL)
        excel.writeWorkbook(
            File(excelDir, "solar-quote-worksheet.xlsx"),
            mapOf(
                "BOM" to (
                    listOf("Item", "Qty", "Unit ZAR", "Supplier") to listOf(
                        listOf("Hybrid inverter 8kW", "1", "42800", "Local distributor"),
                        listOf("Battery 5.1kWh", "2", "18900", "Local distributor"),
                        listOf("PV modules 550W", "12", "2150", "Jhb warehouse"),
                    )
                    ),
            ),
        )
        File(storage.folder(coxId, "cust-atlantic", FolderType.NOTES), "site-access.txt")
            .writeText("Basement parking bay 18.\n")

        db.settingsDao().upsert(
            AppSettingsEntity(
                id = 1,
                themeMode = ThemeMode.SYSTEM.name,
                accentPalette = AccentPalette.TEAL_VAULT.name,
                activeBusinessId = coxId,
            ),
        )
    }
}
