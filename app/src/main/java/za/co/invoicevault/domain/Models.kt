package za.co.invoicevault.domain

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

enum class InvoiceStatus { DRAFT, SENT, PAID, OVERDUE }

enum class DiscountType { NONE, PERCENT, AMOUNT }

enum class TemplateStyle { CLASSIC, MODERN, COMPACT }

enum class FolderCategory { INVOICES, RECEIPTS, EXCEL, IMAGES, NOTES }

enum class ThemeMode { SYSTEM, LIGHT, DARK }

enum class AccentPalette {
    CAPE_GOLD,
    PROTEA_PINK,
    OCEAN_TEAL,
    FYNBOS_GREEN,
    MOUNTAIN_SLATE,
    SUNSET_ORANGE
}

data class LineAmount(
    val description: String,
    val quantity: BigDecimal,
    val unitPrice: BigDecimal,
    val unit: String = "ea"
) {
    val lineTotal: BigDecimal
        get() = quantity.multiply(unitPrice).setScale(2, RoundingMode.HALF_UP)
}

data class InvoiceTotals(
    val subtotal: BigDecimal,
    val discount: BigDecimal,
    val taxable: BigDecimal,
    val vat: BigDecimal,
    val total: BigDecimal
)

object MoneyCalculator {
    val defaultVatRate: BigDecimal = BigDecimal("0.15")

    fun totals(
        lines: List<LineAmount>,
        discountType: DiscountType,
        discountValue: BigDecimal,
        vatRate: BigDecimal = defaultVatRate
    ): InvoiceTotals {
        val subtotal = lines.fold(BigDecimal.ZERO) { acc, line -> acc + line.lineTotal }
            .setScale(2, RoundingMode.HALF_UP)
        val discount = when (discountType) {
            DiscountType.NONE -> BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
            DiscountType.PERCENT -> subtotal
                .multiply(discountValue)
                .divide(BigDecimal("100"), 2, RoundingMode.HALF_UP)
            DiscountType.AMOUNT -> discountValue.setScale(2, RoundingMode.HALF_UP).min(subtotal)
        }
        val taxable = (subtotal - discount).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP)
        val vat = taxable.multiply(vatRate).setScale(2, RoundingMode.HALF_UP)
        val total = (taxable + vat).setScale(2, RoundingMode.HALF_UP)
        return InvoiceTotals(subtotal, discount, taxable, vat, total)
    }
}

object MoneyFormat {
    private val za: Locale = Locale.forLanguageTag("en-ZA")

    fun currency(amount: BigDecimal, currencyCode: String = "ZAR"): String {
        val format = NumberFormat.getCurrencyInstance(za)
        runCatching { format.currency = Currency.getInstance(currencyCode) }
        format.maximumFractionDigits = 2
        format.minimumFractionDigits = 2
        return format.format(amount)
    }

    fun plain(amount: BigDecimal): String = amount.setScale(2, RoundingMode.HALF_UP).toPlainString()
}

object InvoiceNumbering {
    fun format(prefix: String, year: Int, sequence: Int): String {
        val cleanPrefix = prefix.trim().ifBlank { "INV" }.uppercase()
        return "$cleanPrefix-$year-${sequence.toString().padStart(4, '0')}"
    }

    fun nextSequence(existingNumbers: Collection<String>, prefix: String, year: Int): Int {
        val needle = "${prefix.trim().uppercase()}-$year-"
        val max = existingNumbers.mapNotNull { number ->
            if (!number.startsWith(needle)) return@mapNotNull null
            number.removePrefix(needle).toIntOrNull()
        }.maxOrNull() ?: 0
        return max + 1
    }
}
