package za.co.invoicevault.data

object SeedData {
    const val BUSINESS_ID = "biz-atlantic-kloof"
    const val TEMPLATE_MODERN = "tpl-modern-teal"
    const val TEMPLATE_CLASSIC = "tpl-classic-navy"
    const val TEMPLATE_COMPACT = "tpl-compact-gold"

    private const val CUST_MARINE = "cust-table-bay-marine"
    private const val CUST_GUEST = "cust-camps-bay-guest"
    private const val CUST_HARVEST = "cust-stellenbosch-harvest"
    private const val CUST_MEDICAL = "cust-green-point-medical"

    fun business() = Business(
        id = BUSINESS_ID,
        name = "Atlantic & Kloof Bookkeeping (Pty) Ltd",
        tradingName = "Atlantic & Kloof",
        registrationNumber = "2018/142033/07",
        vatNumber = "4350162391",
        email = "accounts@atlantic-kloof.co.za",
        phone = "021 424 0180",
        addressLine1 = "42 Kloof Street",
        addressLine2 = "Gardens",
        city = "Cape Town",
        province = "Western Cape",
        postalCode = "8001",
        country = "South Africa",
        bankName = "Standard Bank",
        accountName = "Atlantic & Kloof Bookkeeping",
        accountNumber = "076543210",
        branchCode = "036009",
        currencyCode = "ZAR",
        defaultVatRate = 15.0,
        invoicePrefix = "INV",
        nextInvoiceNumber = 5,
        defaultTemplateId = TEMPLATE_MODERN
    )

    fun templates(): List<InvoiceTemplate> = listOf(
        InvoiceTemplate(
            id = TEMPLATE_MODERN,
            businessId = BUSINESS_ID,
            name = "Modern Teal",
            layout = TemplateLayout.MODERN,
            primaryColor = 0xFF0B3A4AL,
            accentColor = 0xFF1F8A70L,
            footerText = "Bank: Standard Bank · Acc 076543210 · Branch 036009 · Thank you for your business."
        ),
        InvoiceTemplate(
            id = TEMPLATE_CLASSIC,
            businessId = BUSINESS_ID,
            name = "Classic Navy",
            layout = TemplateLayout.CLASSIC,
            primaryColor = 0xFF12355BL,
            accentColor = 0xFFC9A227L,
            footerText = "Atlantic & Kloof Bookkeeping · Cape Town · VAT 4350162391"
        ),
        InvoiceTemplate(
            id = TEMPLATE_COMPACT,
            businessId = BUSINESS_ID,
            name = "Compact Gold",
            layout = TemplateLayout.COMPACT,
            primaryColor = 0xFF3D2B1FL,
            accentColor = 0xFFC9A227L,
            footerText = "Payment due within 14 days. Interest may be charged on overdue accounts."
        )
    )

    fun customers(): List<Customer> = listOf(
        Customer(
            id = CUST_MARINE,
            businessId = BUSINESS_ID,
            name = "Table Bay Marine Supplies cc",
            email = "ops@tablebaymarine.co.za",
            phone = "021 510 2244",
            address = "14 Marine Drive, Paarden Eiland, Cape Town, 7405",
            vatNumber = "4011223344"
        ),
        Customer(
            id = CUST_GUEST,
            businessId = BUSINESS_ID,
            name = "Camps Bay Guest House",
            email = "stay@campsbaygh.co.za",
            phone = "021 438 1190",
            address = "8 The Drive, Camps Bay, Cape Town, 8005"
        ),
        Customer(
            id = CUST_HARVEST,
            businessId = BUSINESS_ID,
            name = "Stellenbosch Harvest Logistics (Pty) Ltd",
            email = "accounts@stharvest.co.za",
            phone = "021 883 4410",
            address = "22 Helshoogte Road, Stellenbosch, 7600",
            vatNumber = "4229988110"
        ),
        Customer(
            id = CUST_MEDICAL,
            businessId = BUSINESS_ID,
            name = "Green Point Medical Rooms",
            email = "practice@gpmedical.co.za",
            phone = "021 439 6700",
            address = "101 Main Road, Green Point, Cape Town, 8005"
        )
    )

    fun invoicesAndLines(): Pair<List<Invoice>, List<InvoiceLine>> {
        val day = 24L * 60 * 60 * 1000
        val now = System.currentTimeMillis()
        val inv1 = Invoice(
            id = "inv-001",
            businessId = BUSINESS_ID,
            customerId = CUST_MARINE,
            number = "INV-0001",
            issuedOn = now - 40 * day,
            dueOn = now - 26 * day,
            status = InvoiceStatus.PAID,
            notes = "Monthly retainers for June.",
            vatRate = 15.0,
            templateId = TEMPLATE_MODERN
        )
        val inv2 = Invoice(
            id = "inv-002",
            businessId = BUSINESS_ID,
            customerId = CUST_GUEST,
            number = "INV-0002",
            issuedOn = now - 12 * day,
            dueOn = now + 2 * day,
            status = InvoiceStatus.SENT,
            notes = "Includes payroll for 6 staff.",
            vatRate = 15.0,
            discountType = DiscountType.PERCENT,
            discountValue = 5.0,
            templateId = TEMPLATE_CLASSIC
        )
        val inv3 = Invoice(
            id = "inv-003",
            businessId = BUSINESS_ID,
            customerId = CUST_HARVEST,
            number = "INV-0003",
            issuedOn = now - 2 * day,
            dueOn = now + 12 * day,
            status = InvoiceStatus.DRAFT,
            notes = "Draft pending harvest-season hours confirmation.",
            vatRate = 15.0,
            templateId = TEMPLATE_MODERN
        )
        val inv4 = Invoice(
            id = "inv-004",
            businessId = BUSINESS_ID,
            customerId = CUST_MEDICAL,
            number = "INV-0004",
            issuedOn = now - 30 * day,
            dueOn = now - 10 * day,
            status = InvoiceStatus.SENT,
            notes = "VAT201 submission Q1.",
            vatRate = 15.0,
            templateId = TEMPLATE_COMPACT
        )
        val lines = listOf(
            InvoiceLine("l1", inv1.id, "Monthly bookkeeping retainer", 1.0, 4500.0, 0),
            InvoiceLine("l2", inv1.id, "Supplier statement reconciliation", 3.0, 650.0, 1),
            InvoiceLine("l3", inv2.id, "Payroll processing (6 employees)", 1.0, 2800.0, 0),
            InvoiceLine("l4", inv2.id, "UIF / PAYE EMP201 submission", 1.0, 950.0, 1),
            InvoiceLine("l5", inv3.id, "Catch-up bookkeeping (Feb–Apr)", 12.0, 420.0, 0),
            InvoiceLine("l6", inv3.id, "Inventory worksheet review", 1.0, 1500.0, 1),
            InvoiceLine("l7", inv4.id, "VAT201 preparation and eFiling", 1.0, 1850.0, 0),
            InvoiceLine("l8", inv4.id, "Medical aid third-party recs", 4.0, 380.0, 1)
        )
        return listOf(inv1, inv2, inv3, inv4) to lines
    }

    fun receipts(): List<Receipt> {
        val day = 24L * 60 * 60 * 1000
        val now = System.currentTimeMillis()
        return listOf(
            Receipt(
                id = "rcp-001",
                businessId = BUSINESS_ID,
                customerId = CUST_MARINE,
                invoiceId = "inv-001",
                number = "RCP-0001",
                receivedOn = now - 20 * day,
                amount = 7417.50,
                method = "EFT",
                notes = "Paid in full — Standard Bank proof attached in customer folder."
            )
        )
    }

    fun notes(): List<CustomerNote> = listOf(
        CustomerNote(
            id = "note-001",
            businessId = BUSINESS_ID,
            customerId = CUST_HARVEST,
            title = "Seasonal hours",
            body = "Harvest peak is Feb–Apr. Confirm overtime before issuing INV-0003 as final."
        ),
        CustomerNote(
            id = "note-002",
            businessId = BUSINESS_ID,
            customerId = CUST_GUEST,
            title = "WhatsApp preference",
            body = "Owner prefers invoices on WhatsApp (+27 82 555 0199) rather than email."
        )
    )
}
