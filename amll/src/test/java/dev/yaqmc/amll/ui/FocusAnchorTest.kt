package dev.yaqmc.amll.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class FocusAnchorTest {
    @Test fun defaultCenterAnchorUsesThirtyFivePercentMinusHalfTargetHeight() {
        val top = resolveFocusItemTopPx(
            viewportStartPx = 0,
            viewportHeightPx = 1_000,
            targetHeightPx = 120,
            alignPosition = 0.35f,
            alignAnchor = AMLLAlignAnchor.Center,
        )

        assertEquals(290f, top, 0.0001f)
    }

    @Test fun topAnchorPlacesTargetTopDirectlyOnViewportPosition() {
        val top = resolveFocusItemTopPx(
            viewportStartPx = -20,
            viewportHeightPx = 800,
            targetHeightPx = 200,
            alignPosition = 0.25f,
            alignAnchor = AMLLAlignAnchor.Top,
        )

        assertEquals(180f, top, 0.0001f)
    }

    @Test fun bottomAnchorSubtractsFullTargetHeight() {
        val top = resolveFocusItemTopPx(
            viewportStartPx = 10,
            viewportHeightPx = 600,
            targetHeightPx = 90,
            alignPosition = 0.5f,
            alignAnchor = AMLLAlignAnchor.Bottom,
        )

        assertEquals(220f, top, 0.0001f)
    }

    @Test fun upstreamDoesNotClampCustomAlignmentPosition() {
        val top = resolveFocusItemTopPx(
            viewportStartPx = 0,
            viewportHeightPx = 1_000,
            targetHeightPx = 0,
            alignPosition = 1.2f,
            alignAnchor = AMLLAlignAnchor.Top,
        )

        assertEquals(1_200f, top, 0.0001f)
    }

    @Test fun defaultAlignmentReservesEnoughFirstAndLastItemSpace() {
        val padding = resolveFocusEdgePaddingPx(
            viewportHeightPx = 1_000,
            alignPosition = 0.35f,
            minimumPaddingPx = 120f,
        )

        assertEquals(350f, padding.before, 0.0001f)
        assertEquals(650f, padding.after, 0.0001f)
    }

    @Test fun configuredMinimumPaddingStillWinsOnSmallViewport() {
        val padding = resolveFocusEdgePaddingPx(
            viewportHeightPx = 200,
            alignPosition = 0.35f,
            minimumPaddingPx = 120f,
        )

        assertEquals(120f, padding.before, 0.0001f)
        assertEquals(130f, padding.after, 0.0001f)
    }

    @Test fun outOfRangeAlignmentNeverCreatesNegativeContentPadding() {
        val padding = resolveFocusEdgePaddingPx(
            viewportHeightPx = 1_000,
            alignPosition = 1.2f,
            minimumPaddingPx = 0f,
        )

        assertEquals(1_200f, padding.before, 0.0001f)
        assertEquals(0f, padding.after, 0.0001f)
    }
}
