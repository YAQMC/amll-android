package dev.yaqmc.amll.ui

import java.text.BreakIterator
import java.util.Locale
import kotlin.math.max

internal data class BalancedLineChild(
    val widthPx: Float,
    val text: String,
    val isSpace: Boolean = text.trim().isEmpty(),
)

internal data class BalancedLyricLayout(
    val text: String,
    val wordOffsets: IntArray,
    val breaks: List<Int>,
)

private const val OVERFLOW_PENALTY_MULTIPLIER = 1000.0
private const val CJK_BREAK_PENALTY_RATIO = 0.15
private const val NORMAL_BREAK_PENALTY_RATIO = 0.5
private const val SPACE_BREAK_REWARD_RATIO = 0.4
private const val PUNCTUATION_BREAK_REWARD_RATIO = 0.6
private val punctuationEndChars = setOf(
    ',', '.', ';', ':', '!', '?',
    '，', '。', '；', '：', '！', '？', '、',
    '）', '】', '》', '」', '』', '’', '”',
    ')', ']', '}', '>', '~', '…',
)

/**
 * Native port of AMLL's lyric-line dynamic-programming breaker.
 *
 * The cost function minimizes line-length variance while preferring punctuation/space boundaries,
 * mildly discouraging CJK word-boundary cuts, and strongly discouraging arbitrary text cuts. A
 * single unbreakable child wider than the viewport is allowed to overflow on its own line instead
 * of being split internally.
 */
internal fun calculateBalancedBreaks(
    children: List<BalancedLineChild>,
    containerWidthPx: Float,
): List<Int> {
    val n = children.size
    if (n == 0 || containerWidthPx <= 0f) return emptyList()

    val safeWidth = max(1f, containerWidthPx).toDouble()
    val prefixWidth = DoubleArray(n + 1)
    val charOffsets = IntArray(n + 1)
    for (i in 0 until n) {
        prefixWidth[i + 1] = prefixWidth[i] + children[i].widthPx.coerceAtLeast(0f)
        charOffsets[i + 1] = charOffsets[i] + children[i].text.length
    }
    if (prefixWidth[n] <= safeWidth) return emptyList()

    val fullText = buildString { children.forEach { append(it.text) } }
    val cjkBoundaries = cjkWordBoundaries(fullText)

    val penaltyCjk = (safeWidth * CJK_BREAK_PENALTY_RATIO).let { it * it }
    val penaltyNormal = (safeWidth * NORMAL_BREAK_PENALTY_RATIO).let { it * it }
    val dp = DoubleArray(n + 1) { Double.POSITIVE_INFINITY }
    val nextBreak = IntArray(n + 1) { -1 }
    dp[n] = 0.0

    for (i in n - 1 downTo 0) {
        for (j in i + 1..n) {
            val width = prefixWidth[j] - prefixWidth[i]
            val lineCost = if (width > safeWidth) {
                if (j == i + 1) {
                    val overflow = width - safeWidth
                    overflow * overflow * OVERFLOW_PENALTY_MULTIPLIER
                } else {
                    continue
                }
            } else {
                val slack = safeWidth - width
                slack * slack
            }

            var breakPenalty = 0.0
            if (j < n) {
                val previous = children[j - 1]
                val previousTrimmed = previous.text.trimEnd()
                val endsWithPunctuation = previousTrimmed.lastOrNull()?.let {
                    it in punctuationEndChars
                } == true
                breakPenalty = when {
                    endsWithPunctuation -> {
                        val reward = safeWidth * PUNCTUATION_BREAK_REWARD_RATIO
                        -(reward * reward)
                    }
                    previous.isSpace -> {
                        val reward = safeWidth * SPACE_BREAK_REWARD_RATIO
                        -(reward * reward)
                    }
                    charOffsets[j] in cjkBoundaries -> penaltyCjk
                    else -> penaltyNormal
                }
            }

            val total = lineCost + breakPenalty + dp[j]
            if (total < dp[i]) {
                dp[i] = total
                nextBreak[i] = j
            }
        }
    }

    val result = mutableListOf<Int>()
    var current = 0
    while (current < n) {
        val next = nextBreak[current]
        if (next <= current) break
        current = next
        if (current in 1 until n) result += current
    }
    return result
}

/**
 * Inserts only hard line breaks. The caller can then disable soft wrapping so every timed lyric
 * word remains atomic while TextLayoutResult still provides glyph geometry for karaoke effects.
 */
internal fun buildBalancedLyricLayout(
    wordTexts: List<String>,
    wordWidthsPx: List<Float>,
    containerWidthPx: Float,
): BalancedLyricLayout {
    require(wordTexts.size == wordWidthsPx.size) {
        "wordTexts and wordWidthsPx must have the same size"
    }
    if (wordTexts.isEmpty()) {
        return BalancedLyricLayout(text = "", wordOffsets = IntArray(0), breaks = emptyList())
    }

    val children = wordTexts.indices.map { index ->
        BalancedLineChild(widthPx = wordWidthsPx[index], text = wordTexts[index])
    }
    val breaks = calculateBalancedBreaks(children, containerWidthPx)
    val breakSet = breaks.toHashSet()
    val offsets = IntArray(wordTexts.size)
    val text = buildString {
        wordTexts.forEachIndexed { index, word ->
            if (index in breakSet) append('\n')
            offsets[index] = length
            append(word)
        }
    }
    return BalancedLyricLayout(text = text, wordOffsets = offsets, breaks = breaks)
}

private fun cjkWordBoundaries(text: String): Set<Int> {
    if (text.isEmpty()) return emptySet()
    val iterator = BreakIterator.getWordInstance(Locale.ROOT)
    iterator.setText(text)
    val boundaries = mutableSetOf<Int>()

    var start = iterator.first()
    var end = iterator.next()
    while (end != BreakIterator.DONE) {
        if (start > 0 && containsCjk(text, start, end)) boundaries += start
        start = end
        end = iterator.next()
    }
    return boundaries
}

private fun containsCjk(text: String, start: Int, end: Int): Boolean {
    var index = start
    while (index < end) {
        val codePoint = text.codePointAt(index)
        if (isCjkCodePoint(codePoint)) return true
        index += Character.charCount(codePoint)
    }
    return false
}

/** Mirrors upstream's Unified-Ideograph / broad U+0800..U+9FFC CJK test closely on the JVM. */
private fun isCjkCodePoint(codePoint: Int): Boolean =
    codePoint in 0x0800..0x9FFC ||
        codePoint in 0x20000..0x2FA1F
