package dev.yaqmc.amll.state

import dev.yaqmc.amll.model.LyricLine
import dev.yaqmc.amll.model.LyricRuby
import dev.yaqmc.amll.model.LyricWord
import dev.yaqmc.amll.model.MaskObsceneWordsMode
import org.junit.Assert.assertEquals
import org.junit.Test

class ObsceneWordMaskTest {
    @Test fun disabledModeLeavesTextUntouched() {
        assertEquals(
            "  word  ",
            maskObsceneText(
                text = "  word  ",
                mode = MaskObsceneWordsMode.Disabled,
                maskChar = '*',
            ),
        )
    }

    @Test fun fullMaskReplacesOnlyNonWhitespaceCodeUnits() {
        assertEquals(
            "  **** ****  ",
            maskObsceneText(
                text = "  bad! word  ",
                mode = MaskObsceneWordsMode.FullMask,
                maskChar = '*',
            ),
        )
    }

    @Test fun partialMaskKeepsFirstAndLastVisibleCharacters() {
        assertEquals(
            "  s***r  ",
            maskObsceneText(
                text = "  swear  ",
                mode = MaskObsceneWordsMode.PartialMask,
                maskChar = '*',
            ),
        )
        assertEquals(
            "  **  ",
            maskObsceneText(
                text = "  hi  ",
                mode = MaskObsceneWordsMode.PartialMask,
                maskChar = '*',
            ),
        )
    }

    @Test fun maskingChangesOnlyObsceneBaseText() {
        val ruby = listOf(LyricRuby(100, 300, "pronunciation"))
        val clean = LyricWord(
            startTimeMs = 0,
            endTimeMs = 400,
            text = "clean",
            romanText = "clean-roman",
            obscene = false,
            ruby = ruby,
        )
        val obscene = LyricWord(
            startTimeMs = 400,
            endTimeMs = 800,
            text = "damn",
            romanText = "damn-roman",
            obscene = true,
            ruby = ruby,
        )
        val masked = applyObsceneWordMask(
            lines = listOf(LyricLine(words = listOf(clean, obscene))),
            mode = MaskObsceneWordsMode.FullMask,
            maskChar = '*',
        ).single()

        assertEquals("clean", masked.words[0].text)
        assertEquals("****", masked.words[1].text)
        assertEquals("damn-roman", masked.words[1].romanText)
        assertEquals(ruby, masked.words[1].ruby)
        assertEquals(obscene.startTimeMs, masked.words[1].startTimeMs)
        assertEquals(obscene.endTimeMs, masked.words[1].endTimeMs)
    }

    @Test fun playerStateAlwaysReprocessesFromOriginalLyrics() {
        val state = AMLLPlayerState(
            lyricLines = listOf(
                LyricLine(
                    words = listOf(
                        LyricWord(
                            startTimeMs = 0,
                            endTimeMs = 1_000,
                            text = "fuck",
                            obscene = true,
                        ),
                    ),
                ),
            ),
            initialMaskObsceneWordsMode = MaskObsceneWordsMode.FullMask,
        )

        assertEquals("****", state.lyricLines.single().words.single().text)

        state.updateMaskObsceneWordsMode(MaskObsceneWordsMode.PartialMask)
        assertEquals("f**k", state.lyricLines.single().words.single().text)

        state.updateMaskObsceneWordsMode(MaskObsceneWordsMode.Disabled)
        assertEquals("fuck", state.lyricLines.single().words.single().text)

        state.updateMaskObsceneWordChar('#')
        state.updateMaskObsceneWordsMode(MaskObsceneWordsMode.FullMask)
        assertEquals("####", state.lyricLines.single().words.single().text)
    }
}
