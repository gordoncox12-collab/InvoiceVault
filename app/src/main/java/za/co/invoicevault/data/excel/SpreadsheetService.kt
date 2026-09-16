package za.co.invoicevault.data.excel

import org.apache.poi.hssf.usermodel.HSSFWorkbook
import org.dhatim.fastexcel.Workbook
import org.dhatim.fastexcel.reader.ReadableWorkbook
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.Locale

data class SheetTable(
    val sheetName: String,
    val headers: List<String>,
    val rows: List<List<String>>
)

object CsvCodec {
    fun parse(text: String): SheetTable {
        val lines = text.split("\n").map { it.trimEnd('\r') }.filter { it.isNotBlank() }
        if (lines.isEmpty()) return SheetTable("CSV", emptyList(), emptyList())
        val delimiter = detectDelimiter(lines.first())
        val parsed = lines.map { parseLine(it, delimiter) }
        val width = parsed.maxOf { it.size }
        val normalized = parsed.map { row -> row + List(width - row.size) { "" } }
        val headers = normalized.first()
        val rows = normalized.drop(1)
        return SheetTable("CSV", headers, rows)
    }

    fun write(headers: List<String>, rows: List<List<String>>): String {
        val sb = StringBuilder()
        sb.appendLine(headers.joinToString(",") { escape(it) })
        rows.forEach { row ->
            sb.appendLine(row.joinToString(",") { escape(it) })
        }
        return sb.toString()
    }

    fun detectDelimiter(headerLine: String): Char {
        val comma = headerLine.count { it == ',' }
        val semi = headerLine.count { it == ';' }
        val tab = headerLine.count { it == '\t' }
        return when {
            semi > comma && semi >= tab -> ';'
            tab > comma && tab >= semi -> '\t'
            else -> ','
        }
    }

    private fun parseLine(line: String, delimiter: Char): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            when {
                c == '"' -> {
                    if (inQuotes && i + 1 < line.length && line[i + 1] == '"') {
                        current.append('"')
                        i++
                    } else {
                        inQuotes = !inQuotes
                    }
                }
                c == delimiter && !inQuotes -> {
                    result += current.toString()
                    current.clear()
                }
                else -> current.append(c)
            }
            i++
        }
        result += current.toString()
        return result
    }

    private fun escape(value: String): String {
        return if (value.contains(',') || value.contains('"') || value.contains('\n') || value.contains(';')) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else value
    }
}

object ColumnMapper {
    val targetFields = listOf(
        "ignore",
        "name",
        "contactPerson",
        "email",
        "phone",
        "addressLine1",
        "city",
        "province",
        "postalCode",
        "vatNumber",
        "notes",
        "invoiceNumber",
        "description",
        "quantity",
        "unitPrice",
        "unit",
        "issueDate",
        "dueDate",
        "status"
    )

    fun suggest(headers: List<String>): Map<String, String> {
        return headers.associateWith { header ->
            val key = header.lowercase(Locale.ROOT).replace(Regex("[^a-z0-9]+"), "")
            when {
                key in setOf("name", "customer", "customername", "client", "clientname") -> "name"
                key in setOf("contact", "contactperson", "attention") -> "contactPerson"
                key.contains("email") || key.contains("epos") -> "email"
                key.contains("phone") || key.contains("mobile") || key.contains("cell") -> "phone"
                key.contains("address") -> "addressLine1"
                key in setOf("city", "town") -> "city"
                key.contains("province") || key.contains("state") -> "province"
                key.contains("postal") || key == "zip" -> "postalCode"
                key.contains("vat") -> "vatNumber"
                key.contains("note") -> "notes"
                key.contains("invoicenumber") || key == "number" || key == "invoiceno" -> "invoiceNumber"
                key.contains("desc") || key.contains("item") -> "description"
                key.contains("qty") || key.contains("quantity") -> "quantity"
                key.contains("price") || key.contains("unitprice") || key.contains("amount") -> "unitPrice"
                key == "unit" -> "unit"
                key.contains("issue") || key == "date" -> "issueDate"
                key.contains("due") -> "dueDate"
                key.contains("status") -> "status"
                else -> "ignore"
            }
        }
    }

    fun mappedRow(headers: List<String>, row: List<String>, mapping: Map<String, String>): Map<String, String> {
        val out = mutableMapOf<String, String>()
        headers.forEachIndexed { index, header ->
            val field = mapping[header] ?: "ignore"
            if (field != "ignore") {
                out[field] = row.getOrNull(index).orEmpty().trim()
            }
        }
        return out
    }
}

class SpreadsheetService {
    fun read(fileName: String, input: InputStream): SheetTable {
        val lower = fileName.lowercase(Locale.ROOT)
        return when {
            lower.endsWith(".csv") || lower.endsWith(".txt") -> CsvCodec.parse(input.bufferedReader().readText())
            lower.endsWith(".xls") -> readXls(input)
            else -> readXlsx(input)
        }
    }

    fun writeCsv(headers: List<String>, rows: List<List<String>>): ByteArray =
        CsvCodec.write(headers, rows).toByteArray(Charsets.UTF_8)

    fun writeXlsx(
        sheetName: String,
        headers: List<String>,
        rows: List<List<String>>,
        output: OutputStream
    ) {
        Workbook(output, "InvoiceVault", "1.0").use { wb ->
            val sheet = wb.newWorksheet(sheetName)
            headers.forEachIndexed { col, header -> sheet.value(0, col, header) }
            rows.forEachIndexed { rowIndex, row ->
                row.forEachIndexed { col, value -> sheet.value(rowIndex + 1, col, value) }
            }
        }
    }

    fun writeXlsxBytes(sheetName: String, headers: List<String>, rows: List<List<String>>): ByteArray {
        val buffer = ByteArrayOutputStream()
        writeXlsx(sheetName, headers, rows, buffer)
        return buffer.toByteArray()
    }

    private fun readXlsx(input: InputStream): SheetTable {
        ReadableWorkbook(input).use { wb ->
            val sheet = wb.firstSheet
            val collected = sheet.read().map { row ->
                (0 until row.cellCount).map { idx -> row.getCellText(idx).orEmpty() }
            }
            if (collected.isEmpty()) return SheetTable(sheet.name ?: "Sheet1", emptyList(), emptyList())
            val width = collected.maxOf { it.size }
            val normalized = collected.map { row -> row + List(width - row.size) { "" } }
            return SheetTable(sheet.name ?: "Sheet1", normalized.first(), normalized.drop(1))
        }
    }

    private fun readXls(input: InputStream): SheetTable {
        HSSFWorkbook(input).use { wb ->
            val sheet = wb.getSheetAt(0)
            val headerRow = sheet.getRow(0) ?: return SheetTable(sheet.sheetName, emptyList(), emptyList())
            val width = headerRow.lastCellNum.toInt().coerceAtLeast(1)
            val headers = (0 until width).map { idx -> headerRow.getCell(idx)?.toString().orEmpty() }
            val rows = (1..sheet.lastRowNum).mapNotNull { r ->
                val row = sheet.getRow(r) ?: return@mapNotNull null
                (0 until width).map { idx -> row.getCell(idx)?.toString().orEmpty() }
            }
            return SheetTable(sheet.sheetName, headers, rows)
        }
    }
}
