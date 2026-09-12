package dev.yaqmc.amll.ui

internal fun resolveMainLineScaleTarget(
    isActive: Boolean,
    isPlaying: Boolean,
    enableScale: Boolean,
    activeScale: Float,
    inactiveScale: Float,
): Float = if (!enableScale || isActive || !isPlaying) activeScale else inactiveScale

/**
 * AMLL hides rows before scrollToIndex while playing. During an interlude its passed boundary moves
 * to the group immediately after the interlude anchor, so all rows through the anchor are passed.
 */
internal fun resolvePassedBoundary(
    activeGroupIndex: Int,
    interludeAnchorGroupIndex: Int?,
): Int = when {
    interludeAnchorGroupIndex != null -> interludeAnchorGroupIndex + 1
    activeGroupIndex >= 0 -> activeGroupIndex
    else -> 0
}
