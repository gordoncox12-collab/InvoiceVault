package za.co.invoicevault.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import za.co.invoicevault.data.AccentPalette
import za.co.invoicevault.data.ThemeMode

private val Teal = Palette(Color(0xFF0F6C5C), Color(0xFF1F8A70), Color(0xFFC9A227))
private val Mountain = Palette(Color(0xFF1D4E89), Color(0xFF3A7CA5), Color(0xFFD4A373))
private val Protea = Palette(Color(0xFF8C1C3F), Color(0xFFC44569), Color(0xFFE8A87C))
private val Gold = Palette(Color(0xFF7A5C14), Color(0xFFC9A227), Color(0xFF1F4E5F))
private val Forest = Palette(Color(0xFF1B4332), Color(0xFF2D6A4F), Color(0xFF95D5B2))
private val Sunset = Palette(Color(0xFF9A3412), Color(0xFFEA580C), Color(0xFFFBBF24))

private data class Palette(val primary: Color, val secondary: Color, val tertiary: Color)

private fun AccentPalette.toPalette(): Palette = when (this) {
    AccentPalette.CAPE_TEAL -> Teal
    AccentPalette.TABLE_MOUNTAIN -> Mountain
    AccentPalette.PROTEA -> Protea
    AccentPalette.GOLD -> Gold
    AccentPalette.FOREST -> Forest
    AccentPalette.SUNSET -> Sunset
}

fun AccentPalette.label(): String = when (this) {
    AccentPalette.CAPE_TEAL -> "Cape Teal"
    AccentPalette.TABLE_MOUNTAIN -> "Table Mountain"
    AccentPalette.PROTEA -> "Protea"
    AccentPalette.GOLD -> "Winelands Gold"
    AccentPalette.FOREST -> "Kirstenbosch"
    AccentPalette.SUNSET -> "Camps Bay Sunset"
}

private fun light(p: Palette) = lightColorScheme(
    primary = p.primary,
    onPrimary = Color.White,
    primaryContainer = p.primary.copy(alpha = 0.15f),
    onPrimaryContainer = p.primary,
    secondary = p.secondary,
    onSecondary = Color.White,
    tertiary = p.tertiary,
    background = Color(0xFFF7F4EE),
    onBackground = Color(0xFF1A1C19),
    surface = Color(0xFFFFFCF7),
    onSurface = Color(0xFF1A1C19),
    surfaceVariant = Color(0xFFE7E3D8),
    outline = Color(0xFF7A766C)
)

private fun dark(p: Palette) = darkColorScheme(
    primary = p.secondary,
    onPrimary = Color(0xFF00332B),
    primaryContainer = p.primary,
    onPrimaryContainer = Color.White,
    secondary = p.tertiary,
    onSecondary = Color(0xFF1A1203),
    tertiary = p.tertiary,
    background = Color(0xFF121412),
    onBackground = Color(0xFFE4E2DC),
    surface = Color(0xFF1C1E1C),
    onSurface = Color(0xFFE4E2DC),
    surfaceVariant = Color(0xFF454742),
    outline = Color(0xFF8E928A)
)

private val Typography = Typography(
    headlineLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 30.sp),
    headlineSmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 22.sp),
    titleLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 20.sp),
    titleMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 16.sp),
    bodyLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 16.sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 14.sp),
    labelLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 14.sp)
)

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
    val palette = accent.toPalette()
    val scheme: ColorScheme = if (dark) dark(palette) else light(palette)
    MaterialTheme(colorScheme = scheme, typography = Typography, content = content)
}
