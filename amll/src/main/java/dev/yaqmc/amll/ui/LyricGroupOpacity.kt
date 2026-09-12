package dev.yaqmc.amll.ui

internal const val AMLL_HIGHLIGHTED_GROUP_ALPHA = 0.85f
internal const val AMLL_DYNAMIC_GROUP_ALPHA = 1f
internal const val AMLL_NON_DYNAMIC_GROUP_ALPHA = 0.2f
internal const val AMLL_HIDDEN_PASSED_GROUP_ALPHA = 1e-4f

/**
 * Native equivalent of upstream `LyricPlayerBase.resolveOpacity()`.
 *
 * The current Android player only consumes the dynamic/highlighted subset, but keeping the complete
 * rule here prevents future non-dynamic/hide-passed support from inventing different targets.
 */
internal fun resolveLyricGroupOpacity(
    isInViewport: Boolean,
    isHighlighted: Boolean,
    isNonDynamic: Boolean,
    hidePassedLines: Boolean,
    isPlaying: Boolean,
    isPassed: Boolean,
): Float {
    if (!isInViewport) return 0f

    if (hidePassedLines && isPlaying && isPassed) {
        return AMLL_HIDDEN_PASSED_GROUP_ALPHA
    }

    if (isHighlighted) return AMLL_HIGHLIGHTED_GROUP_ALPHA

    return if (isNonDynamic) AMLL_NON_DYNAMIC_GROUP_ALPHA else AMLL_DYNAMIC_GROUP_ALPHA
}

internal fun resolveDynamicLyricGroupOpacity(isHighlighted: Boolean): Float =
    resolveLyricGroupOpacity(
        isInViewport = true,
        isHighlighted = isHighlighted,
        isNonDynamic = false,
        hidePassedLines = false,
        isPlaying = true,
        isPassed = false,
    )
