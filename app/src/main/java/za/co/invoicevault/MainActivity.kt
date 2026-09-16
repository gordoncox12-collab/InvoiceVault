package za.co.invoicevault

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import za.co.invoicevault.ui.VaultViewModel
import za.co.invoicevault.ui.nav.InvoiceVaultNav
import za.co.invoicevault.ui.theme.InvoiceVaultTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as InvoiceVaultApp).container
        setContent {
            val vm: VaultViewModel = viewModel(factory = VaultViewModel.factory(container))
            val prefs by vm.prefs.collectAsStateWithLifecycle()
            InvoiceVaultTheme(themeMode = prefs.themeMode, accent = prefs.accent) {
                InvoiceVaultNav(vm)
            }
        }
    }
}
