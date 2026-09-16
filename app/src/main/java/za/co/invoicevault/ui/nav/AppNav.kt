package za.co.invoicevault.ui.nav

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.GridOn
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import za.co.invoicevault.ui.VaultViewModel
import za.co.invoicevault.ui.screens.BusinessEditScreen
import za.co.invoicevault.ui.screens.BusinessListScreen
import za.co.invoicevault.ui.screens.CustomerEditScreen
import za.co.invoicevault.ui.screens.CustomerFolderScreen
import za.co.invoicevault.ui.screens.CustomersScreen
import za.co.invoicevault.ui.screens.ExcelScreen
import za.co.invoicevault.ui.screens.HomeScreen
import za.co.invoicevault.ui.screens.InvoiceEditorScreen
import za.co.invoicevault.ui.screens.InvoicesScreen
import za.co.invoicevault.ui.screens.ReceiptEditorScreen
import za.co.invoicevault.ui.screens.SettingsScreen
import za.co.invoicevault.ui.screens.SignatureScreen
import za.co.invoicevault.ui.screens.TemplateEditorScreen
import za.co.invoicevault.ui.screens.TemplatesScreen

private data class Tab(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

private val tabs = listOf(
    Tab("home", "Home", Icons.Outlined.Home),
    Tab("customers", "Customers", Icons.Outlined.People),
    Tab("invoices", "Invoices", Icons.Outlined.Description),
    Tab("excel", "Excel", Icons.Outlined.GridOn),
    Tab("settings", "Settings", Icons.Outlined.Settings)
)

@Composable
fun InvoiceVaultNav(vm: VaultViewModel) {
    val nav = rememberNavController()
    val snack = remember { SnackbarHostState() }
    val message by vm.message.collectAsStateWithLifecycle()
    LaunchedEffect(message) {
        val text = message ?: return@LaunchedEffect
        snack.showSnackbar(text)
        vm.clearMessage()
    }
    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route.orEmpty()
    val showBar = tabs.any { route == it.route }

    Scaffold(
        snackbarHost = { SnackbarHost(snack) },
        bottomBar = {
            if (showBar) {
                NavigationBar {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = route == tab.route,
                            onClick = {
                                nav.navigate(tab.route) {
                                    popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.icon, tab.label) },
                            label = { Text(tab.label) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(nav, startDestination = "home", modifier = Modifier.padding(padding)) {
            composable("home") {
                HomeScreen(
                    vm = vm,
                    onCustomers = { nav.navigate("customers") },
                    onInvoices = { nav.navigate("invoices") },
                    onNewInvoice = { nav.navigate("invoice/new") },
                    onInvoice = { nav.navigate("invoice/$it") },
                    onExcel = { nav.navigate("excel") },
                    onBusinesses = { nav.navigate("businesses") }
                )
            }
            composable("customers") {
                CustomersScreen(
                    vm = vm,
                    onOpen = { nav.navigate("customer/$it") },
                    onEdit = { nav.navigate("customer_edit/$it") },
                    onNew = { nav.navigate("customer_edit/new") }
                )
            }
            composable("customer/{id}", arguments = listOf(navArgument("id") { type = NavType.StringType })) {
                val id = it.arguments?.getString("id").orEmpty()
                CustomerFolderScreen(
                    vm = vm,
                    customerId = id,
                    onBack = { nav.popBackStack() },
                    onEditCustomer = { nav.navigate("customer_edit/$id") },
                    onInvoice = { inv -> nav.navigate("invoice/$inv") },
                    onNewInvoice = { nav.navigate("invoice/new?customerId=$id") },
                    onReceipt = { rec -> nav.navigate("receipt/$rec?customerId=$id") },
                    onNewReceipt = { nav.navigate("receipt/new?customerId=$id") }
                )
            }
            composable("customer_edit/{id}", arguments = listOf(navArgument("id") { type = NavType.StringType })) {
                CustomerEditScreen(vm, it.arguments?.getString("id").orEmpty()) { nav.popBackStack() }
            }
            composable("invoices") {
                InvoicesScreen(vm, onOpen = { nav.navigate("invoice/$it") }, onNew = { nav.navigate("invoice/new") })
            }
            composable(
                "invoice/new?customerId={customerId}",
                arguments = listOf(navArgument("customerId") { type = NavType.StringType; defaultValue = "" })
            ) {
                InvoiceEditorScreen(vm, invoiceId = "new", customerId = it.arguments?.getString("customerId").orEmpty(),
                    onBack = { nav.popBackStack() },
                    onSignature = { id -> nav.navigate("signature/$id") }
                )
            }
            composable("invoice/{id}", arguments = listOf(navArgument("id") { type = NavType.StringType })) {
                val id = it.arguments?.getString("id").orEmpty()
                InvoiceEditorScreen(vm, invoiceId = id, customerId = "",
                    onBack = { nav.popBackStack() },
                    onSignature = { nav.navigate("signature/$it") }
                )
            }
            composable("signature/{id}", arguments = listOf(navArgument("id") { type = NavType.StringType })) {
                SignatureScreen(vm, it.arguments?.getString("id").orEmpty()) { nav.popBackStack() }
            }
            composable(
                "receipt/{id}?customerId={customerId}",
                arguments = listOf(
                    navArgument("id") { type = NavType.StringType },
                    navArgument("customerId") { type = NavType.StringType; defaultValue = "" }
                )
            ) {
                ReceiptEditorScreen(
                    vm,
                    it.arguments?.getString("id").orEmpty(),
                    it.arguments?.getString("customerId").orEmpty()
                ) { nav.popBackStack() }
            }
            composable("excel") { ExcelScreen(vm) }
            composable("settings") {
                SettingsScreen(
                    vm,
                    onBusinesses = { nav.navigate("businesses") },
                    onTemplates = { nav.navigate("templates") }
                )
            }
            composable("businesses") {
                BusinessListScreen(vm, onOpen = { nav.navigate("business/$it") }, onNew = { nav.navigate("business/new") }, onBack = { nav.popBackStack() })
            }
            composable("business/{id}", arguments = listOf(navArgument("id") { type = NavType.StringType })) {
                BusinessEditScreen(vm, it.arguments?.getString("id").orEmpty()) { nav.popBackStack() }
            }
            composable("templates") {
                TemplatesScreen(vm, onOpen = { nav.navigate("template/$it") }, onNew = { nav.navigate("template/new") }, onBack = { nav.popBackStack() })
            }
            composable("template/{id}", arguments = listOf(navArgument("id") { type = NavType.StringType })) {
                TemplateEditorScreen(vm, it.arguments?.getString("id").orEmpty()) { nav.popBackStack() }
            }
        }
    }
}
