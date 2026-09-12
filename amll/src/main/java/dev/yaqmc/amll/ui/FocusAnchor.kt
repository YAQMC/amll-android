package dev.yaqmc.amll.ui

import kotlin.math.max

/** Native equivalent of upstream LayoutAlignAnchor. */
enum class AMLLAlignAnchor {
    Top,
    Center,
    Bottom,
}

internal data class FocusEdgePaddingPx(
    val before: Float,
    val after: Float,
)

/**
 * Resolves the target item's top Y using AMLL's viewport-relative alignment model.
 *
 * Upstream first picks `viewportHeight * alignPosition`, then subtracts an offset inside the target
 * according to its anchor: 0 for top, half height for center, full height for bottom.
 */
internal fun resolveFocusItemTopPx(
    viewportStartPx: Int,
    viewportHeightPx: Int,
    targetHeightPx: Int,
    alignPosition: Float,
    alignAnchor: AMLLAlignAnchor,
): Float {
    val targetAnchorOffset = when (alignAnchor) {
        AMLLAlignAnchor.Top -> 0f
        AMLLAlignAnchor.Center -> targetHeightPx / 2f
        AMLLAlignAnchor.Bottom -> targetHeightPx.toFloat()
    }
    return viewportStartPx + viewportHeightPx * alignPosition - targetAnchorOffset
}

/**
 * LazyColumn needs enough real edge space to reproduce AMLL's absolute-layout alignment for the
 * first and last lyric items. Upstream itself does not clamp alignPosition, so negative/outside
 * positions simply collapse the impossible side to the configured minimum padding.
 */
internal fun resolveFocusEdgePaddingPx(
    viewportHeightPx: Int,
    alignPosition: Float,
    minimumPaddingPx: Float,
): FocusEdgePaddingPx {
    val height = viewportHeightPx.coerceAtLeast(0).toFloat()
    val minimum = minimumPaddingPx.coerceAtLeast(0f)
    return FocusEdgePaddingPx(
        before = max(minimum, height * alignPosition).coerceAtLeast(0f),
        after = max(minimum, height * (1f - alignPosition)).coerceAtLeast(0f),
    )
}
