package dev.yaqmc.amll.ui

import dev.yaqmc.amll.model.LyricWord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WordMotionTest {
    @Test fun emphasizeRuleMatchesUpstreamShape() {
        assertTrue(shouldEmphasize(LyricWord(0, 1_000, "啊")))
        assertTrue(shouldEmphasize(LyricWord(0, 1_000, "love")))
        assertFalse(shouldEmphasize(LyricWord(0, 1_000, "I")))
        assertFalse(shouldEmphasize(LyricWord(0, 999, "love")))
        assertFalse(shouldEmphasize(LyricWord(0, 1_000, "toolongword")))
    }

    @Test fun regularWordFloatsUpAndBackgroundDoublesDistance() {
        val word = LyricWord(1_000, 1_300, "I")

        assertEquals(0f, wordMotionAt(word, 1_000, false, false).translateYEm, 0.0001f)
        assertEquals(-0.05f, wordMotionAt(word, 2_000, false, false).translateYEm, 0.0001f)
        assertEquals(-0.10f, wordMotionAt(word, 2_000, true, false).translateYEm, 0.0001f)
    }

    @Test fun emphasizedWordPulsesAndReturnsToBaseScale() {
        val word = LyricWord(1_000, 2_000, "love")

        val middle = wordMotionAt(word, 1_500, false, false)
        val end = wordMotionAt(word, 2_000, false, false)

        assertTrue(middle.scale > 1f)
        assertTrue(middle.emphasis > 0.9f)
        assertEquals(1f, end.scale, 0.0001f)
        assertEquals(0f, end.emphasis, 0.0001f)
    }
}
