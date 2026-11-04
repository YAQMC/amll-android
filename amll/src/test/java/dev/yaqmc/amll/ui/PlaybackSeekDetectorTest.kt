package dev.yaqmc.amll.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Cases ported from upstream AMLL `packages/core/test/seek-detector.test.ts` (main 6911611). */
class PlaybackSeekDetectorTest {
    private class Harness {
        var wall = 0.0
        val detector = PlaybackSeekDetector { wall }

        fun push(mediaMs: Long, wallDeltaMs: Double, isPlaying: Boolean = true): Boolean {
            wall += wallDeltaMs
            return detector.detect(mediaMs, isPlaying)
        }
    }

    @Test fun firstSampleEstablishesBaselineWithoutSeek() {
        assertFalse(Harness().push(5_000L, 0.0))
    }

    @Test fun firstSampleAfterResetIsANewBaseline() {
        val h = Harness()
        h.push(1_000L, 0.0)
        h.detector.reset()
        assertFalse(h.push(61_000L, 16.0))
        assertFalse(h.push(61_016L, 16.0))
    }

    @Test fun normalPlaybackAtFrameAndCoarseCadenceIsNotASeek() {
        val frame = Harness()
        frame.push(0L, 0.0)
        for (i in 1..120) assertFalse(frame.push(i * 16L, 16.0))

        val coarse = Harness()
        coarse.push(0L, 0.0)
        for (i in 1..20) assertFalse(coarse.push(i * 250L, 250.0))
    }

    @Test fun fasterAndSlowerThanRealtimePlaybackIsNotASeek() {
        for (rate in listOf(1.25, 1.5, 2.0)) {
            val h = Harness()
            h.push(0L, 0.0)
            for (i in 1..120) assertFalse(h.push((i * 16 * rate).toLong(), 16.0))
        }
        val slow = Harness()
        slow.push(0L, 0.0)
        for (i in 1..120) assertFalse(slow.push(i * 8L, 16.0))
    }

    @Test fun unchangedProgressIsNotASeek() {
        val h = Harness()
        h.push(1_000L, 0.0)
        h.push(1_016L, 16.0)
        assertFalse(h.push(1_016L, 16.0))
    }

    @Test fun holdingOneProgressValueNeverReportsASeek() {
        val playing = Harness()
        playing.push(1_000L, 0.0)
        repeat(600) { assertFalse(playing.push(1_000L, 8.0)) }

        // A paused host that keeps pushing the same position every frame (YAQMC's overlay does).
        val paused = Harness()
        paused.push(1_000L, 0.0, isPlaying = false)
        repeat(600) { assertFalse(paused.push(1_000L, 8.0, isPlaying = false)) }
    }

    @Test fun stallKeepsBaselineFreshSoTheResumedAdvanceIsNormal() {
        val h = Harness()
        h.push(1_000L, 0.0)
        repeat(10) { h.push(1_000L, 16.0) }
        assertFalse(h.push(1_016L, 16.0))
        assertFalse(h.push(1_032L, 16.0))
    }

    @Test fun playPauseToggleAtTheSamePositionIsNotASeek() {
        val h = Harness()
        h.push(1_000L, 0.0, isPlaying = true)
        assertFalse(h.push(1_000L, 50.0, isPlaying = false))
        assertFalse(h.push(1_000L, 5_000.0, isPlaying = true))
        assertFalse(h.push(1_016L, 16.0, isPlaying = true))
    }

    @Test fun coarselyQuantizedSourceFlagsOnlyItsStepFrames() {
        val h = Harness()
        h.push(0L, 0.0)
        var flagged = 0
        for (frame in 1..240) if (h.push(frame * 16L / 250L * 250L, 16.0)) flagged++
        assertEquals(15, flagged)
    }

    @Test fun backwardProgressIsAlwaysASeek() {
        val h = Harness()
        h.push(1_000L, 0.0)
        h.push(1_016L, 16.0)
        assertTrue(h.push(1_015L, 16.0))

        val farWall = Harness()
        farWall.push(1_000L, 0.0)
        assertTrue(farWall.push(500L, 10_000.0))

        val paused = Harness()
        paused.push(1_000L, 0.0, isPlaying = false)
        assertTrue(paused.push(999L, 16.0, isPlaying = false))
    }

    @Test fun pausedProgressUsesOnlyTheFixedJitterTolerance() {
        val beyond = Harness()
        beyond.push(1_000L, 0.0, isPlaying = false)
        assertTrue(beyond.push(1_500L, 16.0, isPlaying = false))

        val within = Harness()
        within.push(1_000L, 0.0, isPlaying = false)
        assertFalse(within.push(1_100L, 16.0, isPlaying = false))

        val longPause = Harness()
        longPause.push(1_000L, 0.0, isPlaying = false)
        assertTrue(longPause.push(1_500L, 300_000.0, isPlaying = false))
    }

    @Test fun forwardJumpThatOutrunsTheWallClockIsASeek() {
        val h = Harness()
        h.push(1_000L, 0.0)
        assertTrue(h.push(1_500L, 100.0))
    }
}
