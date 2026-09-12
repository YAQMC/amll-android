package dev.yaqmc.amll.ui

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

internal const val AMLL_SOLID_MASK_ALPHA = 0.20f
internal const val AMLL_GRADIENT_BRIGHT_MASK_ALPHA = 1f
internal const val AMLL_GRADIENT_DARK_MASK_ALPHA = 0.40f
internal const val AMLL_SOLID_MASK_TRANSITION_MS = 450
internal const val AMLL_GRADIENT_MASK_TRANSITION_MS = 300

private val AMLL_MASK_EASE_OUT = CubicBezierEasing(0f, 0f, 0.58f, 1f)

internal data class LyricMaskColors(
    val bright: Color,
    val dark: Color,
)

internal fun lyricMaskTransitionDurationMs(active: Boolean): Int =
    if (active) AMLL_GRADIENT_MASK_TRANSITION_MS else AMLL_SOLID_MASK_TRANSITION_MS

/**
 * Mirrors `.lyricLine` / `.gradientMask` custom-property transitions. The native renderer keeps its
 * existing soft leading-edge geometry, while both endpoints now move between AMLL's SOLID and
 * GRADIENT mask colors with the same mode-specific duration and CSS ease-out timing.
 */
@Composable
internal fun animateLyricMaskColors(
    active: Boolean,
    solidColor: Color,
    brightColor: Color,
    darkColor: Color,
): LyricMaskColors {
    val progress by animateFloatAsState(
        targetValue = if (active) 1f else 0f,
        animationSpec = tween(
            durationMillis = lyricMaskTransitionDurationMs(active),
            easing = AMLL_MASK_EASE_OUT,
        ),
        label = "amll-mask-mode",
    )

    return LyricMaskColors(
        bright = lerp(solidColor, brightColor, progress),
        dark = lerp(solidColor, darkColor, progress),
    )
}
