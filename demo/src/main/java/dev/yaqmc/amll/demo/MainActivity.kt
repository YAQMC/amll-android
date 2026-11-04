package dev.yaqmc.amll.demo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import dev.yaqmc.amll.model.LyricLine
import dev.yaqmc.amll.model.LyricRuby
import dev.yaqmc.amll.model.LyricWord
import dev.yaqmc.amll.perf.PerfFixtures
import dev.yaqmc.amll.state.AMLLPlayerState
import dev.yaqmc.amll.ui.AMLLPlayer
import kotlinx.coroutines.delay

/**
 * Interactive demo by default. Benchmark mode (for on-device frame measurement) is selected with
 * intent extras and uses the same deterministic fixtures as the JVM render harness:
 *
 *   adb shell am start -n dev.yaqmc.amll.demo.benchmark/dev.yaqmc.amll.demo.MainActivity \
 *     --es amll.fixture dense-cjk --el amll.startMs 30000 --ez amll.paused false
 *
 * The benchmark clock advances on vsync (`withFrameNanos`), like a host that samples its player
 * on Choreographer frames, so frame pacing is not shaped by a coarse timer.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val fixture = intent.getStringExtra(EXTRA_FIXTURE)
        val startMs = intent.getLongExtra(EXTRA_START_MS, -1L)
        val paused = intent.getBooleanExtra(EXTRA_PAUSED, false)

        setContent {
            val lines = remember(fixture) { fixture?.let(PerfFixtures::byName) ?: sampleLyrics() }
            val state = remember(lines) { AMLLPlayerState(lines) }

            if (fixture == null) {
                LaunchedEffect(Unit) {
                    val startedAt = System.currentTimeMillis()
                    while (true) {
                        state.update((System.currentTimeMillis() - startedAt) % 16_000L)
                        delay(16)
                    }
                }
            } else {
                LaunchedEffect(lines) {
                    val origin = if (startMs >= 0) startMs else lines[lines.size / 3].startTimeMs
                    val end = lines.last().endTimeMs
                    state.seekTo(origin)
                    var firstFrame = -1L
                    while (true) {
                        withFrameNanos { frameNanos ->
                            if (firstFrame < 0) firstFrame = frameNanos
                            val elapsed = if (paused) 0L else (frameNanos - firstFrame) / 1_000_000L
                            // Loop over the document so long recordings keep exercising lyrics.
                            val span = (end - origin).coerceAtLeast(1L)
                            state.update(origin + elapsed % span, isPlaying = !paused)
                        }
                    }
                }
            }

            AMLLPlayer(
                state = state,
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFF312848), Color(0xFF101014))
                        )
                    ),
                onLineClick = { state.seekTo(it.startTimeMs) },
            )
        }
    }

    private companion object {
        const val EXTRA_FIXTURE = "amll.fixture"
        const val EXTRA_START_MS = "amll.startMs"
        const val EXTRA_PAUSED = "amll.paused"
    }
}

private data class DemoAnnotation(
    val roman: String? = null,
    val ruby: List<LyricRuby> = emptyList(),
)

private fun sampleLyrics(): List<LyricLine> = listOf(
    line(
        0,
        listOf(
            "这" to 0L,
            "是" to 350L,
            "一" to 700L,
            "段" to 1050L,
            "原" to 1400L,
            "生" to 1750L,
            "歌" to 2100L,
            "词" to 2450L,
        ),
        "This is native Android lyrics",
        annotations = mapOf(
            0 to DemoAnnotation(
                roman = "zhe",
                ruby = listOf(LyricRuby(0, 350, "ㄓㄜˋ")),
            ),
            1 to DemoAnnotation(
                roman = "shi",
                ruby = listOf(LyricRuby(350, 700, "ㄕˋ")),
            ),
        ),
    ),
    line(3500, listOf("没" to 3500L, "有" to 3850L, " WebView" to 4200L, "，" to 5000L, "没" to 5200L, "有" to 5550L, " DOM" to 5900L), "No WebView, no DOM"),
    line(7200, listOf("Compose" to 7200L, " Canvas" to 8100L, " 直" to 9000L, "接" to 9350L, "渲" to 9700L, "染" to 10050L), "Rendered directly with Compose Canvas", duet = true),
    line(11200, listOf("为" to 11200L, " YAQMC" to 11600L, " 准" to 12700L, "备" to 13100L), "Built for YAQMC"),
)

private fun line(
    start: Long,
    words: List<Pair<String, Long>>,
    translation: String,
    duet: Boolean = false,
    annotations: Map<Int, DemoAnnotation> = emptyMap(),
): LyricLine {
    val mapped = words.mapIndexed { index, (text, wordStart) ->
        val annotation = annotations[index]
        LyricWord(
            startTimeMs = wordStart,
            endTimeMs = words.getOrNull(index + 1)?.second ?: wordStart + 500,
            text = text,
            romanText = annotation?.roman,
            ruby = annotation?.ruby.orEmpty(),
        )
    }
    return LyricLine(
        words = mapped,
        translatedLyric = translation,
        startTimeMs = start,
        endTimeMs = mapped.last().endTimeMs,
        isDuet = duet,
    )
}
