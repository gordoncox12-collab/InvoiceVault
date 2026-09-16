package za.co.invoicevault.ui.screens

import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint
import android.graphics.Path
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import za.co.invoicevault.data.DiscountType
import za.co.invoicevault.data.Invoice
import za.co.invoicevault.data.InvoiceLine
import za.co.invoicevault.data.InvoiceStatus
import za.co.invoicevault.data.Receipt
import za.co.invoicevault.data.breakdown
import za.co.invoicevault.data.formatMoney
import za.co.invoicevault.data.isoDate
import za.co.invoicevault.data.newId
import za.co.invoicevault.data.parseDate
import za.co.invoicevault.ui.VaultViewModel
import za.co.invoicevault.ui.components.IvField
import za.co.invoicevault.ui.components.SectionCard
import za.co.invoicevault.ui.components.StatusChip

@Composable
fun InvoicesScreen(vm: VaultViewModel, onOpen: (String) -> Unit, onNew: () -> Unit) {
    val invoices by vm.invoices.collectAsStateWithLifecycle()
    var filter by remember { mutableStateOf<InvoiceStatus?>(null) }
    val shown = invoices.filter { filter == null || it.invoice.status == filter }
    Scaffold(floatingActionButton = {
        FloatingActionButton(onClick = onNew) { Icon(Icons.Outlined.Add, "New invoice") }
    }) { padding ->
        LazyColumn(Modifier.padding(padding).fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                Text("Invoices", style = MaterialTheme.typography.headlineSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(selected = filter == null, onClick = { filter = null }, label = { Text("All") })
                    InvoiceStatus.entries.forEach { st ->
                        FilterChip(selected = filter == st, onClick = { filter = st }, label = { Text(st.name) })
                    }
                }
            }
            items(shown, key = { it.invoice.id }) { item ->
                SectionCard(modifier = Modifier.clickable { onOpen(item.invoice.id) }) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text(item.invoice.number, style = MaterialTheme.typography.titleMedium)
                            Text(item.customer.name)
                        }
                        Column {
                            StatusChip(item.invoice.status)
                            Text(formatMoney(item.lines.breakdown(item.invoice).total, item.invoice.currencyCode))
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoiceEditorScreen(
    vm: VaultViewModel,
    invoiceId: String,
    customerId: String,
    onBack: () -> Unit,
    onSignature: (String) -> Unit
) {
    val biz by vm.activeBusiness.collectAsStateWithLifecycle()
    val customers by vm.customers.collectAsStateWithLifecycle()
    val templates by vm.templates.collectAsStateWithLifecycle()
    var loaded by remember { mutableStateOf(false) }
    var id by remember { mutableStateOf(if (invoiceId == "new") newId() else invoiceId) }
    var number by remember { mutableStateOf("") }
    var selectedCustomer by remember { mutableStateOf(customerId) }
    var issued by remember { mutableStateOf(isoDate(System.currentTimeMillis())) }
    var due by remember { mutableStateOf(isoDate(System.currentTimeMillis() + 14L * 86400000)) }
    var status by remember { mutableStateOf(InvoiceStatus.DRAFT) }
    var notes by remember { mutableStateOf("") }
    var discountType by remember { mutableStateOf(DiscountType.NONE) }
    var discountValue by remember { mutableStateOf("0") }
    var vatRate by remember { mutableStateOf(biz?.defaultVatRate?.toString() ?: "15") }
    var currency by remember { mutableStateOf(biz?.currencyCode ?: "ZAR") }
    var templateId by remember { mutableStateOf(biz?.defaultTemplateId.orEmpty()) }
    var signaturePath by remember { mutableStateOf("") }
    var pdfPath by remember { mutableStateOf("") }
    var lines by remember { mutableStateOf(listOf(InvoiceLine(invoiceId = id, description = "", quantity = 1.0, unitPrice = 0.0))) }

    LaunchedEffect(invoiceId, biz?.id) {
        if (invoiceId != "new") {
            val details = vm.loadInvoice(invoiceId) ?: return@LaunchedEffect
            id = details.invoice.id
            number = details.invoice.number
            selectedCustomer = details.invoice.customerId
            issued = isoDate(details.invoice.issuedOn)
            due = isoDate(details.invoice.dueOn)
            status = details.invoice.status
            notes = details.invoice.notes
            discountType = details.invoice.discountType
            discountValue = details.invoice.discountValue.toString()
            vatRate = details.invoice.vatRate.toString()
            currency = details.invoice.currencyCode
            templateId = details.invoice.templateId
            signaturePath = details.invoice.signaturePath
            pdfPath = details.invoice.pdfPath
            lines = details.lines.ifEmpty { listOf(InvoiceLine(invoiceId = details.invoice.id, description = "")) }
            loaded = true
        } else if (!loaded && biz != null && number.isBlank()) {
            number = vm.allocateInvoiceNumber()
            if (selectedCustomer.isBlank()) selectedCustomer = customers.firstOrNull()?.id.orEmpty()
            vatRate = biz?.defaultVatRate?.toString() ?: "15"
            currency = biz?.currencyCode ?: "ZAR"
            templateId = biz?.defaultTemplateId.orEmpty()
            loaded = true
        }
    }

    fun currentInvoice(): Invoice? {
        val business = biz ?: return null
        val cust = selectedCustomer.ifBlank { return null }
        return Invoice(
            id = id,
            businessId = business.id,
            customerId = cust,
            number = number,
            issuedOn = parseDate(issued),
            dueOn = parseDate(due),
            status = status,
            notes = notes,
            discountType = discountType,
            discountValue = discountValue.toDoubleOrNull() ?: 0.0,
            vatRate = vatRate.toDoubleOrNull() ?: 15.0,
            currencyCode = currency,
            signaturePath = signaturePath,
            templateId = templateId,
            pdfPath = pdfPath
        )
    }

    val draft = currentInvoice()
    val totals = draft?.let { lines.breakdown(it) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (number.isBlank()) "Invoice" else number) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back") } }
            )
        }
    ) { padding ->
        LazyColumn(Modifier.padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                IvField(number, { number = it }, "Invoice number")
                Text("Customer", style = MaterialTheme.typography.labelLarge)
                customers.forEach { c ->
                    FilterChip(
                        selected = selectedCustomer == c.id,
                        onClick = { selectedCustomer = c.id },
                        label = { Text(c.name) },
                        modifier = Modifier.padding(end = 6.dp, bottom = 4.dp)
                    )
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IvField(issued, { issued = it }, "Issued (yyyy-MM-dd)", modifier = Modifier.weight(1f))
                    IvField(due, { due = it }, "Due (yyyy-MM-dd)", modifier = Modifier.weight(1f))
                }
                Text("Status")
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    InvoiceStatus.entries.forEach { st ->
                        FilterChip(selected = status == st, onClick = { status = st }, label = { Text(st.name) })
                    }
                }
            }
            item { Text("Line items", style = MaterialTheme.typography.titleMedium) }
            items(lines.size) { index ->
                val line = lines[index]
                SectionCard {
                    IvField(line.description, { v -> lines = lines.toMutableList().also { it[index] = line.copy(description = v) } }, "Description")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IvField(line.quantity.toString(), { v -> lines = lines.toMutableList().also { it[index] = line.copy(quantity = v.toDoubleOrNull() ?: 0.0) } }, "Qty", modifier = Modifier.weight(1f))
                        IvField(line.unitPrice.toString(), { v -> lines = lines.toMutableList().also { it[index] = line.copy(unitPrice = v.toDoubleOrNull() ?: 0.0) } }, "Unit price", modifier = Modifier.weight(1f))
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(formatMoney(line.lineTotal, currency))
                        IconButton(onClick = { lines = lines.filterIndexed { i, _ -> i != index }.ifEmpty { listOf(InvoiceLine(invoiceId = id, description = "")) } }) {
                            Icon(Icons.Outlined.Delete, "Remove")
                        }
                    }
                }
            }
            item { OutlinedButton(onClick = { lines = lines + InvoiceLine(invoiceId = id, description = "") }) { Text("Add line") } }
            item {
                Text("Discount")
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    DiscountType.entries.forEach { dt ->
                        FilterChip(selected = discountType == dt, onClick = { discountType = dt }, label = { Text(dt.name) })
                    }
                }
                IvField(discountValue, { discountValue = it }, if (discountType == DiscountType.PERCENT) "Discount %" else "Discount amount")
                IvField(vatRate, { vatRate = it }, "VAT %")
                IvField(currency, { currency = it }, "Currency")
            }
            item {
                if (totals != null) {
                    SectionCard(title = "Totals") {
                        Text("Subtotal ${formatMoney(totals.subtotal, currency)}")
                        Text("Discount ${formatMoney(totals.discount, currency)}")
                        Text("VAT ${formatMoney(totals.vat, currency)}")
                        Text("Total ${formatMoney(totals.total, currency)}", style = MaterialTheme.typography.titleLarge)
                    }
                }
            }
            item {
                Text("Template")
                templates.forEach { tpl ->
                    FilterChip(selected = templateId == tpl.id, onClick = { templateId = tpl.id }, label = { Text(tpl.name) })
                }
                IvField(notes, { notes = it }, "Notes", singleLine = false, minLines = 3)
            }
            item {
                Button(onClick = { currentInvoice()?.let { vm.saveInvoice(it, lines); onBack() } }, enabled = draft != null && number.isNotBlank()) {
                    Text("Save invoice")
                }
                OutlinedButton(onClick = {
                    val inv = currentInvoice() ?: return@OutlinedButton
                    vm.saveInvoice(inv, lines)
                    onSignature(id)
                }) { Text("Capture signature") }
                OutlinedButton(onClick = {
                    val inv = currentInvoice() ?: return@OutlinedButton
                    vm.saveInvoice(inv, lines)
                    vm.generateAndShare(id, VaultViewModel.ShareKind.NONE)
                }) { Text("Generate PDF") }
                OutlinedButton(onClick = {
                    val inv = currentInvoice() ?: return@OutlinedButton
                    vm.saveInvoice(inv, lines)
                    vm.generateAndShare(id, VaultViewModel.ShareKind.WHATSAPP)
                }) { Text("Share on WhatsApp") }
                OutlinedButton(onClick = {
                    val inv = currentInvoice() ?: return@OutlinedButton
                    vm.saveInvoice(inv, lines)
                    vm.generateAndShare(id, VaultViewModel.ShareKind.EMAIL)
                }) { Text("Share by email") }
                if (signaturePath.isNotBlank()) Text("Signature on file", color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignatureScreen(vm: VaultViewModel, invoiceId: String, onBack: () -> Unit) {
    val strokes = remember { mutableStateOf(listOf<List<Offset>>()) }
    var current by remember { mutableStateOf(listOf<Offset>()) }
    var size by remember { mutableStateOf(IntSize.Zero) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sign invoice") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back") } }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Sign in the box. This is stamped onto the invoice PDF.")
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .background(Color.White)
                    .onSizeChanged { size = it }
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { offset -> current = listOf(offset) },
                            onDragEnd = {
                                strokes.value = strokes.value + listOf(current)
                                current = emptyList()
                            },
                            onDrag = { change, _ ->
                                current = current + change.position
                            }
                        )
                    }
            ) {
                Canvas(Modifier.fillMaxSize()) {
                    val all = strokes.value + listOf(current)
                    all.forEach { stroke ->
                        if (stroke.size > 1) {
                            drawPath(
                                path = androidx.compose.ui.graphics.Path().apply {
                                    moveTo(stroke.first().x, stroke.first().y)
                                    stroke.drop(1).forEach { lineTo(it.x, it.y) }
                                },
                                color = Color.Black,
                                style = Stroke(width = 5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                            )
                        }
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { strokes.value = emptyList(); current = emptyList() }) { Text("Clear") }
                Button(onClick = {
                    val w = size.width.coerceAtLeast(1)
                    val h = size.height.coerceAtLeast(1)
                    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                    val canvas = AndroidCanvas(bmp)
                    canvas.drawColor(android.graphics.Color.WHITE)
                    val paint = Paint().apply {
                        color = android.graphics.Color.BLACK
                        style = Paint.Style.STROKE
                        strokeWidth = 6f
                        isAntiAlias = true
                        strokeCap = Paint.Cap.ROUND
                        strokeJoin = Paint.Join.ROUND
                    }
                    (strokes.value + listOf(current)).forEach { stroke ->
                        if (stroke.size > 1) {
                            val p = Path()
                            p.moveTo(stroke.first().x, stroke.first().y)
                            stroke.drop(1).forEach { p.lineTo(it.x, it.y) }
                            canvas.drawPath(p, paint)
                        }
                    }
                    vm.saveSignature(invoiceId, bmp)
                    onBack()
                }) { Text("Stamp on invoice") }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiptEditorScreen(vm: VaultViewModel, id: String, customerId: String, onBack: () -> Unit) {
    val biz by vm.activeBusiness.collectAsStateWithLifecycle()
    val receipts by vm.receipts.collectAsStateWithLifecycle()
    val invoices by vm.invoices.collectAsStateWithLifecycle()
    val existing = receipts.firstOrNull { it.id == id }
    val customers by vm.customers.collectAsStateWithLifecycle()
    var number by remember { mutableStateOf(existing?.number ?: "RCP-${System.currentTimeMillis() % 100000}") }
    var amount by remember { mutableStateOf(existing?.amount?.toString() ?: "") }
    var method by remember { mutableStateOf(existing?.method ?: "EFT") }
    var notes by remember { mutableStateOf(existing?.notes.orEmpty()) }
    var date by remember { mutableStateOf(isoDate(existing?.receivedOn ?: System.currentTimeMillis())) }
    var selectedCustomer by remember { mutableStateOf(existing?.customerId ?: customerId) }
    var invoiceLink by remember { mutableStateOf(existing?.invoiceId.orEmpty()) }
    var imagePath by remember { mutableStateOf(existing?.imagePath.orEmpty()) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        val c = customers.firstOrNull { it.id == selectedCustomer } ?: return@rememberLauncherForActivityResult
        uri ?: return@rememberLauncherForActivityResult
        vm.importMedia(uri, c, za.co.invoicevault.data.VaultFolder.RECEIPTS, "receipt-${number}.jpg", za.co.invoicevault.data.FileKind.RECEIPT, "image/*")
        imagePath = uri.toString()
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Receipt") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back") } }
            )
        }
    ) { padding ->
        LazyColumn(Modifier.padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                IvField(number, { number = it }, "Receipt number")
                Text("Customer")
                customers.forEach { c ->
                    FilterChip(selected = selectedCustomer == c.id, onClick = { selectedCustomer = c.id }, label = { Text(c.name) })
                }
                IvField(amount, { amount = it }, "Amount")
                IvField(method, { method = it }, "Method")
                IvField(date, { date = it }, "Date (yyyy-MM-dd)")
                Text("Linked invoice")
                FilterChip(selected = invoiceLink.isBlank(), onClick = { invoiceLink = "" }, label = { Text("None") })
                invoices.filter { it.invoice.customerId == selectedCustomer }.forEach { inv ->
                    FilterChip(selected = invoiceLink == inv.invoice.id, onClick = { invoiceLink = inv.invoice.id }, label = { Text(inv.invoice.number) })
                }
                IvField(notes, { notes = it }, "Notes", singleLine = false, minLines = 3)
                OutlinedButton(onClick = { picker.launch("image/*") }) { Text("Attach photo / scan") }
                Button(onClick = {
                    val business = biz ?: return@Button
                    val receipt = Receipt(
                        id = if (id == "new") newId() else id,
                        businessId = business.id,
                        customerId = selectedCustomer,
                        invoiceId = invoiceLink,
                        number = number,
                        receivedOn = parseDate(date),
                        amount = amount.toDoubleOrNull() ?: 0.0,
                        method = method,
                        notes = notes,
                        imagePath = imagePath
                    )
                    vm.saveReceipt(receipt, generatePdf = true)
                    onBack()
                }, enabled = selectedCustomer.isNotBlank() && number.isNotBlank()) { Text("Save receipt + PDF") }
                OutlinedButton(onClick = {
                    existing?.let { vm.shareReceipt(it, true) }
                }) { Text("WhatsApp") }
                OutlinedButton(onClick = {
                    existing?.let { vm.shareReceipt(it, false) }
                }) { Text("Email") }
            }
        }
    }
}
