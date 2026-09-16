package za.co.invoicevault.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import za.co.invoicevault.data.excel.ColumnMapper
import za.co.invoicevault.data.excel.CsvCodec
import za.co.invoicevault.data.excel.SheetTable
import za.co.invoicevault.ui.components.DropdownField
import za.co.invoicevault.ui.vm.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExcelScreen(vm: MainViewModel, businessId: Long, onBack: () -> Unit) {
    val repo = vm.repository()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var table by remember { mutableStateOf<SheetTable?>(null) }
    var mapping by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var fileName by remember { mutableStateOf("import.csv") }
    var message by remember { mutableStateOf("Import customers from .xlsx, .xls or CSV with column mapping, or capture app data into a sheet in the customer folder.") }
    var mode by remember { mutableStateOf("customers") }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        val name = uri.lastPathSegment?.substringAfterLast('/').orEmpty().ifBlank { "import.xlsx" }
        fileName = name
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return@rememberLauncherForActivityResult
        val parsed = repo.readSheetFromStream(name, bytes)
        table = parsed
        mapping = ColumnMapper.suggest(parsed.headers)
        message = "Loaded ${parsed.rows.size} rows from ${parsed.sheetName}"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Spreadsheets") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } }
            )
        }
    ) { padding ->
        Column(
            Modifier.padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(message)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { picker.launch(arrayOf("*/*")) }) { Text("Choose .xlsx / .xls / CSV") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { mode = "customers" }) { Text(if (mode == "customers") "Customers ✓" else "Customers") }
                OutlinedButton(onClick = { mode = "capture" }) { Text(if (mode == "capture") "Capture sheet ✓" else "Capture sheet") }
            }
            val current = table
            if (current != null) {
                Text("Column mapping")
                current.headers.forEach { header ->
                    DropdownField(
                        label = header.ifBlank { "(blank column)" },
                        options = ColumnMapper.targetFields,
                        selected = mapping[header] ?: "ignore",
                        onSelect = { mapping = mapping + (header to it) },
                        print = { it }
                    )
                }
                Button(
                    onClick = {
                        scope.launch {
                            val customers = repo.customers(businessId).first()
                            if (mode == "customers") {
                                val n = repo.importCustomers(businessId, current, mapping)
                                message = "Imported $n customers into this business"
                            } else {
                                val customer = customers.firstOrNull()
                                if (customer == null) {
                                    message = "Add a customer first so the captured sheet has a folder"
                                } else {
                                    val bytes = if (fileName.endsWith(".xlsx", true) || fileName.endsWith(".xls", true)) {
                                        repo.writeCustomersXlsx(customers).let {
                                            za.co.invoicevault.data.excel.SpreadsheetService().writeXlsxBytes(
                                                current.sheetName,
                                                current.headers,
                                                current.rows
                                            )
                                        }
                                    } else {
                                        CsvCodec.write(current.headers, current.rows).toByteArray()
                                    }
                                    val stored = repo.captureSheet(
                                        businessId,
                                        customer.id,
                                        fileName.ifBlank { "captured.csv" },
                                        bytes,
                                        if (fileName.endsWith(".xlsx", true)) {
                                            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                                        } else "text/csv"
                                    )
                                    message = "Captured ${current.rows.size} rows into ${stored.name}"
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text(if (mode == "customers") "Import mapped customers" else "Save mapped sheet into customer folder") }
            }
            Button(
                onClick = {
                    scope.launch {
                        val customer = repo.customers(businessId).first().firstOrNull()
                        if (customer != null) {
                            val file = repo.exportCustomersToFolder(businessId, customer.id)
                            message = "Wrote ${file.name} into ${customer.name}'s Excel folder"
                        } else {
                            message = "Add a customer first so the sheet has a folder"
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Capture customers into .xlsx") }
        }
    }
}
