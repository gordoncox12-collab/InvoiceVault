package za.co.invoicevault.excel

import android.content.Context
import android.net.Uri
import jxl.Workbook
import jxl.write.Label
import jxl.write.Number as JxlNumber
import jxl.write.WritableWorkbook
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

data class WorkbookData(
    val sheets: List<SheetData>
) {
    val firstSheet: SheetData get() = sheets.firstOrNull() ?: SheetData("Sheet1", emptyList())
}

data class SheetData(
    val name: String,
    val rows: List<List<String>>
) {
    val headers: List<String> get() = rows.firstOrNull().orEmpty()
    val body: List<List<String>> get() = if (rows.isEmpty()) emptyList() else rows.drop(1)
}

object KnownColumns {
    val options = listOf(
        "Ignore",
        "Customer name",
        "Customer email",
        "Customer phone",
        "Customer address",
        "Customer VAT",
        "Invoice number",
        "Invoice date",
        "Due date",
        "Status",
        "Line description",
        "Quantity",
        "Unit price",
        "Amount",
        "Notes",
        "Discount",
        "VAT rate"
    )
}

class ExcelService(private val context: Context) {

    fun readUri(uri: Uri): WorkbookData {
        val name = displayName(uri).lowercase()
        context.contentResolver.openInputStream(uri)?.use { input ->
            return when {
                name.endsWith(".csv") || name.endsWith(".txt") -> readCsv(input)
                name.endsWith(".xls") && !name.endsWith(".xlsx") -> readXls(input)
                else -> readXlsx(input)
            }
        } ?: error("Could not open spreadsheet")
    }

    fun readFile(file: File): WorkbookData {
        val name = file.name.lowercase()
        file.inputStream().use { input ->
            return when {
                name.endsWith(".csv") || name.endsWith(".txt") -> readCsv(input)
                name.endsWith(".xls") && !name.endsWith(".xlsx") -> readXls(input)
                else -> readXlsx(input)
            }
        }
    }

    fun writeCsv(sheets: List<SheetData>): ByteArray {
        val sheet = sheets.first()
        return buildString {
            sheet.rows.forEach { row ->
                appendLine(row.joinToString(",") { csvEscape(it) })
            }
        }.toByteArray(Charsets.UTF_8)
    }

    fun writeXlsx(sheets: List<SheetData>): ByteArray {
        val shared = linkedMapOf<String, Int>()
        fun idx(value: String): Int = shared.getOrPut(value) { shared.size }
        sheets.forEach { sheet -> sheet.rows.flatten().forEach { idx(it) } }

        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            fun put(path: String, xml: String) {
                zip.putNextEntry(ZipEntry(path))
                zip.write(xml.toByteArray(Charsets.UTF_8))
                zip.closeEntry()
            }
            put("[Content_Types].xml", contentTypes(sheets.size))
            put("_rels/.rels", rels())
            put("xl/workbook.xml", workbookXml(sheets))
            put("xl/_rels/workbook.xml.rels", workbookRels(sheets.size))
            put("xl/sharedStrings.xml", sharedStringsXml(shared.keys.toList()))
            put("xl/styles.xml", stylesXml())
            put("docProps/app.xml", appXml(sheets))
            put("docProps/core.xml", coreXml())
            sheets.forEachIndexed { index, sheet ->
                put("xl/worksheets/sheet${index + 1}.xml", sheetXml(sheet, shared))
            }
        }
        return out.toByteArray()
    }

    fun writeXls(sheets: List<SheetData>, file: File) {
        val wb: WritableWorkbook = Workbook.createWorkbook(file)
        sheets.forEachIndexed { index, sheet ->
            val ws = wb.createSheet(sheet.name.take(31).ifBlank { "Sheet${index + 1}" }, index)
            sheet.rows.forEachIndexed { r, row ->
                row.forEachIndexed { c, value ->
                    val asNumber = value.toDoubleOrNull()
                    if (asNumber != null && value.matches(Regex("-?\\d+(\\.\\d+)?"))) {
                        ws.addCell(JxlNumber(c, r, asNumber))
                    } else {
                        ws.addCell(Label(c, r, value))
                    }
                }
            }
        }
        wb.write()
        wb.close()
    }

    private fun displayName(uri: Uri): String {
        val cursor = context.contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val idx = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (idx >= 0) return it.getString(idx) ?: "sheet.xlsx"
            }
        }
        return uri.lastPathSegment ?: "sheet.xlsx"
    }

    private fun readCsv(input: InputStream): WorkbookData {
        val text = input.bufferedReader(Charsets.UTF_8).readText()
        val rows = parseCsv(text)
        return WorkbookData(listOf(SheetData("CSV", rows)))
    }

    private fun readXls(input: InputStream): WorkbookData {
        val tmp = File.createTempFile("xls", ".xls", context.cacheDir)
        tmp.outputStream().use { input.copyTo(it) }
        val wb = Workbook.getWorkbook(tmp)
        val sheets = wb.sheets.map { sheet ->
            val rows = mutableListOf<List<String>>()
            for (r in 0 until sheet.rows) {
                val row = (0 until sheet.columns).map { c -> sheet.getCell(c, r).contents ?: "" }
                rows += row
            }
            SheetData(sheet.name, trimSheet(rows))
        }
        wb.close()
        tmp.delete()
        return WorkbookData(sheets.ifEmpty { listOf(SheetData("Sheet1", emptyList())) })
    }

    private fun readXlsx(input: InputStream): WorkbookData {
        val contents = mutableMapOf<String, ByteArray>()
        ZipInputStream(input).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                contents[entry.name] = zip.readBytes()
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
        val shared = parseSharedStrings(contents["xl/sharedStrings.xml"])
        val workbookXml = contents["xl/workbook.xml"]?.toString(Charsets.UTF_8).orEmpty()
        val relsXml = contents["xl/_rels/workbook.xml.rels"]?.toString(Charsets.UTF_8).orEmpty()
        val sheetNames = Regex("""<sheet[^>]*name="([^"]+)"""")
            .findAll(workbookXml)
            .map { it.groupValues[1] }
            .toList()
        val relTargets = Regex("""Id="([^"]+)"[^>]*Target="([^"]+)"""")
            .findAll(relsXml)
            .associate { it.groupValues[1] to it.groupValues[2] }
        val rIds = Regex("""r:id="([^"]+)"""")
            .findAll(workbookXml)
            .map { it.groupValues[1] }
            .toList()
        val sheets = sheetNames.mapIndexed { index, name ->
            val rid = rIds.getOrNull(index)
            val target = rid?.let { relTargets[it] } ?: "worksheets/sheet${index + 1}.xml"
            val path = if (target.startsWith("xl/")) target else "xl/" + target.removePrefix("/")
            val xml = contents[path]?.toString(Charsets.UTF_8).orEmpty()
            SheetData(name, parseSheet(xml, shared))
        }
        return WorkbookData(sheets.ifEmpty { listOf(SheetData("Sheet1", emptyList())) })
    }

    private fun parseSharedStrings(bytes: ByteArray?): List<String> {
        if (bytes == null) return emptyList()
        val list = mutableListOf<String>()
        val parser = XmlPullParserFactory.newInstance().newPullParser()
        parser.setInput(bytes.inputStream(), "UTF-8")
        var inT = false
        val sb = StringBuilder()
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> {
                    if (parser.name == "t") {
                        inT = true
                        sb.clear()
                    }
                    if (parser.name == "si") sb.clear()
                }
                XmlPullParser.TEXT -> if (inT) sb.append(parser.text)
                XmlPullParser.END_TAG -> {
                    if (parser.name == "t") inT = false
                    if (parser.name == "si") list += sb.toString()
                }
            }
            event = parser.next()
        }
        return list
    }

    private fun parseSheet(xml: String, shared: List<String>): List<List<String>> {
        if (xml.isBlank()) return emptyList()
        val cells = mutableMapOf<Pair<Int, Int>, String>()
        val parser = XmlPullParserFactory.newInstance().newPullParser()
        parser.setInput(xml.byteInputStream(), "UTF-8")
        var ref = "A1"
        var type = ""
        var inV = false
        var inIs = false
        var text = StringBuilder()
        var event = parser.eventType
        fun commit() {
            val (col, row) = cellRef(ref)
            val raw = text.toString()
            val value = when (type) {
                "s" -> shared.getOrNull(raw.toIntOrNull() ?: -1) ?: raw
                "inlineStr", "str" -> raw
                else -> raw
            }
            cells[row to col] = value
        }
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> {
                    when (parser.name) {
                        "c" -> {
                            ref = parser.getAttributeValue(null, "r") ?: ref
                            type = parser.getAttributeValue(null, "t") ?: ""
                            text = StringBuilder()
                        }
                        "v" -> inV = true
                        "t" -> if (inIs) inV = true
                        "is" -> inIs = true
                    }
                }
                XmlPullParser.TEXT -> if (inV) text.append(parser.text)
                XmlPullParser.END_TAG -> {
                    when (parser.name) {
                        "v" -> inV = false
                        "t" -> inV = false
                        "is" -> inIs = false
                        "c" -> commit()
                    }
                }
            }
            event = parser.next()
        }
        if (cells.isEmpty()) return emptyList()
        val maxRow = cells.keys.maxOf { it.first }
        val maxCol = cells.keys.maxOf { it.second }
        val rows = (0..maxRow).map { r ->
            (0..maxCol).map { c -> cells[r to c].orEmpty() }
        }
        return trimSheet(rows)
    }

    private fun cellRef(ref: String): Pair<Int, Int> {
        val letters = ref.takeWhile { it.isLetter() }
        val digits = ref.dropWhile { it.isLetter() }.toIntOrNull() ?: 1
        var col = 0
        for (ch in letters.uppercase()) {
            col = col * 26 + (ch - 'A' + 1)
        }
        return (digits - 1) to (col - 1)
    }

    private fun trimSheet(rows: List<List<String>>): List<List<String>> {
        val filtered = rows.filter { row -> row.any { it.isNotBlank() } }
        if (filtered.isEmpty()) return emptyList()
        val width = filtered.maxOf { row -> row.indexOfLast { it.isNotBlank() } + 1 }.coerceAtLeast(1)
        return filtered.map { row ->
            val padded = if (row.size >= width) row else row + List(width - row.size) { "" }
            padded.take(width)
        }
    }

    private fun parseCsv(text: String): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        val row = mutableListOf<String>()
        val cell = StringBuilder()
        var inQuotes = false
        var i = 0
        val s = text.replace("\r\n", "\n").replace('\r', '\n')
        while (i < s.length) {
            val ch = s[i]
            when {
                ch == '"' -> {
                    if (inQuotes && i + 1 < s.length && s[i + 1] == '"') {
                        cell.append('"')
                        i++
                    } else {
                        inQuotes = !inQuotes
                    }
                }
                ch == ',' && !inQuotes -> {
                    row += cell.toString()
                    cell.clear()
                }
                ch == '\n' && !inQuotes -> {
                    row += cell.toString()
                    cell.clear()
                    rows += row.toList()
                    row.clear()
                }
                else -> cell.append(ch)
            }
            i++
        }
        if (cell.isNotEmpty() || row.isNotEmpty()) {
            row += cell.toString()
            rows += row.toList()
        }
        return trimSheet(rows)
    }

    private fun csvEscape(value: String): String {
        return if (value.contains(',') || value.contains('"') || value.contains('\n')) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else value
    }

    private fun colName(index: Int): String {
        var n = index + 1
        val sb = StringBuilder()
        while (n > 0) {
            n--
            sb.insert(0, ('A' + n % 26))
            n /= 26
        }
        return sb.toString()
    }

    private fun xml(value: String) = value
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")

    private fun sheetXml(sheet: SheetData, shared: Map<String, Int>): String {
        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        sb.append("""<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><sheetData>""")
        sheet.rows.forEachIndexed { r, row ->
            sb.append("""<row r="${r + 1}">""")
            row.forEachIndexed { c, value ->
                val ref = "${colName(c)}${r + 1}"
                val num = value.toDoubleOrNull()
                if (num != null && value.matches(Regex("-?\\d+(\\.\\d+)?"))) {
                    sb.append("""<c r="$ref" t="n"><v>$value</v></c>""")
                } else {
                    val idx = shared[value] ?: 0
                    sb.append("""<c r="$ref" t="s"><v>$idx</v></c>""")
                }
            }
            sb.append("</row>")
        }
        sb.append("</sheetData></worksheet>")
        return sb.toString()
    }

    private fun sharedStringsXml(values: List<String>): String {
        val body = values.joinToString("") { """<si><t xml:space="preserve">${xml(it)}</t></si>""" }
        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><sst xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" count="${values.size}" uniqueCount="${values.size}">$body</sst>"""
    }

    private fun workbookXml(sheets: List<SheetData>): String {
        val sheetTags = sheets.mapIndexed { index, sheet ->
            """<sheet name="${xml(sheet.name.take(31).ifBlank { "Sheet${index + 1}" })}" sheetId="${index + 1}" r:id="rId${index + 1}"/>"""
        }.joinToString("")
        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"><sheets>$sheetTags</sheets></workbook>"""
    }

    private fun workbookRels(count: Int): String {
        val rels = (1..count).joinToString("") {
            """<Relationship Id="rId$it" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet$it.xml"/>"""
        } + """<Relationship Id="rId${count + 1}" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/sharedStrings" Target="sharedStrings.xml"/><Relationship Id="rId${count + 2}" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>"""
        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">$rels</Relationships>"""
    }

    private fun contentTypes(count: Int): String {
        val sheets = (1..count).joinToString("") {
            """<Override PartName="/xl/worksheets/sheet$it.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>"""
        }
        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types"><Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/><Default Extension="xml" ContentType="application/xml"/><Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/><Override PartName="/xl/sharedStrings.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sharedStrings+xml"/><Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/><Override PartName="/docProps/core.xml" ContentType="application/vnd.openxmlformats-package.core-properties+xml"/><Override PartName="/docProps/app.xml" ContentType="application/vnd.openxmlformats-officedocument.extended-properties+xml"/>$sheets</Types>"""
    }

    private fun rels(): String =
        """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/><Relationship Id="rId2" Type="http://schemas.openxmlformats.org/package/2006/relationships/metadata/core-properties" Target="docProps/core.xml"/><Relationship Id="rId3" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/extended-properties" Target="docProps/app.xml"/></Relationships>"""

    private fun stylesXml(): String =
        """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><fonts count="1"><font><sz val="11"/><name val="Calibri"/></font></fonts><fills count="1"><fill><patternFill patternType="none"/></fill></fills><borders count="1"><border/></borders><cellStyleXfs count="1"><xf/></cellStyleXfs><cellXfs count="1"><xf/></cellXfs></styleSheet>"""

    private fun appXml(sheets: List<SheetData>): String {
        val titles = sheets.joinToString("") { "<vt:lpstr>${xml(it.name)}</vt:lpstr>" }
        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Properties xmlns="http://schemas.openxmlformats.org/officeDocument/2006/extended-properties" xmlns:vt="http://schemas.openxmlformats.org/officeDocument/2006/docPropsVTypes"><Application>InvoiceVault</Application><HeadingPairs><vt:vector size="2" baseType="variant"><vt:variant><vt:lpstr>Worksheets</vt:lpstr></vt:variant><vt:variant><vt:i4>${sheets.size}</vt:i4></vt:variant></vt:vector></HeadingPairs><TitlesOfParts><vt:vector size="${sheets.size}" baseType="lpstr">$titles</vt:vector></TitlesOfParts></Properties>"""
    }

    private fun coreXml(): String =
        """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><cp:coreProperties xmlns:cp="http://schemas.openxmlformats.org/package/2006/metadata/core-properties" xmlns:dc="http://purl.org/dc/elements/1.1/" xmlns:dcterms="http://purl.org/dc/terms/" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"><dc:creator>InvoiceVault</dc:creator><cp:lastModifiedBy>InvoiceVault</cp:lastModifiedBy></cp:coreProperties>"""
}
