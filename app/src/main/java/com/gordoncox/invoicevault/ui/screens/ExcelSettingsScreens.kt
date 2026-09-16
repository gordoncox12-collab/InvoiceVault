package com.gordoncox.invoicevault.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.gordoncox.invoicevault.data.entity.AccentPalette
import com.gordoncox.invoicevault.data.entity.FolderType
import com.gordoncox.invoicevault.data.entity.InvoiceTemplateEntity
import com.gordoncox.invoicevault.data.entity.TemplateLayout
import com.gordoncox.invoicevault.data.entity.ThemeMode
import com.gordoncox.invoicevault.data.excel.CustomerImportFields
import com.gordoncox.invoicevault.data.excel.InvoiceImportFields
import com.gordoncox.invoicevault.data.excel.SheetPreview
import com.gordoncox.invoicevault.data.excel.guessMapping
import com.gordoncox.invoicevault.ui.components.LabeledField
import com.gordoncox.invoicevault.ui.components.SectionTitle
import com.gordoncox.invoicevault.ui.nav.AppViewModel
import com.gordoncox.invoicevault.ui.nav.templateEdit
import com.gordoncox.invoicevault.ui.theme.Palettes
import com.gordoncox.invoicevault.util.Za
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(appVm: AppViewModel, nav: NavHostController) {
    val settings by appVm.settings.collectAsState()
    val mode = runCatching { ThemeMode.valueOf(settings.themeMode) }.getOrDefault(ThemeMode.SYSTEM)
    val accent = runCatching { AccentPalette.valueOf(settings.accentPalette) }.getOrDefault(AccentPalette.TEAL_VAULT)
    Scaffold(topBar = { BackBar("Theme", nav) }) { padding ->
        Column(Modifier.padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionTitle("Appearance")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ThemeMode.entries.forEach { m ->
                    FilterChip(selected = mode == m, onClick = { appVm.setTheme(m) }, label = { Text(m.name.lowercase().replaceFirstChar { it.titlecase() }) })
                }
            }
            SectionTitle("Accent palettes")
            Palettes.forEach { p ->
                Card(
                    onClick = { appVm.setAccent(p.key) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    ListItem(
                        headlineContent = { Text(p.name) },
                        supportingContent = { Text(if (accent == p.key) "Selected" else "Tap to apply") },
                        leadingContent = {
                            Box(Modifier.size(28.dp).clip(CircleShape).background(p.primary))
                        },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplateListScreen(appVm: AppViewModel, nav: NavHostController) {
    val business by appVm.activeBusiness.collectAsState()
    val templates by (business?.let { appVm.repository.templates(it.id) } ?: kotlinx.coroutines.flow.flowOf(emptyList()))
        .collectAsState(initial = emptyList())
    Scaffold(topBar = { BackBar("Templates", nav) }) { padding ->
        Column(Modifier.padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { nav.navigate(templateEdit("new")) }, modifier = Modifier.fillMaxWidth()) { Text("New template") }
            templates.forEach { t ->
                Card(onClick = { nav.navigate(templateEdit(t.id)) }, modifier = Modifier.fillMaxWidth()) {
                    ListItem(
                        headlineContent = { Text(t.name) },
                        supportingContent = { Text("${t.layout}${if (t.isDefault) " · default" else ""}") },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplateEditScreen(id: String, appVm: AppViewModel, nav: NavHostController) {
    val repo = appVm.repository
    val business by appVm.activeBusiness.collectAsState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var name by remember { mutableStateOf("Custom template") }
    var layout by remember { mutableStateOf(TemplateLayout.CLASSIC.name) }
    var footer by remember { mutableStateOf("") }
    var showBank by remember { mutableStateOf(true) }
    var isDefault by remember { mutableStateOf(false) }
    var primary by remember { mutableStateOf(0xFF0F6E56) }
    var accent by remember { mutableStateOf(0xFFC9A227) }
    var logoPath by remember { mutableStateOf<String?>(null) }
    var headerPath by remember { mutableStateOf<String?>(null) }
    var extraPath by remember { mutableStateOf<String?>(null) }
    var existing by remember { mutableStateOf<InvoiceTemplateEntity?>(null) }

    LaunchedEffect(id) {
        if (id != "new") {
            repo.getTemplate(id)?.let { t ->
                existing = t
                name = t.name; layout = t.layout; footer = t.footerText.orEmpty()
                showBank = t.showBankDetails; isDefault = t.isDefault
                primary = t.primaryColor; accent = t.accentColor
                logoPath = t.logoPath; headerPath = t.headerImagePath; extraPath = t.extraImagePath
            }
        }
    }

    val logoPick = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        val biz = business ?: return@rememberLauncherForActivityResult
        if (uri != null) scope.launch { logoPath = repo.copyUriToTemplate(biz.id, uri, "logo.png") }
    }
    val headerPick = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        val biz = business ?: return@rememberLauncherForActivityResult
        if (uri != null) scope.launch { headerPath = repo.copyUriToTemplate(biz.id, uri, "header.jpg") }
    }
    val extraPick = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        val biz = business ?: return@rememberLauncherForActivityResult
        if (uri != null) scope.launch { extraPath = repo.copyUriToTemplate(biz.id, uri, "picture.jpg") }
    }

    Scaffold(topBar = { BackBar("Template", nav) }) { padding ->
        Column(Modifier.padding(padding).padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            LabeledField("Name", name, { name = it })
            Text("Layout")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TemplateLayout.entries.forEach { l ->
                    FilterChip(selected = layout == l.name, onClick = { layout = l.name }, label = { Text(l.name.lowercase().replaceFirstChar { it.titlecase() }) })
                }
            }
            SectionTitle("Colours")
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Palettes.forEach { p ->
                    Box(
                        Modifier.size(36.dp).clip(CircleShape).background(p.primary)
                            .border(
                                if ((primary and 0xFFFFFFL) == (p.primary.toArgb().toLong() and 0xFFFFFFL)) 3.dp else 0.dp,
                                Color.Black,
                                CircleShape,
                            )
                            .clickable {
                                primary = p.primary.toArgb().toLong() and 0xFFFFFFFFL
                                accent = p.secondary.toArgb().toLong() and 0xFFFFFFFFL
                            },
                    )
                }
            }
            LabeledField("Footer", footer, { footer = it }, singleLine = false, minLines = 2)
            FilterChip(selected = showBank, onClick = { showBank = !showBank }, label = { Text("Show bank details") })
            FilterChip(selected = isDefault, onClick = { isDefault = !isDefault }, label = { Text("Default for this business") })
            OutlinedButton(onClick = { logoPick.launch("image/*") }) { Text(if (logoPath == null) "Insert logo" else "Logo added") }
            OutlinedButton(onClick = { headerPick.launch("image/*") }) { Text(if (headerPath == null) "Insert header picture" else "Header picture added") }
            OutlinedButton(onClick = { extraPick.launch("image/*") }) { Text(if (extraPath == null) "Insert extra picture" else "Extra picture added") }
            OutlinedButton(onClick = {
                val clip = context.getSystemService(android.content.ClipboardManager::class.java)
                val item = clip?.primaryClip?.getItemAt(0)
                val uri = item?.uri
                val biz = business
                if (uri != null && biz != null) {
                    scope.launch { extraPath = repo.copyUriToTemplate(biz.id, uri, "pasted.jpg") }
                } else {
                    Toast.makeText(context, "Copy an image first, then paste here", Toast.LENGTH_SHORT).show()
                }
            }) { Text("Paste picture from clipboard") }
            Button(onClick = {
                val biz = business ?: return@Button
                scope.launch {
                    val now = System.currentTimeMillis()
                    repo.saveTemplate(
                        (existing ?: InvoiceTemplateEntity(
                            id = Za.newId(),
                            businessId = biz.id,
                            name = name,
                            isDefault = isDefault,
                            primaryColor = primary,
                            accentColor = accent,
                            logoPath = logoPath,
                            layout = layout,
                            showBankDetails = showBank,
                            footerText = footer.ifBlank { null },
                            headerImagePath = headerPath,
                            extraImagePath = extraPath,
                            createdAt = now,
                            updatedAt = now,
                        )).copy(
                            name = name,
                            isDefault = isDefault,
                            primaryColor = primary,
                            accentColor = accent,
                            logoPath = logoPath,
                            layout = layout,
                            showBankDetails = showBank,
                            footerText = footer.ifBlank { null },
                            headerImagePath = headerPath,
                            extraImagePath = extraPath,
                        ),
                    )
                    nav.popBackStack()
                }
            }, modifier = Modifier.fillMaxWidth()) { Text("Save template") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExcelHubScreen(appVm: AppViewModel, nav: NavHostController) {
    val repo = appVm.repository
    val business by appVm.activeBusiness.collectAsState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var preview by remember { mutableStateOf<SheetPreview?>(null) }
    var pickedUri by remember { mutableStateOf<Uri?>(null) }
    var pickedName by remember { mutableStateOf("") }
    var mode by remember { mutableStateOf("customers") }
    var mapping by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var status by remember { mutableStateOf<String?>(null) }
    val fields = if (mode == "customers") CustomerImportFields.fields else InvoiceImportFields.fields

    val opener = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            pickedUri = uri
            pickedName = uri.lastPathSegment?.substringAfterLast('/') ?: "sheet"
            scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                val p = repo.excel.preview(uri, pickedName)
                preview = p
                mapping = guessMapping(p.headers, fields)
            }
        }
    }

    Scaffold(topBar = { BackBar("Excel", nav) }) { padding ->
        Column(Modifier.padding(padding).padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Import .xlsx, .xls or CSV. Columns are mapped before anything is written locally.")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = mode == "customers", onClick = { mode = "customers" }, label = { Text("Customers") })
                FilterChip(selected = mode == "invoices", onClick = { mode = "invoices" }, label = { Text("Invoices") })
            }
            Button(onClick = {
                opener.launch(
                    arrayOf(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                        "application/vnd.ms-excel",
                        "text/csv",
                        "text/comma-separated-values",
                        "*/*",
                    ),
                )
            }, modifier = Modifier.fillMaxWidth()) { Text("Choose spreadsheet") }
            preview?.let { p ->
                Text("${p.fileName}  ·  ${p.sheetName}  ·  ${p.headers.size} columns")
                p.headers.forEachIndexed { i, h -> Text("Col ${i + 1}: $h") }
                SectionTitle("Column mapping")
                fields.forEach { (key, label) ->
                    val selected = mapping[key] ?: -1
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(label, modifier = Modifier.padding(top = 10.dp))
                        FilterChip(selected = selected < 0, onClick = { mapping = mapping + (key to -1) }, label = { Text("Skip") })
                        p.headers.forEachIndexed { idx, header ->
                            FilterChip(
                                selected = selected == idx,
                                onClick = { mapping = mapping + (key to idx) },
                                label = { Text(header.ifBlank { "Col ${idx + 1}" }.take(16)) },
                            )
                        }
                    }
                }
                Button(onClick = {
                    val biz = business ?: return@Button
                    val uri = pickedUri ?: return@Button
                    scope.launch {
                        val n = if (mode == "customers") {
                            repo.importCustomers(biz.id, uri, pickedName, mapping)
                        } else {
                            repo.importInvoiceLines(biz.id, uri, pickedName, mapping)
                        }
                        status = "Imported $n record(s) offline"
                    }
                }, modifier = Modifier.fillMaxWidth()) { Text("Import") }
            }
            status?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
            SectionTitle("Export this business")
            OutlinedButton(onClick = {
                val biz = business ?: return@OutlinedButton
                scope.launch {
                    val dest = File(context.cacheDir, "${biz.invoicePrefix}-export.xlsx")
                    repo.exportBusinessWorkbook(biz.id, dest, csv = false)
                    containerShare(context, dest, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                }
            }, modifier = Modifier.fillMaxWidth()) { Text("Export XLSX") }
            OutlinedButton(onClick = {
                val biz = business ?: return@OutlinedButton
                scope.launch {
                    val dest = File(context.cacheDir, "${biz.invoicePrefix}-invoices.csv")
                    repo.exportBusinessWorkbook(biz.id, dest, csv = true)
                    containerShare(context, dest, "text/csv")
                }
            }, modifier = Modifier.fillMaxWidth()) { Text("Export CSV") }
            val customers by (business?.let { appVm.customers(it.id) } ?: kotlinx.coroutines.flow.flowOf(emptyList()))
                .collectAsState(initial = emptyList())
            SectionTitle("Capture a sheet into a customer folder")
            customers.take(8).forEach { c ->
                Card(onClick = { nav.navigate(com.gordoncox.invoicevault.ui.nav.sheetCapture(c.id)) }, modifier = Modifier.fillMaxWidth()) {
                    ListItem(headlineContent = { Text(c.name) }, supportingContent = { Text("Write Excel/CSV into their folder") })
                }
            }
        }
    }
}

private fun containerShare(context: android.content.Context, file: File, mime: String) {
    val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
        type = mime
        putExtra(android.content.Intent.EXTRA_STREAM, uri)
        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(android.content.Intent.createChooser(intent, file.name))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SheetCaptureScreen(customerId: String, appVm: AppViewModel, nav: NavHostController) {
    val repo = appVm.repository
    val customer by repo.customer(customerId).collectAsState(initial = null)
    val scope = rememberCoroutineScope()
    var fileName by remember { mutableStateOf("capture.xlsx") }
    val headers = remember { mutableStateListOf("Item", "Qty", "Amount", "Notes") }
    val rows = remember {
        mutableStateListOf(
            mutableStateListOf("", "", "", ""),
            mutableStateListOf("", "", "", ""),
            mutableStateListOf("", "", "", ""),
        )
    }
    Scaffold(topBar = { BackBar("Capture sheet", nav) }) { padding ->
        Column(Modifier.padding(padding).padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Typed here is written into this customer's Excel folder on device storage.")
            LabeledField("File name", fileName, { fileName = it })
            headers.forEachIndexed { i, h ->
                OutlinedTextField(value = h, onValueChange = { headers[i] = it }, label = { Text("Column ${i + 1}") }, modifier = Modifier.fillMaxWidth())
            }
            SectionTitle("Rows")
            rows.forEachIndexed { r, row ->
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    row.forEachIndexed { c, v ->
                        OutlinedTextField(
                            value = v,
                            onValueChange = { row[c] = it },
                            modifier = Modifier.weight(1f),
                            label = { Text(headers.getOrNull(c) ?: "") },
                        )
                    }
                }
            }
            OutlinedButton(onClick = { rows.add(mutableStateListOf("", "", "", "")) }) { Text("Add row") }
            Button(onClick = {
                val c = customer ?: return@Button
                scope.launch {
                    val csv = fileName.endsWith(".csv", true)
                    val name = if (fileName.contains('.')) fileName else "$fileName.xlsx"
                    repo.writeCaptureSheet(
                        c.businessId,
                        c.id,
                        name,
                        headers.toList(),
                        rows.map { it.toList() },
                        csv,
                    )
                    nav.popBackStack()
                }
            }, modifier = Modifier.fillMaxWidth()) { Text("Save to customer folder") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionListScreen(appVm: AppViewModel, nav: NavHostController) {
    val business by appVm.activeBusiness.collectAsState()
    val txs by (business?.let { appVm.transactions(it.id) } ?: kotlinx.coroutines.flow.flowOf(emptyList()))
        .collectAsState(initial = emptyList())
    Scaffold(topBar = { BackBar("Transactions", nav) }) { padding ->
        Column(Modifier.padding(padding).padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Every payment, expense and receipt is stored in Room on this phone.")
            txs.forEach { tx ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    ListItem(
                        headlineContent = { Text("${tx.type}  ${Za.money(tx.amount, tx.currency)}") },
                        supportingContent = {
                            Text(listOfNotNull(tx.method, tx.reference, tx.notes, Za.date(tx.occurredAt)).joinToString(" · "))
                        },
                    )
                }
            }
        }
    }
}
