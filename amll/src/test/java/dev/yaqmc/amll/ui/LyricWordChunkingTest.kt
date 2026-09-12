package dev.yaqmc.amll.ui

import dev.yaqmc.amll.model.LyricRuby
import dev.yaqmc.amll.model.LyricWord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LyricWordChunkingTest {
    private fun word(
        text: String,
        start: Long,
        end: Long,
        roman: String? = null,
        ruby: List<LyricRuby> = emptyList(),
    ) = LyricWord(start, end, text, romanText = roman, ruby = ruby)

    @Test fun consecutiveLatinFragmentsWithoutSpaceShareOneLayoutChunk() {
        val plan = chunkAndSplitLyricWords(
            listOf(
                word(" su", 0, 300),
                word("gar so", 300, 900),
                word("sweet", 900, 1200),
            )
        )

        assertEquals(listOf(" ", "sugar", " ", "sosweet"), plan.chunks.map { it.text })
        assertEquals(listOf(" ", "su", "gar", " ", "so", "sweet"), plan.words.map { it.text })
    }

    @Test fun splitWordTimingIsDistributedByNonWhitespaceLength() {
        val plan = chunkAndSplitLyricWords(listOf(word("ab cd", 1000, 1400)))
        val atoms = plan.words

        assertEquals(listOf("ab", " ", "cd"), atoms.map { it.text })
        assertEquals(1000L, atoms[0].startTimeMs)
        assertEquals(1200L, atoms[0].endTimeMs)
        assertEquals(1200L, atoms[1].startTimeMs)
        assertEquals(1200L, atoms[1].endTimeMs)
        assertEquals(1200L, atoms[2].startTimeMs)
        assertEquals(1400L, atoms[2].endTimeMs)
    }

    @Test fun matchingRomanPartsFollowSplitBaseWords() {
        val plan = chunkAndSplitLyricWords(
            listOf(word("ni hao", 0, 600, roman = "nǐ hǎo"))
        )

        assertEquals(listOf("ni", " ", "hao"), plan.words.map { it.text })
        assertEquals(listOf("nǐ", "", "hǎo"), plan.words.map { it.romanText.orEmpty() })
    }

    @Test fun unmatchedRomanTextStaysOnFirstNonSpacePart() {
        val plan = chunkAndSplitLyricWords(
            listOf(word("hello world", 0, 1000, roman = "single"))
        )

        assertEquals("single", plan.words[0].romanText)
        assertEquals("", plan.words.last().romanText)
    }

    @Test fun cjkWithoutRomanizationSplitsIntoTimedCharacters() {
        val plan = chunkAndSplitLyricWords(listOf(word("你好", 1000, 1600)))

        assertEquals(listOf("你", "好"), plan.words.map { it.text })
        assertEquals(1000L, plan.words[0].startTimeMs)
        assertEquals(1300L, plan.words[0].endTimeMs)
        assertEquals(1300L, plan.words[1].startTimeMs)
        assertEquals(1600L, plan.words[1].endTimeMs)
        assertEquals(2, plan.chunks.size)
    }

    @Test fun cjkWithRomanizationRemainsOneAtom() {
        val plan = chunkAndSplitLyricWords(listOf(word("你好", 1000, 1600, roman = "ni hao")))

        assertEquals(1, plan.words.size)
        assertEquals("你好", plan.words.single().text)
        assertEquals("ni hao", plan.words.single().romanText)
    }

    @Test fun rubyWordKeepsContentAtomicAndMovesEdgeSpacesOutside() {
        val ruby = listOf(LyricRuby(100, 300, "か"))
        val plan = chunkAndSplitLyricWords(
            listOf(word("  歌 ", 100, 500, roman = "uta", ruby = ruby))
        )

        assertEquals(listOf("  ", "歌", " "), plan.words.map { it.text })
        assertEquals(100L, plan.words.first().startTimeMs)
        assertEquals(100L, plan.words.first().endTimeMs)
        assertEquals(ruby, plan.words[1].ruby)
        assertTrue(plan.words.first().ruby.isEmpty())
        assertEquals(500L, plan.words.last().startTimeMs)
        assertEquals(500L, plan.words.last().endTimeMs)
    }

    @Test fun mergedLatinChunkCanEmphasizeEvenWhenChildrenAreTooShort() {
        val plan = chunkAndSplitLyricWords(
            listOf(
                word("su", 0, 500),
                word("gar", 500, 1000),
            )
        )
        val chunk = plan.chunks.single()

        assertFalse(shouldEmphasize(chunk.words[0]))
        assertFalse(shouldEmphasize(chunk.words[1]))
        assertEquals("sugar", chunk.emphasisWord.text)
        assertEquals(0L, chunk.emphasisWord.startTimeMs)
        assertEquals(1000L, chunk.emphasisWord.endTimeMs)
        assertTrue(chunk.emphasized)
    }

    @Test fun trailingWhitespaceDoesNotBecomeLastEmphasizedChunk() {
        val plan = chunkAndSplitLyricWords(
            listOf(word("hello ", 0, 1000))
        )

        assertEquals(0, plan.lastContentChunkIndex)
        assertTrue(plan.chunks.last().isSpace)
    }
}
