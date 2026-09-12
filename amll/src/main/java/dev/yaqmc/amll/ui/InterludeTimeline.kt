package dev.yaqmc.amll.ui

import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin

internal const val MIN_INTERLUDE_GAP_MS: Long = 4_000L
private const val TARGET_BREATHE_DURATION_MS = 4_500f

internal data class LyricInterlude(
    val startTimeMs: Long,
    val endTimeMs: Long,
    /** Group immediately before the gap, or -1 when the interlude is before the first lyric. */
    val anchorGroupIndex: Int,
)

internal data class InterludeVisualState(
    val scale: Float = 0f,
    val globalAlpha: Float = 0f,
    val dotAlphas: List<Float> = listOf(0f, 0f, 0f),
)

/**
 * Mirrors AMLL's timeline rule: a silent lyric gap of at least four seconds becomes an interlude.
 * The gap starts at the latest end time seen before the next group, so overlapping lines do not
 * accidentally create false interludes.
 */
internal fun calculateInterludes(groups: List<LyricLineGroup>): List<LyricInterlude> {
    if (groups.isEmpty()) return emptyList()

    val result = mutableListOf<LyricInterlude>()
    var latestEndMs = 0L
    var anchorGroupIndex = -1

    groups.forEachIndexed { index, group ->
        val groupStartMs = min(
            group.main.startTimeMs,
            group.background?.startTimeMs ?: group.main.startTimeMs,
        )

        if (groupStartMs - latestEndMs >= MIN_INTERLUDE_GAP_MS) {
            result += LyricInterlude(
                startTimeMs = latestEndMs,
                endTimeMs = groupStartMs,
                anchorGroupIndex = anchorGroupIndex,
            )
        }

        val groupEndMs = max(
            group.main.endTimeMs,
            group.background?.endTimeMs ?: group.main.endTimeMs,
        )
        latestEndMs = max(latestEndMs, groupEndMs)
        anchorGroupIndex = index
    }

    return result
}

internal fun activeInterludeAt(
    interludes: List<LyricInterlude>,
    positionMs: Long,
): LyricInterlude? = interludes.firstOrNull { interlude ->
    positionMs >= interlude.startTimeMs && positionMs < interlude.endTimeMs
}

/**
 * Media-time-derived version of AMLL's three-dot interlude animation.
 *
 * - first 2s: exponential scale-in
 * - first 500ms hidden, next 500ms fade-in
 * - middle: roughly +/-5% sinusoidal breathing
 * - dots brighten in thirds, never below 25% while visible
 * - final 750ms: back-eased contraction
 * - final 375ms: fade-out
 */
internal fun interludeVisualStateAt(
    interlude: LyricInterlude,
    positionMs: Long,
): InterludeVisualState {
    val totalMs = (interlude.endTimeMs - interlude.startTimeMs).toFloat()
    val currentMs = (positionMs - interlude.startTimeMs).toFloat()
    if (totalMs <= 0f || currentMs < 0f || currentMs > totalMs) {
        return InterludeVisualState()
    }

    val breatheCount = ceil(totalMs / TARGET_BREATHE_DURATION_MS).coerceAtLeast(1f)
    val breatheDuration = totalMs / breatheCount

    var scale = sin(
        1.5f * PI.toFloat() - (currentMs / breatheDuration) * 2f * PI.toFloat(),
    ) / 20f + 1f
    var globalAlpha = 1f

    if (currentMs < 2_000f) {
        scale *= easeOutExpo((currentMs / 2_000f).coerceIn(0f, 1f))
    }

    if (currentMs < 500f) {
        globalAlpha = 0f
    } else if (currentMs < 1_000f) {
        globalAlpha *= (currentMs - 500f) / 500f
    }

    val remainingMs = totalMs - currentMs
    if (remainingMs < 750f) {
        val x = ((750f - remainingMs) / 750f / 2f).coerceIn(0f, 1f)
        scale *= 1f - easeInOutBack(x)
    }
    if (remainingMs < 375f) {
        globalAlpha *= (remainingMs / 375f).coerceIn(0f, 1f)
    }

    val dotsDuration = max(0f, totalMs - 750f)
    scale = max(0f, scale) * 0.7f

    fun dotAlpha(delay: Float): Float {
        if (dotsDuration <= 0f) return 0.25f
        val raw = (((currentMs - delay) * 3f) / dotsDuration) * 0.75f
        return raw.coerceIn(0.25f, 1f)
    }

    val dot0 = dotAlpha(0f)
    val dot1 = dotAlpha(dotsDuration / 3f)
    val dot2 = dotAlpha((dotsDuration / 3f) * 2f)

    return InterludeVisualState(
        scale = scale,
        globalAlpha = globalAlpha.coerceIn(0f, 1f),
        dotAlphas = listOf(
            (globalAlpha * dot0).coerceIn(0f, 1f),
            (globalAlpha * dot1).coerceIn(0f, 1f),
            (globalAlpha * dot2).coerceIn(0f, 1f),
        ),
    )
}

private fun easeOutExpo(x: Float): Float =
    if (x >= 1f) 1f else 1f - 2f.pow(-10f * x.coerceIn(0f, 1f))

private fun easeInOutBack(x: Float): Float {
    val c1 = 1.70158f
    val c2 = c1 * 1.525f
    val t = x.coerceIn(0f, 1f)
    return if (t < 0.5f) {
        ((2f * t).pow(2) * ((c2 + 1f) * 2f * t - c2)) / 2f
    } else {
        (((2f * t - 2f).pow(2) * ((c2 + 1f) * (t * 2f - 2f) + c2)) + 2f) / 2f
    }
}
