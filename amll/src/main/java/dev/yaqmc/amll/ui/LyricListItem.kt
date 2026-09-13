package dev.yaqmc.amll.ui

internal sealed interface LyricListItem {
    val stableKey: String

    data class Group(
        val groupIndex: Int,
        val group: LyricLineGroup,
    ) : LyricListItem {
        override val stableKey: String = "line:${group.main.startTimeMs}:$groupIndex"
    }

    data class Interlude(
        val interlude: LyricInterlude,
    ) : LyricListItem {
        override val stableKey: String =
            "interlude:${interlude.startTimeMs}:${interlude.endTimeMs}:${interlude.anchorGroupIndex}"
    }

    data object BottomLine : LyricListItem {
        override val stableKey: String = "bottom-line"
    }
}

internal fun buildLyricListItems(
    groups: List<LyricLineGroup>,
    interludes: List<LyricInterlude>,
    includeBottomLine: Boolean = false,
): List<LyricListItem> {
    if (groups.isEmpty()) return emptyList()

    val interludeByAnchor = interludes.associateBy(LyricInterlude::anchorGroupIndex)
    val bottomLineCount = if (includeBottomLine) 1 else 0
    val result = ArrayList<LyricListItem>(groups.size + interludes.size + bottomLineCount)

    interludeByAnchor[-1]?.let { result += LyricListItem.Interlude(it) }
    groups.forEachIndexed { index, group ->
        result += LyricListItem.Group(index, group)
        interludeByAnchor[index]?.let { result += LyricListItem.Interlude(it) }
    }
    if (includeBottomLine) {
        result += LyricListItem.BottomLine
    }
    return result
}

internal fun lyricFocusItemIndex(
    items: List<LyricListItem>,
    activeGroupIndex: Int,
    activeInterlude: LyricInterlude?,
    isEndOfSong: Boolean = false,
    hasBottomLine: Boolean = false,
): Int {
    if (activeInterlude != null) {
        return items.indexOfFirst { item ->
            item is LyricListItem.Interlude && item.interlude == activeInterlude
        }
    }

    if (isEndOfSong) {
        if (hasBottomLine) {
            val bottomIndex = items.indexOfFirst { it is LyricListItem.BottomLine }
            if (bottomIndex >= 0) return bottomIndex
        }
        return items.indexOfLast { it is LyricListItem.Group }
    }

    return items.indexOfFirst { item ->
        item is LyricListItem.Group && item.groupIndex == activeGroupIndex
    }
}
