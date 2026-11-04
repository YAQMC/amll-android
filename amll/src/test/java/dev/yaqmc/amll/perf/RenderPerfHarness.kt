package dev.yaqmc.amll.perf

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.RenderNode
import android.view.View
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import dev.yaqmc.amll.state.AMLLPlayerState
import dev.yaqmc.amll.ui.AMLLPlayer
import dev.yaqmc.amll.ui.AmllRenderStats
import java.io.File
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowLooper

/**
 * Opt-in JVM render harness (`-Pamll.perf=true`). Renders the real [AMLLPlayer] under Robolectric
 * native graphics and drives a deterministic 120 Hz clock. Per frame it records main-thread CPU
 * time, allocated bytes and [AmllRenderStats] deltas.
 *
 * This is limited evidence: JVM/Robolectric raster costs are not device GPU/RenderThread costs.
 * It is meaningful for main-thread work (composition, layout, draw recording, allocation) when the
 * baseline and candidate run under identical conditions.
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
class RenderPerfHarness(
    private val fixture: String,
    private val scenario: String,
) {
    @get:Rule
    val rule = createComposeRule()

    private val threads = JvmThreadCounters()

    @Test
    fun measure() {
        assumeTrue(System.getProperty("amll.perf") == "true")
        val lines = PerfFixtures.byName(fixture)
        val startMs = lines[lines.size / 3].startTimeMs + 50
        val state = AMLLPlayerState(lines, initialPositionMs = startMs, initialIsPlaying = true)
        rule.mainClock.autoAdvance = false
        var root: View? = null
        rule.setContent {
            val view = LocalView.current
            SideEffect { root = view }
            AMLLPlayer(state = state, modifier = Modifier.fillMaxSize())
        }
        settle(1_000)
        val recorder = FrameRecorder(root!!)

        var t = startMs
        val frameMs = longArrayOf(8, 8, 9) // 120 Hz cadence: 8.33 ms average
        var frame = 0
        fun step(playing: Boolean, advanceMedia: Boolean) {
            if (advanceMedia) t += frameMs[frame % 3]
            when (scenario) {
                "seek" -> if (frame % 60 == 0) state.seekTo(t) else state.update(t, playing)
                else -> state.update(t, playing)
            }
            rule.mainClock.advanceTimeBy(frameMs[frame % 3])
            ShadowLooper.idleMainLooper()
            recorder.record()
            frame++
        }

        val playing = scenario != "paused"
        repeat(WARMUP_FRAMES) { step(playing, advanceMedia = playing) }

        AmllRenderStats.reset()
        AmllRenderStats.enabled = true
        val cpu = LongArray(MEASURED_FRAMES)
        val alloc = LongArray(MEASURED_FRAMES)
        val tid = Thread.currentThread().id
        repeat(MEASURED_FRAMES) { i ->
            if (scenario == "seek" && i % 60 == 30) t = (t + 7_000).coerceAtMost(lines.last().endTimeMs)
            val c0 = threads.cpuNanos()
            val a0 = threads.allocatedBytes(tid)
            step(playing, advanceMedia = playing)
            cpu[i] = threads.cpuNanos() - c0
            alloc[i] = threads.allocatedBytes(tid) - a0
        }
        AmllRenderStats.enabled = false

        val sortedCpu = cpu.sorted()
        fun pct(p: Double) = sortedCpu[((sortedCpu.size - 1) * p).toInt()] / 1_000_000.0
        val result = linkedMapOf<String, Any>(
            "label" to System.getProperty("amll.perf.label", "local"),
            "fixture" to fixture,
            "scenario" to scenario,
            "frames" to MEASURED_FRAMES,
            "cpuMsP50" to pct(0.50),
            "cpuMsP95" to pct(0.95),
            "cpuMsP99" to pct(0.99),
            "cpuMsMean" to cpu.average() / 1_000_000.0,
            "allocKbPerFrame" to alloc.average() / 1024.0,
            "playerCompositionsPerFrame" to AmllRenderStats.playerCompositions.toDouble() / MEASURED_FRAMES,
            "groupCompositionsPerFrame" to AmllRenderStats.groupCompositions.toDouble() / MEASURED_FRAMES,
            "karaokeCompositionsPerFrame" to AmllRenderStats.karaokeCompositions.toDouble() / MEASURED_FRAMES,
            "karaokeDrawsPerFrame" to AmllRenderStats.karaokeDraws.toDouble() / MEASURED_FRAMES,
            "seekEpochs" to AmllRenderStats.seekEpochs,
        )
        writeJson(result)
    }

    private fun settle(ms: Long) {
        rule.mainClock.advanceTimeBy(ms)
        ShadowLooper.idleMainLooper()
    }

    private fun writeJson(result: Map<String, Any>) {
        val out = File(System.getProperty("amll.perf.out", "build/perf"), "runs").apply { mkdirs() }
        val json = result.entries.joinToString(",\n  ", "{\n  ", "\n}\n") { (k, v) ->
            "\"$k\": " + if (v is String) "\"$v\"" else v.toString()
        }
        File(out, "${result["label"]}-$fixture-$scenario-${System.nanoTime()}.json").writeText(json)
        println("AMLL_PERF $json")
    }

    companion object {
        private const val WARMUP_FRAMES = 360
        private const val MEASURED_FRAMES = 600

        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}-{1}")
        fun params(): List<Array<String>> =
            PerfFixtures.names.flatMap { fixture ->
                listOf("playing", "paused", "seek").map { arrayOf(fixture, it) }
            }
    }
}

/** Fixed-timestamp screenshots for before/after visual comparison (same opt-in flag). */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
class RenderScreenshots(private val fixture: String) {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun capture() {
        assumeTrue(System.getProperty("amll.perf") == "true")
        val lines = PerfFixtures.byName(fixture)
        val state = AMLLPlayerState(lines, initialPositionMs = 0, initialIsPlaying = true)
        rule.mainClock.autoAdvance = false
        var root: View? = null
        rule.setContent {
            val view = LocalView.current
            SideEffect { root = view }
            AMLLPlayer(state = state, modifier = Modifier.fillMaxSize())
        }
        val dir = File(System.getProperty("amll.perf.out", "build/perf"), "screens/${System.getProperty("amll.perf.label", "local")}")
            .apply { mkdirs() }
        // Mid-word, mid-line (emphasis), line start, and an interlude/paused frame.
        val target = lines[lines.size / 3]
        val samples = listOf(
            "line-start" to target.startTimeMs + 5,
            "mid-line" to (target.startTimeMs + target.endTimeMs) / 2,
            "line-end" to target.endTimeMs - 30,
        )
        samples.forEach { (name, position) ->
            state.seekTo(position)
            // Play forward 1.5 s at frame cadence so springs/masks reach a deterministic point.
            var t = position
            repeat(180) {
                t += 8
                state.update(t, true)
                rule.mainClock.advanceTimeBy(8)
                ShadowLooper.idleMainLooper()
            }
            val view = root!!
            val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            bitmap.eraseColor(android.graphics.Color.BLACK)
            view.draw(Canvas(bitmap))
            File(dir, "$fixture-$name.png").outputStream().use {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
            }
        }
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun params(): List<Array<String>> = PerfFixtures.names.map { arrayOf(it) }
    }
}

/**
 * Unit tests compile against android.jar, which lacks java.lang.management; the host JVM running
 * Robolectric has it, so the HotSpot thread counters are reached reflectively (same cost for every run).
 */
internal class JvmThreadCounters {
    private val bean = Class.forName("java.lang.management.ManagementFactory")
        .getMethod("getThreadMXBean").invoke(null)
    private val sunBean = Class.forName("com.sun.management.ThreadMXBean")
    private val cpu = sunBean.getMethod("getCurrentThreadCpuTime")
    private val allocated = sunBean.getMethod("getThreadAllocatedBytes", Long::class.javaPrimitiveType)

    fun cpuNanos(): Long = cpu.invoke(bean) as Long
    fun allocatedBytes(threadId: Long): Long = allocated.invoke(bean, threadId) as Long
}

/**
 * Records the real root view into a hardware [RenderNode] each frame, as the device main thread
 * does for a frame: only layers invalidated since the previous recording re-run their draw code.
 */
internal class FrameRecorder(private val view: View) {
    private val node = RenderNode("amll-perf-frame")

    fun record() {
        node.setPosition(0, 0, view.width, view.height)
        val canvas = node.beginRecording(view.width, view.height)
        try {
            view.draw(canvas)
        } finally {
            node.endRecording()
        }
    }
}
