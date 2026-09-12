package dev.yaqmc.amll.model

import org.junit.Assert.assertEquals
import org.junit.Test

class LyricRubyTest {
    @Test
    fun rubyProgressUsesItsOwnTiming() {
        val ruby = LyricRuby(
            startTimeMs = 1_000,
            endTimeMs = 1_500,
            text = "か",
        )

        assertEquals(0f, ruby.progressAt(999), 0.0001f)
        assertEquals(0.5f, ruby.progressAt(1_250), 0.0001f)
        assertEquals(1f, ruby.progressAt(1_500), 0.0001f)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rubyRejectsNegativeStartTime() {
        LyricRuby(-1, 10, "x")
    }

    @Test(expected = IllegalArgumentException::class)
    fun rubyRejectsEndBeforeStart() {
        LyricRuby(100, 99, "x")
    }

    @Test
    fun addingRubyKeepsExistingWordDefaultsCompatible() {
        val word = LyricWord(0, 1_000, "歌", romanText = "uta")
        assertEquals("uta", word.romanText)
        assertEquals(emptyList<LyricRuby>(), word.ruby)
    }
}
