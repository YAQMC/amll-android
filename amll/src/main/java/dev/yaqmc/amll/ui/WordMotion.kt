package dev.yaqmc.amll.ui

import dev.yaqmc.amll.model.LyricWord
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

internal data class WordMotion(
    val translateYEm: Float = 0f,
)

internal data class CharacterMotion(
    val translateXEm: Float = 0f,
    val translateYEm: Float = 0f,
    val scale: Float = 1f,
    val glowAlpha: Float = 0f,
    val glowRadiusEm: Float = 0f,
)

private data class EmphasisParams(
    val durationMs: Float,
    val amount: Float,
    val blur: Float,
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
 * Every AMLL word gets a small ease-out upward drift, including emphasized words. Character-level
 * emphasis is additive and is calculated separately by [characterMotionAt].
 */
internal fun wordMotionAt(
    word: LyricWord,
    positionMs: Long,
    isBackground: Boolean,
): WordMotion {
    val durationMs = max(1_000L, word.endTimeMs - word.startTimeMs).toFloat()
    val progress = normalized(
        positionMs.toFloat(),
        word.startTimeMs.toFloat(),
        word.startTimeMs + durationMs,
    )
    val eased = cubicBezierYForX(0f, 0f, 0.58f, 1f, progress)
    val backgroundMultiplier = if (isBackground) 2f else 1f
    return WordMotion(translateYEm = -0.05f * backgroundMultiplier * eased)
}

/**
 * Native equivalent of upstream `createEmphasizeAnimation()` for one grapheme. The character
 * delay, scale pulse, horizontal push, vertical lift and glow strength all follow AMLL's formulas.
 * The returned transform is additive on top of [wordMotionAt].
 */
internal fun characterMotionAt(
    word: LyricWord,
    positionMs: Long,
    characterIndex: Int,
    totalCharacters: Int,
    isBackground: Boolean,
    isLastWord: Boolean,
): CharacterMotion {
    if (!shouldEmphasize(word) || totalCharacters <= 0) return CharacterMotion()

    val params = emphasisParams(word, isLastWord)
    val anchorCount = max(1, totalCharacters)
    val staggerMs = params.durationMs / 2.5f / anchorCount * characterIndex
    val characterStartMs = word.startTimeMs + staggerMs

    val glowProgress = normalized(
        positionMs.toFloat(),
        characterStartMs,
        characterStartMs + params.durationMs,
    )
    val emphasis = emphasizeEasing(glowProgress)

    val floatStartMs = characterStartMs - 400f
    val floatProgress = normalized(
        positionMs.toFloat(),
        floatStartMs,
        floatStartMs + params.durationMs * 1.4f,
    )
    val backgroundMultiplier = if (isBackground) 2f else 1f
    val floatY = sin(floatProgress * PI.toFloat()) * backgroundMultiplier

    val horizontalPush =
        -emphasis * 0.03f * params.amount * (totalCharacters / 2f - characterIndex)
    val emphasisLift = -emphasis * 0.025f * params.amount

    return CharacterMotion(
        translateXEm = horizontalPush,
        translateYEm = emphasisLift - floatY * 0.05f,
        scale = 1f + emphasis * 0.1f * params.amount,
        glowAlpha = (emphasis * params.blur).coerceIn(0f, 1f),
        glowRadiusEm = min(0.3f, params.blur * 0.3f),
    )
}

private fun emphasisParams(word: LyricWord, isLastWord: Boolean): EmphasisParams {
    var durationMs = max(1_000L, word.endTimeMs - word.startTimeMs).toFloat()

    var amount = durationMs / 2_000f
    amount = if (amount > 1f) sqrt(amount) else amount.pow(3)

    var blur = durationMs / 3_000f
    blur = if (blur > 1f) sqrt(blur) else blur.pow(3)

    amount *= 0.6f
    blur *= 0.5f

    if (isLastWord) {
        amount *= 1.6f
        blur *= 1.5f
        durationMs *= 1.2f
    }

    return EmphasisParams(
        durationMs = durationMs,
        amount = min(1.2f, amount),
        blur = min(0.8f, blur),
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

/** Small bezier-easing compatible solver for the CSS cubic-bezier curves used by AMLL. */
private fun cubicBezierYForX(x1: Float, y1: Float, x2: Float, y2: Float, x: Float): Float {
    val target = x.coerceIn(0f, 1f)
    var t = target

    repeat(6) {
        val currentX = cubic(t, x1, x2)
        val derivative = cubicDerivative(t, x1, x2)
        if (abs(derivative) > 1e-5f) {
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
