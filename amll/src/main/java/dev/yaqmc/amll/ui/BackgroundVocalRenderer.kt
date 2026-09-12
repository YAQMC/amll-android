package dev.yaqmc.amll.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.yaqmc.amll.model.LyricLine
import kotlin.math.roundToInt

/**
 * Background-vocal renderer driven by the same vertical spring policy as lyric focus motion.
 *
 * This intentionally keeps the background measured even when hidden. Upstream does the same and
 * derives translation, wrapper scale and (for a background-first wrapper) occupied layout height
 * from that measured height on every spring frame. The lyric line itself has a second, independent
 * 100%/75% scale spring, matching upstream `scaleForBGSpringParams`.
 */
@Composable
internal fun AMLLBackgroundVocal(
    line: LyricLine,
    mainIsDuet: Boolean,
    active: Boolean,
    isPlaying: Boolean,
    placedFirst: Boolean,
    positionMs: Long,
    style: AMLLStyle,
    springSpec: FocusSpringSpec,
) {
    val visible = active || !isPlaying
    val hiddenSlideY = backgroundHiddenSlideY(placedFirst)
    val slideY by animateFloatAsState(
        targetValue = if (visible) 0f else hiddenSlideY,
        animationSpec = amllTransformAnimationSpec(
            enableSpring = style.enableSpring,
            dampingRatio = springSpec.dampingRatio,
            stiffness = springSpec.stiffness,
        ),
        label = "amll-background-slide",
    )
    val wrapperAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(
            durationMillis = 300,
            easing = AMLL_CSS_EASE,
        ),
        label = "amll-background-wrapper-alpha",
    )
    val lineScaleSpec = backgroundLineScaleSpringSpec()
    val lineScale by animateFloatAsState(
        targetValue = if (active || !isPlaying) 1f else style.backgroundInactiveScale,
        animationSpec = amllTransformAnimationSpec(
            enableSpring = style.enableSpring,
            stiffness = lineScaleSpec.stiffness,
            dampingRatio = lineScaleSpec.dampingRatio,
        ),
        label = "amll-background-line-scale",
    )
    val frame = backgroundVocalFrame(slideY)
    val alignment = if (mainIsDuet) Alignment.End else Alignment.Start
    val textAlign = if (mainIsDuet) TextAlign.End else TextAlign.Start
    val density = androidx.compose.ui.platform.LocalDensity.current
    val groupContentGapPx = with(density) {
        resolveAMLLLineLayoutPx(style.lineFontSize.toPx()).groupContentGapPx.roundToInt()
    }
    val originX = if (mainIsDuet) 1f else 0f
    val originY = if (placedFirst) 1f else 0f

    Layout(
        modifier = Modifier.fillMaxWidth(),
        content = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        scaleX = lineScale
                        scaleY = lineScale
                        transformOrigin = TransformOrigin(originX, 0.5f)
                    },
                horizontalAlignment = alignment,
            ) {
                KaraokeText(
                    line = line,
                    positionMs = positionMs,
                    style = TextStyle(
                        fontSize = style.lineFontSize * style.backgroundLineScale,
                        fontWeight = style.lyricFontWeight,
                        textAlign = textAlign,
                    ),
                    active = active,
                    activeColor = style.activeColor,
                    inactiveColor = style.inactiveColor,
                    minimumHeight = 28.dp,
                    fadeWidthEm = style.wordFadeWidthEm,
                )
            }
        },
    ) { measurables, constraints ->
        val placeable = measurables.single().measure(constraints)
        val backgroundHeight = placeable.height
        val layoutFrame = backgroundVocalLayoutFrame(
            backgroundHeightPx = backgroundHeight,
            gapPx = groupContentGapPx,
            placedFirst = placedFirst,
            visible = visible,
            frame = frame,
        )

        layout(placeable.width, layoutFrame.layoutHeightPx) {
            placeable.placeWithLayer(0, layoutFrame.childYpx) {
                translationY = frame.translationYFraction * backgroundHeight
                scaleX = frame.scale
                scaleY = frame.scale
                alpha = wrapperAlpha * style.backgroundAlpha
                transformOrigin = TransformOrigin(originX, originY)
            }
        }
    }
}
