package dev.yaqmc.amll.ui

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import dev.yaqmc.amll.model.LyricWord

internal data class WordAnnotationLayout(
    val roman: TextLayoutResult? = null,
    val romanHasVisibleText: Boolean = false,
    val ruby: TextLayoutResult? = null,
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
        // Upstream creates romanWord for every dynamic word once any word has romanization and
        // fills missing entries with NBSP. Measuring that blank box matters for narrow punctuation
        // and keeps the row's roman band height stable.
        val romanLayout = if (hasRomanLine) {
            measurer.measure(
                text = if (romanText.isNotEmpty()) romanText else "\u00A0",
                style = annotationStyle,
            )
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

        WordAnnotationLayout(
            roman = romanLayout,
            romanHasVisibleText = romanText.isNotEmpty(),
            ruby = rubyLayout,
        )
    }
}

/**
 * Draws ruby and per-word romanization under the same word-level mask as the base glyphs.
 *
 * Upstream applies `mask-image` to `mainWordEl`, whose descendants contain ruby, the base word and
 * romanization. The annotation layers therefore do not have independent highlight progress; they
 * sample the exact same horizontal bright-to-dark boundary as the base word.
 */
internal fun DrawScope.drawWordAnnotations(
    layout: WordAnnotationLayout,
    baseBounds: Rect,
    baseTranslateX: Float,
    baseTranslateY: Float,
    wordTranslateY: Float,
    romanEndPaddingPx: Float,
    wordMask: WordMaskGradientPx?,
    activeColor: Color,
    inactiveColor: Color,
) {
    val centerX = baseBounds.center.x + baseTranslateX

    layout.ruby?.let { ruby ->
        val left = resolveCenteredAnnotationLeftPx(
            centerXPx = centerX,
            contentWidthPx = ruby.size.width.toFloat(),
        )
        val top = baseBounds.top + baseTranslateY - ruby.size.height
        drawMaskedAnnotation(
            layout = ruby,
            left = left,
            top = top + wordTranslateY,
            wordMask = wordMask,
            wordMaskTranslateX = baseTranslateX,
            activeColor = activeColor,
            inactiveColor = inactiveColor,
        )
    }

    layout.roman?.let { roman ->
        // NBSP-only layouts reserve upstream's wordBody footprint/height but do not need a draw.
        if (!layout.romanHasVisibleText) return@let
        val left = resolveCenteredAnnotationLeftPx(
            centerXPx = centerX,
            contentWidthPx = roman.size.width.toFloat(),
            endPaddingPx = romanEndPaddingPx,
        )
        val top = baseBounds.bottom + baseTranslateY
        drawMaskedAnnotation(
            layout = roman,
            left = left,
            top = top + wordTranslateY,
            wordMask = wordMask,
            wordMaskTranslateX = baseTranslateX,
            activeColor = activeColor,
            inactiveColor = inactiveColor,
        )
    }
}

private fun DrawScope.drawMaskedAnnotation(
    layout: TextLayoutResult,
    left: Float,
    top: Float,
    wordMask: WordMaskGradientPx?,
    wordMaskTranslateX: Float,
    activeColor: Color,
    inactiveColor: Color,
) {
    withTransform({ translate(left, top) }) {
        if (wordMask == null) {
            drawText(layout, color = inactiveColor)
            return@withTransform
        }

        // The base-word mask is expressed in shared lyric-layout coordinates and then translated by
        // baseTranslateX. Convert those endpoints into this annotation layout's local coordinates.
        val localStartX = wordMask.fadeStartX + wordMaskTranslateX - left
        val localEndX = wordMask.fadeEndX + wordMaskTranslateX - left
        drawText(
            layout,
            brush = Brush.horizontalGradient(
                colors = listOf(activeColor, inactiveColor),
                startX = localStartX,
                endX = localEndX,
            ),
        )
    }
}
