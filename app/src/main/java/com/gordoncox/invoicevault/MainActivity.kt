package com.gordoncox.invoicevault

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.gordoncox.invoicevault.data.entity.AccentPalette
import com.gordoncox.invoicevault.data.entity.ThemeMode
import com.gordoncox.invoicevault.ui.screens.InvoiceVaultRoot
import com.gordoncox.invoicevault.ui.theme.InvoiceVaultTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as InvoiceVaultApp
        setContent {
            val settings by app.container.repo.settings.collectAsState(
                initial = com.gordoncox.invoicevault.data.entity.AppSettingsEntity(activeBusinessId = null),
            )
            val mode = runCatching { ThemeMode.valueOf(settings.themeMode) }.getOrDefault(ThemeMode.SYSTEM)
            val accent = runCatching { AccentPalette.valueOf(settings.accentPalette) }.getOrDefault(AccentPalette.TEAL_VAULT)
            InvoiceVaultTheme(themeMode = mode, accent = accent) {
                InvoiceVaultRoot(app.container)
            }
        }
    }
}
