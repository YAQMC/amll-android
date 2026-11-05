package dev.yaqmc.amll.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import dev.yaqmc.amll.model.LyricLine
import dev.yaqmc.amll.model.LyricWord
import dev.yaqmc.amll.state.AMLLPlayerState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowLooper

/** Host-facing style options: each must change the drawn output, and none may disturb playback state. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
class StyleOptionsBehaviorTest {
    @get:Rule
    val rule = createComposeRule()

    private var root: View? = null

    @Test
    fun lineTextAlignMapsCssValuesAndKeepsDuetAtTheEnd() {
        assertEquals(TextAlign.Start, AMLLStyle().lineTextAlign(isDuet = false))
        assertEquals(TextAlign.Start, AMLLStyle(textAlign = TextAlign.Left).lineTextAlign(false))
        assertEquals(TextAlign.Center, AMLLStyle(textAlign = TextAlign.Center).lineTextAlign(false))
        assertEquals(TextAlign.End, AMLLStyle(textAlign = TextAlign.Right).lineTextAlign(false))
        assertEquals(TextAlign.End, AMLLStyle(textAlign = TextAlign.Center).lineTextAlign(true))
        assertEquals(TextAlign.End, AMLLStyle(textAlign = TextAlign.Start).lineTextAlign(true))
    }

    @Test
    fun textAlignMovesTheDrawnLine() {
        val lines = listOf(LyricLine(words = listOf(LyricWord(0, 5_000, "Hi")), startTimeMs = 0, endTimeMs = 5_000))
        val state = AMLLPlayerState(lines, initialPositionMs = 1_000, initialIsPlaying = false)
        var style by mutableStateOf(AMLLStyle(enableBlur = false))
        show { AMLLPlayer(state = state, style = style) }

        val start = inkCenterX(capture())
        style = style.copy(textAlign = TextAlign.Center)
        val center = inkCenterX(capture())
        style = style.copy(textAlign = TextAlign.Right)
        val end = inkCenterX(capture())
        val mid = root!!.width / 2f

        assertTrue("start ink at $start should sit left of centre $mid", start < mid * 0.6f)
        assertEquals("centred ink $center should sit at $mid", mid, center, mid * 0.08f)
        assertTrue("end ink at $end should sit right of centre $mid", end > mid * 1.4f)
    }

    @Test
    fun duetLineIsDrawnAtTheEndEdge() {
        val lines = listOf(
            LyricLine(words = listOf(LyricWord(0, 5_000, "Hi")), startTimeMs = 0, endTimeMs = 5_000, isDuet = true),
        )
        val state = AMLLPlayerState(lines, initialPositionMs = 1_000, initialIsPlaying = false)
        show { AMLLPlayer(state = state, style = AMLLStyle(enableBlur = false)) }
        val center = inkCenterX(capture())
        assertTrue("duet ink at $center should sit right of centre", center > root!!.width / 2f * 1.4f)
    }

    @Test
    fun liveStyleChangesNeitherSeekNorReplaceLyrics() {
        val lines = dev.yaqmc.amll.perf.PerfFixtures.denseCjk(12)
        val state = AMLLPlayerState(lines, initialPositionMs = lines[5].startTimeMs + 200, initialIsPlaying = true)
        var style by mutableStateOf(AMLLStyle())
        show { AMLLPlayer(state = state, style = style) }
        val linesBefore = state.lyricLines
        AmllRenderStats.reset()
        AmllRenderStats.enabled = true
        try {
            style = style.copy(lineFontSize = 40.sp, textAlign = TextAlign.Center, lineHeight = 1.4.em)
            advance(500)
            style = style.copy(enableWordEmphasis = false, enableBlur = false, alignPosition = 0.5f)
            advance(500)
            assertEquals(0L, AmllRenderStats.seekEpochs)
        } finally {
            AmllRenderStats.enabled = false
        }
        assertTrue(linesBefore === state.lyricLines)
        assertEquals(lines[5].startTimeMs + 200, state.positionMs)
    }

    @Test
    fun lineHeightChangesWrappedLineHeight() {
        val line = LyricLine(
            words = "one two three four five six seven eight nine ten".split(' ').mapIndexed { i, w ->
                LyricWord(i * 200L, i * 200L + 200, "$w ")
            },
        )
        var lineHeight by mutableStateOf(1.05.em)
        var height = 0
        show {
            Box(Modifier.width(220.dp).onSizeChanged { height = it.height }) {
                KaraokeText(
                    line = line,
                    positionMs = { 0L },
                    style = TextStyle(fontSize = 32.sp, lineHeight = lineHeight),
                    active = false,
                    activeColor = Color.White,
                    inactiveColor = Color.White.copy(alpha = 0.4f),
                )
            }
        }
        capture()
        val tight = height
        lineHeight = 1.6.em
        capture()
        assertTrue(tight > 0)
        assertTrue("1.6em ($height px) must be taller than 1.05em ($tight px)", height > tight)
    }

    @Test
    fun disablingWordEmphasisChangesOnlyEmphasizedWords() {
        // A long, short-text word qualifies for AMLL's emphasize animation; mid-word it is lifted/glowing.
        val emphasized = LyricLine(words = listOf(LyricWord(0, 2_400, "Shine")), startTimeMs = 0, endTimeMs = 2_400)
        val plain = LyricLine(words = listOf(LyricWord(0, 300, "quick")), startTimeMs = 0, endTimeMs = 300)
        var line by mutableStateOf(emphasized)
        var position by mutableStateOf(1_600L)
        var enabled by mutableStateOf(true)
        show {
            Box(Modifier.width(360.dp)) {
                KaraokeText(
                    line = line,
                    positionMs = { position },
                    style = TextStyle(fontSize = 40.sp),
                    active = true,
                    activeColor = Color.White,
                    inactiveColor = Color.White.copy(alpha = 0.4f),
                    enableEmphasis = enabled,
                )
            }
        }
        val on = capture()
        enabled = false
        val off = capture()
        assertTrue("emphasis must change an emphasized word's pixels", differs(on, off))
        line = plain
        position = 150L
        val plainOff = capture()
        enabled = true
        val plainOn = capture()
        assertTrue("a plain word must render identically", !differs(plainOn, plainOff))
    }

    // ---- helpers ----------------------------------------------------------------------------

    private fun show(content: @androidx.compose.runtime.Composable () -> Unit) {
        rule.mainClock.autoAdvance = false
        rule.setContent {
            val view = LocalView.current
            SideEffect { root = view }
            Box(Modifier.size(400.dp, 320.dp).background(Color.Black)) { content() }
        }
        advance(1_000)
    }

    private fun capture(): Bitmap {
        advance(1_000)
        val view = root!!
        view.draw(Canvas(Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)))
        advance(16)
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(android.graphics.Color.BLACK)
        view.draw(Canvas(bitmap))
        return bitmap
    }

    private fun inkCenterX(bitmap: Bitmap): Float {
        var min = Int.MAX_VALUE
        var max = -1
        for (y in 0 until bitmap.height) for (x in 0 until bitmap.width) {
            if (android.graphics.Color.red(bitmap.getPixel(x, y)) > 40) {
                if (x < min) min = x
                if (x > max) max = x
            }
        }
        assertTrue("no lyric ink drawn", max >= 0)
        return (min + max) / 2f
    }

    private fun differs(a: Bitmap, b: Bitmap): Boolean {
        if (a.width != b.width || a.height != b.height) return true
        for (y in 0 until a.height) for (x in 0 until a.width) if (a.getPixel(x, y) != b.getPixel(x, y)) return true
        return false
    }

    private fun advance(ms: Long) {
        rule.mainClock.advanceTimeBy(ms)
        ShadowLooper.idleMainLooper()
    }
}
