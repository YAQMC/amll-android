package dev.yaqmc.amll.ui

import kotlin.math.max
import kotlin.math.min

private const val SEEK_JITTER_TOLERANCE_MS = 150.0
private const val SEEK_DRIFT_SLACK = 0.5
private const val SEEK_MAX_TRUSTED_GAP_MS = 800.0

/** Native port of AMLL's monotonic-wall-clock seek detector. */
internal class PlaybackSeekDetector(
    private val nowMs: () -> Double = { System.nanoTime() / 1_000_000.0 },
) {
    private var lastMediaTimeMs = 0L
    private var lastWallTimeMs = 0.0
    private var lastIsPlaying = false
    private var hasBaseline = false

    fun detect(positionMs: Long, isPlaying: Boolean): Boolean {
        val mediaTime = positionMs.coerceAtLeast(0L)
        val wall = nowMs()

        if (!hasBaseline) {
            rebase(mediaTime, wall, isPlaying)
            hasBaseline = true
            return false
        }

        // Upstream invokes its seek detector when a new media-time sample is pushed, not for a
        // play/pause flag change by itself. Compose observes both states, so suppress the synthetic
        // equal-time sample produced only by a playback-state transition and simply rebase it.
        if (mediaTime == lastMediaTimeMs && isPlaying != lastIsPlaying) {
            rebase(mediaTime, wall, isPlaying)
            return false
        }

        if (mediaTime <= lastMediaTimeMs) {
            rebase(mediaTime, wall, isPlaying)
            return true
        }

        val mediaDelta = (mediaTime - lastMediaTimeMs).toDouble()
        val elapsed = max(0.0, wall - lastWallTimeMs)
        val wallDelta = min(elapsed, SEEK_MAX_TRUSTED_GAP_MS)
        rebase(mediaTime, wall, isPlaying)

        val expected = if (isPlaying) wallDelta else 0.0
        val tolerance = if (isPlaying) {
            max(SEEK_JITTER_TOLERANCE_MS, wallDelta * SEEK_DRIFT_SLACK)
        } else {
            SEEK_JITTER_TOLERANCE_MS
        }
        val drift = mediaDelta - expected
        return drift > tolerance
    }

    fun reset() {
        hasBaseline = false
        lastMediaTimeMs = 0L
        lastWallTimeMs = 0.0
        lastIsPlaying = false
    }

    private fun rebase(mediaTimeMs: Long, wallTimeMs: Double, isPlaying: Boolean) {
        lastMediaTimeMs = mediaTimeMs
        lastWallTimeMs = wallTimeMs
        lastIsPlaying = isPlaying
    }
}
