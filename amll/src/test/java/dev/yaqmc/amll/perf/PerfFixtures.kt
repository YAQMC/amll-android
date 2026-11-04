package dev.yaqmc.amll.perf

import dev.yaqmc.amll.model.LyricLine
import dev.yaqmc.amll.model.LyricRuby
import dev.yaqmc.amll.model.LyricWord

/**
 * Deterministic synthetic lyric documents for the render/perf harness. No external data: every
 * fixture is a pure function of its parameters so baseline and candidate runs see identical input.
 */
internal object PerfFixtures {
    private const val CJK = "夜空に光る星の歌声が遠く響いて心の奥まで届く日々"
    private val LATIN = listOf(
        "whispering", "through", "the", "silver", "evening", "light", "we", "follow",
        "everything", "that", "glows", "beyond", "horizons", "forever",
    )

    /** Word-level CJK, one grapheme per word, translation + romanization on alternating lines. */
    fun denseCjk(lines: Int = 60): List<LyricLine> {
        var t = 2_000L
        return List(lines) { lineIndex ->
            val words = List(16) { i ->
                val start = t + i * 180L
                LyricWord(
                    startTimeMs = start,
                    endTimeMs = start + 180L,
                    text = CJK[(lineIndex * 7 + i) % CJK.length].toString(),
                    romanText = if (lineIndex % 8 == 0) "ka" else null,
                )
            }
            t += 16 * 180L + 300L
            LyricLine(
                words = words,
                translatedLyric = if (lineIndex % 2 == 0) "Stars sing across the night sky" else "",
                romanLyric = if (lineIndex % 2 == 0) "yozora ni hikaru hoshi no utagoe" else "",
            )
        }
    }

    /** Long Latin lines with emphasis-length words (>= 1 s) and ruby on a few words. */
    fun latinLongRuby(lines: Int = 40): List<LyricLine> {
        var t = 1_000L
        return List(lines) { lineIndex ->
            val words = LATIN.mapIndexed { i, text ->
                val duration = if (i % 5 == 4) 1_200L else 260L
                val start = t
                t += duration
                LyricWord(
                    startTimeMs = start,
                    endTimeMs = start + duration,
                    text = if (i == LATIN.lastIndex) text else "$text ",
                    ruby = if (lineIndex % 4 == 1 && i == 3) {
                        listOf(
                            LyricRuby(start, start + duration / 2, "しろ"),
                            LyricRuby(start + duration / 2, start + duration, "がね"),
                        )
                    } else {
                        emptyList()
                    },
                )
            }
            t += 400L
            LyricLine(words = words)
        }
    }

    /** Alternating duet lines, overlapping background vocals, and long gaps that create interludes. */
    fun duetBackgroundInterlude(lines: Int = 40): List<LyricLine> {
        var t = 1_000L
        val out = ArrayList<LyricLine>()
        repeat(lines) { lineIndex ->
            val words = List(8) { i ->
                val start = t + i * 220L
                LyricWord(start, start + 220L, if (i == 7) "voice" else "voice ")
            }
            out += LyricLine(words = words, isDuet = lineIndex % 2 == 1)
            if (lineIndex % 3 == 0) {
                val bgWords = List(4) { i ->
                    val start = t + 400L + i * 300L
                    LyricWord(start, start + 300L, if (i == 3) "ooh" else "ooh ")
                }
                out += LyricLine(words = bgWords, isBackground = true, isDuet = lineIndex % 2 == 1)
            }
            t += 8 * 220L + if (lineIndex % 10 == 9) 9_000L else 350L
        }
        return out
    }

    /** Emoji ZWJ sequences, flags, combining marks and CJK in one document (text-boundary cases). */
    fun emojiCombining(lines: Int = 30): List<LyricLine> {
        val tokens = listOf("👨‍👩‍👧 ", "🇯🇵 ", "été ", "愛 ", "não ", "🎶", "ก่", "x")
        var t = 1_000L
        return List(lines) { lineIndex ->
            val words = List(tokens.size) { i ->
                val start = t + i * 240L
                LyricWord(start, start + 240L, tokens[(i + lineIndex) % tokens.size])
            }
            t += tokens.size * 240L + 300L
            LyricLine(words = words, translatedLyric = if (lineIndex % 3 == 0) "mixed scripts" else "")
        }
    }

    fun byName(name: String): List<LyricLine> = when (name) {
        "dense-cjk" -> denseCjk()
        "latin-long-ruby" -> latinLongRuby()
        "duet-bg-interlude" -> duetBackgroundInterlude()
        "emoji-combining" -> emojiCombining()
        else -> error("unknown fixture $name")
    }

    val names = listOf("dense-cjk", "latin-long-ruby", "duet-bg-interlude", "emoji-combining")
}
