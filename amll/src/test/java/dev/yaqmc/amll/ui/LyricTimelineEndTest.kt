package dev.yaqmc.amll.ui

import dev.yaqmc.amll.model.LyricLine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LyricTimelineEndTest {
    @Test fun emptyTimelineNeverEnds() {
        assertNull(lyricTimelineMaxEndTimeMs(emptyList()))
        assertFalse(isLyricTimelineEndOfSong(emptyList(), 10_000L))
    }

    @Test fun maximumEndTimeIsNotAssumedToBelongToLastStartingLine() {
        val groups = groupLyricLines(
            listOf(
                line(start = 0L, end = 10_000L),
                line(start = 9_000L, end = 9_500L),
            )
        )

        assertEquals(10_000L, lyricTimelineMaxEndTimeMs(groups))
        assertFalse(isLyricTimelineEndOfSong(groups, 9_999L))
        assertTrue(isLyricTimelineEndOfSong(groups, 10_000L))
    }

    @Test fun backgroundVocalDoesNotExtendTheMainGroupTimelineBound() {
        val groups = groupLyricLines(
            listOf(
                line(start = 0L, end = 5_000L),
                line(start = 1_000L, end = 7_000L, background = true),
            )
        )

        assertEquals(5_000L, lyricTimelineMaxEndTimeMs(groups))
        assertFalse(isLyricTimelineEndOfSong(groups, 4_999L))
        assertTrue(isLyricTimelineEndOfSong(groups, 5_000L))
    }

    @Test fun bottomLineBecomesTheEndOfSongFocusWhenPresent() {
        val groups = groupLyricLines(
            listOf(
                line(start = 0L, end = 1_000L),
                line(start = 2_000L, end = 3_000L),
            )
        )
        val items = buildLyricListItems(
            groups = groups,
            interludes = emptyList(),
            includeBottomLine = true,
        )

        val focusIndex = lyricFocusItemIndex(
            items = items,
            activeGroupIndex = 1,
            activeInterlude = null,
            isEndOfSong = true,
            hasBottomLine = true,
        )

        assertEquals(items.lastIndex, focusIndex)
        assertTrue(items[focusIndex] is LyricListItem.BottomLine)
    }

    @Test fun finalLyricGroupBecomesTheEndOfSongFocusWithoutBottomContent() {
        val groups = groupLyricLines(
            listOf(
                line(start = 0L, end = 10_000L),
                line(start = 9_000L, end = 9_500L),
            )
        )
        val items = buildLyricListItems(groups, emptyList())

        val focusIndex = lyricFocusItemIndex(
            items = items,
            activeGroupIndex = 0,
            activeInterlude = null,
            isEndOfSong = true,
            hasBottomLine = false,
        )

        val focused = items[focusIndex] as LyricListItem.Group
        assertEquals(groups.lastIndex, focused.groupIndex)
    }

    @Test fun normalPlaybackStillFollowsTheActiveGroup() {
        val groups = groupLyricLines(
            listOf(
                line(start = 0L, end = 1_000L),
                line(start = 2_000L, end = 3_000L),
            )
        )
        val items = buildLyricListItems(
            groups = groups,
            interludes = emptyList(),
            includeBottomLine = true,
        )

        val focusIndex = lyricFocusItemIndex(
            items = items,
            activeGroupIndex = 0,
            activeInterlude = null,
            isEndOfSong = false,
            hasBottomLine = true,
        )

        val focused = items[focusIndex] as LyricListItem.Group
        assertEquals(0, focused.groupIndex)
    }

    private fun line(
        start: Long,
        end: Long,
        background: Boolean = false,
    ): LyricLine = LyricLine(
        words = emptyList(),
        startTimeMs = start,
        endTimeMs = end,
        isBackground = background,
    )
}
