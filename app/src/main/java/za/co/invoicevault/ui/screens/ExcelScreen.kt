package za.co.invoicevault.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import za.co.invoicevault.excel.KnownColumns
import za.co.invoicevault.ui.VaultViewModel
import za.co.invoicevault.ui.components.SectionCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExcelScreen(vm: VaultViewModel) {
    val workbook by vm.workbook.collectAsStateWithLifecycle()
    val mapping by vm.columnMap.collectAsStateWithLifecycle()
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { vm.openWorkbook(it) }
    }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Excel & CSV", style = MaterialTheme.typography.headlineSmall)
            Text("Import .xlsx, .xls or CSV. Map columns, then capture the rows into this business vault. Export writes a spreadsheet into the customer Excel folder.")
        }
        item {
            SectionCard(title = "Import") {
                Button(onClick = { picker.launch("*/*") }) { Text("Choose spreadsheet") }
                workbook?.let { book ->
                    Text("${book.firstSheet.name}: ${book.firstSheet.body.size} data rows, ${book.sheets.size} sheet(s)")
                }
            }
        }
        if (workbook != null) {
            item { Text("Column mapping", style = MaterialTheme.typography.titleMedium) }
            itemsIndexed(workbook!!.firstSheet.headers) { index, header ->
                var expanded by remember { mutableStateOf(false) }
                val current = mapping.getOrNull(index) ?: "Ignore"
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                    OutlinedTextField(
                        value = current,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(header.ifBlank { "Column ${index + 1}" }) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                        modifier = Modifier.menuAnchor().padding(bottom = 4.dp)
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        KnownColumns.options.forEach { option ->
                            DropdownMenuItem(text = { Text(option) }, onClick = {
                                vm.updateMapping(index, option)
                                expanded = false
                            })
                        }
                    }
                }
            }
            item {
                Button(onClick = { vm.importMappedRows() }) { Text("Import into InvoiceVault") }
                val preview = workbook!!.firstSheet.body.take(3)
                if (preview.isNotEmpty()) {
                    SectionCard(title = "Preview") {
                        preview.forEach { row ->
                            Text(row.joinToString(" · "))
                        }
                    }
                }
            }
        }
        item {
            SectionCard(title = "Export from the app") {
                OutlinedButton(onClick = {
                    vm.exportWorkbook("customers")?.let { file ->
                        vm.share.shareFile(file, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "Customers", "InvoiceVault customers")
                    }
                }) { Text("Export customers (.xlsx)") }
                OutlinedButton(onClick = {
                    vm.exportWorkbook("invoices")?.let { file ->
                        vm.share.shareFile(file, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "Invoices", "InvoiceVault invoices")
                    }
                }) { Text("Export invoices (.xlsx)") }
                OutlinedButton(onClick = {
                    vm.exportWorkbook("all")?.let { file ->
                        vm.share.shareFile(file, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "Vault", "InvoiceVault export")
                    }
                }) { Text("Export workbook (customers + invoices)") }
                OutlinedButton(onClick = {
                    val file = vm.exportCsv()
                    vm.share.shareFile(file, "text/csv", "Invoices CSV", "InvoiceVault CSV")
                }) { Text("Export invoices (.csv)") }
            }
        }
    }
}
