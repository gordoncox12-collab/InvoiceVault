package za.co.invoicevault.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import za.co.invoicevault.data.db.TemplateEntity
import za.co.invoicevault.domain.AccentPalette
import za.co.invoicevault.domain.TemplateStyle
import za.co.invoicevault.domain.ThemeMode
import za.co.invoicevault.ui.components.DropdownField
import za.co.invoicevault.ui.theme.label
import za.co.invoicevault.ui.vm.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplateListScreen(vm: MainViewModel, onEdit: (Long) -> Unit, onNew: () -> Unit) {
    val templates by vm.templates.collectAsStateWithLifecycle()
    Scaffold(
        topBar = { TopAppBar(title = { Text("Invoice templates") }) },
        floatingActionButton = { FloatingActionButton(onClick = onNew) { Icon(Icons.Default.Add, null) } }
    ) { padding ->
        LazyColumn(Modifier.padding(padding)) {
            items(templates, key = { it.id }) { template ->
                ListItem(
                    headlineContent = { Text(template.name) },
                    supportingContent = { Text("${template.style.name.lowercase()} · ${if (template.showLogo) "logo on" else "logo off"}") },
                    modifier = Modifier.clickable { onEdit(template.id) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplateEditScreen(vm: MainViewModel, templateId: Long, onDone: () -> Unit) {
    val repo = vm.repository()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var template by remember {
        mutableStateOf(
            TemplateEntity(
                name = "New template",
                style = TemplateStyle.MODERN,
                primaryColor = 0xFF123524L,
                accentColor = 0xFFC4A35AL
            )
        )
    }
    LaunchedEffect(templateId) {
        if (templateId != 0L) repo.template(templateId)?.let { template = it }
    }
    val logoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val business = repo.currentBusiness() ?: return@launch
            val customer = repo.customers(business.id).first().firstOrNull() ?: return@launch
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return@launch
            val file = repo.saveLocalImage(business.id, customer.id, bytes, "template-logo.png")
            template = template.copy(logoPath = file.absolutePath, showLogo = true)
        }
    }
    val bannerPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val business = repo.currentBusiness() ?: return@launch
            val first = repo.customers(business.id).first().firstOrNull() ?: return@launch
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return@launch
            val file = repo.saveLocalImage(business.id, first.id, bytes, "template-banner.png")
            template = template.copy(bannerImagePath = file.absolutePath)
        }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit template") },
                navigationIcon = { IconButton(onClick = onDone) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(template.name, { template = template.copy(name = it) }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
            DropdownField("Style", TemplateStyle.entries, template.style, { template = template.copy(style = it) }, { it.name.lowercase() })
            OutlinedTextField(template.primaryColor.toULong().toString(16), {
                template = template.copy(primaryColor = it.toLongOrNull(16) ?: template.primaryColor)
            }, label = { Text("Primary colour (ARGB hex)") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(template.accentColor.toULong().toString(16), {
                template = template.copy(accentColor = it.toLongOrNull(16) ?: template.accentColor)
            }, label = { Text("Accent colour (ARGB hex)") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(template.footerText, { template = template.copy(footerText = it) }, label = { Text("Footer") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(template.defaultNotes, { template = template.copy(defaultNotes = it) }, label = { Text("Default notes") }, modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { logoPicker.launch("image/*") }) { Text("Logo picture") }
                OutlinedButton(onClick = { bannerPicker.launch("image/*") }) { Text("Banner picture") }
            }
            if (!template.logoPath.isNullOrBlank()) Text("Logo saved on device")
            if (!template.bannerImagePath.isNullOrBlank()) Text("Banner saved on device")
            Button(onClick = { scope.launch { repo.saveTemplate(template); onDone() } }, modifier = Modifier.fillMaxWidth()) { Text("Save template") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(vm: MainViewModel, onBusinesses: () -> Unit, onExcel: (Long) -> Unit) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val selectedId by vm.selectedBusinessId.collectAsStateWithLifecycle()
    Scaffold(topBar = { TopAppBar(title = { Text("Settings") }) }) { padding ->
        Column(Modifier.padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Appearance")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ThemeMode.entries.forEach { mode ->
                    FilterChip(selected = settings.themeMode == mode, onClick = { vm.setTheme(mode) }, label = { Text(mode.name.lowercase().replaceFirstChar { it.titlecase() }) })
                }
            }
            Text("Accent palettes")
            AccentPalette.entries.chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    row.forEach { palette ->
                        FilterChip(
                            selected = settings.accent == palette,
                            onClick = { vm.setAccent(palette) },
                            label = { Text(palette.label()) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
            Button(onClick = onBusinesses, modifier = Modifier.fillMaxWidth()) { Text("Business profiles") }
            selectedId?.let { id ->
                Button(onClick = { onExcel(id) }, modifier = Modifier.fillMaxWidth()) { Text("Excel / CSV import & export") }
            }
            Text("InvoiceVault keeps every invoice, receipt, sheet, picture and note on this device. No account and no backend.")
            Text("Currency default is ZAR. VAT default is 15%.")
        }
    }
}
