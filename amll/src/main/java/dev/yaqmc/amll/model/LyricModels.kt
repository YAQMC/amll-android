package dev.yaqmc.amll.model

import androidx.compose.runtime.Immutable

/** Timed pronunciation segment attached above a lyric word, equivalent to AMLL's ruby word base. */
@Immutable
data class LyricRuby(
    val startTimeMs: Long,
    val endTimeMs: Long,
    val text: String,
) {
    init {
        require(startTimeMs >= 0) { "startTimeMs must be >= 0" }
        require(endTimeMs >= startTimeMs) { "endTimeMs must be >= startTimeMs" }
    }

    fun progressAt(positionMs: Long): Float = when {
        positionMs <= startTimeMs -> 0f
        positionMs >= endTimeMs -> 1f
        endTimeMs == startTimeMs -> 1f
        else -> ((positionMs - startTimeMs).toFloat() / (endTimeMs - startTimeMs)).coerceIn(0f, 1f)
    }
}

@Immutable
data class LyricWord(
    val startTimeMs: Long,
    val endTimeMs: Long,
    val text: String,
    val romanText: String? = null,
    val obscene: Boolean = false,
    val ruby: List<LyricRuby> = emptyList(),
) {
    init {
        require(startTimeMs >= 0) { "startTimeMs must be >= 0" }
        require(endTimeMs >= startTimeMs) { "endTimeMs must be >= startTimeMs" }
    }

    fun progressAt(positionMs: Long): Float = when {
        positionMs <= startTimeMs -> 0f
        positionMs >= endTimeMs -> 1f
        endTimeMs == startTimeMs -> 1f
        else -> ((positionMs - startTimeMs).toFloat() / (endTimeMs - startTimeMs)).coerceIn(0f, 1f)
    }
}

@Immutable
data class LyricLine(
    val words: List<LyricWord>,
    val translatedLyric: String = "",
    val romanLyric: String = "",
    val startTimeMs: Long = words.minOfOrNull(LyricWord::startTimeMs) ?: 0L,
    val endTimeMs: Long = words.maxOfOrNull(LyricWord::endTimeMs) ?: startTimeMs,
    val isBackground: Boolean = false,
    val isDuet: Boolean = false,
) {
    init {
        require(startTimeMs >= 0) { "startTimeMs must be >= 0" }
        require(endTimeMs >= startTimeMs) { "endTimeMs must be >= startTimeMs" }
    }

    val text: String get() = words.joinToString(separator = "") { it.text }
}
