package dev.yaqmc.amll.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class LineLayoutTest {
    @Test fun convertsCssEmGeometryFromBaseFont() {
        val layout = resolveAMLLLineLayoutPx(baseFontPx = 40f)

        assertEquals(16f, layout.groupVerticalPaddingPx, 0.0001f)
        assertEquals(12f, layout.groupContentGapPx, 0.0001f)
        assertEquals(20f, layout.subLineFontPx, 0.0001f)
        assertEquals(30f, layout.subLineHeightPx, 0.0001f)
    }

    @Test fun negativeBaseFontIsSafelyClamped() {
        val layout = resolveAMLLLineLayoutPx(baseFontPx = -10f)

        assertEquals(0f, layout.groupVerticalPaddingPx, 0.0001f)
        assertEquals(0f, layout.groupContentGapPx, 0.0001f)
        assertEquals(0f, layout.subLineFontPx, 0.0001f)
        assertEquals(0f, layout.subLineHeightPx, 0.0001f)
    }

    @Test fun duetLayoutReservesFifteenPercentOppositeSpeakerInset() {
        assertEquals(0.85f, lyricContentWidthFraction(hasDuetLine = true), 0.0001f)
        assertEquals(1f, lyricContentWidthFraction(hasDuetLine = false), 0.0001f)
    }
}
