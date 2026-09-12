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

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(max(minimumHeight.value, height.value).dp)
        ) {
            drawText(layout, color = inactiveColor)
            if (!active || line.text.isEmpty()) return@Canvas

            val highlightPath = buildHighlightPath(layout, line, positionMs)
            clipPath(highlightPath) {
                drawText(layout, color = activeColor)
            }
        }
    }
}

private fun buildHighlightPath(
    layout: TextLayoutResult,
    line: LyricLine,
    positionMs: Long,
): Path {
    val path = Path()
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
            if (charProgress <= 0f) continue

            path.addRect(
                Rect(
                    left = box.left,
                    top = box.top,
                    right = box.left + box.width * charProgress,
                    bottom = box.bottom,
                )
            )
        }
        offset += wordText.length
    }

    return path
}
