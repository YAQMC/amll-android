package dev.yaqmc.amll.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ManualScrollInteractionTest {
    @Test fun exactlyTenPixelsDoesNotConfirmIntent() {
        val tracker = TouchScrollIntentTracker()
        tracker.onDown(20f, 30f)

        assertFalse(tracker.onMove(30f, 30f))
        assertFalse(tracker.isConfirmed)
        assertFalse(tracker.onMove(20f, 40f))
        assertFalse(tracker.isConfirmed)
    }

    @Test fun eitherAxisPastTenPixelsConfirmsOnlyOnce() {
        val tracker = TouchScrollIntentTracker()
        tracker.onDown(100f, 100f)

        assertTrue(tracker.onMove(111f, 100f))
        assertTrue(tracker.isConfirmed)
        assertFalse(tracker.onMove(130f, 160f))
    }

    @Test fun diagonalMotionMustStillCrossAnAxisThreshold() {
        val tracker = TouchScrollIntentTracker()
        tracker.onDown(0f, 0f)

        assertFalse(tracker.onMove(8f, 8f))
        assertTrue(tracker.onMove(8f, 10.1f))
    }

    @Test fun reanchorPreservesConfirmedInteraction() {
        val tracker = TouchScrollIntentTracker()
        tracker.onDown(0f, 0f)
        assertTrue(tracker.onMove(0f, 11f))

        tracker.reanchor(50f, 50f)

        assertTrue(tracker.isConfirmed)
        assertFalse(tracker.onMove(50f, 80f))
    }

    @Test fun upResetsTrackerForNextGesture() {
        val tracker = TouchScrollIntentTracker()
        tracker.onDown(0f, 0f)
        assertTrue(tracker.onMove(11f, 0f))

        tracker.onUpOrCancel()
        tracker.onDown(50f, 50f)

        assertFalse(tracker.isConfirmed)
        assertFalse(tracker.onMove(55f, 55f))
    }
}
