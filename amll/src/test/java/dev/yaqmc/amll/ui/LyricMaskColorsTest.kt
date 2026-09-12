package dev.yaqmc.amll.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class LyricMaskColorsTest {
    @Test fun upstreamMaskAlphaTargetsStayExact() {
        assertEquals(0.20f, AMLL_SOLID_MASK_ALPHA, 0f)
        assertEquals(1.00f, AMLL_GRADIENT_BRIGHT_MASK_ALPHA, 0f)
        assertEquals(0.40f, AMLL_GRADIENT_DARK_MASK_ALPHA, 0f)
    }

    @Test fun upstreamMaskTransitionDurationsStayExact() {
        assertEquals(300, lyricMaskTransitionDurationMs(active = true))
        assertEquals(450, lyricMaskTransitionDurationMs(active = false))
    }
}
