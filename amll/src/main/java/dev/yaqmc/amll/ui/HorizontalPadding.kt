package dev.yaqmc.amll.ui

internal const val AMLL_NARROW_PADDING_DP = 20f
internal const val AMLL_NARROW_PADDING_BREAKPOINT_DP = 500f

/**
 * Mirrors `--lyric-line-padding-x`: 20 CSS px on <=500px viewports, otherwise 1em.
 * A fixed host override wins when supplied.
 */
internal fun resolveAMLLHorizontalPaddingPx(
    viewportWidthPx: Int,
    density: Float,
    lineFontSizePx: Float,
    fixedPaddingPx: Float? = null,
): Float {
    fixedPaddingPx?.let { return it.coerceAtLeast(0f) }

    val safeDensity = density.takeIf { it > 0f && it.isFinite() } ?: 1f
    val viewportWidthDp = viewportWidthPx.coerceAtLeast(0) / safeDensity
    return if (viewportWidthDp <= AMLL_NARROW_PADDING_BREAKPOINT_DP) {
        AMLL_NARROW_PADDING_DP * safeDensity
    } else {
        lineFontSizePx.coerceAtLeast(0f)
    }
}
