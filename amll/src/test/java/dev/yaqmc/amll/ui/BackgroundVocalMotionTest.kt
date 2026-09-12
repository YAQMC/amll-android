package dev.yaqmc.amll.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class BackgroundVocalMotionTest {
    @Test fun firstHiddenStateUsesPositiveEightyAndZeroOccupiedHeight() {
        assertEquals(80f, backgroundHiddenSlideY(placedFirst = true), 0.0001f)
        val frame = backgroundVocalFrame(80f)

        assertEquals(0f, frame.activeProgress, 0.0001f)
        assertEquals(0.8f, frame.scale, 0.0001f)
        assertEquals(0.8f, frame.translationYFraction, 0.0001f)
        assertEquals(0f, frame.firstOccupiedHeightFraction, 0.0001f)
    }

    @Test fun postHiddenStateUsesNegativeEightyWithSameScaleEnvelope() {
        assertEquals(-80f, backgroundHiddenSlideY(placedFirst = false), 0.0001f)
        val frame = backgroundVocalFrame(-80f)

        assertEquals(0f, frame.activeProgress, 0.0001f)
        assertEquals(0.8f, frame.scale, 0.0001f)
        assertEquals(-0.8f, frame.translationYFraction, 0.0001f)
    }

    @Test fun halfSlideProducesHalfExpansionAndNineTenthsScale() {
        val frame = backgroundVocalFrame(40f)

        assertEquals(0.5f, frame.activeProgress, 0.0001f)
        assertEquals(0.9f, frame.scale, 0.0001f)
        assertEquals(0.4f, frame.translationYFraction, 0.0001f)
        assertEquals(0.5f, frame.firstOccupiedHeightFraction, 0.0001f)
    }

    @Test fun activeStateFullyOccupiesAndHasIdentityTransform() {
        val frame = backgroundVocalFrame(0f)

        assertEquals(1f, frame.activeProgress, 0.0001f)
        assertEquals(1f, frame.scale, 0.0001f)
        assertEquals(0f, frame.translationYFraction, 0.0001f)
        assertEquals(1f, frame.firstOccupiedHeightFraction, 0.0001f)
    }

    @Test fun overshootBeyondHiddenRangeClampsProgressButKeepsSpringTranslation() {
        val frame = backgroundVocalFrame(100f)

        assertEquals(0f, frame.activeProgress, 0.0001f)
        assertEquals(0.8f, frame.scale, 0.0001f)
        assertEquals(1f, frame.translationYFraction, 0.0001f)
    }

    @Test fun firstBackgroundUnfoldsMeasuredHeightButKeepsFlexGap() {
        val layout = backgroundVocalLayoutFrame(
            backgroundHeightPx = 100,
            gapPx = 12,
            placedFirst = true,
            visible = true,
            frame = backgroundVocalFrame(40f),
        )

        assertEquals(62, layout.layoutHeightPx)
        assertEquals(-50, layout.childYpx)
    }

    @Test fun firstHiddenBackgroundCollapsesHeightButRetainsGap() {
        val layout = backgroundVocalLayoutFrame(
            backgroundHeightPx = 100,
            gapPx = 12,
            placedFirst = true,
            visible = false,
            frame = backgroundVocalFrame(80f),
        )

        assertEquals(12, layout.layoutHeightPx)
        assertEquals(-100, layout.childYpx)
    }

    @Test fun postBackgroundSwitchesLayoutContributionWithVisibility() {
        val shown = backgroundVocalLayoutFrame(
            backgroundHeightPx = 100,
            gapPx = 12,
            placedFirst = false,
            visible = true,
            frame = backgroundVocalFrame(-40f),
        )
        val hidden = backgroundVocalLayoutFrame(
            backgroundHeightPx = 100,
            gapPx = 12,
            placedFirst = false,
            visible = false,
            frame = backgroundVocalFrame(-40f),
        )

        assertEquals(112, shown.layoutHeightPx)
        assertEquals(12, shown.childYpx)
        assertEquals(0, hidden.layoutHeightPx)
        assertEquals(0, hidden.childYpx)
    }
}
