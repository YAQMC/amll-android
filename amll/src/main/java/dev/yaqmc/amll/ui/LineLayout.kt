package dev.yaqmc.amll.ui

internal const val AMLL_GROUP_VERTICAL_PADDING_EM = 0.4f
internal const val AMLL_GROUP_CONTENT_GAP_EM = 0.3f
internal const val AMLL_SUBLINE_FONT_SCALE = 0.5f
internal const val AMLL_SUBLINE_LINE_HEIGHT_EM = 0.75f
internal const val AMLL_DUET_SIDE_INSET_FRACTION = 0.15f

internal data class AMLLLineLayoutPx(
    val groupVerticalPaddingPx: Float,
    val groupContentGapPx: Float,
    val subLineFontPx: Float,
    val subLineHeightPx: Float,
)

/**
 * Converts the em-based geometry from upstream lyric-player.module.css into pixels.
 *
 * The DOM renderer inherits `em` from the lyric player's base/main font size, so all spacing here
 * intentionally scales from the same native main-line font measurement.
 */
internal fun resolveAMLLLineLayoutPx(baseFontPx: Float): AMLLLineLayoutPx {
    val safeBase = baseFontPx.coerceAtLeast(0f)
    return AMLLLineLayoutPx(
        groupVerticalPaddingPx = safeBase * AMLL_GROUP_VERTICAL_PADDING_EM,
        groupContentGapPx = safeBase * AMLL_GROUP_CONTENT_GAP_EM,
        subLineFontPx = safeBase * AMLL_SUBLINE_FONT_SCALE,
        subLineHeightPx = safeBase * AMLL_SUBLINE_LINE_HEIGHT_EM,
    )
}

internal fun lyricContentWidthFraction(hasDuetLine: Boolean): Float =
    if (hasDuetLine) 1f - AMLL_DUET_SIDE_INSET_FRACTION else 1f
