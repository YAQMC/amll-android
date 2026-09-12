package dev.yaqmc.amll.ui

import dev.yaqmc.amll.model.LyricWord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BalancedLineLayoutTest {
    @Test fun noBreakWhenEverythingFits() {
        val children = listOf(
            BalancedLineChild(30f, "hello"),
            BalancedLineChild(20f, " world"),
        )

        assertTrue(calculateBalancedBreaks(children, 80f).isEmpty())
    }

    @Test fun equalWordsBalanceAcrossRows() {
        val children = List(4) { index -> BalancedLineChild(40f, "w$index") }

        assertEquals(listOf(2), calculateBalancedBreaks(children, 100f))
    }

    @Test fun punctuationBoundaryIsPreferredWhenCostsAreClose() {
        val children = listOf(
            BalancedLineChild(30f, "one"),
            BalancedLineChild(30f, ","),
            BalancedLineChild(30f, "two"),
            BalancedLineChild(30f, "three"),
        )

        assertEquals(listOf(2), calculateBalancedBreaks(children, 70f))
    }

    @Test fun oversizedWordRemainsAtomic() {
        val children = listOf(
            BalancedLineChild(140f, "supercalifragilistic"),
            BalancedLineChild(30f, " next"),
        )

        assertEquals(listOf(1), calculateBalancedBreaks(children, 100f))
    }

    @Test fun hardBreakOffsetsAccountForInsertedNewlines() {
        val layout = buildBalancedLyricLayout(
            wordTexts = listOf("alpha", " beta", " gamma", " delta"),
            wordWidthsPx = listOf(40f, 40f, 40f, 40f),
            containerWidthPx = 100f,
        )

        assertEquals(listOf(2), layout.breaks)
        assertEquals("alpha beta\n gamma delta", layout.text)
        assertEquals(0, layout.wordOffsets[0])
        assertEquals(5, layout.wordOffsets[1])
        assertEquals(11, layout.wordOffsets[2])
        assertEquals(17, layout.wordOffsets[3])
    }

    @Test fun chunkBreakKeepsOffsetsForEveryTimedChildWord() {
        val plan = RenderWordPlan(
            listOf(
                RenderWordChunk(
                    listOf(
                        LyricWord(0, 400, "su"),
                        LyricWord(400, 800, "gar"),
                    )
                ),
                RenderWordChunk(listOf(LyricWord(800, 800, " "))),
                RenderWordChunk(listOf(LyricWord(800, 1200, "sweet"))),
            )
        )

        val layout = buildBalancedLyricLayout(
            plan = plan,
            chunkWidthsPx = listOf(80f, 10f, 80f),
            containerWidthPx = 100f,
        )

        assertEquals("sugar \nsweet", layout.text)
        assertEquals(0, layout.wordOffsets[0])
        assertEquals(2, layout.wordOffsets[1])
        assertEquals(5, layout.wordOffsets[2])
        assertEquals(7, layout.wordOffsets[3])
    }
}
