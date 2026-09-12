package dev.yaqmc.amll.ui

import dev.yaqmc.amll.model.LyricLine
import dev.yaqmc.amll.model.LyricWord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LyricLineGroupingTest {
    private fun line(text: String, start: Long, background: Boolean = false) = LyricLine(
        words = listOf(LyricWord(start, start + 1000, text)),
        isBackground = background,
    )

    @Test fun backgroundLineIsAttachedToPreviousPrimaryLine() {
        val groups = groupLyricLines(
            listOf(
                line("main", 1000),
                line("bg", 1000, background = true),
                line("next", 3000),
            )
        )

        assertEquals(2, groups.size)
        assertEquals("main", groups[0].main.text)
        assertEquals("bg", groups[0].background?.text)
        assertEquals(0, groups[0].mainIndex)
        assertEquals("next", groups[1].main.text)
        assertEquals(2, groups[1].mainIndex)
        assertNull(groups[1].background)
    }

    @Test fun secondBackgroundLineRemainsAddressableInsteadOfBeingDropped() {
        val groups = groupLyricLines(
            listOf(
                line("main", 1000),
                line("bg1", 1000, background = true),
                line("bg2", 1100, background = true),
            )
        )

        assertEquals(2, groups.size)
        assertEquals("bg1", groups[0].background?.text)
        assertEquals("bg2", groups[1].main.text)
    }
}
