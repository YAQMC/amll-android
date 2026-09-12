package dev.yaqmc.amll.ui

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State

private const val AMLL_GROUP_OPACITY_DURATION_MS = 400
private val AMLL_GROUP_OPACITY_EASING = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1f)

/** Native equivalent of `.lyricLineWrapper { transition: opacity 0.4s ease; }`. */
@Composable
internal fun animateLyricGroupOpacity(targetAlpha: Float): State<Float> = animateFloatAsState(
    targetValue = targetAlpha,
    animationSpec = tween(
        durationMillis = AMLL_GROUP_OPACITY_DURATION_MS,
        easing = AMLL_GROUP_OPACITY_EASING,
    ),
    label = "amll-group-opacity",
)
