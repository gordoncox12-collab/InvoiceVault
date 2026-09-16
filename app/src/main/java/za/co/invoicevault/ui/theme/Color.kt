package za.co.invoicevault.ui.theme

import androidx.compose.ui.graphics.Color
import za.co.invoicevault.domain.AccentPalette

data class PaletteColors(
    val primary: Color,
    val onPrimary: Color,
    val secondary: Color,
    val tertiary: Color
)

fun AccentPalette.colors(): PaletteColors = when (this) {
    AccentPalette.CAPE_GOLD -> PaletteColors(Color(0xFFC4A35A), Color(0xFF1B1404), Color(0xFF123524), Color(0xFF8C6B2F))
    AccentPalette.PROTEA_PINK -> PaletteColors(Color(0xFFC73E66), Color(0xFFFFFFFF), Color(0xFF5C1A33), Color(0xFFE8A0B4))
    AccentPalette.OCEAN_TEAL -> PaletteColors(Color(0xFF0E7C7B), Color(0xFFFFFFFF), Color(0xFF123524), Color(0xFF5CC8C6))
    AccentPalette.FYNBOS_GREEN -> PaletteColors(Color(0xFF2D6A4F), Color(0xFFFFFFFF), Color(0xFF081C15), Color(0xFF95D5B2))
    AccentPalette.MOUNTAIN_SLATE -> PaletteColors(Color(0xFF3D5A80), Color(0xFFFFFFFF), Color(0xFF1D2D44), Color(0xFF98C1D9))
    AccentPalette.SUNSET_ORANGE -> PaletteColors(Color(0xFFE76F51), Color(0xFF2B0D05), Color(0xFF9C2F1A), Color(0xFFF4A261))
}

fun AccentPalette.label(): String = when (this) {
    AccentPalette.CAPE_GOLD -> "Cape Gold"
    AccentPalette.PROTEA_PINK -> "Protea Pink"
    AccentPalette.OCEAN_TEAL -> "Ocean Teal"
    AccentPalette.FYNBOS_GREEN -> "Fynbos Green"
    AccentPalette.MOUNTAIN_SLATE -> "Mountain Slate"
    AccentPalette.SUNSET_ORANGE -> "Sunset Orange"
}
