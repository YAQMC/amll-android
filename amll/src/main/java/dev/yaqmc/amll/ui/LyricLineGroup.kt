package dev.yaqmc.amll.ui

import dev.yaqmc.amll.model.LyricLine

internal data class LyricLineGroup(
    val mainIndex: Int,
    val main: LyricLine,
    val background: LyricLine? = null,
) {
    /**
     * AMLL compares the first real word timestamps rather than line startTime because optimized
     * lyrics may synchronize the main/background line timestamps while preserving vocal order.
     */
    val backgroundStartsFirst: Boolean
        get() {
            val bg = background ?: return false
            val bgStart = bg.words.firstOrNull()?.startTimeMs ?: bg.startTimeMs
            val mainStart = main.words.firstOrNull()?.startTimeMs ?: main.startTimeMs
            return bgStart < mainStart
        }
}

internal fun shouldPlaceBackgroundFirst(
    group: LyricLineGroup,
    alwaysPostpositionBackground: Boolean,
): Boolean = !alwaysPostpositionBackground && group.backgroundStartsFirst

/**
 * AMLL treats one background-vocal line as part of the preceding primary line rather than
 * as an independent scroll item. Keep that relationship in the native renderer as well.
 */
internal fun groupLyricLines(lines: List<LyricLine>): List<LyricLineGroup> {
    if (lines.isEmpty()) return emptyList()

    val groups = mutableListOf<LyricLineGroup>()
    lines.forEachIndexed { index, line ->
        if (line.isBackground && groups.isNotEmpty() && groups.last().background == null) {
            val last = groups.last()
            groups[groups.lastIndex] = last.copy(background = line)
        } else {
            groups += LyricLineGroup(mainIndex = index, main = line)
        }
    }
    return groups
}
