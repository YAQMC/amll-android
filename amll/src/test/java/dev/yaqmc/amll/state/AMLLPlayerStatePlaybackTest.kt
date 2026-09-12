package dev.yaqmc.amll.state

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class AMLLPlayerStatePlaybackTest {
    @Test fun repeatedEqualSeekSamplesStillAdvanceInternalVersion() {
        val state = AMLLPlayerState(initialPositionMs = 1_000L)

        assertEquals(0L, state.positionUpdateVersion)
        state.seekTo(2_000L)
        assertEquals(1L, state.positionUpdateVersion)
        assertEquals(2_000L, state.positionMs)

        state.seekTo(2_000L)
        assertEquals(2L, state.positionUpdateVersion)
        assertEquals(2_000L, state.positionMs)
    }

    @Test fun negativePositionIsClampedButStillCountsAsAPushedSample() {
        val state = AMLLPlayerState(initialPositionMs = 500L)

        state.update(-100L)

        assertEquals(0L, state.positionMs)
        assertEquals(1L, state.positionUpdateVersion)
    }

    @Test fun combinedUpdateSynchronizesPositionAndPlayState() {
        val state = AMLLPlayerState(initialPositionMs = 500L, initialIsPlaying = true)

        state.update(positionMs = 1_250L, isPlaying = false)

        assertEquals(1_250L, state.positionMs)
        assertFalse(state.isPlaying)
        assertEquals(1L, state.positionUpdateVersion)
    }
}
