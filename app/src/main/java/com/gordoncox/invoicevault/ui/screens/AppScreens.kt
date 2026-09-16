package com.gordoncox.invoicevault.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.gordoncox.invoicevault.data.entity.FolderType
import com.gordoncox.invoicevault.data.entity.InvoiceEntity
import com.gordoncox.invoicevault.data.entity.InvoiceStatus
import com.gordoncox.invoicevault.di.AppContainer
import com.gordoncox.invoicevault.ui.components.EmptyState
import com.gordoncox.invoicevault.ui.components.MoneyText
import com.gordoncox.invoicevault.ui.components.SectionTitle
import com.gordoncox.invoicevault.ui.components.StatCard
import com.gordoncox.invoicevault.ui.components.StatusChip
import com.gordoncox.invoicevault.ui.nav.AppViewModel
import com.gordoncox.invoicevault.ui.nav.Routes
import com.gordoncox.invoicevault.ui.nav.businessEdit
import com.gordoncox.invoicevault.ui.nav.customerDetail
import com.gordoncox.invoicevault.ui.nav.customerEdit
import com.gordoncox.invoicevault.ui.nav.invoiceEdit
import com.gordoncox.invoicevault.ui.nav.invoiceView
import com.gordoncox.invoicevault.ui.nav.vaultModel
import com.gordoncox.invoicevault.util.Za

@Composable
fun InvoiceVaultRoot(@Suppress("UNUSED_PARAMETER") container: AppContainer) {
    val appVm: AppViewModel = vaultModel { AppViewModel(it.repo) }
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route
    val tabs = listOf(Routes.HOME, Routes.INVOICES, Routes.CUSTOMERS, Routes.MORE)
    val showBar = route in tabs
    Scaffold(
        bottomBar = {
            if (showBar) {
                NavigationBar {
                    NavigationBarItem(
                        selected = route == Routes.HOME,
                        onClick = { nav.tab(Routes.HOME) },
                        icon = { Icon(Icons.Default.Business, null) },
                        label = { Text("Home") },
                    )
                    NavigationBarItem(
                        selected = route == Routes.INVOICES,
                        onClick = { nav.tab(Routes.INVOICES) },
                        icon = { Icon(Icons.Default.Description, null) },
                        label = { Text("Invoices") },
                    )
                    NavigationBarItem(
                        selected = route == Routes.CUSTOMERS,
                        onClick = { nav.tab(Routes.CUSTOMERS) },
                        icon = { Icon(Icons.Default.People, null) },
                        label = { Text("Customers") },
                    )
                    NavigationBarItem(
                        selected = route == Routes.MORE,
                        onClick = { nav.tab(Routes.MORE) },
                        icon = { Icon(Icons.Default.MoreVert, null) },
                        label = { Text("More") },
                    )
                }
            }
        },
    ) { padding ->
        NavHost(nav, startDestination = Routes.HOME, modifier = Modifier.padding(padding)) {
            composable(Routes.HOME) { HomeScreen(appVm, nav) }
            composable(Routes.INVOICES) { InvoiceListScreen(appVm, nav) }
            composable(Routes.CUSTOMERS) { CustomerListScreen(appVm, nav) }
            composable(Routes.MORE) { MoreScreen(nav) }
            composable(Routes.SETTINGS) { SettingsScreen(appVm, nav) }
            composable(Routes.EXCEL) { ExcelHubScreen(appVm, nav) }
            composable(Routes.TEMPLATES) { TemplateListScreen(appVm, nav) }
            composable(Routes.TRANSACTIONS) { TransactionListScreen(appVm, nav) }
            composable(
                Routes.BUSINESS_EDIT,
                arguments = listOf(navArgument("id") { type = NavType.StringType }),
            ) { BusinessEditScreen(it.arguments?.getString("id") ?: "new", appVm, nav) }
            composable(
                Routes.CUSTOMER_EDIT,
                arguments = listOf(navArgument("id") { type = NavType.StringType }),
            ) { CustomerEditScreen(it.arguments?.getString("id") ?: "new", appVm, nav) }
            composable(
                Routes.CUSTOMER_DETAIL,
                arguments = listOf(navArgument("id") { type = NavType.StringType }),
            ) { CustomerDetailScreen(it.arguments?.getString("id") ?: "", appVm, nav) }
            composable(
                Routes.INVOICE_EDIT,
                arguments = listOf(navArgument("id") { type = NavType.StringType }),
            ) { InvoiceEditorScreen(it.arguments?.getString("id") ?: "new", appVm, nav) }
            composable(
                Routes.INVOICE_VIEW,
                arguments = listOf(navArgument("id") { type = NavType.StringType }),
            ) { InvoiceViewScreen(it.arguments?.getString("id") ?: "", appVm, nav) }
            composable(
                Routes.SIGNATURE,
                arguments = listOf(navArgument("id") { type = NavType.StringType }),
            ) { SignatureScreen(it.arguments?.getString("id") ?: "", appVm, nav) }
            composable(
                Routes.TEMPLATE_EDIT,
                arguments = listOf(navArgument("id") { type = NavType.StringType }),
            ) { TemplateEditScreen(it.arguments?.getString("id") ?: "new", appVm, nav) }
            composable(
                Routes.NOTE_EDIT,
                arguments = listOf(
                    navArgument("customerId") { type = NavType.StringType },
                    navArgument("noteId") { type = NavType.StringType },
                ),
            ) {
                NoteEditScreen(
                    it.arguments?.getString("customerId") ?: "",
                    it.arguments?.getString("noteId") ?: "new",
                    appVm,
                    nav,
                )
            }
            composable(
                Routes.FOLDER,
                arguments = listOf(
                    navArgument("customerId") { type = NavType.StringType },
                    navArgument("type") { type = NavType.StringType },
                ),
            ) {
                FolderScreen(
                    it.arguments?.getString("customerId") ?: "",
                    it.arguments?.getString("type") ?: FolderType.INVOICES.name,
                    appVm,
                    nav,
                )
            }
            composable(
                Routes.SHEET_CAPTURE,
                arguments = listOf(navArgument("customerId") { type = NavType.StringType }),
            ) { SheetCaptureScreen(it.arguments?.getString("customerId") ?: "", appVm, nav) }
        }
    }
}

private fun NavHostController.tab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackBar(title: String, nav: NavHostController, actions: @Composable () -> Unit = {}) {
    TopAppBar(
        title = { Text(title) },
        navigationIcon = {
            IconButton(onClick = { nav.popBackStack() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
        },
        actions = { actions() },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(appVm: AppViewModel, nav: NavHostController) {
    val business by appVm.activeBusiness.collectAsState()
    val businesses by appVm.businesses.collectAsState()
    val invoices by (business?.let { appVm.invoices(it.id) } ?: kotlinx.coroutines.flow.flowOf(emptyList()))
        .collectAsState(initial = emptyList())
    val customers by (business?.let { appVm.customers(it.id) } ?: kotlinx.coroutines.flow.flowOf(emptyList()))
        .collectAsState(initial = emptyList())
    var menu by remember { mutableStateOf(false) }
    val outstanding = invoices.filter { it.status == InvoiceStatus.SENT.name || it.status == InvoiceStatus.OVERDUE.name }.sumOf { it.total }
    val paid = invoices.filter { it.status == InvoiceStatus.PAID.name }.sumOf { it.total }
    val overdue = invoices.count { it.status == InvoiceStatus.OVERDUE.name }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(business?.name ?: "InvoiceVault", style = MaterialTheme.typography.titleMedium)
                        Text("Offline  ·  ${business?.defaultCurrency ?: "ZAR"}", style = MaterialTheme.typography.labelSmall)
                    }
                },
                actions = {
                    IconButton(onClick = { menu = true }) { Icon(Icons.Default.Business, contentDescription = "Business") }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        businesses.forEach { b ->
                            DropdownMenuItem(
                                text = { Text(b.name) },
                                onClick = { appVm.selectBusiness(b.id); menu = false },
                            )
                        }
                        DropdownMenuItem(text = { Text("New business") }, onClick = { menu = false; nav.navigate(businessEdit("new")) })
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { nav.navigate(invoiceEdit("new")) }) {
                Icon(Icons.Default.Add, contentDescription = "New invoice")
            }
        },
    ) { padding ->
        LazyColumn(Modifier.padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatCard("Outstanding", Za.money(outstanding), Modifier.weight(1f))
                    StatCard("Collected", Za.money(paid), Modifier.weight(1f))
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatCard("Overdue", overdue.toString(), Modifier.weight(1f))
                    StatCard("Customers", customers.size.toString(), Modifier.weight(1f))
                }
            }
            item { SectionTitle("Quick actions") }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                    FilledTonalButton(onClick = { nav.navigate(invoiceEdit("new")) }) { Text("New invoice") }
                    OutlinedButton(onClick = { nav.navigate(customerEdit("new")) }) { Text("Add customer") }
                    OutlinedButton(onClick = { nav.navigate(Routes.EXCEL) }) { Text("Excel") }
                    OutlinedButton(onClick = { nav.navigate(businessEdit(business?.id ?: "new")) }) { Text("Edit business") }
                }
            }
            item { SectionTitle("Recent invoices") }
            if (invoices.isEmpty()) {
                item { EmptyState("No invoices yet", "Create an invoice — it is stored on this device only.") }
            }
            items(invoices.take(8), key = { it.id }) { inv ->
                InvoiceRow(inv, customers.firstOrNull { it.id == inv.customerId }?.name) {
                    nav.navigate(invoiceView(inv.id))
                }
            }
        }
    }
}

@Composable
fun InvoiceRow(invoice: InvoiceEntity, customerName: String?, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(invoice.number, fontWeight = FontWeight.SemiBold)
                Text(customerName ?: "Customer", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(Za.date(invoice.issueDate), style = MaterialTheme.typography.labelSmall)
            }
            Column(horizontalAlignment = Alignment.End) {
                MoneyText(invoice.total, invoice.currency)
                StatusChip(invoice.status)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoiceListScreen(appVm: AppViewModel, nav: NavHostController) {
    val business by appVm.activeBusiness.collectAsState()
    val invoices by (business?.let { appVm.invoices(it.id) } ?: kotlinx.coroutines.flow.flowOf(emptyList()))
        .collectAsState(initial = emptyList())
    val customers by (business?.let { appVm.customers(it.id) } ?: kotlinx.coroutines.flow.flowOf(emptyList()))
        .collectAsState(initial = emptyList())
    var filter by rememberSaveable { mutableStateOf("All") }
    val shown = invoices.filter { filter == "All" || it.status.equals(filter, true) }
    Scaffold(
        topBar = { TopAppBar(title = { Text("Invoices") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { nav.navigate(invoiceEdit("new")) }) {
                Icon(Icons.Default.Add, null)
            }
        },
    ) { padding ->
        Column(Modifier.padding(padding).padding(16.dp)) {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("All", "DRAFT", "SENT", "PAID", "OVERDUE").forEach { opt ->
                    FilterChip(selected = filter == opt, onClick = { filter = opt }, label = { Text(opt.lowercase().replaceFirstChar { it.titlecase() }) })
                }
            }
            Spacer(Modifier.height(12.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(shown, key = { it.id }) { inv ->
                    InvoiceRow(inv, customers.firstOrNull { it.id == inv.customerId }?.name) {
                        nav.navigate(invoiceView(inv.id))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerListScreen(appVm: AppViewModel, nav: NavHostController) {
    val business by appVm.activeBusiness.collectAsState()
    val customers by (business?.let { appVm.customers(it.id) } ?: kotlinx.coroutines.flow.flowOf(emptyList()))
        .collectAsState(initial = emptyList())
    Scaffold(
        topBar = { TopAppBar(title = { Text("Customers") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { nav.navigate(customerEdit("new")) }) { Icon(Icons.Default.Add, null) }
        },
    ) { padding ->
        LazyColumn(Modifier.padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(customers, key = { it.id }) { c ->
                Card(onClick = { nav.navigate(customerDetail(c.id)) }, modifier = Modifier.fillMaxWidth()) {
                    ListItem(
                        headlineContent = { Text(c.name) },
                        supportingContent = { Text(listOfNotNull(c.city, c.phone).joinToString(" · ")) },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreScreen(nav: NavHostController) {
    Scaffold(topBar = { TopAppBar(title = { Text("More") }) }) { padding ->
        Column(Modifier.padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            MoreRow("Excel import / export", "Map columns, capture sheets, CSV/XLS/XLSX") { nav.navigate(Routes.EXCEL) }
            MoreRow("Invoice templates", "Logo, colours, layout, pictures") { nav.navigate(Routes.TEMPLATES) }
            MoreRow("Transactions", "Payments, expenses, receipts") { nav.navigate(Routes.TRANSACTIONS) }
            MoreRow("Theme & accents", "Light, dark, system and palettes") { nav.navigate(Routes.SETTINGS) }
            MoreRow("New business profile", "Separate books and folders") { nav.navigate(businessEdit("new")) }
        }
    }
}

@Composable
private fun MoreRow(title: String, subtitle: String, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        ListItem(headlineContent = { Text(title) }, supportingContent = { Text(subtitle) })
    }
}
