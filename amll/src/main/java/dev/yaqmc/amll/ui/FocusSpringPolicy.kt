package dev.yaqmc.amll.ui

import kotlin.math.pow
import kotlin.math.sqrt

internal data class FocusSpringSpec(
    val stiffness: Float,
    val dampingRatio: Float,
)

private const val UPSTREAM_POS_Y_MASS = 0.9f
private const val SLOW_STIFFNESS = 90f
private const val SLOW_DAMPING = 15f
private const val MEDIUM_STIFFNESS = 140f
private const val MEDIUM_DAMPING = 22f
private const val MIN_INTERVAL_MS = 100f
private const val MAX_INTERVAL_MS = 800f
private const val MIN_STIFFNESS = 170f
private const val MAX_STIFFNESS = 220f
private const val DAMPING_MULTIPLIER = 2.2f
private const val INTERVAL_EXPONENT = 0.2f

/**
 * Native equivalent of upstream `getPosYSpringPolicy()` converted to Compose's spring parameters.
 *
 * AMLL expresses a physical spring as mass/damping/stiffness. Compose fixes mass to one and asks
 * for damping ratio + stiffness, so we preserve the same natural frequency by using k/m and derive
 * the damping ratio from c / (2 * sqrt(k*m)).
 */
internal fun focusSpringSpec(
    isSeeking: Boolean,
    isInterludeActive: Boolean,
    intervalMs: Long?,
    isEndOfSong: Boolean = false,
): FocusSpringSpec {
    val physical = when {
        isSeeking || isInterludeActive -> PhysicalSpring(SLOW_STIFFNESS, SLOW_DAMPING)
        isEndOfSong -> PhysicalSpring(MEDIUM_STIFFNESS, MEDIUM_DAMPING)
        intervalMs == null -> PhysicalSpring(SLOW_STIFFNESS, SLOW_DAMPING)
        else -> {
            val clamped = intervalMs.toFloat().coerceIn(MIN_INTERVAL_MS, MAX_INTERVAL_MS)
            var ratio = 1f - (clamped - MIN_INTERVAL_MS) / (MAX_INTERVAL_MS - MIN_INTERVAL_MS)
            ratio = ratio.pow(INTERVAL_EXPONENT)
            val stiffness = MIN_STIFFNESS + ratio * (MAX_STIFFNESS - MIN_STIFFNESS)
            PhysicalSpring(
                stiffness = stiffness,
                damping = sqrt(stiffness) * DAMPING_MULTIPLIER,
            )
        }
    }

    return physical.toComposeSpec()
}

private data class PhysicalSpring(
    val stiffness: Float,
    val damping: Float,
) {
    fun toComposeSpec(): FocusSpringSpec {
        val composeStiffness = stiffness / UPSTREAM_POS_Y_MASS
        val dampingRatio = damping / (2f * sqrt(stiffness * UPSTREAM_POS_Y_MASS))
        return FocusSpringSpec(
            stiffness = composeStiffness,
            dampingRatio = dampingRatio,
        )
    }
}
