package za.co.invoicevault.ui.screens

import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import za.co.invoicevault.data.AccentPalette
import za.co.invoicevault.data.Business
import za.co.invoicevault.data.InvoiceTemplate
import za.co.invoicevault.data.TemplateLayout
import za.co.invoicevault.data.ThemeMode
import za.co.invoicevault.data.newId
import za.co.invoicevault.ui.VaultViewModel
import za.co.invoicevault.ui.components.IvField
import za.co.invoicevault.ui.components.SectionCard
import za.co.invoicevault.ui.theme.label

@Composable
fun SettingsScreen(vm: VaultViewModel, onBusinesses: () -> Unit, onTemplates: () -> Unit) {
    val prefs by vm.prefs.collectAsStateWithLifecycle()
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Settings", style = MaterialTheme.typography.headlineSmall) }
        item {
            SectionCard(title = "Theme") {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ThemeMode.entries.forEach { mode ->
                        FilterChip(selected = prefs.themeMode == mode, onClick = { vm.setTheme(mode) }, label = { Text(mode.name) })
                    }
                }
            }
        }
        item {
            SectionCard(title = "Accent palette") {
                AccentPalette.entries.forEach { accent ->
                    FilterChip(selected = prefs.accent == accent, onClick = { vm.setAccent(accent) }, label = { Text(accent.label()) })
                }
            }
        }
        item {
            SectionCard(title = "Business & templates") {
                Button(onClick = onBusinesses) { Text("Business profiles") }
                OutlinedButton(onClick = onTemplates) { Text("Invoice templates") }
            }
        }
        item {
            SectionCard(title = "About") {
                Text("InvoiceVault 0.1.0")
                Text("Offline-first invoicing for South African businesses. Data stays on this phone, organised into per-business and per-customer folders.")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusinessListScreen(vm: VaultViewModel, onOpen: (String) -> Unit, onNew: () -> Unit, onBack: () -> Unit) {
    val businesses by vm.businesses.collectAsStateWithLifecycle()
    val prefs by vm.prefs.collectAsStateWithLifecycle()
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Businesses") }, navigationIcon = {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back") }
            })
        }
    ) { padding ->
        LazyColumn(Modifier.padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item { Button(onClick = onNew) { Text("Add business") } }
            items(businesses, key = { it.id }) { biz ->
                SectionCard(modifier = Modifier.clickable { onOpen(biz.id) }) {
                    Text(biz.displayName, style = MaterialTheme.typography.titleMedium)
                    Text("${biz.city} · ${biz.currencyCode}")
                    if (prefs.activeBusinessId == biz.id) Text("Active vault", color = MaterialTheme.colorScheme.primary)
                    OutlinedButton(onClick = { vm.setActiveBusiness(biz.id) }) { Text("Work in this business") }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusinessEditScreen(vm: VaultViewModel, id: String, onBack: () -> Unit) {
    val businesses by vm.businesses.collectAsStateWithLifecycle()
    val existing = businesses.firstOrNull { it.id == id }
    var name by remember { mutableStateOf(existing?.name.orEmpty()) }
    var trading by remember { mutableStateOf(existing?.tradingName.orEmpty()) }
    var reg by remember { mutableStateOf(existing?.registrationNumber.orEmpty()) }
    var vat by remember { mutableStateOf(existing?.vatNumber.orEmpty()) }
    var email by remember { mutableStateOf(existing?.email.orEmpty()) }
    var phone by remember { mutableStateOf(existing?.phone.orEmpty()) }
    var a1 by remember { mutableStateOf(existing?.addressLine1.orEmpty()) }
    var a2 by remember { mutableStateOf(existing?.addressLine2.orEmpty()) }
    var city by remember { mutableStateOf(existing?.city ?: "Cape Town") }
    var province by remember { mutableStateOf(existing?.province ?: "Western Cape") }
    var code by remember { mutableStateOf(existing?.postalCode.orEmpty()) }
    var bank by remember { mutableStateOf(existing?.bankName.orEmpty()) }
    var accName by remember { mutableStateOf(existing?.accountName.orEmpty()) }
    var accNo by remember { mutableStateOf(existing?.accountNumber.orEmpty()) }
    var branch by remember { mutableStateOf(existing?.branchCode.orEmpty()) }
    var currency by remember { mutableStateOf(existing?.currencyCode ?: "ZAR") }
    var vatRate by remember { mutableStateOf(existing?.defaultVatRate?.toString() ?: "15") }
    var prefix by remember { mutableStateOf(existing?.invoicePrefix ?: "INV") }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        val biz = existing ?: return@rememberLauncherForActivityResult
        uri?.let { vm.saveLogo(it, biz.copy(name = name.ifBlank { biz.name })) }
    }
    Scaffold(topBar = {
        TopAppBar(title = { Text(if (id == "new") "New business" else "Edit business") }, navigationIcon = {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back") }
        })
    }) { padding ->
        LazyColumn(Modifier.padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item { IvField(name, { name = it }, "Legal name") }
            item { IvField(trading, { trading = it }, "Trading name") }
            item { IvField(reg, { reg = it }, "Registration number") }
            item { IvField(vat, { vat = it }, "VAT number") }
            item { IvField(email, { email = it }, "Email") }
            item { IvField(phone, { phone = it }, "Phone") }
            item { IvField(a1, { a1 = it }, "Address line 1") }
            item { IvField(a2, { a2 = it }, "Address line 2") }
            item { IvField(city, { city = it }, "City") }
            item { IvField(province, { province = it }, "Province") }
            item { IvField(code, { code = it }, "Postal code") }
            item { IvField(bank, { bank = it }, "Bank") }
            item { IvField(accName, { accName = it }, "Account name") }
            item { IvField(accNo, { accNo = it }, "Account number") }
            item { IvField(branch, { branch = it }, "Branch code") }
            item { IvField(currency, { currency = it }, "Currency") }
            item { IvField(vatRate, { vatRate = it }, "Default VAT %") }
            item { IvField(prefix, { prefix = it }, "Invoice prefix") }
            item { OutlinedButton(onClick = { picker.launch("image/*") }) { Text("Choose logo") } }
            item {
                Button(onClick = {
                    vm.saveBusiness(
                        Business(
                            id = if (id == "new") newId() else id,
                            name = name,
                            tradingName = trading,
                            registrationNumber = reg,
                            vatNumber = vat,
                            email = email,
                            phone = phone,
                            addressLine1 = a1,
                            addressLine2 = a2,
                            city = city,
                            province = province,
                            postalCode = code,
                            bankName = bank,
                            accountName = accName,
                            accountNumber = accNo,
                            branchCode = branch,
                            currencyCode = currency.ifBlank { "ZAR" },
                            defaultVatRate = vatRate.toDoubleOrNull() ?: 15.0,
                            invoicePrefix = prefix.ifBlank { "INV" },
                            nextInvoiceNumber = existing?.nextInvoiceNumber ?: 1,
                            logoPath = existing?.logoPath.orEmpty(),
                            defaultTemplateId = existing?.defaultTemplateId.orEmpty()
                        )
                    )
                    onBack()
                }, enabled = name.isNotBlank()) { Text("Save business") }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplatesScreen(vm: VaultViewModel, onOpen: (String) -> Unit, onNew: () -> Unit, onBack: () -> Unit) {
    val templates by vm.templates.collectAsStateWithLifecycle()
    Scaffold(topBar = {
        TopAppBar(title = { Text("Invoice templates") }, navigationIcon = {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back") }
        })
    }) { padding ->
        LazyColumn(Modifier.padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item { Button(onClick = onNew) { Text("New template") } }
            items(templates, key = { it.id }) { tpl ->
                SectionCard(modifier = Modifier.clickable { onOpen(tpl.id) }) {
                    Text(tpl.name, style = MaterialTheme.typography.titleMedium)
                    Text("${tpl.layout} · footer: ${tpl.footerText.take(48)}")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplateEditorScreen(vm: VaultViewModel, id: String, onBack: () -> Unit) {
    val templates by vm.templates.collectAsStateWithLifecycle()
    val biz by vm.activeBusiness.collectAsStateWithLifecycle()
    val existing = templates.firstOrNull { it.id == id }
    var name by remember { mutableStateOf(existing?.name ?: "Custom template") }
    var layout by remember { mutableStateOf(existing?.layout ?: TemplateLayout.MODERN) }
    var primary by remember { mutableStateOf(existing?.primaryColor?.toString(16) ?: "ff0b3a4a") }
    var accent by remember { mutableStateOf(existing?.accentColor?.toString(16) ?: "ff1f8a70") }
    var footer by remember { mutableStateOf(existing?.footerText.orEmpty()) }
    var showVat by remember { mutableStateOf(existing?.showVat ?: true) }
    val context = LocalContext.current
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        val template = currentTemplate(existing, id, biz?.id.orEmpty(), name, layout, primary, accent, footer, showVat)
        uri?.let { vm.saveTemplateImage(it, template, banner = false) }
    }
    val bannerPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        val template = currentTemplate(existing, id, biz?.id.orEmpty(), name, layout, primary, accent, footer, showVat)
        uri?.let { vm.saveTemplateImage(it, template, banner = true) }
    }
    Scaffold(topBar = {
        TopAppBar(title = { Text("Template") }, navigationIcon = {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back") }
        })
    }) { padding ->
        LazyColumn(Modifier.padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item { IvField(name, { name = it }, "Name") }
            item {
                Text("Layout")
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TemplateLayout.entries.forEach { item ->
                        FilterChip(selected = layout == item, onClick = { layout = item }, label = { Text(item.name) })
                    }
                }
            }
            item { IvField(primary, { primary = it }, "Primary colour (hex ARGB)") }
            item { IvField(accent, { accent = it }, "Accent colour (hex ARGB)") }
            item { IvField(footer, { footer = it }, "Footer text", singleLine = false, minLines = 3) }
            item {
                Row {
                    Text("Show VAT", modifier = Modifier.weight(1f))
                    Switch(checked = showVat, onCheckedChange = { showVat = it })
                }
            }
            item { OutlinedButton(onClick = { picker.launch("image/*") }) { Text("Insert / replace logo") } }
            item { OutlinedButton(onClick = { bannerPicker.launch("image/*") }) { Text("Insert picture / banner") } }
            item {
                OutlinedButton(onClick = {
                    val uri = pasteUri(context)
                    val template = currentTemplate(existing, id, biz?.id.orEmpty(), name, layout, primary, accent, footer, showVat)
                    uri?.let { vm.saveTemplateImage(it, template, banner = false) }
                }) { Text("Paste picture from clipboard") }
            }
            item {
                Button(onClick = {
                    vm.saveTemplate(currentTemplate(existing, id, biz?.id.orEmpty(), name, layout, primary, accent, footer, showVat))
                    onBack()
                }) { Text("Save template") }
            }
        }
    }
}

private fun currentTemplate(
    existing: InvoiceTemplate?,
    id: String,
    businessId: String,
    name: String,
    layout: TemplateLayout,
    primary: String,
    accent: String,
    footer: String,
    showVat: Boolean
) = InvoiceTemplate(
    id = if (id == "new") newId() else id,
    businessId = businessId,
    name = name,
    layout = layout,
    primaryColor = primary.toLongOrNull(16) ?: 0xFF0B3A4A,
    accentColor = accent.toLongOrNull(16) ?: 0xFF1F8A70,
    footerText = footer,
    showVat = showVat,
    logoPath = existing?.logoPath.orEmpty(),
    bannerPath = existing?.bannerPath.orEmpty()
)

private fun pasteUri(context: Context): Uri? {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = clipboard.primaryClip ?: return null
    if (clip.itemCount == 0) return null
    val desc = clip.description
    return if (desc.hasMimeType(ClipDescription.MIMETYPE_TEXT_URILIST) || desc.hasMimeType("image/*")) {
        clip.getItemAt(0).uri
    } else clip.getItemAt(0).uri
}
