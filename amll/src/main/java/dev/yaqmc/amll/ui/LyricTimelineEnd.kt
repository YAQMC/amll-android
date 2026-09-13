package dev.yaqmc.amll.ui

/**
 * Upstream AMLL considers the lyric timeline ended once media time reaches the latest `endTime`
 * across every lyric group. A group's bound comes from its main lyric line; background-vocal timing
 * does not independently extend the group lifetime. The last group by start time is not necessarily
 * the one that ends last when main vocals overlap, so this still scans every group.
 */
internal fun lyricTimelineMaxEndTimeMs(groups: List<LyricLineGroup>): Long? {
    if (groups.isEmpty()) return null

    var maxEndTimeMs = 0L
    for (group in groups) {
        maxEndTimeMs = maxOf(maxEndTimeMs, group.main.endTimeMs)
    }
    return maxEndTimeMs
}

internal fun isLyricTimelineEndOfSong(
    groups: List<LyricLineGroup>,
    positionMs: Long,
): Boolean {
    val maxEndTimeMs = lyricTimelineMaxEndTimeMs(groups) ?: return false
    return positionMs >= maxEndTimeMs
}
