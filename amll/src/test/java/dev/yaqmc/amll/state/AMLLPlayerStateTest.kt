package dev.yaqmc.amll.state

import dev.yaqmc.amll.model.LyricLine
import dev.yaqmc.amll.model.LyricWord
import org.junit.Assert.assertEquals
import org.junit.Test

class AMLLPlayerStateTest {
    private val lines = listOf(
        LyricLine(listOf(LyricWord(1000, 1500, "A"))),
        LyricLine(listOf(LyricWord(3000, 3500, "B"))),
        LyricLine(listOf(LyricWord(5000, 5500, "C"))),
    )

    @Test fun activeLineUsesLastStartedLineAcrossGaps() {
        assertEquals(0, findActiveLineIndex(lines, 0))
        assertEquals(0, findActiveLineIndex(lines, 2000))
        assertEquals(1, findActiveLineIndex(lines, 3200))
        assertEquals(2, findActiveLineIndex(lines, 9000))
    }

    @Test fun backgroundLineDoesNotStealPrimaryFocus() {
        val withBackground = listOf(
            LyricLine(listOf(LyricWord(1000, 2000, "Lead"))),
            LyricLine(
                words = listOf(LyricWord(1000, 1800, "BG")),
                isBackground = true,
            ),
            LyricLine(listOf(LyricWord(3000, 4000, "Next"))),
        )

        assertEquals(0, findActiveLineIndex(withBackground, 1500))
        assertEquals(0, findActiveLineIndex(withBackground, 2500))
        assertEquals(2, findActiveLineIndex(withBackground, 3200))
    }
}
