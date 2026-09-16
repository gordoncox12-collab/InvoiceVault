package za.co.invoicevault.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import za.co.invoicevault.data.db.InvoiceEntity
import za.co.invoicevault.domain.InvoiceStatus
import za.co.invoicevault.domain.LineAmount
import za.co.invoicevault.domain.MoneyCalculator
import za.co.invoicevault.domain.MoneyFormat
import za.co.invoicevault.ui.components.DropdownField
import za.co.invoicevault.ui.components.SectionCard
import za.co.invoicevault.ui.components.StatRow
import za.co.invoicevault.ui.components.StatusChip
import za.co.invoicevault.ui.vm.MainViewModel
import java.math.BigDecimal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    vm: MainViewModel,
    onNewInvoice: (Long) -> Unit,
    onOpenInvoice: (Long) -> Unit
) {
    val businesses by vm.businesses.collectAsStateWithLifecycle()
    val invoices by vm.invoices.collectAsStateWithLifecycle()
    val customers by vm.customers.collectAsStateWithLifecycle()
    val selectedId by vm.selectedBusinessId.collectAsStateWithLifecycle()
    val selected = businesses.firstOrNull { it.id == selectedId }
    val overdue = invoices.count { it.status == InvoiceStatus.OVERDUE }
    val paid = invoices.count { it.status == InvoiceStatus.PAID }
    val draft = invoices.count { it.status == InvoiceStatus.DRAFT }

    Scaffold(
        topBar = { TopAppBar(title = { Text("InvoiceVault") }) },
        floatingActionButton = {
            if (selected != null) {
                FloatingActionButton(onClick = { onNewInvoice(selected.id) }) {
                    Icon(Icons.Default.Add, contentDescription = "New invoice")
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                DropdownField(
                    label = "Active business",
                    options = businesses,
                    selected = selected,
                    onSelect = vm::selectBusiness,
                    print = { it.displayName }
                )
            }
            item {
                StatRow(
                    listOf(
                        "Drafts" to draft.toString(),
                        "Paid" to paid.toString(),
                        "Overdue" to overdue.toString()
                    )
                )
            }
            item {
                SectionCard(title = selected?.displayName ?: "Cape Town workspace") {
                    Text(selected?.let { "${it.city}, ${it.province} · VAT ${it.vatNumber.ifBlank { "n/a" }}" } ?: "Sample businesses load on first launch.")
                    Text("${customers.size} customers · ${invoices.size} invoices · currency ${selected?.defaultCurrency ?: "ZAR"}")
                }
            }
            item { Text("Recent invoices") }
            items(invoices.take(8), key = { it.id }) { invoice ->
                InvoiceRow(invoice, onOpenInvoice)
            }
        }
    }
}

@Composable
fun InvoiceListScreen(
    vm: MainViewModel,
    onOpen: (Long) -> Unit,
    onNew: (Long) -> Unit
) {
    val invoices by vm.invoices.collectAsStateWithLifecycle()
    val selectedId by vm.selectedBusinessId.collectAsStateWithLifecycle()
    Scaffold(
        topBar = { TopAppBar(title = { Text("Invoices") }) },
        floatingActionButton = {
            selectedId?.let { id ->
                FloatingActionButton(onClick = { onNew(id) }) { Icon(Icons.Default.Add, null) }
            }
        }
    ) { padding ->
        LazyColumn(Modifier.padding(padding).fillMaxSize()) {
            items(invoices, key = { it.id }) { InvoiceRow(it, onOpen) }
        }
    }
}

@Composable
fun InvoiceRow(invoice: InvoiceEntity, onOpen: (Long) -> Unit) {
    ListItem(
        headlineContent = { Text(invoice.number) },
        supportingContent = { Text(invoice.currency) },
        trailingContent = { StatusChip(invoice.status) },
        modifier = Modifier.fillMaxWidth().clickable { onOpen(invoice.id) }
    )
}

fun invoiceTotalPreview(lines: List<LineAmount>, vat: Double = 0.15): String {
    val totals = MoneyCalculator.totals(lines, za.co.invoicevault.domain.DiscountType.NONE, BigDecimal.ZERO, BigDecimal.valueOf(vat))
    return MoneyFormat.currency(totals.total)
}
