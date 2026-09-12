package dev.yaqmc.amll.ui

import dev.yaqmc.amll.model.LyricWord

/**
 * One indivisible layout unit. Child words keep their own timings/motion/annotations; the chunk
 * controls line breaking and the shared emphasize envelope used by upstream's wrapper animation.
 */
internal data class RenderWordChunk(
    val words: List<LyricWord>,
) {
    init {
        require(words.isNotEmpty()) { "RenderWordChunk must contain at least one word" }
    }

    val text: String = words.joinToString(separator = "") { it.text }
    val startTimeMs: Long = words.minOf(LyricWord::startTimeMs)
    val endTimeMs: Long = words.maxOf(LyricWord::endTimeMs)
    val isSpace: Boolean = words.all { it.text.trim().isEmpty() }

    /** Upstream counts JS string length, i.e. UTF-16 code units, across all ruby segments. */
    val rubyCharacterCount: Int = words.sumOf { word ->
        word.ruby.sumOf { ruby -> ruby.text.length }
    }

    /**
     * Synthetic timing word equivalent to the merged word created by upstream `buildWord()`.
     * Carry merged ruby solely so `characterMotionAt()` can reproduce upstream's ruby-based stagger
     * denominator without adding a separate renderer-side parameter.
     */
    val emphasisWord: LyricWord = LyricWord(
        startTimeMs = startTimeMs,
        endTimeMs = endTimeMs,
        text = text,
        ruby = words.flatMap(LyricWord::ruby),
    )

    /**
     * Upstream emphasizes a chunk when any child qualifies, or when the merged non-CJK chunk
     * qualifies. This is what lets short syllable fragments combine into one long-word pulse.
     */
    val emphasized: Boolean =
        words.any { word -> shouldEmphasize(word) } ||
            (!isCjkText(text) && shouldEmphasize(emphasisWord))
}

internal data class RenderWordPlan(
    val chunks: List<RenderWordChunk>,
) {
    val words: List<LyricWord> = chunks.flatMap(RenderWordChunk::words)
    val lastContentChunkIndex: Int = chunks.indexOfLast { !it.isSpace }
}

/**
 * Native port of upstream `chunkAndSplitLyricWords`.
 *
 * - whitespace is preserved as zero-duration atoms;
 * - ruby words remain atomic apart from leading/trailing whitespace;
 * - regular words containing whitespace are split and their timing is distributed by non-space
 *   UTF-16 code-unit length;
 * - multi-character CJK words without per-word romanization are split to characters;
 * - consecutive non-space, non-ruby, non-CJK atoms are grouped into one unbreakable chunk.
 */
internal fun chunkAndSplitLyricWords(words: List<LyricWord>): RenderWordPlan {
    if (words.isEmpty()) return RenderWordPlan(emptyList())

    val chunks = mutableListOf<RenderWordChunk>()
    var mergeable = mutableListOf<LyricWord>()

    fun flushMergeable() {
        if (mergeable.isNotEmpty()) {
            chunks += RenderWordChunk(mergeable.toList())
            mergeable = mutableListOf()
        }
    }

    fun processAtom(atom: LyricWord) {
        val isSpace = atom.text.trim().isEmpty()
        val hasRuby = atom.ruby.isNotEmpty()
        val isCjk = isCjkText(atom.text)
        val canMerge = !isSpace && !hasRuby && !isCjk

        if (canMerge) {
            mergeable += atom
        } else {
            flushMergeable()
            chunks += RenderWordChunk(listOf(atom))
        }
    }

    words.forEach { word ->
        val content = word.text.trim()
        if (content.isEmpty()) {
            processAtom(word)
            return@forEach
        }

        if (word.ruby.isNotEmpty()) {
            val leading = word.text.takeWhile(Char::isWhitespace)
            val trailing = word.text.takeLastWhile(Char::isWhitespace)

            if (leading.isNotEmpty()) {
                processAtom(
                    word.copy(
                        endTimeMs = word.startTimeMs,
                        text = leading,
                        romanText = "",
                        ruby = emptyList(),
                    )
                )
            }

            processAtom(word.copy(text = content))

            if (trailing.isNotEmpty()) {
                processAtom(
                    word.copy(
                        startTimeMs = word.endTimeMs,
                        text = trailing,
                        romanText = "",
                        ruby = emptyList(),
                    )
                )
            }
            return@forEach
        }

        val parts = splitKeepingWhitespace(word.text)
        val nonSpaceLength = word.text.count { !it.isWhitespace() }.coerceAtLeast(1)
        val timeSpan = word.endTimeMs - word.startTimeMs
        val timePerUnit = timeSpan.toDouble() / nonSpaceLength

        val wordParts = word.text.trim().split(Regex("\\s+")).filter(String::isNotEmpty)
        val romanTrimmed = word.romanText?.trim().orEmpty()
        val romanParts = if (romanTrimmed.isNotEmpty()) {
            romanTrimmed.split(Regex("\\s+")).filter(String::isNotEmpty)
        } else {
            emptyList()
        }
        val romanPartsMatch = wordParts.isNotEmpty() && wordParts.size == romanParts.size

        var currentOffset = 0
        var nonSpaceIndex = 0
        parts.forEach { part ->
            if (part.trim().isEmpty()) {
                val at = word.startTimeMs + (currentOffset * timePerUnit).toLong()
                processAtom(
                    word.copy(
                        startTimeMs = at,
                        endTimeMs = at,
                        text = part,
                        romanText = "",
                        ruby = emptyList(),
                    )
                )
                return@forEach
            }

            val partRoman = when {
                romanParts.isEmpty() -> ""
                romanPartsMatch -> romanParts.getOrElse(nonSpaceIndex) { "" }
                nonSpaceIndex == 0 -> word.romanText.orEmpty()
                else -> ""
            }
            nonSpaceIndex++

            if (isCjkText(part) && part.codePointCount(0, part.length) > 1 && romanTrimmed.isEmpty()) {
                val codePoints = part.codePoints().toArray()
                codePoints.forEach { codePoint ->
                    val charText = String(Character.toChars(codePoint))
                    val unitLength = charText.count { !it.isWhitespace() }.coerceAtLeast(1)
                    val start = word.startTimeMs + (currentOffset * timePerUnit).toLong()
                    currentOffset += unitLength
                    val end = if (currentOffset >= nonSpaceLength) {
                        word.endTimeMs
                    } else {
                        word.startTimeMs + (currentOffset * timePerUnit).toLong()
                    }
                    processAtom(
                        word.copy(
                            startTimeMs = start,
                            endTimeMs = end.coerceAtLeast(start),
                            text = charText,
                            romanText = "",
                            ruby = emptyList(),
                        )
                    )
                }
            } else {
                val unitLength = part.count { !it.isWhitespace() }.coerceAtLeast(1)
                val start = word.startTimeMs + (currentOffset * timePerUnit).toLong()
                currentOffset += unitLength
                val end = if (currentOffset >= nonSpaceLength) {
                    word.endTimeMs
                } else {
                    word.startTimeMs + (currentOffset * timePerUnit).toLong()
                }
                processAtom(
                    word.copy(
                        startTimeMs = start,
                        endTimeMs = end.coerceAtLeast(start),
                        text = part,
                        romanText = partRoman,
                        ruby = emptyList(),
                    )
                )
            }
        }
    }

    flushMergeable()
    return RenderWordPlan(chunks)
}

private fun splitKeepingWhitespace(text: String): List<String> {
    if (text.isEmpty()) return emptyList()
    val result = mutableListOf<String>()
    var start = 0
    var whitespace = text[0].isWhitespace()

    for (index in 1 until text.length) {
        val nextWhitespace = text[index].isWhitespace()
        if (nextWhitespace != whitespace) {
            result += text.substring(start, index)
            start = index
            whitespace = nextWhitespace
        }
    }
    result += text.substring(start)
    return result
}

/** Closely follows AMLL's broad `isCJK` predicate while handling supplementary ideographs safely. */
internal fun isCjkText(text: String): Boolean {
    if (text.isEmpty()) return false
    var index = 0
    while (index < text.length) {
        val codePoint = text.codePointAt(index)
        val cjk = codePoint in 0x0800..0x9FFC || codePoint in 0x20000..0x2FA1F
        if (!cjk) return false
        index += Character.charCount(codePoint)
    }
    return true
}
