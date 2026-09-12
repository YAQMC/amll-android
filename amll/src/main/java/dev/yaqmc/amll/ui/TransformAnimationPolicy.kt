package dev.yaqmc.amll.ui

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween

internal const val AMLL_DISABLED_SPRING_TRANSFORM_DURATION_MS = 500
internal val AMLL_CSS_EASE = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1f)

internal data class TransformAnimationPolicy(
    val usePhysicalSpring: Boolean,
    val fallbackDurationMs: Int,
)

internal fun resolveTransformAnimationPolicy(enableSpring: Boolean): TransformAnimationPolicy =
    TransformAnimationPolicy(
        usePhysicalSpring = enableSpring,
        fallbackDurationMs = AMLL_DISABLED_SPRING_TRANSFORM_DURATION_MS,
    )

internal fun amllTransformAnimationSpec(
    enableSpring: Boolean,
    stiffness: Float,
    dampingRatio: Float,
): FiniteAnimationSpec<Float> {
    val policy = resolveTransformAnimationPolicy(enableSpring)
    return if (policy.usePhysicalSpring) {
        spring(
            stiffness = stiffness,
            dampingRatio = dampingRatio,
        )
    } else {
        tween(
            durationMillis = policy.fallbackDurationMs,
            easing = AMLL_CSS_EASE,
        )
    }
}
