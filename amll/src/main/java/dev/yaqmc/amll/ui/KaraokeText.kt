package dev.yaqmc.amll.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.yaqmc.amll.model.LyricLine
import kotlin.math.max
import kotlin.math.min

@Composable
internal fun KaraokeText(
    line: LyricLine,
    positionMs: Long,
    style: TextStyle,
    active: Boolean,
    activeColor: Color,
    inactiveColor: Color,
    modifier: Modifier = Modifier,
    minimumHeight: Dp = 48.dp,
    fadeWidthEm: Float = 1f,
) {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val text = line.text.ifEmpty { " " }

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val widthPx = with(density) { maxWidth.roundToPx() }.coerceAtLeast(1)
        val layout = remember(text, style, widthPx, measurer) {
            measurer.measure(
                text = text,
                style = style,
                constraints = Constraints(maxWidth = widthPx),
            )
        }
        val height = with(density) { layout.size.height.toDp() }
        val fadeWidthPx = with(density) {
            style.fontSize.toPx() * fadeWidthEm.coerceAtLeast(0.0001f)
        }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(max(minimumHeight.value, height.value).dp)
        ) {
            drawText(layout, color = inactiveColor)
            if (!active || line.text.isEmpty()) return@Canvas

            val highlight = buildHighlightPaths(layout, line, positionMs, fadeWidthPx)

            clipPath(highlight.solid) {
                drawText(layout, color = activeColor)
            }
            clipPath(highlight.midFade) {
                drawText(layout, color = activeColor.copy(alpha = activeColor.alpha * 0.62f))
            }
            clipPath(highlight.edgeFade) {
                drawText(layout, color = activeColor.copy(alpha = activeColor.alpha * 0.28f))
            }
        }
    }
}

private data class HighlightPaths(
    val solid: Path,
    val midFade: Path,
    val edgeFade: Path,
)

/**
 * Builds a three-band approximation of AMLL's soft karaoke mask. Completed glyphs are solid;
 * the live leading edge is split into two progressively dimmer bands instead of ending on a
 * hard rectangular boundary.
 */
private fun buildHighlightPaths(
    layout: TextLayoutResult,
    line: LyricLine,
    positionMs: Long,
    fadeWidthPx: Float,
): HighlightPaths {
    val solid = Path()
    val midFade = Path()
    val edgeFade = Path()
    var offset = 0

    line.words.forEach { word ->
        val wordText = word.text
        val progress = word.progressAt(positionMs)
        if (wordText.isEmpty()) return@forEach

        for (localIndex in wordText.indices) {
            val globalIndex = offset + localIndex
            if (globalIndex >= layout.layoutInput.text.length) break

            val charStart = localIndex.toFloat() / wordText.length
            val charEnd = (localIndex + 1).toFloat() / wordText.length
            if (progress <= charStart) continue

            val box = layout.getBoundingBox(globalIndex)
            val charProgress = ((progress - charStart) / (charEnd - charStart)).coerceIn(0f, 1f)
            if (charProgress >= 0.999f) {
                solid.addRect(box)
                continue
            }
            if (charProgress <= 0f) continue

            val highlightedRight = box.left + box.width * charProgress
            val fadeWidth = min(fadeWidthPx, highlightedRight - box.left).coerceAtLeast(0f)
            val fadeStart = highlightedRight - fadeWidth
            val fadeMid = fadeStart + fadeWidth * 0.58f

            if (fadeStart > box.left) {
                solid.addRect(Rect(box.left, box.top, fadeStart, box.bottom))
            }
            if (fadeMid > fadeStart) {
                midFade.addRect(Rect(fadeStart, box.top, fadeMid, box.bottom))
            }
            if (highlightedRight > fadeMid) {
                edgeFade.addRect(Rect(fadeMid, box.top, highlightedRight, box.bottom))
            }
        }
        offset += wordText.length
    }

    return HighlightPaths(solid, midFade, edgeFade)
}
