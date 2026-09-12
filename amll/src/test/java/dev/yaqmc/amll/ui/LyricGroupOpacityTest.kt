package dev.yaqmc.amll.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class LyricGroupOpacityTest {
    @Test fun dynamicHighlightedGroupUsesEightyFivePercent() {
        assertEquals(0.85f, resolveDynamicLyricGroupOpacity(isHighlighted = true), 0.0001f)
    }

    @Test fun ordinaryDynamicGroupStaysFullyOpaque() {
        assertEquals(1f, resolveDynamicLyricGroupOpacity(isHighlighted = false), 0.0001f)
    }

    @Test fun offscreenGroupIsCulled() {
        assertEquals(
            0f,
            resolveLyricGroupOpacity(
                isInViewport = false,
                isHighlighted = true,
                isNonDynamic = false,
                hidePassedLines = false,
                isPlaying = true,
                isPassed = false,
            ),
            0.0001f,
        )
    }

    @Test fun hiddenPassedPlayingLineUsesTinyNonZeroAlpha() {
        assertEquals(
            1e-4f,
            resolveLyricGroupOpacity(
                isInViewport = true,
                isHighlighted = false,
                isNonDynamic = false,
                hidePassedLines = true,
                isPlaying = true,
                isPassed = true,
            ),
            0.000001f,
        )
    }

    @Test fun pausedPassedLineIsNotHidden() {
        assertEquals(
            1f,
            resolveLyricGroupOpacity(
                isInViewport = true,
                isHighlighted = false,
                isNonDynamic = false,
                hidePassedLines = true,
                isPlaying = false,
                isPassed = true,
            ),
            0.0001f,
        )
    }

    @Test fun ordinaryNonDynamicLineUsesTwentyPercent() {
        assertEquals(
            0.2f,
            resolveLyricGroupOpacity(
                isInViewport = true,
                isHighlighted = false,
                isNonDynamic = true,
                hidePassedLines = false,
                isPlaying = true,
                isPassed = false,
            ),
            0.0001f,
        )
    }

    @Test fun highlightedNonDynamicLineStillWinsWithEightyFivePercent() {
        assertEquals(
            0.85f,
            resolveLyricGroupOpacity(
                isInViewport = true,
                isHighlighted = true,
                isNonDynamic = true,
                hidePassedLines = false,
                isPlaying = true,
                isPassed = false,
            ),
            0.0001f,
        )
    }
}
