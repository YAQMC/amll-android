package dev.yaqmc.amll.ui

import androidx.compose.ui.text.font.FontWeight
import org.junit.Assert.assertEquals
import org.junit.Test

class TypographyStyleTest {
    @Test fun defaultLyricWeightMatchesReactFullSixHundred() {
        assertEquals(FontWeight.SemiBold, AMLLStyle().lyricFontWeight)
        assertEquals(600, AMLLStyle().lyricFontWeight.weight)
    }
}
