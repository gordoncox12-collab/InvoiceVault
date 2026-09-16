package za.co.invoicevault.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import za.co.invoicevault.domain.AccentPalette
import za.co.invoicevault.domain.ThemeMode

@Composable
fun InvoiceVaultTheme(
    themeMode: ThemeMode,
    accent: AccentPalette,
    content: @Composable () -> Unit
) {
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
    }
    val palette = accent.colors()
    val colors = if (dark) {
        darkColorScheme(
            primary = palette.primary,
            onPrimary = palette.onPrimary,
            secondary = palette.secondary,
            tertiary = palette.tertiary,
            background = Color(0xFF101410),
            surface = Color(0xFF171C17)
        )
    } else {
        lightColorScheme(
            primary = palette.primary,
            onPrimary = palette.onPrimary,
            secondary = palette.secondary,
            tertiary = palette.tertiary,
            background = Color(0xFFF7F4EC),
            surface = Color(0xFFFFFBF3)
        )
    }
    MaterialTheme(colorScheme = colors, content = content)
}
