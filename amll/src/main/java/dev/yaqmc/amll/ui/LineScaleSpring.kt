package dev.yaqmc.amll.ui

import kotlin.math.sqrt

internal data class LineScaleSpringSpec(
    val stiffness: Float,
    val dampingRatio: Float,
)

private data class PhysicalScaleSpring(
    val mass: Float,
    val damping: Float,
    val stiffness: Float,
) {
    fun toComposeSpec(): LineScaleSpringSpec = LineScaleSpringSpec(
        stiffness = stiffness / mass,
        dampingRatio = damping / (2f * sqrt(stiffness * mass)),
    )
}

/** Upstream `scaleSpringParams`: mass=2, damping=25, stiffness=100. */
internal fun mainLineScaleSpringSpec(): LineScaleSpringSpec =
    PhysicalScaleSpring(
        mass = 2f,
        damping = 25f,
        stiffness = 100f,
    ).toComposeSpec()

/** Upstream `scaleForBGSpringParams`: mass=1, damping=20, stiffness=50. */
internal fun backgroundLineScaleSpringSpec(): LineScaleSpringSpec =
    PhysicalScaleSpring(
        mass = 1f,
        damping = 20f,
        stiffness = 50f,
    ).toComposeSpec()

internal fun mainLineScaleTarget(
    active: Boolean,
    isPlaying: Boolean,
    activeScale: Float,
    inactiveScale: Float,
): Float = if (active || !isPlaying) activeScale else inactiveScale

internal fun backgroundLineScaleTarget(
    active: Boolean,
    isPlaying: Boolean,
    inactiveScale: Float,
): Float = if (active || !isPlaying) 1f else inactiveScale
