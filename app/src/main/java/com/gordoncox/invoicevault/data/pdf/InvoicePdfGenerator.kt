package com.gordoncox.invoicevault.data.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.gordoncox.invoicevault.data.entity.BusinessEntity
import com.gordoncox.invoicevault.data.entity.CustomerEntity
import com.gordoncox.invoicevault.data.entity.InvoiceImageEntity
import com.gordoncox.invoicevault.data.entity.InvoiceLineItemEntity
import com.gordoncox.invoicevault.data.entity.InvoiceTemplateEntity
import com.gordoncox.invoicevault.data.entity.InvoiceWithDetails
import com.gordoncox.invoicevault.data.entity.TemplateLayout
import com.gordoncox.invoicevault.data.files.LocalStorage
import com.gordoncox.invoicevault.util.Money
import com.gordoncox.invoicevault.util.Za
import java.io.File
import java.io.FileOutputStream

class InvoicePdfGenerator(
    private val context: Context,
    private val storage: LocalStorage,
) {
    fun generate(
        invoice: InvoiceWithDetails,
        business: BusinessEntity,
        customer: CustomerEntity,
        template: InvoiceTemplateEntity?,
        dest: File,
    ): File {
        dest.parentFile?.mkdirs()
        val pageWidth = 595
        val pageHeight = 842
        val doc = PdfDocument()
        var pageNumber = 1
        var page = doc.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
        var canvas = page.canvas

        val primary = Color.parseColor(toHex(template?.primaryColor ?: 0xFF0F6E56))
        val accent = Color.parseColor(toHex(template?.accentColor ?: 0xFFC9A227))
        val layout = template?.layout ?: TemplateLayout.CLASSIC.name

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = primary
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textSize = if (layout == TemplateLayout.COMPACT.name) 18f else 22f
        }
        val heading = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = primary
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textSize = 11f
        }
        val body = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1F2933")
            textSize = 9.5f
        }
        val muted = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#52606D")
            textSize = 8.5f
        }
        val white = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 9f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = primary }
        val band = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = accent }

        var y = 36f
        canvas.drawRect(RectF(0f, 0f, pageWidth.toFloat(), 8f), fill)
        canvas.drawRect(RectF(0f, 8f, pageWidth.toFloat(), 12f), band)

        val logoPath = template?.logoPath ?: business.logoPath
        if (!logoPath.isNullOrBlank()) {
            decode(logoPath)?.let { bmp ->
                val h = 48
                val w = (bmp.width.toFloat() / bmp.height * h).toInt().coerceIn(40, 120)
                canvas.drawBitmap(bmp, null, Rect(36, y.toInt(), 36 + w, y.toInt() + h), null)
            }
        }

        val headerImage = template?.headerImagePath
        if (!headerImage.isNullOrBlank() && layout == TemplateLayout.MODERN.name) {
            decode(headerImage)?.let { bmp ->
                canvas.drawBitmap(bmp, null, Rect(pageWidth - 150, 20, pageWidth - 36, 72), null)
            }
        }

        canvas.drawText(business.name, 36f, y + 64f, titlePaint)
        y += 78f
        listOfNotNull(
            business.tradingName?.takeIf { it.isNotBlank() && it != business.name }?.let { "t/a $it" },
            business.addressLine1,
            listOfNotNull(business.addressLine2, "${business.city}, ${business.province} ${business.postalCode}").joinToString(" "),
            business.country,
            "Tel ${business.phone}  ·  ${business.email}",
            business.vatNumber?.let { "VAT $it" },
            business.registrationNumber?.let { "Reg $it" },
        ).forEach {
            canvas.drawText(it, 36f, y, muted)
            y += 12f
        }

        canvas.drawText("TAX INVOICE", pageWidth - 36f - white.measureText("TAX INVOICE") - 16f, 48f, titlePaint)
        canvas.drawText(invoice.invoice.number, pageWidth - 36f - body.measureText(invoice.invoice.number), 66f, body)
        canvas.drawText("Status: ${invoice.invoice.status}", pageWidth - 36f - muted.measureText("Status: ${invoice.invoice.status}"), 80f, muted)

        y += 8f
        canvas.drawRect(RectF(36f, y, pageWidth - 36f, y + 22f), fill)
        canvas.drawText("Bill to", 44f, y + 15f, white)
        y += 36f
        canvas.drawText(customer.name, 36f, y, heading)
        y += 14f
        listOfNotNull(
            customer.contactName?.let { "Attn: $it" },
            customer.addressLine1,
            listOfNotNull(customer.city, customer.province, customer.postalCode).joinToString(", ").ifBlank { null },
            customer.email,
            customer.phone,
            customer.vatNumber?.let { "VAT $it" },
        ).forEach {
            canvas.drawText(it, 36f, y, body)
            y += 12f
        }

        y += 8f
        canvas.drawText("Issue date  ${Za.date(invoice.invoice.issueDate)}", 36f, y, body)
        canvas.drawText("Due date  ${Za.date(invoice.invoice.dueDate)}", 280f, y, body)
        canvas.drawText(invoice.invoice.currency, 480f, y, heading)
        y += 18f

        val cols = floatArrayOf(36f, 250f, 330f, 410f, 559f)
        canvas.drawRect(RectF(36f, y, pageWidth - 36f, y + 18f), fill)
        canvas.drawText("Description", cols[0] + 6f, y + 13f, white)
        canvas.drawText("Qty", cols[1] + 6f, y + 13f, white)
        canvas.drawText("Unit", cols[2] + 6f, y + 13f, white)
        canvas.drawText("Amount", cols[3] + 6f, y + 13f, white)
        y += 22f

        fun newPage() {
            doc.finishPage(page)
            pageNumber += 1
            page = doc.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
            canvas = page.canvas
            y = 48f
        }

        invoice.items.sortedBy { it.position }.forEachIndexed { index, item ->
            if (y > 720f) newPage()
            if (index % 2 == 1) {
                val zebra = Paint().apply { color = Color.parseColor("#F4F7F6") }
                canvas.drawRect(RectF(36f, y - 10f, pageWidth - 36f, y + 8f), zebra)
            }
            val line = Money.lineTotal(item.quantity, item.unitPrice)
            canvas.drawText(item.description.take(42), cols[0] + 6f, y, body)
            canvas.drawText(trimNum(item.quantity), cols[1] + 6f, y, body)
            canvas.drawText(Za.money(item.unitPrice, invoice.invoice.currency), cols[2] + 6f, y, body)
            canvas.drawText(Za.money(line, invoice.invoice.currency), cols[3] + 6f, y, body)
            y += 16f
        }

        y += 10f
        val totals = Money.totals(
            invoice.items.map { Money.lineTotal(it.quantity, it.unitPrice) },
            invoice.invoice.discountAmount,
            invoice.invoice.discountPercent,
            invoice.invoice.vatPercent,
        )
        fun totalRow(label: String, value: String, bold: Boolean = false) {
            val p = if (bold) heading else body
            canvas.drawText(label, 360f, y, p)
            canvas.drawText(value, 480f, y, p)
            y += 14f
        }
        totalRow("Subtotal", Za.money(totals.subtotal, invoice.invoice.currency))
        if (totals.discount > 0) totalRow("Discount", "- ${Za.money(totals.discount, invoice.invoice.currency)}")
        totalRow("VAT ${trimNum(invoice.invoice.vatPercent)}%", Za.money(totals.vat, invoice.invoice.currency))
        canvas.drawRect(RectF(350f, y - 4f, pageWidth - 36f, y + 16f), fill)
        canvas.drawText("Total", 360f, y + 10f, white)
        canvas.drawText(Za.money(totals.total, invoice.invoice.currency), 460f, y + 10f, white)
        y += 32f

        invoice.invoice.notes?.takeIf { it.isNotBlank() }?.let {
            canvas.drawText("Notes", 36f, y, heading)
            y += 14f
            wrap(it, body, 360f).forEach { line ->
                canvas.drawText(line, 36f, y, body)
                y += 12f
            }
            y += 8f
        }
        invoice.invoice.terms?.takeIf { it.isNotBlank() }?.let {
            canvas.drawText("Terms", 36f, y, heading)
            y += 14f
            wrap(it, body, 360f).forEach { line ->
                canvas.drawText(line, 36f, y, body)
                y += 12f
            }
            y += 8f
        }

        if (template?.showBankDetails != false) {
            canvas.drawText("Bank details", 36f, y, heading)
            y += 14f
            listOfNotNull(
                business.bankName,
                business.bankAccountName?.let { "Account name: $it" },
                business.bankAccountNumber?.let { "Account no: $it" },
                business.bankBranchCode?.let { "Branch: $it" },
            ).forEach {
                canvas.drawText(it, 36f, y, body)
                y += 12f
            }
            y += 8f
        }

        val sig = invoice.invoice.signaturePath
        if (!sig.isNullOrBlank()) {
            if (y > 680f) newPage()
            canvas.drawText("Authorised signature", 36f, y, heading)
            y += 6f
            decode(sig)?.let { bmp ->
                canvas.drawBitmap(bmp, null, Rect(36, y.toInt(), 200, y.toInt() + 54), null)
            }
            y += 64f
        }

        fun drawAttached(path: String, label: String) {
            if (y > 700f) newPage()
            canvas.drawText(label, 36f, y, heading)
            y += 6f
            decode(path)?.let { bmp ->
                val maxW = 240
                val maxH = 140
                val scale = minOf(maxW / bmp.width.toFloat(), maxH / bmp.height.toFloat(), 1f)
                val w = (bmp.width * scale).toInt()
                val h = (bmp.height * scale).toInt()
                canvas.drawBitmap(bmp, null, Rect(36, y.toInt(), 36 + w, y.toInt() + h), null)
                y += h + 12f
            }
        }
        template?.extraImagePath?.takeIf { it.isNotBlank() }?.let { drawAttached(it, "Template image") }
        invoice.images.sortedBy { it.sortOrder }.forEach { img ->
            drawAttached(img.path, "Attached image")
        }

        template?.footerText?.takeIf { it.isNotBlank() }?.let {
            canvas.drawText(it.take(90), 36f, pageHeight - 28f, muted)
        } ?: canvas.drawText(
            "Generated offline by InvoiceVault  ·  ${business.name}",
            36f,
            pageHeight - 28f,
            muted,
        )
        canvas.drawRect(RectF(0f, pageHeight - 8f, pageWidth.toFloat(), pageHeight.toFloat()), fill)

        doc.finishPage(page)
        FileOutputStream(dest).use { doc.writeTo(it) }
        doc.close()
        return dest
    }

    private fun decode(relativeOrAbsolute: String): Bitmap? {
        val file = File(relativeOrAbsolute).takeIf { it.exists() }
            ?: storage.resolve(context, relativeOrAbsolute)
        if (!file.exists()) return null
        return BitmapFactory.decodeFile(file.absolutePath)
    }

    private fun toHex(color: Long): String {
        val v = (color and 0xFFFFFFL).toInt()
        return String.format("#%06X", v)
    }

    private fun trimNum(value: Double): String =
        if (value % 1.0 == 0.0) value.toInt().toString() else Money.round(value).toString()

    private fun wrap(text: String, paint: Paint, maxWidth: Float): List<String> {
        val words = text.split(Regex("\\s+"))
        val lines = mutableListOf<String>()
        var current = ""
        for (w in words) {
            val trial = if (current.isEmpty()) w else "$current $w"
            if (paint.measureText(trial) > maxWidth && current.isNotEmpty()) {
                lines += current
                current = w
            } else {
                current = trial
            }
        }
        if (current.isNotEmpty()) lines += current
        return lines
    }
}
