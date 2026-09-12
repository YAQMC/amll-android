package dev.yaqmc.amll.ui

import dev.yaqmc.amll.model.LyricWord
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

internal data class WordMotion(
    val translateYEm: Float = 0f,
    val scale: Float = 1f,
    val emphasis: Float = 0f,
)

/** Mirrors AMLL's eligibility rule for its emphasized long-word animation. */
internal fun shouldEmphasize(word: LyricWord): Boolean {
    val duration = word.endTimeMs - word.startTimeMs
    if (duration < 1_000L) return false

    val text = word.text.trim()
    if (containsCjk(text)) return true
    return text.length in 2..7
}

/**
 * Time-derived word motion. The renderer never starts its own clock: seeking to the same media
 * time always produces the same transform, which is important for YAQMC/MediaSession sync.
 */
internal fun wordMotionAt(
    word: LyricWord,
    positionMs: Long,
    isBackground: Boolean,
    isLastWord: Boolean,
): WordMotion {
    val durationMs = max(1_000L, word.endTimeMs - word.startTimeMs).toFloat()
    val backgroundMultiplier = if (isBackground) 2f else 1f

    if (!shouldEmphasize(word)) {
        val progress = normalized(positionMs.toFloat(), word.startTimeMs.toFloat(), word.startTimeMs + durationMs)
        // Upstream's regular float is an ease-out from 0 to -0.05em (twice that for BG vocals).
        val eased = 1f - (1f - progress).pow(3)
        return WordMotion(translateYEm = -0.05f * backgroundMultiplier * eased)
    }

    var emphasizedDuration = durationMs
    var amount = durationMs / 2_000f
    amount = if (amount > 1f) sqrt(amount) else amount.pow(3)
    amount *= 0.6f

    if (isLastWord) {
        amount *= 1.6f
        emphasizedDuration *= 1.2f
    }
    amount = min(1.2f, amount)

    val emphasisProgress = normalized(
        positionMs.toFloat(),
        word.startTimeMs.toFloat(),
        word.startTimeMs + emphasizedDuration,
    )
    val emphasis = emphasizeEasing(emphasisProgress)

    // AMLL begins the emphasized float 400ms before the word and lets it return to baseline.
    val floatProgress = normalized(
        positionMs.toFloat(),
        word.startTimeMs - 400f,
        word.startTimeMs - 400f + emphasizedDuration * 1.4f,
    )
    val floatAmount = sin(floatProgress * PI.toFloat())

    return WordMotion(
        translateYEm = -floatAmount * 0.05f * backgroundMultiplier,
        scale = 1f + emphasis * 0.1f * amount,
        emphasis = emphasis,
    )
}

private fun emphasizeEasing(x: Float): Float {
    val clamped = x.coerceIn(0f, 1f)
    return if (clamped < 0.5f) {
        cubicBezierYForX(0.2f, 0.4f, 0.58f, 1f, clamped / 0.5f)
    } else {
        1f - cubicBezierYForX(0.3f, 0f, 0.58f, 1f, (clamped - 0.5f) / 0.5f)
    }
}

/** Small bezier-easing compatible solver; accurate enough for frame-time animation sampling. */
private fun cubicBezierYForX(x1: Float, y1: Float, x2: Float, y2: Float, x: Float): Float {
    val target = x.coerceIn(0f, 1f)
    var t = target

    repeat(6) {
        val currentX = cubic(t, x1, x2)
        val derivative = cubicDerivative(t, x1, x2)
        if (kotlin.math.abs(derivative) > 1e-5f) {
            t = (t - (currentX - target) / derivative).coerceIn(0f, 1f)
        }
    }
    return cubic(t, y1, y2).coerceIn(0f, 1f)
}

private fun cubic(t: Float, p1: Float, p2: Float): Float {
    val oneMinusT = 1f - t
    return 3f * oneMinusT * oneMinusT * t * p1 +
        3f * oneMinusT * t * t * p2 +
        t * t * t
}

private fun cubicDerivative(t: Float, p1: Float, p2: Float): Float {
    val oneMinusT = 1f - t
    return 3f * oneMinusT * oneMinusT * p1 +
        6f * oneMinusT * t * (p2 - p1) +
        3f * t * t * (1f - p2)
}

private fun normalized(value: Float, start: Float, end: Float): Float {
    if (end <= start) return if (value >= end) 1f else 0f
    return ((value - start) / (end - start)).coerceIn(0f, 1f)
}

private fun containsCjk(text: String): Boolean = text.any { char ->
    when (Character.UnicodeBlock.of(char)) {
        Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS,
        Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A,
        Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS,
        Character.UnicodeBlock.HIRAGANA,
        Character.UnicodeBlock.KATAKANA,
        Character.UnicodeBlock.HANGUL_SYLLABLES,
        Character.UnicodeBlock.HANGUL_JAMO -> true
        else -> false
    }
}
