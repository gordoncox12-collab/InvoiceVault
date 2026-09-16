package com.gordoncox.invoicevault

import com.gordoncox.invoicevault.data.entity.AccentPalette
import com.gordoncox.invoicevault.data.entity.LogoAlignment
import com.gordoncox.invoicevault.data.entity.MarginPreset
import com.gordoncox.invoicevault.data.entity.PicturePlacement
import com.gordoncox.invoicevault.data.entity.TemplateLayout
import com.gordoncox.invoicevault.data.entity.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TemplateLayoutTest {
    @Test
    fun adjustableLayoutsArePresent() {
        val names = TemplateLayout.entries.map { it.name }.toSet()
        assertTrue(names.containsAll(setOf("CLASSIC", "MODERN", "COMPACT", "LETTERHEAD", "MINIMAL")))
    }

    @Test
    fun layoutKnobsExist() {
        assertEquals(3, LogoAlignment.entries.size)
        assertEquals(3, MarginPreset.entries.size)
        assertEquals(3, PicturePlacement.entries.size)
        assertEquals(3, ThemeMode.entries.size)
        assertTrue(AccentPalette.entries.size >= 8)
    }
}
