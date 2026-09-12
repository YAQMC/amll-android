package dev.yaqmc.amll.ui

import kotlin.math.abs
import kotlin.math.min

private const val AMLL_MAX_BLUR_PX = 5f

/**
 * Port of AMLL's distance-based lyric blur rule.
 *
 * The line above the focus is intentionally one blur step stronger than the line below it,
 * matching upstream's `scrollToIndex` / `latestHighlightedIndex` calculation.
 */
internal fun resolveBlurRadiusPx(
    index: Int,
    scrollToIndex: Int,
    latestHighlightedIndex: Int,
    isFocused: Boolean,
    autoAlignSuspended: Boolean,
    isNarrowViewport: Boolean,
    enabled: Boolean,
): Float {
    if (!enabled || autoAlignSuspended || isFocused) return 0f

    val distance = if (index < scrollToIndex) {
        abs(scrollToIndex - index) + 1
    } else {
        abs(index - latestHighlightedIndex)
    }

    val level = 1f + distance
    val adjusted = if (isNarrowViewport) level * 0.8f else level
    return min(AMLL_MAX_BLUR_PX, adjusted)
}
