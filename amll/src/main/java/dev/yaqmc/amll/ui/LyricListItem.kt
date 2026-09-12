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
}

internal fun buildLyricListItems(
    groups: List<LyricLineGroup>,
    interludes: List<LyricInterlude>,
): List<LyricListItem> {
    if (groups.isEmpty()) return emptyList()

    val interludeByAnchor = interludes.associateBy(LyricInterlude::anchorGroupIndex)
    val result = ArrayList<LyricListItem>(groups.size + interludes.size)

    interludeByAnchor[-1]?.let { result += LyricListItem.Interlude(it) }
    groups.forEachIndexed { index, group ->
        result += LyricListItem.Group(index, group)
        interludeByAnchor[index]?.let { result += LyricListItem.Interlude(it) }
    }
    return result
}

internal fun lyricFocusItemIndex(
    items: List<LyricListItem>,
    activeGroupIndex: Int,
    activeInterlude: LyricInterlude?,
): Int {
    if (activeInterlude != null) {
        return items.indexOfFirst { item ->
            item is LyricListItem.Interlude && item.interlude == activeInterlude
        }
    }
    return items.indexOfFirst { item ->
        item is LyricListItem.Group && item.groupIndex == activeGroupIndex
    }
}
