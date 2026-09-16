package za.co.invoicevault.ui.screens

import android.content.ActivityNotFoundException
import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import za.co.invoicevault.data.db.InvoiceLineEntity
import za.co.invoicevault.domain.DiscountType
import za.co.invoicevault.domain.InvoiceStatus
import za.co.invoicevault.domain.LineAmount
import za.co.invoicevault.domain.MoneyCalculator
import za.co.invoicevault.domain.MoneyFormat
import za.co.invoicevault.ui.components.DropdownField
import za.co.invoicevault.ui.vm.InvoiceEditorViewModel
import java.io.ByteArrayOutputStream
import java.math.BigDecimal
import android.content.ClipboardManager as AndroidClipboard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoiceEditorScreen(
    onBack: () -> Unit,
    vm: InvoiceEditorViewModel = viewModel()
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val invoice = state.invoice

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbar.showSnackbar(it)
            vm.consumeMessage()
        }
    }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null && invoice != null) {
            scope.launch {
                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return@launch
                val file = vm.repository().saveLocalImage(invoice.businessId, invoice.customerId, bytes, "invoice-image.png")
                vm.attachImage(file.absolutePath)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(invoice?.number ?: "Invoice") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        if (invoice == null) {
            Text("Loading…", modifier = Modifier.padding(padding).padding(16.dp))
            return@Scaffold
        }
        val totals = MoneyCalculator.totals(
            state.lines.map { LineAmount(it.description, it.quantity.toBigDecimal(), it.unitPrice.toBigDecimal(), it.unit) },
            invoice.discountType,
            BigDecimal.valueOf(invoice.discountValue),
            BigDecimal.valueOf(invoice.vatRate)
        )
        Column(
            Modifier.padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            DropdownField("Customer", state.customers, state.customers.firstOrNull { it.id == invoice.customerId }, {
                vm.updateInvoice { inv -> inv.copy(customerId = it.id) }
            }, { it.name })
            DropdownField("Status", InvoiceStatus.entries, invoice.status, vm::setStatus, { it.name })
            DropdownField("Template", state.templates, state.templates.firstOrNull { it.id == invoice.templateId }, {
                vm.updateInvoice { inv -> inv.copy(templateId = it.id) }
            }, { "${it.name} (${it.style.name.lowercase()})" })
            OutlinedTextField(invoice.notes, { vm.updateInvoice { inv -> inv.copy(notes = it) } }, label = { Text("Notes") }, modifier = Modifier.fillMaxWidth())
            DropdownField("Discount", DiscountType.entries, invoice.discountType, {
                vm.setDiscount(it, invoice.discountValue)
            }, { it.name })
            OutlinedTextField(
                invoice.discountValue.toString(),
                { vm.setDiscount(invoice.discountType, it.toDoubleOrNull() ?: 0.0) },
                label = { Text("Discount value") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                (invoice.vatRate * 100).toString(),
                { vm.updateInvoice { inv -> inv.copy(vatRate = (it.toDoubleOrNull() ?: 15.0) / 100.0) } },
                label = { Text("VAT % (SA default 15)") },
                modifier = Modifier.fillMaxWidth()
            )
            Text("Line items")
            state.lines.forEachIndexed { index, line ->
                LineEditor(line, onChange = { updated ->
                    vm.updateLines(state.lines.mapIndexed { i, existing -> if (i == index) updated else existing })
                }, onDelete = { vm.removeLine(index) })
            }
            OutlinedButton(onClick = vm::addLine) { Icon(Icons.Default.Add, null); Text("Add line") }
            Text("Subtotal ${MoneyFormat.currency(totals.subtotal, invoice.currency)}")
            Text("Discount ${MoneyFormat.currency(totals.discount, invoice.currency)}")
            Text("VAT ${MoneyFormat.currency(totals.vat, invoice.currency)}")
            Text("Total ${MoneyFormat.currency(totals.total, invoice.currency)}")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { imagePicker.launch("image/*") }) { Icon(Icons.Default.Image, null); Text("Insert picture") }
                OutlinedButton(onClick = {
                    pasteImage(context)?.let { bytes ->
                        scope.launch {
                            val file = vm.repository().saveLocalImage(invoice.businessId, invoice.customerId, bytes, "pasted.png")
                            vm.attachImage(file.absolutePath)
                        }
                    }
                }) { Icon(Icons.Default.ContentPaste, null); Text("Paste picture") }
            }
            if (!invoice.imagePath.isNullOrBlank()) Text("Picture attached")
            if (!invoice.signaturePath.isNullOrBlank()) Text("Signature attached")
            SignaturePad(onCapture = { vm.saveSignatureBitmap(it) })
            Button(onClick = { vm.save {} }, modifier = Modifier.fillMaxWidth()) { Text("Save invoice") }
            Button(onClick = vm::renderPdf, modifier = Modifier.fillMaxWidth()) { Text("Generate PDF") }
            val pdf = state.pdfFile
            if (pdf != null) {
                Button(onClick = {
                    val intent = vm.repository().share.emailIntent(pdf, "Invoice ${invoice.number}", "Please find invoice ${invoice.number} attached.")
                    context.startActivity(intent)
                }, modifier = Modifier.fillMaxWidth()) { Text("Share via Email") }
                Button(onClick = {
                    try {
                        context.startActivity(vm.repository().share.whatsappIntent(pdf, "Invoice ${invoice.number}"))
                    } catch (_: ActivityNotFoundException) {
                        context.startActivity(vm.repository().share.whatsappChooser(pdf, "Invoice ${invoice.number}"))
                    }
                }, modifier = Modifier.fillMaxWidth()) { Text("Share via WhatsApp") }
            }
        }
    }
}

@Composable
private fun LineEditor(line: InvoiceLineEntity, onChange: (InvoiceLineEntity) -> Unit, onDelete: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(bottom = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        OutlinedTextField(line.description, { onChange(line.copy(description = it)) }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth())
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(line.quantity.toString(), { onChange(line.copy(quantity = it.toDoubleOrNull() ?: 0.0)) }, label = { Text("Qty") }, modifier = Modifier.weight(1f))
            OutlinedTextField(line.unitPrice.toString(), { onChange(line.copy(unitPrice = it.toDoubleOrNull() ?: 0.0)) }, label = { Text("Unit ex VAT") }, modifier = Modifier.weight(1f))
            OutlinedTextField(line.unit, { onChange(line.copy(unit = it)) }, label = { Text("Unit") }, modifier = Modifier.weight(1f))
            IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, contentDescription = "Remove") }
        }
    }
}

@Composable
fun SignaturePad(onCapture: (Bitmap) -> Unit) {
    val strokes = remember { mutableStateListOf<List<Offset>>() }
    var current = remember { mutableStateListOf<Offset>() }
    Text("Handwritten signature")
    Canvas(
        Modifier
            .fillMaxWidth()
            .height(140.dp)
            .background(Color.White)
            .border(1.dp, Color.Gray)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        current.clear()
                        current.add(offset)
                    },
                    onDragEnd = {
                        strokes.add(current.toList())
                        current.clear()
                    },
                    onDrag = { change, _ ->
                        current.add(change.position)
                    }
                )
            }
    ) {
        fun drawStroke(points: List<Offset>) {
            if (points.size < 2) return
            val path = Path().apply {
                moveTo(points.first().x, points.first().y)
                points.drop(1).forEach { lineTo(it.x, it.y) }
            }
            drawPath(path, Color.Black, style = Stroke(width = 4f))
        }
        strokes.forEach { drawStroke(it) }
        drawStroke(current.toList())
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = { strokes.clear(); current.clear() }) { Text("Clear") }
        Button(onClick = {
            val bitmap = android.graphics.Bitmap.createBitmap(900, 320, android.graphics.Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(bitmap)
            canvas.drawColor(android.graphics.Color.WHITE)
            val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.BLACK
                strokeWidth = 6f
                style = android.graphics.Paint.Style.STROKE
                strokeJoin = android.graphics.Paint.Join.ROUND
                strokeCap = android.graphics.Paint.Cap.ROUND
            }
            strokes.forEach { points ->
                if (points.size < 2) return@forEach
                val path = android.graphics.Path()
                path.moveTo(points.first().x * 2f, points.first().y * 2f)
                points.drop(1).forEach { path.lineTo(it.x * 2f, it.y * 2f) }
                canvas.drawPath(path, paint)
            }
            onCapture(bitmap)
        }) { Text("Stamp signature") }
    }
}

private fun pasteImage(context: Context): ByteArray? {
    val clipboard = context.getSystemService(AndroidClipboard::class.java) ?: return null
    val clip = clipboard.primaryClip ?: return null
    if (clip.itemCount == 0) return null
    val uri = clip.getItemAt(0).uri ?: return null
    return context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
}

@Suppress("DEPRECATION")
fun decodeBitmap(context: Context, uri: Uri): Bitmap {
    return if (Build.VERSION.SDK_INT >= 28) {
        ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri))
    } else {
        MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
    }
}
