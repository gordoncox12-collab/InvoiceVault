package com.gordoncox.invoicevault.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.gordoncox.invoicevault.data.entity.AccentPalette
import com.gordoncox.invoicevault.data.entity.ThemeMode

data class Palette(
    val name: String,
    val key: AccentPalette,
    val primary: Color,
    val secondary: Color,
    val tertiary: Color,
)

val Palettes = listOf(
    Palette("Teal Vault", AccentPalette.TEAL_VAULT, Color(0xFF0F6E56), Color(0xFFC9A227), Color(0xFF3B6D11)),
    Palette("Cape Ocean", AccentPalette.CAPE_OCEAN, Color(0xFF0B6E99), Color(0xFF0F6E56), Color(0xFFC9A227)),
    Palette("Fynbos", AccentPalette.FYNBOS, Color(0xFF3B6D11), Color(0xFF0F6E56), Color(0xFFB45309)),
    Palette("Pinotage", AccentPalette.PINOTAGE, Color(0xFF7A1F3D), Color(0xFFB45309), Color(0xFF0F6E56)),
    Palette("Protea Gold", AccentPalette.PROTEA_GOLD, Color(0xFFB45309), Color(0xFF7A1F3D), Color(0xFF0B6E99)),
    Palette("Table Mountain", AccentPalette.SLATE, Color(0xFF334155), Color(0xFF0B6E99), Color(0xFFC9A227)),
    Palette("Indigo Night", AccentPalette.INDIGO_NIGHT, Color(0xFF3730A3), Color(0xFF0F766E), Color(0xFFC9A227)),
    Palette("Coral Bay", AccentPalette.CORAL_BAY, Color(0xFFC2410C), Color(0xFF0F6E56), Color(0xFF1D4ED8)),
)

private fun light(p: Palette) = lightColorScheme(
    primary = p.primary,
    onPrimary = Color.White,
    primaryContainer = p.primary.copy(alpha = 0.14f),
    onPrimaryContainer = p.primary,
    secondary = p.secondary,
    onSecondary = Color.White,
    tertiary = p.tertiary,
    background = Color(0xFFF7F4EF),
    surface = Color(0xFFFFFBF7),
    surfaceVariant = Color(0xFFE7EEEA),
    onBackground = Color(0xFF1C1917),
    onSurface = Color(0xFF1C1917),
    outline = Color(0xFF8A9A94),
)

private fun dark(p: Palette) = darkColorScheme(
    primary = p.primary.copy(red = (p.primary.red + 0.18f).coerceAtMost(1f), green = (p.primary.green + 0.12f).coerceAtMost(1f)),
    onPrimary = Color(0xFF06281F),
    primaryContainer = p.primary.copy(alpha = 0.35f),
    secondary = p.secondary,
    tertiary = p.tertiary,
    background = Color(0xFF121816),
    surface = Color(0xFF1A221F),
    surfaceVariant = Color(0xFF24302C),
    onBackground = Color(0xFFF4EFE6),
    onSurface = Color(0xFFF4EFE6),
    outline = Color(0xFF7A8A84),
)

fun paletteOf(key: AccentPalette): Palette = Palettes.first { it.key == key }

@Composable
fun InvoiceVaultTheme(
    themeMode: ThemeMode,
    accent: AccentPalette,
    content: @Composable () -> Unit,
) {
    val palette = paletteOf(accent)
    val dark = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    val scheme: ColorScheme = if (dark) dark(palette) else light(palette)
    MaterialTheme(colorScheme = scheme, content = content)
}
