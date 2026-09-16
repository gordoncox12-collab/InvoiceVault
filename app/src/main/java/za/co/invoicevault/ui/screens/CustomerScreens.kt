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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import za.co.invoicevault.data.Customer
import za.co.invoicevault.data.CustomerNote
import za.co.invoicevault.data.FileKind
import za.co.invoicevault.data.VaultFolder
import za.co.invoicevault.data.breakdown
import za.co.invoicevault.data.formatDate
import za.co.invoicevault.data.formatMoney
import za.co.invoicevault.data.newId
import za.co.invoicevault.ui.VaultViewModel
import za.co.invoicevault.ui.components.IvField
import za.co.invoicevault.ui.components.SectionCard
import za.co.invoicevault.ui.components.StatusChip

@Composable
fun CustomersScreen(vm: VaultViewModel, onOpen: (String) -> Unit, onEdit: (String) -> Unit, onNew: () -> Unit) {
    val customers by vm.customers.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }
    val filtered = customers.filter { it.name.contains(query, true) || it.email.contains(query, true) }
    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = onNew) { Icon(Icons.Outlined.Add, "Add customer") }
        }
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Text("Customers", style = MaterialTheme.typography.headlineSmall)
                IvField(query, { query = it }, "Search")
            }
            items(filtered, key = { it.id }) { customer ->
                SectionCard(modifier = Modifier.clickable { onOpen(customer.id) }) {
                    Text(customer.name, style = MaterialTheme.typography.titleMedium)
                    if (customer.email.isNotBlank()) Text(customer.email, style = MaterialTheme.typography.bodyMedium)
                    if (customer.phone.isNotBlank()) Text(customer.phone, style = MaterialTheme.typography.bodySmall)
                    TextButton(onClick = { onEdit(customer.id) }) { Text("Edit") }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerEditScreen(vm: VaultViewModel, id: String, onBack: () -> Unit) {
    val biz by vm.activeBusiness.collectAsStateWithLifecycle()
    val customers by vm.customers.collectAsStateWithLifecycle()
    val existing = customers.firstOrNull { it.id == id }
    var name by remember { mutableStateOf(existing?.name.orEmpty()) }
    var email by remember { mutableStateOf(existing?.email.orEmpty()) }
    var phone by remember { mutableStateOf(existing?.phone.orEmpty()) }
    var address by remember { mutableStateOf(existing?.address.orEmpty()) }
    var vat by remember { mutableStateOf(existing?.vatNumber.orEmpty()) }
    var notes by remember { mutableStateOf(existing?.notes.orEmpty()) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (id == "new") "New customer" else "Edit customer") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back") } }
            )
        }
    ) { padding ->
        LazyColumn(Modifier.padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item { IvField(name, { name = it }, "Name") }
            item { IvField(email, { email = it }, "Email") }
            item { IvField(phone, { phone = it }, "Phone") }
            item { IvField(vat, { vat = it }, "VAT number") }
            item { IvField(address, { address = it }, "Address", singleLine = false, minLines = 3) }
            item { IvField(notes, { notes = it }, "Notes", singleLine = false, minLines = 3) }
            item {
                androidx.compose.material3.Button(
                    onClick = {
                        val businessId = biz?.id ?: return@Button
                        vm.saveCustomer(
                            Customer(
                                id = if (id == "new") newId() else id,
                                businessId = businessId,
                                name = name,
                                email = email,
                                phone = phone,
                                address = address,
                                vatNumber = vat,
                                notes = notes,
                                createdAt = existing?.createdAt ?: System.currentTimeMillis()
                            )
                        )
                        onBack()
                    },
                    enabled = name.isNotBlank()
                ) { Text("Save customer") }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerFolderScreen(
    vm: VaultViewModel,
    customerId: String,
    onBack: () -> Unit,
    onEditCustomer: () -> Unit,
    onInvoice: (String) -> Unit,
    onNewInvoice: () -> Unit,
    onReceipt: (String) -> Unit,
    onNewReceipt: () -> Unit
) {
    val customers by vm.customers.collectAsStateWithLifecycle()
    val customer = customers.firstOrNull { it.id == customerId }
    val invoices by vm.invoicesFor(customerId).collectAsStateWithLifecycle()
    val receipts by vm.receiptsFor(customerId).collectAsStateWithLifecycle()
    val notes by vm.notesFor(customerId).collectAsStateWithLifecycle()
    val files by vm.filesFor(customerId).collectAsStateWithLifecycle()
    var tab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Invoices", "Receipts", "Files", "Notes")
    val context = LocalContext.current
    val pickFile = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        val c = customer ?: return@rememberLauncherForActivityResult
        uri ?: return@rememberLauncherForActivityResult
        val name = uri.lastPathSegment?.substringAfterLast('/').orEmpty().ifBlank { "file-${System.currentTimeMillis()}" }
        val mime = context.contentResolver.getType(uri) ?: "application/octet-stream"
        val (folder, kind) = when {
            mime.contains("sheet") || name.endsWith(".xlsx") || name.endsWith(".xls") || name.endsWith(".csv") ->
                VaultFolder.EXCEL to FileKind.EXCEL
            mime.startsWith("image") -> VaultFolder.IMAGES to FileKind.IMAGE
            mime.contains("pdf") -> VaultFolder.INVOICES to FileKind.INVOICE_PDF
            else -> VaultFolder.IMAGES to FileKind.OTHER
        }
        vm.importMedia(uri, c, folder, name, kind, mime)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(customer?.name ?: "Customer") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back") } },
                actions = { TextButton(onClick = onEditCustomer) { Text("Edit") } }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            Text(
                customer?.address?.ifBlank { "No address on file" }.orEmpty(),
                modifier = Modifier.padding(horizontal = 16.dp),
                style = MaterialTheme.typography.bodyMedium
            )
            ScrollableTabRow(selectedTabIndex = tab) {
                tabs.forEachIndexed { index, label ->
                    Tab(selected = tab == index, onClick = { tab = index }, text = { Text(label) })
                }
            }
            when (tab) {
                0 -> LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    item { OutlinedButton(onClick = onNewInvoice) { Text("New invoice for this customer") } }
                    items(invoices, key = { it.invoice.id }) { inv ->
                        SectionCard(modifier = Modifier.clickable { onInvoice(inv.invoice.id) }) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column {
                                    Text(inv.invoice.number, style = MaterialTheme.typography.titleMedium)
                                    Text(formatDate(inv.invoice.issuedOn))
                                }
                                Column {
                                    StatusChip(inv.invoice.status)
                                    Text(formatMoney(inv.lines.breakdown(inv.invoice).total, inv.invoice.currencyCode))
                                }
                            }
                        }
                    }
                }
                1 -> LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    item { OutlinedButton(onClick = onNewReceipt) { Text("Record receipt") } }
                    items(receipts, key = { it.id }) { rec ->
                        SectionCard(modifier = Modifier.clickable { onReceipt(rec.id) }) {
                            Text(rec.number, style = MaterialTheme.typography.titleMedium)
                            Text("${formatMoney(rec.amount)} · ${rec.method} · ${formatDate(rec.receivedOn)}")
                            if (rec.notes.isNotBlank()) Text(rec.notes)
                        }
                    }
                }
                2 -> LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { pickFile.launch("*/*") }) { Text("Add file") }
                            OutlinedButton(onClick = {
                                val c = customer ?: return@OutlinedButton
                                pasteClipboardImage(context)?.let { uri ->
                                    vm.importMedia(uri, c, VaultFolder.IMAGES, "pasted-${System.currentTimeMillis()}.png", FileKind.IMAGE, "image/*")
                                }
                            }) { Text("Paste image") }
                        }
                    }
                    items(files, key = { it.id }) { file ->
                        SectionCard {
                            Text(file.displayName, style = MaterialTheme.typography.titleMedium)
                            Text("${file.kind} · ${formatDate(file.createdAt)}")
                            Text(file.path, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                        }
                    }
                }
                else -> {
                    var title by remember { mutableStateOf("") }
                    var body by remember { mutableStateOf("") }
                    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            IvField(title, { title = it }, "Note title")
                            IvField(body, { body = it }, "Note", singleLine = false, minLines = 4)
                            OutlinedButton(onClick = {
                                val c = customer ?: return@OutlinedButton
                                if (title.isBlank()) return@OutlinedButton
                                vm.saveNote(CustomerNote(businessId = c.businessId, customerId = c.id, title = title, body = body))
                                title = ""; body = ""
                            }) { Text("Save note to folder") }
                        }
                        items(notes, key = { it.id }) { note ->
                            SectionCard {
                                Text(note.title, style = MaterialTheme.typography.titleMedium)
                                Text(note.body)
                                Text(formatDate(note.updatedAt), style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun pasteClipboardImage(context: Context): Uri? {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = clipboard.primaryClip ?: return null
    if (clip.itemCount == 0) return null
    val desc = clip.description
    return if (desc.hasMimeType(ClipDescription.MIMETYPE_TEXT_URILIST) || desc.hasMimeType("image/*")) {
        clip.getItemAt(0).uri
    } else clip.getItemAt(0).uri
}
