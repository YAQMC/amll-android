package dev.yaqmc.amll.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class WordMaskGradientTest {
    @Test fun fadeWidthUsesMeasuredWordHeight() {
        val gradient = resolveWordMaskGradientPx(
            wordLeftPx = 10f,
            wordWidthPx = 100f,
            wordHeightPx = 40f,
            startTimeMs = 1_000L,
            endTimeMs = 2_000L,
            positionMs = 1_500L,
            fadeWidthFactor = 1f,
        )

        assertEquals(40f, gradient.fadeEndX - gradient.fadeStartX, 0.0001f)
        assertEquals(40f, gradient.fadeStartX, 0.0001f)
        assertEquals(80f, gradient.fadeEndX, 0.0001f)
    }

    @Test fun fadeIsHalfEnteredAtWordStartAndHalfExitedAtWordEnd() {
        val atStart = resolveWordMaskGradientPx(
            wordLeftPx = 0f,
            wordWidthPx = 100f,
            wordHeightPx = 20f,
            startTimeMs = 1_000L,
            endTimeMs = 2_000L,
            positionMs = 1_000L,
            fadeWidthFactor = 1f,
        )
        val atEnd = resolveWordMaskGradientPx(
            wordLeftPx = 0f,
            wordWidthPx = 100f,
            wordHeightPx = 20f,
            startTimeMs = 1_000L,
            endTimeMs = 2_000L,
            positionMs = 2_000L,
            fadeWidthFactor = 1f,
        )

        assertEquals(-10f, atStart.fadeStartX, 0.0001f)
        assertEquals(10f, atStart.fadeEndX, 0.0001f)
        assertEquals(90f, atEnd.fadeStartX, 0.0001f)
        assertEquals(110f, atEnd.fadeEndX, 0.0001f)
    }

    @Test fun farOutsideTimingClampsToFullyDarkOrFullyBright() {
        val before = resolveWordMaskGradientPx(
            wordLeftPx = 30f,
            wordWidthPx = 80f,
            wordHeightPx = 20f,
            startTimeMs = 1_000L,
            endTimeMs = 2_000L,
            positionMs = 0L,
            fadeWidthFactor = 1f,
        )
        val after = resolveWordMaskGradientPx(
            wordLeftPx = 30f,
            wordWidthPx = 80f,
            wordHeightPx = 20f,
            startTimeMs = 1_000L,
            endTimeMs = 2_000L,
            positionMs = 3_000L,
            fadeWidthFactor = 1f,
        )

        assertEquals(10f, before.fadeStartX, 0.0001f)
        assertEquals(30f, before.fadeEndX, 0.0001f)
        assertEquals(110f, after.fadeStartX, 0.0001f)
        assertEquals(130f, after.fadeEndX, 0.0001f)
    }
}
