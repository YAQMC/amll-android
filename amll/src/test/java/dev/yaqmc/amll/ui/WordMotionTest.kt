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

    @Test fun everyWordKeepsRegularFloatAndBackgroundDoublesDistance() {
        val shortWord = LyricWord(1_000, 1_300, "I")
        val emphasizedWord = LyricWord(1_000, 2_000, "love")

        assertEquals(0f, wordMotionAt(shortWord, 1_000, false).translateYEm, 0.0001f)
        assertEquals(-0.05f, wordMotionAt(shortWord, 2_000, false).translateYEm, 0.0001f)
        assertEquals(-0.10f, wordMotionAt(shortWord, 2_000, true).translateYEm, 0.0001f)
        assertTrue(wordMotionAt(emphasizedWord, 1_500, false).translateYEm < 0f)
    }

    @Test fun emphasizedCharactersAreStaggeredAndPushAwayFromWordCenter() {
        val word = LyricWord(1_000, 2_000, "love")

        val first = characterMotionAt(word, 1_500, 0, 4, false, false)
        val last = characterMotionAt(word, 1_500, 3, 4, false, false)

        assertTrue(first.scale > last.scale)
        assertTrue(first.glowAlpha > last.glowAlpha)
        assertTrue(first.translateXEm < 0f)
        assertTrue(last.translateXEm > 0f)
    }

    @Test fun emphasizedCharacterFloatStartsBeforeWordAndBackgroundDoublesIt() {
        val word = LyricWord(1_000, 2_000, "love")

        val normal = characterMotionAt(word, 800, 0, 4, false, false)
        val background = characterMotionAt(word, 800, 0, 4, true, false)

        assertTrue(normal.translateYEm < 0f)
        assertEquals(normal.translateYEm * 2f, background.translateYEm, 0.0001f)
        assertEquals(1f, normal.scale, 0.0001f)
        assertEquals(0f, normal.glowAlpha, 0.0001f)
    }

    @Test fun finalWordGetsStrongerEmphasis() {
        val word = LyricWord(1_000, 2_000, "love")

        val ordinaryPeak = characterMotionAt(word, 1_500, 0, 4, false, false)
        val finalPeak = characterMotionAt(word, 1_600, 0, 4, false, true)

        assertTrue(finalPeak.scale > ordinaryPeak.scale)
        assertTrue(finalPeak.glowAlpha > ordinaryPeak.glowAlpha)
        assertTrue(finalPeak.glowRadiusEm > ordinaryPeak.glowRadiusEm)
    }
}
