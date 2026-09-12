package dev.yaqmc.amll.ui

import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.math.sqrt

class LineScaleSpringTest {
    @Test fun mainLineSpringPreservesUpstreamNaturalFrequencyAndDamping() {
        val spec = mainLineScaleSpringSpec()

        assertEquals(50f, spec.stiffness, 0.0001f)
        assertEquals(25f / (2f * sqrt(200f)), spec.dampingRatio, 0.0001f)
    }

    @Test fun backgroundLineSpringPreservesUpstreamOverdampedPhysics() {
        val spec = backgroundLineScaleSpringSpec()

        assertEquals(50f, spec.stiffness, 0.0001f)
        assertEquals(20f / (2f * sqrt(50f)), spec.dampingRatio, 0.0001f)
    }

    @Test fun inactivePlayingMainLineUsesNinetySevenPercentTarget() {
        assertEquals(
            0.97f,
            mainLineScaleTarget(
                active = false,
                isPlaying = true,
                activeScale = 1f,
                inactiveScale = 0.97f,
            ),
            0.0001f,
        )
    }

    @Test fun activeOrPausedMainLineReturnsToFullScale() {
        assertEquals(1f, mainLineScaleTarget(true, true, 1f, 0.97f), 0.0001f)
        assertEquals(1f, mainLineScaleTarget(false, false, 1f, 0.97f), 0.0001f)
    }

    @Test fun inactivePlayingBackgroundLineUsesSeventyFivePercentTarget() {
        assertEquals(
            0.75f,
            backgroundLineScaleTarget(
                active = false,
                isPlaying = true,
                inactiveScale = 0.75f,
            ),
            0.0001f,
        )
    }

    @Test fun activeOrPausedBackgroundLineReturnsToFullScale() {
        assertEquals(1f, backgroundLineScaleTarget(true, true, 0.75f), 0.0001f)
        assertEquals(1f, backgroundLineScaleTarget(false, false, 0.75f), 0.0001f)
    }
}
