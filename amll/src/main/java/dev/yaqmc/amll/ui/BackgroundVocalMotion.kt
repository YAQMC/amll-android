package dev.yaqmc.amll.ui

import kotlin.math.abs
import kotlin.math.roundToInt

internal const val AMLL_BACKGROUND_HIDDEN_SLIDE = 80f
internal const val AMLL_BACKGROUND_MIN_SCALE = 0.8f

internal data class BackgroundVocalFrame(
    val activeProgress: Float,
    val scale: Float,
    /** Translation as a fraction of the measured background height. */
    val translationYFraction: Float,
    /** Fraction of measured height contributed to layout for a background-first wrapper. */
    val firstOccupiedHeightFraction: Float,
)

internal data class BackgroundVocalLayoutFrame(
    val layoutHeightPx: Int,
    val childYpx: Int,
)

/**
 * Native equivalent of `LyricLineGroup.renderStyles()` background geometry.
 *
 * Upstream drives background vocals with a spring whose coordinate is in [-80, 80]. Both first
 * and post-positioned vocals derive translation + scale from the same coordinate. A background-first
 * wrapper additionally uses a negative measured-height margin, so its occupied height unfolds from
 * zero to the full measured height with the same progress.
 */
internal fun backgroundVocalFrame(slideY: Float): BackgroundVocalFrame {
    val progress = (1f - abs(slideY) / AMLL_BACKGROUND_HIDDEN_SLIDE).coerceIn(0f, 1f)
    return BackgroundVocalFrame(
        activeProgress = progress,
        scale = AMLL_BACKGROUND_MIN_SCALE + progress * (1f - AMLL_BACKGROUND_MIN_SCALE),
        translationYFraction = slideY / 100f,
        firstOccupiedHeightFraction = progress,
    )
}

internal fun backgroundHiddenSlideY(placedFirst: Boolean): Float =
    if (placedFirst) AMLL_BACKGROUND_HIDDEN_SLIDE else -AMLL_BACKGROUND_HIDDEN_SLIDE

/**
 * Resolves the layout box around the always-measured background child.
 *
 * Background-first wrappers remain in normal flow upstream and collapse themselves with a negative
 * measured-height margin, so their occupied height follows spring progress while the flex gap stays.
 * Post-positioned wrappers switch between relative (visible) and absolute (hidden), so their layout
 * contribution is full immediately when visible and zero immediately when hidden.
 */
internal fun backgroundVocalLayoutFrame(
    backgroundHeightPx: Int,
    gapPx: Int,
    placedFirst: Boolean,
    visible: Boolean,
    frame: BackgroundVocalFrame,
): BackgroundVocalLayoutFrame {
    val height = backgroundHeightPx.coerceAtLeast(0)
    val gap = gapPx.coerceAtLeast(0)
    val occupiedBackgroundHeight = when {
        placedFirst -> (height * frame.firstOccupiedHeightFraction).roundToInt()
        visible -> height
        else -> 0
    }
    val occupiedGap = if (placedFirst || visible) gap else 0
    val childY = if (placedFirst) -(height - occupiedBackgroundHeight) else occupiedGap

    return BackgroundVocalLayoutFrame(
        layoutHeightPx = occupiedBackgroundHeight + occupiedGap,
        childYpx = childY,
    )
}
