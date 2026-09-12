package dev.yaqmc.amll.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FocusSpringPolicyTest {
    @Test fun missingIntervalUsesSlowUpstreamSpring() {
        val spec = focusSpringSpec(
            isSeeking = false,
            isInterludeActive = false,
            intervalMs = null,
        )

        assertEquals(100f, spec.stiffness, 0.0001f)
        assertEquals(0.8333333f, spec.dampingRatio, 0.0001f)
    }

    @Test fun seekAndInterludeAlwaysUseSlowPolicy() {
        val seek = focusSpringSpec(true, false, 100L)
        val interlude = focusSpringSpec(false, true, 100L)

        assertEquals(100f, seek.stiffness, 0.0001f)
        assertEquals(seek, interlude)
    }

    @Test fun endOfSongUsesMediumPolicy() {
        val spec = focusSpringSpec(
            isSeeking = false,
            isInterludeActive = false,
            intervalMs = 200L,
            isEndOfSong = true,
        )

        assertEquals(140f / 0.9f, spec.stiffness, 0.0001f)
        assertEquals(22f / (2f * kotlin.math.sqrt(140f * 0.9f)), spec.dampingRatio, 0.0001f)
    }

    @Test fun shorterLyricIntervalsProduceStifferFocusMotion() {
        val fast = focusSpringSpec(false, false, 100L)
        val slow = focusSpringSpec(false, false, 800L)

        assertEquals(220f / 0.9f, fast.stiffness, 0.0001f)
        assertEquals(170f / 0.9f, slow.stiffness, 0.0001f)
        assertTrue(fast.stiffness > slow.stiffness)
    }

    @Test fun intervalIsClampedToUpstreamRange() {
        val below = focusSpringSpec(false, false, 0L)
        val min = focusSpringSpec(false, false, 100L)
        val above = focusSpringSpec(false, false, 5_000L)
        val max = focusSpringSpec(false, false, 800L)

        assertEquals(min.stiffness, below.stiffness, 0.0001f)
        assertEquals(max.stiffness, above.stiffness, 0.0001f)
        assertEquals(min.dampingRatio, below.dampingRatio, 0.0001f)
        assertEquals(max.dampingRatio, above.dampingRatio, 0.0001f)
    }
}
