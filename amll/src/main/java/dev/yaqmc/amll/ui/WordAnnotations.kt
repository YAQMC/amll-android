package dev.yaqmc.amll.ui

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import dev.yaqmc.amll.model.LyricRuby
import dev.yaqmc.amll.model.LyricWord
import kotlin.math.max
import kotlin.math.min

internal data class AnnotationRange(
    val start: Int,
    val endExclusive: Int,
    val timing: LyricRuby,
)

internal data class WordAnnotationLayout(
    val roman: TextLayoutResult? = null,
    val ruby: TextLayoutResult? = null,
    val rubyRanges: List<AnnotationRange> = emptyList(),
)

internal fun measureWordAnnotations(
    words: List<LyricWord>,
    measurer: TextMeasurer,
    annotationStyle: TextStyle,
): List<WordAnnotationLayout> {
    val hasRomanLine = words.any { !it.romanText.isNullOrBlank() }
    val hasRubyLine = words.any { word -> word.ruby.any { it.text.isNotBlank() } }

    return words.map { word ->
        val romanText = word.romanText?.trim().orEmpty()
        val romanLayout = if (hasRomanLine && romanText.isNotEmpty()) {
            measurer.measure(text = romanText, style = annotationStyle)
        } else {
            null
        }

        val rubySegments = if (hasRubyLine) word.ruby.filter { it.text.isNotBlank() } else emptyList()
        val rubyText = buildString {
            rubySegments.forEach { append(it.text) }
        }
        val rubyLayout = if (rubyText.isNotEmpty()) {
            measurer.measure(text = rubyText, style = annotationStyle)
        } else {
            null
        }

        var offset = 0
        val ranges = rubySegments.map { segment ->
            val start = offset
            offset += segment.text.length
            AnnotationRange(start, offset, segment)
        }

        WordAnnotationLayout(
            roman = romanLayout,
            ruby = rubyLayout,
            rubyRanges = ranges,
        )
    }
}

internal fun DrawScope.drawWordAnnotations(
    word: LyricWord,
    layout: WordAnnotationLayout,
    baseBounds: Rect,
    baseOffsetY: Float,
    wordTranslateY: Float,
    annotationGapPx: Float,
    positionMs: Long,
    active: Boolean,
    activeColor: Color,
    inactiveColor: Color,
) {
    layout.ruby?.let { ruby ->
        val left = baseBounds.center.x - ruby.size.width / 2f
        val top = baseOffsetY + baseBounds.top - annotationGapPx - ruby.size.height
        drawRubyAnnotation(
            layout = ruby,
            ranges = layout.rubyRanges,
            left = left,
            top = top + wordTranslateY,
            positionMs = positionMs,
            active = active,
            activeColor = activeColor,
            inactiveColor = inactiveColor,
        )
    }

    layout.roman?.let { roman ->
        val left = baseBounds.center.x - roman.size.width / 2f
        val top = baseOffsetY + baseBounds.bottom + annotationGapPx
        drawProgressAnnotation(
            layout = roman,
            left = left,
            top = top + wordTranslateY,
            progress = word.progressAt(positionMs),
            active = active,
            activeColor = activeColor,
            inactiveColor = inactiveColor,
        )
    }
}

private fun DrawScope.drawProgressAnnotation(
    layout: TextLayoutResult,
    left: Float,
    top: Float,
    progress: Float,
    active: Boolean,
    activeColor: Color,
    inactiveColor: Color,
) {
    withTransform({ translate(left, top) }) {
        drawText(layout, color = inactiveColor)
        if (!active || progress <= 0f) return@withTransform

        clipRect(
            left = 0f,
            top = 0f,
            right = layout.size.width * progress.coerceIn(0f, 1f),
            bottom = layout.size.height.toFloat(),
        ) {
            drawText(layout, color = activeColor)
        }
    }
}

private fun DrawScope.drawRubyAnnotation(
    layout: TextLayoutResult,
    ranges: List<AnnotationRange>,
    left: Float,
    top: Float,
    positionMs: Long,
    active: Boolean,
    activeColor: Color,
    inactiveColor: Color,
) {
    withTransform({ translate(left, top) }) {
        drawText(layout, color = inactiveColor)
        if (!active) return@withTransform

        ranges.forEach { range ->
            val progress = range.timing.progressAt(positionMs)
            if (progress <= 0f || range.start >= range.endExclusive) return@forEach

            val bounds = annotationRangeBounds(layout, range.start, range.endExclusive) ?: return@forEach
            val highlightedRight = bounds.left + bounds.width * progress.coerceIn(0f, 1f)
            clipRect(
                left = bounds.left,
                top = bounds.top,
                right = highlightedRight,
                bottom = bounds.bottom,
            ) {
                drawText(layout, color = activeColor)
            }
        }
    }
}

private fun annotationRangeBounds(
    layout: TextLayoutResult,
    start: Int,
    endExclusive: Int,
): Rect? {
    val textLength = layout.layoutInput.text.length
    if (start >= endExclusive || start >= textLength) return null

    var left = Float.POSITIVE_INFINITY
    var top = Float.POSITIVE_INFINITY
    var right = Float.NEGATIVE_INFINITY
    var bottom = Float.NEGATIVE_INFINITY

    for (offset in start until min(endExclusive, textLength)) {
        val box = layout.getBoundingBox(offset)
        left = min(left, box.left)
        top = min(top, box.top)
        right = max(right, box.right)
        bottom = max(bottom, box.bottom)
    }

    return if (left.isFinite()) Rect(left, top, right, bottom) else null
}
