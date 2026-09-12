package dev.yaqmc.amll.state

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AMLLPlayerStatePlaybackTest {
    @Test fun repeatedEqualExplicitSeekSamplesAdvanceBothVersions() {
        val state = AMLLPlayerState(initialPositionMs = 1_000L)

        assertEquals(0L, state.positionUpdateVersion)
        assertEquals(0L, state.explicitSeekVersion)

        state.seekTo(2_000L)
        assertEquals(1L, state.positionUpdateVersion)
        assertEquals(1L, state.explicitSeekVersion)
        assertEquals(2_000L, state.positionMs)

        state.seekTo(2_000L)
        assertEquals(2L, state.positionUpdateVersion)
        assertEquals(2L, state.explicitSeekVersion)
        assertEquals(2_000L, state.positionMs)
    }

    @Test fun ordinaryPositionSamplesDoNotBecomeExplicitSeeks() {
        val state = AMLLPlayerState(initialPositionMs = 1_000L)

        state.update(1_100L)
        state.update(positionMs = 1_200L, isPlaying = false)

        assertEquals(2L, state.positionUpdateVersion)
        assertEquals(0L, state.explicitSeekVersion)
        assertEquals(1_200L, state.positionMs)
        assertFalse(state.isPlaying)
    }

    @Test fun negativePositionIsClampedButStillCountsAsAPushedSample() {
        val state = AMLLPlayerState(initialPositionMs = 500L)

        state.update(-100L)

        assertEquals(0L, state.positionMs)
        assertEquals(1L, state.positionUpdateVersion)
        assertEquals(0L, state.explicitSeekVersion)
    }

    @Test fun combinedUpdateSynchronizesPositionAndPlayState() {
        val state = AMLLPlayerState(initialPositionMs = 500L, initialIsPlaying = true)

        state.update(positionMs = 1_250L, isPlaying = false)

        assertEquals(1_250L, state.positionMs)
        assertFalse(state.isPlaying)
        assertEquals(1L, state.positionUpdateVersion)
        assertEquals(0L, state.explicitSeekVersion)
    }

    @Test fun automaticSeekDetectionDefaultsOnAndCanBeChanged() {
        val state = AMLLPlayerState()
        assertTrue(state.autoSeekDetectionEnabled)

        state.updateAutoSeekDetectionEnabled(false)
        assertFalse(state.autoSeekDetectionEnabled)

        state.updateAutoSeekDetectionEnabled(true)
        assertTrue(state.autoSeekDetectionEnabled)
    }

    @Test fun automaticSeekDetectionCanStartDisabled() {
        val state = AMLLPlayerState(initialAutoSeekDetectionEnabled = false)
        assertFalse(state.autoSeekDetectionEnabled)
    }
}
