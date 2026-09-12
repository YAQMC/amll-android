package dev.yaqmc.amll.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.yaqmc.amll.model.LyricLine
import dev.yaqmc.amll.model.LyricWord
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
        val fontSizePx = with(density) { style.fontSize.toPx() }
        val fadeWidthPx = fontSizePx * fadeWidthEm.coerceAtLeast(0.0001f)

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(max(minimumHeight.value, height.value).dp)
        ) {
            if (line.text.isEmpty()) {
                drawText(layout, color = inactiveColor)
                return@Canvas
            }

            var textOffset = 0
            line.words.forEachIndexed { wordIndex, word ->
                val geometry = buildWordGeometry(
                    layout = layout,
                    word = word,
                    wordOffset = textOffset,
                    positionMs = positionMs,
                    fadeWidthPx = fadeWidthPx,
                )
                textOffset += word.text.length

                val bounds = geometry.bounds ?: return@forEachIndexed
                val motion = if (active) {
                    wordMotionAt(
                        word = word,
                        positionMs = positionMs,
                        isBackground = line.isBackground,
                        isLastWord = wordIndex == line.words.lastIndex,
                    )
                } else {
                    WordMotion()
                }

                drawWord(
                    layout = layout,
                    geometry = geometry,
                    motion = motion,
                    fontSizePx = fontSizePx,
                    active = active,
                    activeColor = activeColor,
                    inactiveColor = inactiveColor,
                    pivot = bounds.center,
                )
            }
        }
    }
}

private data class WordGeometry(
    val full: Path,
    val solid: Path,
    val midFade: Path,
    val edgeFade: Path,
    val bounds: Rect?,
)

private fun DrawScope.drawWord(
    layout: TextLayoutResult,
    geometry: WordGeometry,
    motion: WordMotion,
    fontSizePx: Float,
    active: Boolean,
    activeColor: Color,
    inactiveColor: Color,
    pivot: Offset,
) {
    val yOffset = motion.translateYEm * fontSizePx

    // A small overdraw layer gives emphasized words a brighter pulse without relying on
    // RenderEffect, so the behavior remains available on YAQMC's Android 8 minimum.
    if (active && motion.emphasis > 0.001f) {
        withTransform({
            translate(top = yOffset)
            scale(
                scaleX = motion.scale + 0.018f * motion.emphasis,
                scaleY = motion.scale + 0.018f * motion.emphasis,
                pivot = pivot,
            )
        }) {
            clipPath(geometry.full) {
                drawText(
                    layout,
                    color = activeColor.copy(alpha = activeColor.alpha * 0.16f * motion.emphasis),
                )
            }
        }
    }

    withTransform({
        translate(top = yOffset)
        scale(scaleX = motion.scale, scaleY = motion.scale, pivot = pivot)
    }) {
        clipPath(geometry.full) {
            drawText(layout, color = inactiveColor)
        }

        if (!active) return@withTransform

        clipPath(geometry.solid) {
            drawText(layout, color = activeColor)
        }
        clipPath(geometry.midFade) {
            drawText(layout, color = activeColor.copy(alpha = activeColor.alpha * 0.62f))
        }
        clipPath(geometry.edgeFade) {
            drawText(layout, color = activeColor.copy(alpha = activeColor.alpha * 0.28f))
        }
    }
}

/**
 * Builds the full word clip plus a three-band approximation of AMLL's soft karaoke mask.
 * Geometry is kept per word so native word motion can be applied without relaying out text.
 */
private fun buildWordGeometry(
    layout: TextLayoutResult,
    word: LyricWord,
    wordOffset: Int,
    positionMs: Long,
    fadeWidthPx: Float,
): WordGeometry {
    val full = Path()
    val solid = Path()
    val midFade = Path()
    val edgeFade = Path()
    val wordText = word.text
    val progress = word.progressAt(positionMs)

    var minLeft = Float.POSITIVE_INFINITY
    var minTop = Float.POSITIVE_INFINITY
    var maxRight = Float.NEGATIVE_INFINITY
    var maxBottom = Float.NEGATIVE_INFINITY

    if (wordText.isEmpty()) {
        return WordGeometry(full, solid, midFade, edgeFade, null)
    }

    for (localIndex in wordText.indices) {
        val globalIndex = wordOffset + localIndex
        if (globalIndex >= layout.layoutInput.text.length) break

        val box = layout.getBoundingBox(globalIndex)
        full.addRect(box)
        minLeft = min(minLeft, box.left)
        minTop = min(minTop, box.top)
        maxRight = max(maxRight, box.right)
        maxBottom = max(maxBottom, box.bottom)

        val charStart = localIndex.toFloat() / wordText.length
        val charEnd = (localIndex + 1).toFloat() / wordText.length
        if (progress <= charStart) continue

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

    val bounds = if (minLeft.isFinite()) {
        Rect(minLeft, minTop, maxRight, maxBottom)
    } else {
        null
    }
    return WordGeometry(full, solid, midFade, edgeFade, bounds)
}
