package dev.yaqmc.amll.demo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import dev.yaqmc.amll.model.LyricLine
import dev.yaqmc.amll.model.LyricWord
import dev.yaqmc.amll.state.AMLLPlayerState
import dev.yaqmc.amll.ui.AMLLPlayer
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val state = remember { AMLLPlayerState(sampleLyrics()) }

            LaunchedEffect(Unit) {
                val startedAt = System.currentTimeMillis()
                while (true) {
                    state.update((System.currentTimeMillis() - startedAt) % 16_000L)
                    delay(16)
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
}

private fun sampleLyrics(): List<LyricLine> = listOf(
    line(0, listOf("这" to 0L, "是" to 350L, "一" to 700L, "段" to 1050L, "原" to 1400L, "生" to 1750L, "歌" to 2100L, "词" to 2450L), "This is native Android lyrics"),
    line(3500, listOf("没" to 3500L, "有" to 3850L, " WebView" to 4200L, "，" to 5000L, "没" to 5200L, "有" to 5550L, " DOM" to 5900L), "No WebView, no DOM"),
    line(7200, listOf("Compose" to 7200L, " Canvas" to 8100L, " 直" to 9000L, "接" to 9350L, "渲" to 9700L, "染" to 10050L), "Rendered directly with Compose Canvas", duet = true),
    line(11200, listOf("为" to 11200L, " YAQMC" to 11600L, " 准" to 12700L, "备" to 13100L), "Built for YAQMC"),
)

private fun line(
    start: Long,
    words: List<Pair<String, Long>>,
    translation: String,
    duet: Boolean = false,
): LyricLine {
    val mapped = words.mapIndexed { index, (text, wordStart) ->
        LyricWord(
            startTimeMs = wordStart,
            endTimeMs = words.getOrNull(index + 1)?.second ?: wordStart + 500,
            text = text,
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
