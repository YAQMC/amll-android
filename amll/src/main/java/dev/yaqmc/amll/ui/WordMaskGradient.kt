package dev.yaqmc.amll.ui

/** Horizontal bright-to-dark fade interval for one timed word, in lyric-layout coordinates. */
internal data class WordMaskGradientPx(
    val fadeStartX: Float,
    val fadeEndX: Float,
)

/**
 * Native form of AMLL's moving CSS mask image.
 *
 * Upstream renders an image containing one word-width of bright mask, a fade whose width is
 * `wordHeight * wordFadeWidth`, and one word-width of dark mask. Moving that image is equivalent to
 * moving the fade center linearly through the measured word width. The CSS clamp lets the fade
 * travel half a fade-width before word start and after word end so the word becomes fully dark /
 * fully bright outside that interval.
 */
internal fun resolveWordMaskGradientPx(
    wordLeftPx: Float,
    wordWidthPx: Float,
    wordHeightPx: Float,
    startTimeMs: Long,
    endTimeMs: Long,
    positionMs: Long,
    fadeWidthFactor: Float,
): WordMaskGradientPx {
    val width = wordWidthPx.coerceAtLeast(0f)
    val fadeWidth = (wordHeightPx.coerceAtLeast(0f) * fadeWidthFactor.coerceAtLeast(0f))
        .coerceAtLeast(0.0001f)
    val halfFade = fadeWidth / 2f
    val durationMs = (endTimeMs - startTimeMs).coerceAtLeast(1L)
    val rawCenter = wordLeftPx +
        (positionMs - startTimeMs).toFloat() / durationMs.toFloat() * width
    val center = rawCenter.coerceIn(
        minimumValue = wordLeftPx - halfFade,
        maximumValue = wordLeftPx + width + halfFade,
    )

    return WordMaskGradientPx(
        fadeStartX = center - halfFade,
        fadeEndX = center + halfFade,
    )
}
