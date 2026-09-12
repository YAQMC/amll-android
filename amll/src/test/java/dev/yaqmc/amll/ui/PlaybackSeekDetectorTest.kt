package dev.yaqmc.amll.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackSeekDetectorTest {
    @Test fun firstSampleEstablishesBaselineWithoutSeek() {
        var wall = 1_000.0
        val detector = PlaybackSeekDetector { wall }

        assertFalse(detector.detect(5_000L, isPlaying = true))
    }

    @Test fun normalPlayingProgressTracksWallClock() {
        var wall = 0.0
        val detector = PlaybackSeekDetector { wall }
        assertFalse(detector.detect(1_000L, true))

        wall = 100.0
        assertFalse(detector.detect(1_100L, true))

        wall = 260.0
        assertFalse(detector.detect(1_260L, true))
    }

    @Test fun largeForwardDriftIsDetectedAsSeek() {
        var wall = 0.0
        val detector = PlaybackSeekDetector { wall }
        assertFalse(detector.detect(1_000L, true))

        wall = 100.0
        assertTrue(detector.detect(1_500L, true))
    }

    @Test fun equalOrBackwardMediaTimeIsSeekWhenPlaybackStateIsUnchanged() {
        var wall = 0.0
        val detector = PlaybackSeekDetector { wall }
        assertFalse(detector.detect(1_000L, true))

        wall = 50.0
        assertTrue(detector.detect(1_000L, true))

        wall = 100.0
        assertTrue(detector.detect(900L, true))
    }

    @Test fun playbackOnlyToggleAtSamePositionRebasesWithoutSeek() {
        var wall = 0.0
        val detector = PlaybackSeekDetector { wall }
        assertFalse(detector.detect(1_000L, true))

        wall = 50.0
        assertFalse(detector.detect(1_000L, false))

        wall = 100.0
        assertFalse(detector.detect(1_000L, true))

        // A repeated position sample under the same playback state still preserves upstream's
        // non-advancing-media-time seek rule.
        wall = 150.0
        assertTrue(detector.detect(1_000L, true))
    }

    @Test fun pausedPlaybackKeepsOnlyFixedJitterTolerance() {
        var wall = 0.0
        val detector = PlaybackSeekDetector { wall }
        assertFalse(detector.detect(1_000L, false))

        wall = 5_000.0
        assertFalse(detector.detect(1_150L, false))

        wall = 10_000.0
        assertTrue(detector.detect(1_301L, false))
    }

    @Test fun resetMakesNextSampleANewBaseline() {
        var wall = 0.0
        val detector = PlaybackSeekDetector { wall }
        assertFalse(detector.detect(1_000L, true))

        wall = 100.0
        assertTrue(detector.detect(2_000L, true))

        detector.reset()
        wall = 200.0
        assertFalse(detector.detect(10_000L, true))
    }
}
