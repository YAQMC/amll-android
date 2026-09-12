package dev.yaqmc.amll.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class HorizontalPaddingTest {
    @Test fun narrowViewportUsesTwentyDp() {
        assertEquals(
            40f,
            resolveAMLLHorizontalPaddingPx(
                viewportWidthPx = 1_000,
                density = 2f,
                lineFontSizePx = 68f,
            ),
            0.0001f,
        )
    }

    @Test fun wideViewportUsesOneEm() {
        assertEquals(
            68f,
            resolveAMLLHorizontalPaddingPx(
                viewportWidthPx = 1_004,
                density = 2f,
                lineFontSizePx = 68f,
            ),
            0.0001f,
        )
    }

    @Test fun fixedOverrideWins() {
        assertEquals(
            24f,
            resolveAMLLHorizontalPaddingPx(
                viewportWidthPx = 2_000,
                density = 2f,
                lineFontSizePx = 68f,
                fixedPaddingPx = 24f,
            ),
            0.0001f,
        )
    }
}
