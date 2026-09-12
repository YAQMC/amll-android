package dev.yaqmc.amll.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class LyricBehaviorFlagsTest {
    @Test fun disabledScaleKeepsInactiveMainLineAtActiveScale() {
        assertEquals(
            1f,
            resolveMainLineScaleTarget(
                isActive = false,
                isPlaying = true,
                enableScale = false,
                activeScale = 1f,
                inactiveScale = 0.97f,
            ),
            0.0001f,
        )
    }

    @Test fun enabledScaleOnlyShrinksInactivePlayingMainLine() {
        assertEquals(
            0.97f,
            resolveMainLineScaleTarget(false, true, true, 1f, 0.97f),
            0.0001f,
        )
        assertEquals(
            1f,
            resolveMainLineScaleTarget(true, true, true, 1f, 0.97f),
            0.0001f,
        )
        assertEquals(
            1f,
            resolveMainLineScaleTarget(false, false, true, 1f, 0.97f),
            0.0001f,
        )
    }

    @Test fun interludeMovesPassedBoundaryPastItsAnchor() {
        assertEquals(4, resolvePassedBoundary(activeGroupIndex = 5, interludeAnchorGroupIndex = 3))
    }

    @Test fun normalPlaybackUsesCurrentScrollGroupAsPassedBoundary() {
        assertEquals(5, resolvePassedBoundary(activeGroupIndex = 5, interludeAnchorGroupIndex = null))
    }
}
