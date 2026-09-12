package dev.yaqmc.amll.state

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.yaqmc.amll.model.LyricLine

@Stable
class AMLLPlayerState(
    lyricLines: List<LyricLine> = emptyList(),
    initialPositionMs: Long = 0L,
    initialIsPlaying: Boolean = true,
    initialAutoSeekDetectionEnabled: Boolean = true,
) {
    private var _lyricLines: List<LyricLine> by mutableStateOf(
        lyricLines.sortedBy(LyricLine::startTimeMs)
    )

    val lyricLines: List<LyricLine>
        get() = _lyricLines

    var positionMs: Long by mutableLongStateOf(initialPositionMs.coerceAtLeast(0L))
        private set

    /**
     * Internal sequence for consumers that must observe every pushed media-time sample, including
     * repeated equal values that Compose's value state would otherwise coalesce.
     */
    internal var positionUpdateVersion: Long by mutableLongStateOf(0L)
        private set

    /**
     * Separate sequence for host-declared seeks. Ordinary playback-clock samples increment only
     * [positionUpdateVersion], while [seekTo] increments both so the renderer can preserve an
     * explicit seek even when automatic seek inference is disabled.
     */
    internal var explicitSeekVersion: Long by mutableLongStateOf(0L)
        private set

    var isPlaying: Boolean by mutableStateOf(initialIsPlaying)
        private set

    /** Mirrors upstream `setEnableAutoSeekDetection()`, which defaults to enabled. */
    var autoSeekDetectionEnabled: Boolean by mutableStateOf(initialAutoSeekDetectionEnabled)
        private set

    val activeLineIndex: Int
        get() = findActiveLineIndex(_lyricLines, positionMs)

    fun setLyricLines(lines: List<LyricLine>) {
        _lyricLines = lines.sortedBy(LyricLine::startTimeMs)
    }

    fun updatePlaybackState(isPlaying: Boolean) {
        this.isPlaying = isPlaying
    }

    fun updateAutoSeekDetectionEnabled(enable: Boolean) {
        autoSeekDetectionEnabled = enable
    }

    fun seekTo(positionMs: Long) {
        pushPosition(positionMs)
        explicitSeekVersion++
    }

    /**
     * Preferred host-integration entry point when a playback clock delivers position and play state
     * together. The position sample is still observed even when it is equal to the previous value,
     * preserving AMLL seek/stall inference semantics without marking every clock tick as an explicit
     * seek.
     */
    fun update(positionMs: Long, isPlaying: Boolean) {
        this.isPlaying = isPlaying
        pushPosition(positionMs)
    }

    fun update(positionMs: Long) {
        pushPosition(positionMs)
    }

    private fun pushPosition(positionMs: Long) {
        this.positionMs = positionMs.coerceAtLeast(0L)
        positionUpdateVersion++
    }
}

internal fun findActiveLineIndex(lines: List<LyricLine>, positionMs: Long): Int {
    if (lines.isEmpty()) return -1

    var low = 0
    var high = lines.lastIndex
    var candidate = -1

    while (low <= high) {
        val mid = (low + high) ushr 1
        if (lines[mid].startTimeMs <= positionMs) {
            candidate = mid
            low = mid + 1
        } else {
            high = mid - 1
        }
    }

    if (candidate < 0) return lines.indexOfFirst { !it.isBackground }.takeIf { it >= 0 } ?: 0

    while (candidate > 0 && lines[candidate].isBackground) {
        candidate--
    }

    return candidate
}
