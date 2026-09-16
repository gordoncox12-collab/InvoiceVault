package za.co.invoicevault.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
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
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import za.co.invoicevault.data.db.CustomerEntity
import za.co.invoicevault.data.db.FolderFileEntity
import za.co.invoicevault.domain.FolderCategory
import za.co.invoicevault.ui.vm.MainViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerFolderScreen(
    vm: MainViewModel,
    customerId: Long,
    onBack: () -> Unit,
    onInvoice: (Long) -> Unit
) {
    val repo = vm.repository()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var customer by remember { mutableStateOf<CustomerEntity?>(null) }
    var category by remember { mutableStateOf(FolderCategory.INVOICES) }
    var noteTitle by remember { mutableStateOf("Site note") }
    var noteBody by remember { mutableStateOf("") }
    val filesFlow = remember(customerId, category) { repo.folderFiles(customerId, category) }
    val files by filesFlow.collectAsStateWithLifecycle(emptyList())
    val invoices by repo.invoicesForCustomer(customerId).collectAsStateWithLifecycle(emptyList())

    LaunchedEffect(customerId) { customer = repo.customer(customerId) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        val cust = customer ?: return@rememberLauncherForActivityResult
        if (uri == null) return@rememberLauncherForActivityResult
        val name = uri.lastPathSegment?.substringAfterLast('/').orEmpty().ifBlank { "file" }
        val mime = context.contentResolver.getType(uri).orEmpty()
        scope.launch {
            repo.importUriToFolder(cust.businessId, cust.id, category, uri, name, mime)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(customer?.name ?: "Customer folder") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).padding(16.dp).fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Files stay on this phone, split by business and customer.")
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                FolderCategory.entries.forEach { cat ->
                    FilterChip(
                        selected = category == cat,
                        onClick = { category = cat },
                        label = { Text(cat.name.lowercase().replaceFirstChar { it.titlecase() }) }
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { picker.launch(arrayOf("*/*")) }) {
                    Icon(Icons.Default.UploadFile, null)
                    Text("Add file")
                }
                if (category == FolderCategory.EXCEL) {
                    Button(onClick = {
                        val cust = customer ?: return@Button
                        scope.launch { repo.exportCustomersToFolder(cust.businessId, cust.id) }
                    }) { Text("Export customers .xlsx") }
                }
            }
            if (category == FolderCategory.NOTES) {
                OutlinedTextField(noteTitle, { noteTitle = it }, label = { Text("Note title") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(noteBody, { noteBody = it }, label = { Text("Note") }, modifier = Modifier.fillMaxWidth())
                Button(onClick = {
                    val cust = customer ?: return@Button
                    scope.launch {
                        repo.saveNote(cust.businessId, cust.id, noteTitle, noteBody)
                        noteBody = ""
                    }
                }) {
                    Icon(Icons.Default.NoteAdd, null)
                    Text("Save note")
                }
            }
            if (category == FolderCategory.INVOICES) {
                invoices.forEach { inv ->
                    ListItem(
                        headlineContent = { Text(inv.number) },
                        supportingContent = { Text(inv.status.name) },
                        modifier = Modifier.clickable { onInvoice(inv.id) }
                    )
                }
            }
            LazyColumn(Modifier.weight(1f)) {
                items(files, key = { it.id }) { file ->
                    FolderRow(file, onOpen = {
                        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", File(file.filePath))
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            setDataAndType(uri, file.mimeType.ifBlank { "*/*" })
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        runCatching { context.startActivity(intent) }
                    }, onDelete = { scope.launch { repo.deleteFolderFile(file) } })
                }
            }
        }
    }
}

@Composable
private fun FolderRow(file: FolderFileEntity, onOpen: () -> Unit, onDelete: () -> Unit) {
    ListItem(
        headlineContent = { Text(file.displayName) },
        supportingContent = { Text(file.mimeType) },
        trailingContent = { Text("Delete", modifier = Modifier.clickable { onDelete() }) },
        modifier = Modifier.clickable { onOpen() }
    )
}
