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
) {
    private var _lyricLines: List<LyricLine> by mutableStateOf(
        lyricLines.sortedBy(LyricLine::startTimeMs)
    )

    val lyricLines: List<LyricLine>
        get() = _lyricLines

    var positionMs: Long by mutableLongStateOf(initialPositionMs.coerceAtLeast(0L))
        private set

    var isPlaying: Boolean by mutableStateOf(initialIsPlaying)
        private set

    val activeLineIndex: Int
        get() = findActiveLineIndex(_lyricLines, positionMs)

    fun setLyricLines(lines: List<LyricLine>) {
        _lyricLines = lines.sortedBy(LyricLine::startTimeMs)
    }

    fun setPlaying(isPlaying: Boolean) {
        this.isPlaying = isPlaying
    }

    fun seekTo(positionMs: Long) {
        this.positionMs = positionMs.coerceAtLeast(0L)
    }

    fun update(positionMs: Long) = seekTo(positionMs)
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
