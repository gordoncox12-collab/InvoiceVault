package za.co.invoicevault.pdf

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import za.co.invoicevault.data.Business
import za.co.invoicevault.data.Customer
import za.co.invoicevault.data.Invoice
import za.co.invoicevault.data.InvoiceLine
import za.co.invoicevault.data.InvoiceTemplate
import za.co.invoicevault.data.Receipt
import za.co.invoicevault.data.TemplateLayout
import za.co.invoicevault.data.breakdown
import za.co.invoicevault.data.formatDate
import za.co.invoicevault.data.formatMoney
import java.io.File

class InvoicePdfRenderer {

    fun renderInvoice(
        target: File,
        business: Business,
        customer: Customer,
        invoice: Invoice,
        lines: List<InvoiceLine>,
        template: InvoiceTemplate?
    ) {
        val pageWidth = 595
        val pageHeight = 842
        val doc = PdfDocument()
        val money = { v: Double -> formatMoney(v, invoice.currencyCode) }
        val totals = lines.breakdown(invoice)
        val tpl = template ?: InvoiceTemplate(businessId = business.id, name = "Default")
        val primary = color(tpl.primaryColor)
        val accent = color(tpl.accentColor)
        val headerH = when (tpl.layout) {
            TemplateLayout.COMPACT -> 110f
            TemplateLayout.CLASSIC -> 150f
            TemplateLayout.MODERN -> 170f
        }

        val perPage = if (tpl.layout == TemplateLayout.COMPACT) 22 else 16
        val pages = (lines.size.coerceAtLeast(1) + perPage - 1) / perPage

        for (pageIndex in 0 until pages) {
            val page = doc.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageIndex + 1).create())
            val c = page.canvas
            val title = paint(22f, Typeface.DEFAULT_BOLD, Color.WHITE)
            val h2 = paint(14f, Typeface.DEFAULT_BOLD, primary)
            val body = paint(10f, Typeface.DEFAULT, Color.DKGRAY)
            val small = paint(9f, Typeface.DEFAULT, Color.GRAY)
            val white = paint(10f, Typeface.DEFAULT, Color.WHITE)
            val moneyPaint = paint(10f, Typeface.DEFAULT, Color.BLACK)
            moneyPaint.textAlign = Paint.Align.RIGHT
            val headerPaint = Paint().apply { color = primary; isAntiAlias = true }

            if (tpl.layout == TemplateLayout.MODERN || tpl.layout == TemplateLayout.COMPACT) {
                c.drawRect(0f, 0f, pageWidth.toFloat(), headerH, headerPaint)
                drawLogo(c, tpl.logoPath.ifBlank { business.logoPath }, 28f, 24f, 64f)
                c.drawText(business.displayName, 100f, 48f, title)
                c.drawText(if (pageIndex == 0) "TAX INVOICE" else "TAX INVOICE (cont.)", 100f, 74f, white)
                c.drawText(invoice.number, pageWidth - 36f - white.measureText(invoice.number), 48f, white.apply { textAlign = Paint.Align.LEFT; textSize = 12f })
            } else {
                drawLogo(c, tpl.logoPath.ifBlank { business.logoPath }, 36f, 28f, 72f)
                c.drawText(business.displayName, 120f, 50f, h2.apply { textSize = 18f; color = primary })
                c.drawText("TAX INVOICE  ${invoice.number}", 120f, 74f, body)
                c.drawRect(36f, headerH - 8f, pageWidth - 36f, headerH - 4f, Paint().apply { color = accent })
            }

            var y = headerH + 24f
            if (pageIndex == 0) {
                y = drawBlock(c, 36f, y, "From", business.displayName, business.fullAddress, "VAT ${business.vatNumber}", "Tel ${business.phone}")
                y = drawBlock(c, 310f, headerH + 24f, "Bill to", customer.name, customer.address, customer.email, customer.phone)
                y = maxOf(y, headerH + 110f)
                c.drawText("Issued  ${formatDate(invoice.issuedOn)}", 36f, y, small)
                c.drawText("Due  ${formatDate(invoice.dueOn)}", 220f, y, small)
                c.drawText("Status  ${invoice.status.name}", 400f, y, small)
                y += 18f
            }

            val tableTop = y
            val colDesc = 36f
            val colQty = 340f
            val colPrice = 420f
            val colTotal = 555f
            val band = Paint().apply { color = accent }
            c.drawRect(36f, tableTop - 14f, pageWidth - 36f, tableTop + 8f, band)
            val head = paint(9f, Typeface.DEFAULT_BOLD, Color.WHITE)
            c.drawText("Description", colDesc + 6f, tableTop, head)
            c.drawText("Qty", colQty, tableTop, head)
            c.drawText("Unit", colPrice, tableTop, head)
            head.textAlign = Paint.Align.RIGHT
            c.drawText("Amount", colTotal, tableTop, head)

            y = tableTop + 22f
            val slice = lines.drop(pageIndex * perPage).take(perPage)
            slice.forEachIndexed { i, line ->
                if (i % 2 == 0) {
                    c.drawRect(36f, y - 12f, pageWidth - 36f, y + 10f, Paint().apply { color = 0x11000000 })
                }
                c.drawText(line.description.take(48), colDesc + 6f, y, body)
                c.drawText(trimQty(line.quantity), colQty, y, body)
                moneyPaint.color = Color.BLACK
                c.drawText(money(line.unitPrice), colPrice + 50f, y, moneyPaint)
                c.drawText(money(line.lineTotal), colTotal, y, moneyPaint)
                y += 18f
            }

            if (pageIndex == pages - 1) {
                y += 10f
                fun row(label: String, value: String, bold: Boolean = false) {
                    val p = paint(if (bold) 12f else 10f, if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT, if (bold) primary else Color.BLACK)
                    p.textAlign = Paint.Align.LEFT
                    val r = paint(if (bold) 12f else 10f, if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT, if (bold) primary else Color.BLACK)
                    r.textAlign = Paint.Align.RIGHT
                    c.drawText(label, 340f, y, p)
                    c.drawText(value, colTotal, y, r)
                    y += 16f
                }
                row("Subtotal", money(totals.subtotal))
                if (totals.discount > 0) row("Discount", "- ${money(totals.discount)}")
                if (tpl.showVat) row("VAT ${trimQty(invoice.vatRate)}%", money(totals.vat))
                c.drawRect(330f, y - 12f, pageWidth - 36f, y + 14f, Paint().apply { color = primary })
                val totalPaint = paint(12f, Typeface.DEFAULT_BOLD, Color.WHITE)
                totalPaint.textAlign = Paint.Align.LEFT
                c.drawText("Total due", 340f, y + 6f, totalPaint)
                totalPaint.textAlign = Paint.Align.RIGHT
                c.drawText(money(totals.total), colTotal, y + 6f, totalPaint)
                y += 36f

                if (invoice.notes.isNotBlank()) {
                    c.drawText("Notes", 36f, y, h2.apply { textSize = 12f; color = primary })
                    y += 16f
                    wrap(c, invoice.notes, 36f, y, 280f, body)
                }

                val sig = loadBitmap(invoice.signaturePath)
                if (sig != null) {
                    val dest = RectF(340f, y, 555f, y + 70f)
                    c.drawBitmap(sig, null, dest, Paint(Paint.FILTER_BITMAP_FLAG))
                    c.drawText("Authorised signature", 340f, y + 84f, small)
                }

                val footerY = pageHeight - 48f
                c.drawRect(0f, footerY - 16f, pageWidth.toFloat(), pageHeight.toFloat(), headerPaint)
                wrap(c, tpl.footerText.ifBlank { business.fullAddress }, 36f, footerY, pageWidth - 72f, white)
            } else {
                c.drawText("Continued on next page", 36f, pageHeight - 36f, small)
            }

            doc.finishPage(page)
        }

        target.parentFile?.mkdirs()
        target.outputStream().use { doc.writeTo(it) }
        doc.close()
    }

    fun renderReceipt(
        target: File,
        business: Business,
        customer: Customer,
        receipt: Receipt,
        invoiceNumber: String?
    ) {
        val doc = PdfDocument()
        val page = doc.startPage(PdfDocument.PageInfo.Builder(595, 842, 1).create())
        val c = page.canvas
        val primary = Paint().apply { color = 0xFF0B3A4A.toInt(); isAntiAlias = true }
        c.drawRect(0f, 0f, 595f, 120f, primary)
        val title = paint(22f, Typeface.DEFAULT_BOLD, Color.WHITE)
        c.drawText("RECEIPT", 36f, 56f, title)
        c.drawText(receipt.number, 36f, 84f, paint(12f, Typeface.DEFAULT, Color.WHITE))
        val body = paint(11f, Typeface.DEFAULT, Color.DKGRAY)
        var y = 160f
        c.drawText(business.displayName, 36f, y, paint(14f, Typeface.DEFAULT_BOLD, 0xFF0B3A4A.toInt()))
        y += 24f
        c.drawText("Received from: ${customer.name}", 36f, y, body); y += 20f
        c.drawText("Date: ${formatDate(receipt.receivedOn)}", 36f, y, body); y += 20f
        c.drawText("Amount: ${formatMoney(receipt.amount, business.currencyCode)}", 36f, y, paint(14f, Typeface.DEFAULT_BOLD, Color.BLACK)); y += 24f
        c.drawText("Method: ${receipt.method}", 36f, y, body); y += 20f
        if (!invoiceNumber.isNullOrBlank()) {
            c.drawText("Against invoice: $invoiceNumber", 36f, y, body); y += 20f
        }
        if (receipt.notes.isNotBlank()) {
            y += 8f
            wrap(c, receipt.notes, 36f, y, 500f, body)
        }
        val image = loadBitmap(receipt.imagePath)
        if (image != null) {
            c.drawBitmap(image, null, RectF(36f, 420f, 559f, 760f), Paint(Paint.FILTER_BITMAP_FLAG))
        }
        doc.finishPage(page)
        target.parentFile?.mkdirs()
        target.outputStream().use { doc.writeTo(it) }
        doc.close()
    }

    private fun color(value: Long): Int = (value or 0xFF000000L).toInt()

    private fun paint(size: Float, typeface: Typeface, color: Int) = Paint().apply {
        isAntiAlias = true
        textSize = size
        this.typeface = typeface
        this.color = color
    }

    private fun drawLogo(c: Canvas, path: String, x: Float, y: Float, size: Float) {
        val bmp = loadBitmap(path)
        if (bmp != null) {
            c.drawBitmap(bmp, null, RectF(x, y, x + size, y + size), Paint(Paint.FILTER_BITMAP_FLAG))
        } else {
            c.drawRoundRect(RectF(x, y, x + size, y + size), 8f, 8f, Paint().apply { color = 0x33FFFFFF })
        }
    }

    private fun drawBlock(
        c: Canvas,
        x: Float,
        yStart: Float,
        heading: String,
        vararg lines: String
    ): Float {
        var y = yStart
        c.drawText(heading.uppercase(), x, y, paint(9f, Typeface.DEFAULT_BOLD, Color.GRAY))
        y += 16f
        lines.filter { it.isNotBlank() }.forEach { line ->
            line.split("\n").forEach {
                c.drawText(it.take(42), x, y, paint(10f, Typeface.DEFAULT, Color.BLACK))
                y += 14f
            }
        }
        return y
    }

    private fun wrap(c: Canvas, text: String, x: Float, yStart: Float, width: Float, paint: Paint): Float {
        var y = yStart
        val words = text.split(" ")
        var line = ""
        for (word in words) {
            val trial = if (line.isEmpty()) word else "$line $word"
            if (paint.measureText(trial) > width) {
                c.drawText(line, x, y, paint)
                y += 14f
                line = word
            } else {
                line = trial
            }
        }
        if (line.isNotBlank()) {
            c.drawText(line, x, y, paint)
            y += 14f
        }
        return y
    }

    private fun loadBitmap(path: String): Bitmap? {
        if (path.isBlank()) return null
        val file = File(path)
        if (!file.exists()) return null
        return BitmapFactory.decodeFile(path)
    }

    private fun trimQty(value: Double): String =
        if (value % 1.0 == 0.0) value.toInt().toString() else "%.2f".format(value)
}
