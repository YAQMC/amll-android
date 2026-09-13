package dev.yaqmc.amll.ui

import dev.yaqmc.amll.model.LyricWord
import kotlin.math.max
import kotlin.math.min

/** Horizontal bright-to-dark fade interval for one timed word, in lyric-layout coordinates. */
internal data class WordMaskGradientPx(
    val fadeStartX: Float,
    val fadeEndX: Float,
)

/**
 * Converts the parent word's fixed mask interval into the local coordinates used while drawing one
 * transformed emphasized grapheme.
 *
 * Upstream applies `mask-image` to the word element and applies translate/scale to descendant
 * grapheme spans. The mask therefore stays fixed in word space while the glyph moves underneath it.
 * Native drawing applies the grapheme transform to the Canvas itself, so the interval must be
 * inverse-mapped before drawing to keep its final screen-space position unchanged.
 */
internal fun resolveCharacterLocalWordMask(
    wordMask: WordMaskGradientPx,
    characterTranslateXPx: Float,
    characterScale: Float,
    pivotXPx: Float,
): WordMaskGradientPx {
    val scale = characterScale.coerceAtLeast(0.0001f)

    fun inverseMap(parentX: Float): Float =
        pivotXPx + (parentX - characterTranslateXPx - pivotXPx) / scale

    return WordMaskGradientPx(
        fadeStartX = inverseMap(wordMask.fadeStartX),
        fadeEndX = inverseMap(wordMask.fadeEndX),
    )
}

/**
 * Returns the horizontal sweep progress used by AMLL's word-level mask.
 *
 * Plain words move linearly from [LyricWord.startTimeMs] to [LyricWord.endTimeMs]. When ruby timing
 * exists, upstream divides the whole measured word width by the total ruby UTF-16 code-unit count,
 * advances each segment during its own clamped timing window, and holds the mask still in gaps.
 * Kotlin [String.length] uses the same UTF-16 code-unit count as JavaScript string length.
 *
 * Values outside the word timing intentionally remain outside 0..1 so the caller can keep AMLL's
 * half-fade lead-in/tail before clamping the final mask center.
 */
internal fun resolveWordMaskSweepProgress(
    word: LyricWord,
    positionMs: Long,
): Float {
    val durationMs = (word.endTimeMs - word.startTimeMs).coerceAtLeast(1L)
    if (positionMs < word.startTimeMs) {
        return (positionMs - word.startTimeMs).toFloat() / durationMs.toFloat()
    }
    if (positionMs > word.endTimeMs) {
        return 1f + (positionMs - word.endTimeMs).toFloat() / durationMs.toFloat()
    }

    val rubySegments = word.ruby.filter { it.text.isNotBlank() }
    val rubyCharacterCount = rubySegments.sumOf { it.text.length }
    if (rubyCharacterCount <= 0) {
        return (positionMs - word.startTimeMs).toFloat() / durationMs.toFloat()
    }

    var completedCharacters = 0
    rubySegments.forEach { ruby ->
        val rubyStart = max(ruby.startTimeMs, word.startTimeMs)
        val rubyEnd = min(max(ruby.endTimeMs, rubyStart), word.endTimeMs)
        val characterCount = ruby.text.length

        if (positionMs < rubyStart) {
            return completedCharacters.toFloat() / rubyCharacterCount.toFloat()
        }

        if (positionMs <= rubyEnd) {
            val rubyDuration = rubyEnd - rubyStart
            if (rubyDuration <= 0L) {
                completedCharacters += characterCount
                return@forEach
            }
            val segmentProgress =
                (positionMs - rubyStart).toFloat() / rubyDuration.toFloat()
            return (
                completedCharacters + characterCount * segmentProgress.coerceIn(0f, 1f)
                ) / rubyCharacterCount.toFloat()
        }

        completedCharacters += characterCount
    }

    return 1f
}

/**
 * Native form of AMLL's moving CSS mask image for an already-resolved sweep progress.
 *
 * Upstream renders an image containing one word-width of bright mask, a fade whose width is
 * `wordHeight * wordFadeWidth`, and one word-width of dark mask. Moving that image is equivalent to
 * moving the fade center through the measured word width. The CSS clamp lets the fade travel half
 * a fade-width before word start and after word end so the word becomes fully dark / fully bright
 * outside that interval.
 */
internal fun resolveWordMaskGradientPx(
    wordLeftPx: Float,
    wordWidthPx: Float,
    wordHeightPx: Float,
    sweepProgress: Float,
    fadeWidthFactor: Float,
): WordMaskGradientPx {
    val width = wordWidthPx.coerceAtLeast(0f)
    val fadeWidth = (wordHeightPx.coerceAtLeast(0f) * fadeWidthFactor.coerceAtLeast(0f))
        .coerceAtLeast(0.0001f)
    val halfFade = fadeWidth / 2f
    val rawCenter = wordLeftPx + sweepProgress * width
    val center = rawCenter.coerceIn(
        minimumValue = wordLeftPx - halfFade,
        maximumValue = wordLeftPx + width + halfFade,
    )

    return WordMaskGradientPx(
        fadeStartX = center - halfFade,
        fadeEndX = center + halfFade,
    )
}

/** Plain-word compatibility overload retained for existing callers/tests. */
internal fun resolveWordMaskGradientPx(
    wordLeftPx: Float,
    wordWidthPx: Float,
    wordHeightPx: Float,
    startTimeMs: Long,
    endTimeMs: Long,
    positionMs: Long,
    fadeWidthFactor: Float,
): WordMaskGradientPx {
    val durationMs = (endTimeMs - startTimeMs).coerceAtLeast(1L)
    val progress = (positionMs - startTimeMs).toFloat() / durationMs.toFloat()
    return resolveWordMaskGradientPx(
        wordLeftPx = wordLeftPx,
        wordWidthPx = wordWidthPx,
        wordHeightPx = wordHeightPx,
        sweepProgress = progress,
        fadeWidthFactor = fadeWidthFactor,
    )
}

/** Ruby-aware overload used by the renderer. */
internal fun resolveWordMaskGradientPx(
    wordLeftPx: Float,
    wordWidthPx: Float,
    wordHeightPx: Float,
    word: LyricWord,
    positionMs: Long,
    fadeWidthFactor: Float,
): WordMaskGradientPx = resolveWordMaskGradientPx(
    wordLeftPx = wordLeftPx,
    wordWidthPx = wordWidthPx,
    wordHeightPx = wordHeightPx,
    sweepProgress = resolveWordMaskSweepProgress(word, positionMs),
    fadeWidthFactor = fadeWidthFactor,
)
