package dev.yaqmc.amll.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.yaqmc.amll.model.LyricLine
import dev.yaqmc.amll.model.LyricWord
import dev.yaqmc.amll.perf.PerfFixtures
import dev.yaqmc.amll.state.AMLLPlayerState
import java.lang.ref.WeakReference
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowLooper

/**
 * Behavioral checks for the render-path optimizations: cached geometry must follow every layout
 * input, deferred clock reads must not leave stale pixels, a paused clock must not churn, and a
 * track change must not retain the previous track.
 *
 * Pixel checks mutate an input in place, capture, then re-key the subtree (discarding every
 * remembered cache) under the same final inputs and capture again: both must be identical.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
class RendererBehaviorTest {
    @get:Rule
    val rule = createComposeRule()

    private val line = LyricLine(
        words = listOf(
            LyricWord(0, 400, "Glowing "),
            LyricWord(400, 1_700, "everywhere "),
            LyricWord(1_700, 2_100, "夜空 "),
            LyricWord(2_100, 2_600, "👨‍👩‍👧"),
        ),
    )
    private val style = TextStyle(fontSize = 32.sp, fontWeight = FontWeight.SemiBold)
    private var root: View? = null
    private var epoch by mutableIntStateOf(0)

    @Test
    fun cachedGeometryFollowsWidthChanges() {
        var width by mutableFloatStateOf(360f)
        show { Box(Modifier.width(width.dp)) { karaoke(positionMs = 900, active = true) } }
        width = 220f
        val mutated = capture()
        assertSamePixels(freshCapture(), mutated)
    }

    @Test
    fun cachedGeometryFollowsFontScaleChanges() {
        var fontScale by mutableFloatStateOf(1f)
        show {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                Box(Modifier.width(360.dp)) { karaoke(positionMs = 1_200, active = true) }
            }
        }
        fontScale = 1.3f
        val mutated = capture()
        assertSamePixels(freshCapture(), mutated)
    }

    @Test
    fun cachedGeometryFollowsTextChanges() {
        var text by mutableStateOf(line)
        show { Box(Modifier.width(360.dp)) { karaoke(positionMs = 1_000, active = true, lyric = text) } }
        text = LyricLine(words = listOf(LyricWord(0, 900, "different "), LyricWord(900, 2_000, "words")))
        val mutated = capture()
        assertSamePixels(freshCapture(), mutated)
    }

    @Test
    fun lineThatIdledWhileInactiveRendersCurrentTimeWhenActivated() {
        var active by mutableStateOf(false)
        val position = mutableLongStateOf(0L)
        show {
            Box(Modifier.width(360.dp)) {
                KaraokeText(
                    line = line,
                    positionMs = { position.longValue },
                    style = style,
                    active = active,
                    activeColor = Color.White,
                    inactiveColor = Color.White.copy(alpha = 0.4f),
                )
            }
        }
        // While settled and inactive the line does not observe the clock; move time a long way.
        repeat(60) {
            position.longValue += 30
            advance(8)
        }
        active = true
        advance(1_000) // the mask-mode color transition settles
        val mutated = capture()
        assertSamePixels(freshCapture(), mutated)
    }

    @Test
    fun pausedClockPushingEqualSamplesNeitherSeeksNorRecomposes() {
        val lines = PerfFixtures.denseCjk(12)
        val held = lines[4].startTimeMs + 100
        val state = AMLLPlayerState(lines, initialPositionMs = held, initialIsPlaying = false)
        rule.mainClock.autoAdvance = false
        rule.setContent { AMLLPlayer(state = state) }
        advance(1_000)

        val stats = measure {
            repeat(240) {
                state.update(positionMs = held, isPlaying = false)
                advance(8)
            }
        }
        assertEquals(0L, stats.seekEpochs)
        assertEquals(0L, stats.playerCompositions)
        assertEquals(0L, stats.karaokeCompositions)

        // An explicit seek is still honored while paused.
        val seek = measure {
            state.seekTo(lines[8].startTimeMs)
            advance(8)
        }
        assertEquals(1L, seek.seekEpochs)
    }

    @Test
    fun steadyPlaybackRedrawsOnlyTimeDependentLines() {
        val lines = PerfFixtures.denseCjk(30)
        var t = lines[10].startTimeMs + 50
        val state = AMLLPlayerState(lines, initialPositionMs = t, initialIsPlaying = true)
        rule.mainClock.autoAdvance = false
        var view: View? = null
        rule.setContent {
            val current = LocalView.current
            SideEffect { view = current }
            AMLLPlayer(state = state)
        }
        advance(1_000)
        val node = android.graphics.RenderNode("probe")
        fun recordFrame() {
            val target = view!!
            node.setPosition(0, 0, target.width, target.height)
            val canvas = node.beginRecording(target.width, target.height)
            target.draw(canvas)
            node.endRecording()
        }
        recordFrame()
        // Inside one word (no line change), only the active line depends on time.
        val stats = measure {
            repeat(10) {
                t += 8
                state.update(t, true)
                advance(8)
                recordFrame()
            }
        }
        assertEquals(0L, stats.karaokeCompositions)
        assertEquals(10L, stats.karaokeDraws)
    }

    @Test
    fun trackChangesDoNotRetainEarlierTracks() {
        val state = AMLLPlayerState()
        var first: List<LyricLine>? = PerfFixtures.latinLongRuby(12)
        val firstRef = WeakReference(first!!.first())
        state.setLyricLines(first)
        state.update(first[2].startTimeMs + 300, true)
        rule.mainClock.autoAdvance = false
        rule.setContent { AMLLPlayer(state = state) }
        advance(1_000)

        // Snapshot state may keep the immediately previous value until its next write, so the
        // contract is bounded retention: two track changes later the first track must be gone.
        for (next in listOf(PerfFixtures.denseCjk(12), PerfFixtures.emojiCombining(12))) {
            state.setLyricLines(next)
            state.seekTo(next[2].startTimeMs + 300)
            advance(1_000)
        }
        @Suppress("UNUSED_VALUE")
        first = null

        for (attempt in 0 until 20) {
            System.gc()
            System.runFinalization()
            if (firstRef.get() == null) break
            Thread.sleep(20)
        }
        assertNull("first track's lines are still reachable after two track changes", firstRef.get())
    }

    @Test
    fun graphemeRangesKeepClustersWholeAndTrimWhitespace() {
        fun clusters(text: String) =
            graphemeRanges(text).map { text.substring(it.start, it.endExclusive) }
        assertEquals(listOf("👨‍👩‍👧"), clusters("👨‍👩‍👧"))
        assertEquals(listOf("🇯🇵", "a"), clusters("🇯🇵a"))
        assertEquals(listOf("é", "t"), clusters("ét"))
        assertEquals(listOf("夜", "空"), clusters("  夜空 "))
        assertTrue(graphemeRanges("   ").isEmpty())
        assertTrue(graphemeRanges("").isEmpty())
    }

    // ---- helpers ----------------------------------------------------------------------------

    @Composable
    private fun karaoke(positionMs: Long, active: Boolean, lyric: LyricLine = line) {
        KaraokeText(
            line = lyric,
            positionMs = { positionMs },
            style = style,
            active = active,
            activeColor = Color.White,
            inactiveColor = Color.White.copy(alpha = 0.4f),
        )
    }

    private fun show(content: @Composable () -> Unit) {
        rule.mainClock.autoAdvance = false
        rule.setContent {
            val view = LocalView.current
            SideEffect { root = view }
            // Fixed outer size: the host view wraps content, and captures must compare equal canvases.
            Box(Modifier.size(400.dp, 320.dp).background(Color.Black)) { key(epoch) { content() } }
        }
        advance(1_000)
    }

    private fun capture(): Bitmap {
        advance(1_000)
        val view = root!!
        // Robolectric runs Compose layout only inside an explicit draw (measureAndLayout), and
        // BoxWithConstraints subcomposes during that layout. On a device the following frame picks
        // up that content; here a warm-up pass plays that role so every capture is a settled frame.
        view.draw(Canvas(Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)))
        advance(16)
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(android.graphics.Color.BLACK)
        view.draw(Canvas(bitmap))
        return bitmap
    }

    /** Recreate the subtree (no remembered state survives) under the current inputs. */
    private fun freshCapture(): Bitmap {
        epoch += 1
        return capture()
    }

    private data class Stats(
        val seekEpochs: Long,
        val playerCompositions: Long,
        val karaokeCompositions: Long,
        val karaokeDraws: Long,
    )

    private fun measure(block: () -> Unit): Stats {
        AmllRenderStats.reset()
        AmllRenderStats.enabled = true
        try {
            block()
            return Stats(
                AmllRenderStats.seekEpochs,
                AmllRenderStats.playerCompositions,
                AmllRenderStats.karaokeCompositions,
                AmllRenderStats.karaokeDraws,
            )
        } finally {
            AmllRenderStats.enabled = false
        }
    }

    private fun advance(ms: Long) {
        rule.mainClock.advanceTimeBy(ms)
        ShadowLooper.idleMainLooper()
    }

    private fun assertSamePixels(expected: Bitmap, actual: Bitmap) {
        assertEquals(expected.width, actual.width)
        assertEquals(expected.height, actual.height)
        var differing = 0
        for (y in 0 until expected.height) for (x in 0 until expected.width) {
            if (expected.getPixel(x, y) != actual.getPixel(x, y)) differing++
        }
        if (differing != 0) {
            val dir = java.io.File(System.getProperty("amll.perf.out", "build/perf"), "behavior-diffs").apply { mkdirs() }
            val name = Throwable().stackTrace.first { it.methodName.startsWith("cached") || it.methodName.startsWith("line") }.methodName
            java.io.File(dir, "$name-fresh.png").outputStream().use { expected.compress(Bitmap.CompressFormat.PNG, 100, it) }
            java.io.File(dir, "$name-mutated.png").outputStream().use { actual.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
        assertEquals("pixels differing from a fresh render", 0, differing)
    }
}
