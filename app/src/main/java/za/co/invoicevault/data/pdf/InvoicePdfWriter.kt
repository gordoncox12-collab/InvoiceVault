package za.co.invoicevault.data.pdf

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import za.co.invoicevault.data.db.BusinessEntity
import za.co.invoicevault.data.db.CustomerEntity
import za.co.invoicevault.data.db.InvoiceLineEntity
import za.co.invoicevault.data.db.InvoiceWithLines
import za.co.invoicevault.data.db.TemplateEntity
import za.co.invoicevault.domain.InvoiceStatus
import za.co.invoicevault.domain.LineAmount
import za.co.invoicevault.domain.MoneyCalculator
import za.co.invoicevault.domain.MoneyFormat
import za.co.invoicevault.domain.TemplateStyle
import java.io.File
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class InvoicePdfWriter {

    private val pageWidth = 595
    private val pageHeight = 842
    private val margin = 36f
    private val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.forLanguageTag("en-ZA"))

    fun write(
        destination: File,
        business: BusinessEntity,
        customer: CustomerEntity,
        invoice: InvoiceWithLines,
        template: TemplateEntity?
    ): File {
        val doc = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val page = doc.startPage(pageInfo)
        val canvas = page.canvas
        val style = template?.style ?: TemplateStyle.MODERN
        val primary = (template?.primaryColor ?: 0xFF123524L).toInt()
        val accent = (template?.accentColor ?: 0xFFC4A35AL).toInt()

        var y = when (style) {
            TemplateStyle.MODERN -> drawModernHeader(canvas, business, invoice.invoice.number, invoice.invoice.status, primary, accent, template)
            TemplateStyle.CLASSIC -> drawClassicHeader(canvas, business, invoice.invoice.number, primary, template)
            TemplateStyle.COMPACT -> drawCompactHeader(canvas, business, invoice.invoice.number, primary, accent, template)
        }

        y = drawParties(canvas, business, customer, invoice, y)
        y = drawLines(canvas, invoice, business.defaultCurrency, y, accent, style)
        y = drawTotals(canvas, invoice, business.defaultCurrency, y, primary)
        y = drawNotesAndImages(canvas, invoice, template, y)
        drawFooter(canvas, business, template, invoice.invoice.footer)

        doc.finishPage(page)
        destination.parentFile?.mkdirs()
        destination.outputStream().use { doc.writeTo(it) }
        doc.close()
        return destination
    }

    private fun drawModernHeader(
        canvas: Canvas,
        business: BusinessEntity,
        number: String,
        status: InvoiceStatus,
        primary: Int,
        accent: Int,
        template: TemplateEntity?
    ): Float {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = primary }
        canvas.drawRect(0f, 0f, pageWidth.toFloat(), 118f, paint)
        paint.color = accent
        canvas.drawRect(0f, 118f, pageWidth.toFloat(), 124f, paint)

        val title = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 22f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        canvas.drawText(business.displayName, margin, 42f, title)
        title.textSize = 12f
        title.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        canvas.drawText("TAX INVOICE", margin, 64f, title)
        canvas.drawText(number, margin, 84f, title)
        canvas.drawText(status.name, pageWidth - margin - title.measureText(status.name), 84f, title)
        drawLogo(canvas, template?.logoPath ?: business.logoPath, pageWidth - margin - 72f, 28f, 72f, 72f)
        return 148f
    }

    private fun drawClassicHeader(
        canvas: Canvas,
        business: BusinessEntity,
        number: String,
        primary: Int,
        template: TemplateEntity?
    ): Float {
        val title = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = primary
            textSize = 24f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
        }
        canvas.drawText("TAX INVOICE", margin, 48f, title)
        title.textSize = 14f
        canvas.drawText(business.displayName, margin, 70f, title)
        val line = Paint().apply { color = primary; strokeWidth = 1.5f }
        canvas.drawLine(margin, 80f, pageWidth - margin, 80f, line)
        canvas.drawLine(margin, 84f, pageWidth - margin, 84f, line)
        title.typeface = Typeface.create(Typeface.SERIF, Typeface.NORMAL)
        title.textSize = 12f
        canvas.drawText(number, margin, 102f, title)
        drawLogo(canvas, template?.logoPath ?: business.logoPath, pageWidth - margin - 72f, 28f, 72f, 64f)
        return 124f
    }

    private fun drawCompactHeader(
        canvas: Canvas,
        business: BusinessEntity,
        number: String,
        primary: Int,
        accent: Int,
        template: TemplateEntity?
    ): Float {
        val bar = Paint().apply { color = accent }
        canvas.drawRect(margin, 28f, margin + 8f, 88f, bar)
        val title = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = primary
            textSize = 16f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        canvas.drawText("${business.displayName}  ·  $number", margin + 16f, 50f, title)
        title.textSize = 10f
        title.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        canvas.drawText("Tax invoice · 15% VAT", margin + 16f, 68f, title)
        drawLogo(canvas, template?.logoPath ?: business.logoPath, pageWidth - margin - 56f, 28f, 56f, 56f)
        return 108f
    }

    private fun drawParties(
        canvas: Canvas,
        business: BusinessEntity,
        customer: CustomerEntity,
        invoice: InvoiceWithLines,
        startY: Float
    ): Float {
        val label = textPaint(10f, Color.GRAY, true)
        val body = textPaint(11f, Color.BLACK, false)
        var y = startY
        canvas.drawText("FROM", margin, y, label)
        canvas.drawText("BILL TO", 300f, y, label)
        y += 16f
        val from = listOf(
            business.name,
            business.addressLine1,
            listOf(business.city, business.province, business.postalCode).filter { it.isNotBlank() }.joinToString(", "),
            business.email,
            "VAT ${business.vatNumber}".takeIf { business.vatNumber.isNotBlank() }.orEmpty()
        ).filter { it.isNotBlank() }
        val to = listOf(
            customer.name,
            customer.contactPerson,
            customer.addressLine1,
            listOf(customer.city, customer.province, customer.postalCode).filter { it.isNotBlank() }.joinToString(", "),
            customer.email,
            "VAT ${customer.vatNumber}".takeIf { customer.vatNumber.isNotBlank() }.orEmpty()
        ).filter { it.isNotBlank() }
        from.forEachIndexed { i, line -> canvas.drawText(line, margin, y + i * 14f, body) }
        to.forEachIndexed { i, line -> canvas.drawText(line, 300f, y + i * 14f, body) }
        y += 14f * maxOf(from.size, to.size) + 18f
        canvas.drawText("Issue ${dateFormat.format(Date(invoice.invoice.issueDate))}", margin, y, body)
        canvas.drawText("Due ${dateFormat.format(Date(invoice.invoice.dueDate))}", 300f, y, body)
        return y + 22f
    }

    private fun drawLines(
        canvas: Canvas,
        invoice: InvoiceWithLines,
        currency: String,
        startY: Float,
        accent: Int,
        style: TemplateStyle
    ): Float {
        val header = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = accent }
        val headerH = if (style == TemplateStyle.COMPACT) 18f else 22f
        canvas.drawRoundRect(RectF(margin, startY, pageWidth - margin, startY + headerH), 4f, 4f, header)
        val headText = textPaint(10f, Color.WHITE, true)
        val yHead = startY + headerH - 6f
        canvas.drawText("Description", margin + 8f, yHead, headText)
        canvas.drawText("Qty", 340f, yHead, headText)
        canvas.drawText("Unit", 390f, yHead, headText)
        canvas.drawText("Total", 480f, yHead, headText)
        var y = startY + headerH + 14f
        val row = textPaint(10f, Color.DKGRAY, false)
        invoice.lines.sortedBy { it.position }.forEach { line ->
            val amount = LineAmount(line.description, line.quantity.toBigDecimal(), line.unitPrice.toBigDecimal(), line.unit)
            canvas.drawText(line.description.take(42), margin + 8f, y, row)
            canvas.drawText(trimNum(line.quantity), 340f, y, row)
            canvas.drawText(MoneyFormat.currency(amount.unitPrice, currency), 390f, y, row)
            canvas.drawText(MoneyFormat.currency(amount.lineTotal, currency), 480f, y, row)
            y += if (style == TemplateStyle.COMPACT) 14f else 16f
        }
        return y + 8f
    }

    private fun drawTotals(
        canvas: Canvas,
        invoice: InvoiceWithLines,
        currency: String,
        startY: Float,
        primary: Int
    ): Float {
        val lines = invoice.lines.map {
            LineAmount(it.description, it.quantity.toBigDecimal(), it.unitPrice.toBigDecimal(), it.unit)
        }
        val totals = MoneyCalculator.totals(
            lines,
            invoice.invoice.discountType,
            BigDecimal.valueOf(invoice.invoice.discountValue),
            BigDecimal.valueOf(invoice.invoice.vatRate)
        )
        val label = textPaint(11f, Color.DKGRAY, false)
        val value = textPaint(11f, Color.BLACK, true)
        var y = startY
        fun row(name: String, amount: java.math.BigDecimal, emphasize: Boolean = false) {
            canvas.drawText(name, 360f, y, label)
            val paint = if (emphasize) textPaint(13f, primary, true) else value
            val text = MoneyFormat.currency(amount, currency)
            canvas.drawText(text, pageWidth - margin - paint.measureText(text), y, paint)
            y += 16f
        }
        row("Subtotal (ex VAT)", totals.subtotal)
        if (totals.discount > BigDecimal.ZERO) row("Discount", totals.discount.negate())
        row("VAT ${trimNum(invoice.invoice.vatRate * 100)}%", totals.vat)
        row("Total", totals.total, emphasize = true)
        return y + 10f
    }

    private fun drawNotesAndImages(
        canvas: Canvas,
        invoice: InvoiceWithLines,
        template: TemplateEntity?,
        startY: Float
    ): Float {
        var y = startY
        val body = textPaint(10f, Color.DKGRAY, false)
        if (invoice.invoice.notes.isNotBlank()) {
            canvas.drawText("Notes", margin, y, textPaint(11f, Color.BLACK, true))
            y += 14f
            invoice.invoice.notes.split("\n").take(6).forEach {
                canvas.drawText(it.take(90), margin, y, body)
                y += 13f
            }
        }
        y += 8f
        val imagePath = invoice.invoice.imagePath ?: template?.bannerImagePath
        if (!imagePath.isNullOrBlank()) {
            drawImage(canvas, imagePath, margin, y, 180f, 90f)
        }
        val sig = invoice.invoice.signaturePath
        if (!sig.isNullOrBlank()) {
            canvas.drawText("Authorised signature", 340f, y, textPaint(10f, Color.GRAY, false))
            drawImage(canvas, sig, 340f, y + 6f, 180f, 70f)
            y += 80f
        } else if (!imagePath.isNullOrBlank()) {
            y += 96f
        }
        return y
    }

    private fun drawFooter(canvas: Canvas, business: BusinessEntity, template: TemplateEntity?, footer: String) {
        val paint = textPaint(9f, Color.GRAY, false)
        val bank = listOfNotNull(
            business.bankName.takeIf { it.isNotBlank() }?.let { "Bank: $it" },
            business.accountName.takeIf { it.isNotBlank() }?.let { "Acc name: $it" },
            business.accountNumber.takeIf { it.isNotBlank() }?.let { "Acc no: $it" },
            business.branchCode.takeIf { it.isNotBlank() }?.let { "Branch: $it" }
        ).joinToString(" · ")
        val extra = template?.footerText.orEmpty()
        canvas.drawText(footer.take(90), margin, pageHeight - 42f, paint)
        if (bank.isNotBlank()) canvas.drawText(bank.take(110), margin, pageHeight - 28f, paint)
        if (extra.isNotBlank()) canvas.drawText(extra.take(110), margin, pageHeight - 16f, paint)
    }

    private fun drawLogo(canvas: Canvas, path: String?, x: Float, y: Float, w: Float, h: Float) {
        if (!path.isNullOrBlank()) drawImage(canvas, path, x, y, w, h)
    }

    private fun drawImage(canvas: Canvas, path: String, x: Float, y: Float, w: Float, h: Float) {
        val file = File(path)
        if (!file.exists()) return
        val bmp = BitmapFactory.decodeFile(path) ?: return
        val dest = RectF(x, y, x + w, y + h)
        val src = Rect(0, 0, bmp.width, bmp.height)
        canvas.drawBitmap(bmp, src, dest, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
        if (!bmp.isRecycled) bmp.recycle()
    }

    private fun textPaint(size: Float, color: Int, bold: Boolean): Paint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            textSize = size
            typeface = Typeface.create(Typeface.SANS_SERIF, if (bold) Typeface.BOLD else Typeface.NORMAL)
        }

    private fun trimNum(value: Double): String =
        if (value % 1.0 == 0.0) value.toInt().toString() else value.toString()
}
