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
    private var hasBaseline = false

    fun detect(positionMs: Long, isPlaying: Boolean): Boolean {
        val mediaTime = positionMs.coerceAtLeast(0L)
        val wall = nowMs()

        if (!hasBaseline) {
            rebase(mediaTime, wall)
            hasBaseline = true
            return false
        }

        // Only backward progress is unconditionally a seek (upstream `SeekDetector`, AMLL main
        // 6911611). An unchanged sample — a stalled or coarsely updated clock, a paused host that
        // keeps pushing, or a play/pause toggle — is not a seek; it falls through with zero media
        // delta and rebases, keeping the wall-clock baseline fresh for the next real advance.
        if (mediaTime < lastMediaTimeMs) {
            rebase(mediaTime, wall)
            return true
        }

        val mediaDelta = (mediaTime - lastMediaTimeMs).toDouble()
        val elapsed = max(0.0, wall - lastWallTimeMs)
        val wallDelta = min(elapsed, SEEK_MAX_TRUSTED_GAP_MS)
        rebase(mediaTime, wall)

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
    }

    private fun rebase(mediaTimeMs: Long, wallTimeMs: Double) {
        lastMediaTimeMs = mediaTimeMs
        lastWallTimeMs = wallTimeMs
    }
}
