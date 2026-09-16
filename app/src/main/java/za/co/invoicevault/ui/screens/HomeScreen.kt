package za.co.invoicevault.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import za.co.invoicevault.data.InvoiceStatus
import za.co.invoicevault.data.breakdown
import za.co.invoicevault.data.formatDate
import za.co.invoicevault.data.formatMoney
import za.co.invoicevault.ui.VaultViewModel
import za.co.invoicevault.ui.components.MoneyText
import za.co.invoicevault.ui.components.SectionCard
import za.co.invoicevault.ui.components.StatusChip

@Composable
fun HomeScreen(
    vm: VaultViewModel,
    onCustomers: () -> Unit,
    onInvoices: () -> Unit,
    onNewInvoice: () -> Unit,
    onInvoice: (String) -> Unit,
    onExcel: () -> Unit,
    onBusinesses: () -> Unit
) {
    val business by vm.activeBusiness.collectAsStateWithLifecycle()
    val invoices by vm.invoices.collectAsStateWithLifecycle()
    val customers by vm.customers.collectAsStateWithLifecycle()
    val outstanding by vm.outstanding.collectAsStateWithLifecycle()
    val paid = invoices.filter { it.invoice.status == InvoiceStatus.PAID }.sumOf { it.lines.breakdown(it.invoice).total }
    val overdue = invoices.count { it.invoice.status == InvoiceStatus.OVERDUE }
    val currency = business?.currencyCode ?: "ZAR"

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("InvoiceVault", style = MaterialTheme.typography.headlineLarge)
            Text("Offline invoicing · Cape Town / ${currency}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
        }
        item {
            SectionCard {
                Text(business?.displayName ?: "No business yet", style = MaterialTheme.typography.titleLarge)
                Text(business?.fullAddress?.replace("\n", " · ").orEmpty(), style = MaterialTheme.typography.bodyMedium)
                TextButton(onClick = onBusinesses) { Text("Switch or edit business") }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                SectionCard(modifier = Modifier.weight(1f), title = "Outstanding") {
                    MoneyText(outstanding, currency)
                }
                SectionCard(modifier = Modifier.weight(1f), title = "Paid") {
                    MoneyText(paid, currency)
                }
            }
        }
        item {
            SectionCard(title = "This vault") {
                Text("${customers.size} customers · ${invoices.size} invoices · $overdue overdue")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onNewInvoice) { Text("New invoice") }
                    FilledTonalButton(onClick = onCustomers) { Text("Customers") }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(onClick = onInvoices) { Text("All invoices") }
                    FilledTonalButton(onClick = onExcel) { Text("Excel import") }
                }
            }
        }
        item { Text("Recent invoices", style = MaterialTheme.typography.titleMedium) }
        items(invoices.take(8), key = { it.invoice.id }) { item ->
            SectionCard(modifier = Modifier.clickable { onInvoice(item.invoice.id) }) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(Modifier.weight(1f)) {
                        Text(item.invoice.number, style = MaterialTheme.typography.titleMedium)
                        Text(item.customer.name, style = MaterialTheme.typography.bodyMedium)
                        Text(formatDate(item.invoice.issuedOn), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                    }
                    Column {
                        StatusChip(item.invoice.status)
                        Spacer(Modifier.height(6.dp))
                        Text(formatMoney(item.lines.breakdown(item.invoice).total, currency))
                    }
                }
            }
        }
    }
}
