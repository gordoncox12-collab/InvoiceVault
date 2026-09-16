package za.co.invoicevault.data.excel

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SpreadsheetServiceTest {
    @Test
    fun csvRoundTripAndSemicolonDetection() {
        val text = "name;email;city\nObservatory Coffee Lab;sipho@obs.co.za;Observatory\n"
        val table = CsvCodec.parse(text)
        assertEquals(listOf("name", "email", "city"), table.headers)
        assertEquals("Observatory Coffee Lab", table.rows[0][0])
        val written = CsvCodec.write(table.headers, table.rows)
        assertTrue(written.contains("Observatory Coffee Lab"))
    }

    @Test
    fun quotedCommasSurvive() {
        val table = CsvCodec.parse("name,address\n\"V&A Waterfront, Dock Road\",Cape Town\n")
        assertEquals("V&A Waterfront, Dock Road", table.rows[0][0])
    }

    @Test
    fun columnMapperSuggestsSaInvoiceHeaders() {
        val mapping = ColumnMapper.suggest(listOf("Customer name", "E-mail", "VAT no", "Qty", "Unit price"))
        assertEquals("name", mapping["Customer name"])
        assertEquals("email", mapping["E-mail"])
        assertEquals("vatNumber", mapping["VAT no"])
        assertEquals("quantity", mapping["Qty"])
        assertEquals("unitPrice", mapping["Unit price"])
    }

    @Test
    fun xlsxWriteAndRead() {
        val service = SpreadsheetService()
        val headers = listOf("name", "city")
        val rows = listOf(listOf("Sea Point Guesthouse", "Cape Town"))
        val bytes = service.writeXlsxBytes("Customers", headers, rows)
        val table = service.read("customers.xlsx", bytes.inputStream())
        assertEquals(headers, table.headers)
        assertEquals(rows, table.rows)
    }
}
