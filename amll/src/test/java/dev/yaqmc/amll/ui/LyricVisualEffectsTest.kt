package dev.yaqmc.amll.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class LyricVisualEffectsTest {
    @Test
    fun focusedOrManualScrollDisablesBlur() {
        assertEquals(
            0f,
            resolveBlurRadiusPx(4, 4, 4, true, false, false, true),
            0.0001f,
        )
        assertEquals(
            0f,
            resolveBlurRadiusPx(1, 4, 4, false, true, false, true),
            0.0001f,
        )
    }

    @Test
    fun precedingLineIsOneBlurStepStrongerThanFollowingLine() {
        val previous = resolveBlurRadiusPx(3, 4, 4, false, false, false, true)
        val next = resolveBlurRadiusPx(5, 4, 4, false, false, false, true)

        assertEquals(3f, previous, 0.0001f)
        assertEquals(2f, next, 0.0001f)
    }

    @Test
    fun narrowViewportUsesUpstreamPointEightMultiplier() {
        assertEquals(
            1.6f,
            resolveBlurRadiusPx(5, 4, 4, false, false, true, true),
            0.0001f,
        )
    }

    @Test
    fun blurIsClampedToFivePixelsAndCanBeDisabled() {
        assertEquals(
            5f,
            resolveBlurRadiusPx(20, 4, 4, false, false, false, true),
            0.0001f,
        )
        assertEquals(
            0f,
            resolveBlurRadiusPx(20, 4, 4, false, false, false, false),
            0.0001f,
        )
    }
}
