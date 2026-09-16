package za.co.invoicevault

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import za.co.invoicevault.ui.screens.BusinessEditScreen
import za.co.invoicevault.ui.screens.BusinessListScreen
import za.co.invoicevault.ui.screens.CustomerEditScreen
import za.co.invoicevault.ui.screens.CustomerFolderScreen
import za.co.invoicevault.ui.screens.CustomerListScreen
import za.co.invoicevault.ui.screens.ExcelScreen
import za.co.invoicevault.ui.screens.HomeScreen
import za.co.invoicevault.ui.screens.InvoiceEditorScreen
import za.co.invoicevault.ui.screens.InvoiceListScreen
import za.co.invoicevault.ui.screens.SettingsScreen
import za.co.invoicevault.ui.screens.TemplateEditScreen
import za.co.invoicevault.ui.screens.TemplateListScreen
import za.co.invoicevault.ui.theme.InvoiceVaultTheme
import za.co.invoicevault.ui.vm.MainViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val vm: MainViewModel = viewModel()
            val settings by vm.settings.collectAsStateWithLifecycle()
            InvoiceVaultTheme(themeMode = settings.themeMode, accent = settings.accent) {
                InvoiceVaultNav(vm)
            }
        }
    }
}

private data class Tab(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

@Composable
private fun InvoiceVaultNav(vm: MainViewModel) {
    val nav = rememberNavController()
    val tabs = listOf(
        Tab("home", "Home", Icons.Default.Home),
        Tab("invoices", "Invoices", Icons.Default.Description),
        Tab("customers", "Customers", Icons.Default.People),
        Tab("templates", "Templates", Icons.Default.Palette),
        Tab("settings", "Settings", Icons.Default.Settings)
    )
    val backStack by nav.currentBackStackEntryAsState()
    val current = backStack?.destination?.route.orEmpty()
    val showBar = tabs.any { current == it.route }

    Scaffold(
        bottomBar = {
            if (showBar) {
                NavigationBar {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = current == tab.route,
                            onClick = {
                                nav.navigate(tab.route) {
                                    popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(navController = nav, startDestination = "home", modifier = Modifier.padding(padding)) {
            composable("home") {
                HomeScreen(
                    vm = vm,
                    onNewInvoice = { businessId -> nav.navigate("invoice/0?businessId=$businessId&customerId=0") },
                    onOpenInvoice = { id -> nav.navigate("invoice/$id?businessId=0&customerId=0") }
                )
            }
            composable("invoices") {
                InvoiceListScreen(
                    vm = vm,
                    onOpen = { id -> nav.navigate("invoice/$id?businessId=0&customerId=0") },
                    onNew = { businessId -> nav.navigate("invoice/0?businessId=$businessId&customerId=0") }
                )
            }
            composable("customers") {
                CustomerListScreen(
                    vm = vm,
                    onEdit = { id, businessId -> nav.navigate("customer/$id?businessId=$businessId") },
                    onFolder = { id -> nav.navigate("folder/$id") },
                    onNew = { businessId -> nav.navigate("customer/0?businessId=$businessId") }
                )
            }
            composable("templates") {
                TemplateListScreen(vm, onEdit = { nav.navigate("template/$it") }, onNew = { nav.navigate("template/0") })
            }
            composable("settings") {
                SettingsScreen(
                    vm = vm,
                    onBusinesses = { nav.navigate("businesses") },
                    onExcel = { nav.navigate("excel/$it") }
                )
            }
            composable("businesses") {
                BusinessListScreen(vm, onEdit = { nav.navigate("business/$it") }, onNew = { nav.navigate("business/0") })
            }
            composable("business/{id}", arguments = listOf(navArgument("id") { type = NavType.StringType })) {
                val id = it.arguments?.getString("id")?.toLongOrNull() ?: 0L
                BusinessEditScreen(vm, id) { nav.popBackStack() }
            }
            composable(
                "customer/{id}?businessId={businessId}",
                arguments = listOf(
                    navArgument("id") { type = NavType.StringType },
                    navArgument("businessId") { type = NavType.StringType; defaultValue = "0" }
                )
            ) {
                val id = it.arguments?.getString("id")?.toLongOrNull() ?: 0L
                val businessId = it.arguments?.getString("businessId")?.toLongOrNull() ?: 0L
                CustomerEditScreen(vm, id, businessId) { nav.popBackStack() }
            }
            composable("folder/{customerId}", arguments = listOf(navArgument("customerId") { type = NavType.StringType })) {
                val id = it.arguments?.getString("customerId")?.toLongOrNull() ?: 0L
                CustomerFolderScreen(vm, id, onBack = { nav.popBackStack() }, onInvoice = { inv ->
                    nav.navigate("invoice/$inv?businessId=0&customerId=0")
                })
            }
            composable(
                "invoice/{invoiceId}?businessId={businessId}&customerId={customerId}",
                arguments = listOf(
                    navArgument("invoiceId") { type = NavType.StringType },
                    navArgument("businessId") { type = NavType.StringType; defaultValue = "0" },
                    navArgument("customerId") { type = NavType.StringType; defaultValue = "0" }
                )
            ) {
                InvoiceEditorScreen(onBack = { nav.popBackStack() })
            }
            composable("template/{id}", arguments = listOf(navArgument("id") { type = NavType.StringType })) {
                val id = it.arguments?.getString("id")?.toLongOrNull() ?: 0L
                TemplateEditScreen(vm, id) { nav.popBackStack() }
            }
            composable("excel/{businessId}", arguments = listOf(navArgument("businessId") { type = NavType.StringType })) {
                val id = it.arguments?.getString("businessId")?.toLongOrNull() ?: 0L
                ExcelScreen(vm, id) { nav.popBackStack() }
            }
        }
    }
}
