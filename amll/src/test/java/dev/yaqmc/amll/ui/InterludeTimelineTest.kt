package dev.yaqmc.amll.ui

import dev.yaqmc.amll.model.LyricLine
import dev.yaqmc.amll.model.LyricWord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class InterludeTimelineTest {
    @Test
    fun detectsInitialAndBetweenLineInterludesAtFourSeconds() {
        val groups = groupLyricLines(
            listOf(
                line(5_000, 7_000, "one"),
                line(11_000, 12_000, "two"),
                line(15_999, 17_000, "three"),
            ),
        )

        val interludes = calculateInterludes(groups)

        assertEquals(2, interludes.size)
        assertEquals(LyricInterlude(0, 5_000, -1), interludes[0])
        assertEquals(LyricInterlude(7_000, 11_000, 0), interludes[1])
    }

    @Test
    fun latestOverlappingEndPreventsFalseInterlude() {
        val groups = groupLyricLines(
            listOf(
                line(0, 10_000, "held"),
                line(4_000, 5_000, "overlap"),
                line(11_000, 12_000, "next"),
            ),
        )

        assertTrue(calculateInterludes(groups).isEmpty())
    }

    @Test
    fun activeInterludeUsesStartInclusiveEndExclusiveBounds() {
        val gap = LyricInterlude(4_000, 8_000, 0)
        val gaps = listOf(gap)

        assertEquals(gap, activeInterludeAt(gaps, 4_000))
        assertEquals(gap, activeInterludeAt(gaps, 7_999))
        assertNull(activeInterludeAt(gaps, 8_000))
    }

    @Test
    fun dotsFadeInSequenceAndFadeOutAtEnd() {
        val gap = LyricInterlude(0, 9_000, -1)

        assertEquals(0f, interludeVisualStateAt(gap, 250).globalAlpha, 0.0001f)
        assertTrue(interludeVisualStateAt(gap, 750).globalAlpha in 0.49f..0.51f)

        val middle = interludeVisualStateAt(gap, 4_500)
        assertTrue(middle.scale > 0.5f)
        assertTrue(middle.dotAlphas[0] >= middle.dotAlphas[1])
        assertTrue(middle.dotAlphas[1] >= middle.dotAlphas[2])

        val ending = interludeVisualStateAt(gap, 8_900)
        assertTrue(ending.globalAlpha < 0.3f)
    }

    @Test
    fun interludeBecomesItsOwnFocusableListItem() {
        val groups = groupLyricLines(
            listOf(
                line(0, 1_000, "a"),
                line(5_000, 6_000, "b"),
            ),
        )
        val gaps = calculateInterludes(groups)
        val items = buildLyricListItems(groups, gaps)

        assertEquals(3, items.size)
        assertTrue(items[1] is LyricListItem.Interlude)
        assertEquals(1, lyricFocusItemIndex(items, activeGroupIndex = 0, activeInterlude = gaps.single()))
    }

    private fun line(start: Long, end: Long, text: String): LyricLine = LyricLine(
        words = listOf(LyricWord(start, end, text)),
    )
}
