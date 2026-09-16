package za.co.invoicevault.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class MoneyCalculatorTest {
    @Test
    fun vatFifteenPercentOnExclusiveLines() {
        val lines = listOf(
            LineAmount("Hike", BigDecimal("2"), BigDecimal("1000.00")),
            LineAmount("Permit", BigDecimal("1"), BigDecimal("150.00"))
        )
        val totals = MoneyCalculator.totals(lines, DiscountType.NONE, BigDecimal.ZERO)
        assertEquals(BigDecimal("2150.00"), totals.subtotal)
        assertEquals(BigDecimal("322.50"), totals.vat)
        assertEquals(BigDecimal("2472.50"), totals.total)
    }

    @Test
    fun percentDiscountThenVat() {
        val lines = listOf(LineAmount("Catering", BigDecimal("10"), BigDecimal("100.00")))
        val totals = MoneyCalculator.totals(lines, DiscountType.PERCENT, BigDecimal("10"))
        assertEquals(BigDecimal("1000.00"), totals.subtotal)
        assertEquals(BigDecimal("100.00"), totals.discount)
        assertEquals(BigDecimal("900.00"), totals.taxable)
        assertEquals(BigDecimal("135.00"), totals.vat)
        assertEquals(BigDecimal("1035.00"), totals.total)
    }

    @Test
    fun amountDiscountCannotExceedSubtotal() {
        val lines = listOf(LineAmount("Item", BigDecimal.ONE, BigDecimal("50.00")))
        val totals = MoneyCalculator.totals(lines, DiscountType.AMOUNT, BigDecimal("80.00"))
        assertEquals(BigDecimal.ZERO.setScale(2), totals.taxable)
        assertEquals(BigDecimal.ZERO.setScale(2), totals.total)
    }
}

class InvoiceNumberingTest {
    @Test
    fun formatsPrefixedYearSequence() {
        assertEquals("TMW-2026-0007", InvoiceNumbering.format("tmw", 2026, 7))
    }

    @Test
    fun nextSequenceSkipsExisting() {
        val next = InvoiceNumbering.nextSequence(
            listOf("TMW-2026-0001", "TMW-2026-0004", "KGC-2026-0009"),
            "TMW",
            2026
        )
        assertEquals(5, next)
    }
}
