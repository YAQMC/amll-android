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
import java.text.BreakIterator
import java.util.Locale
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
    val renderPlan = remember(line.words) { chunkAndSplitLyricWords(line.words) }
    val renderWords = renderPlan.words
    val annotationStyle = remember(style) {
        style.copy(
            fontSize = style.fontSize * 0.5f,
            lineHeight = style.fontSize * 0.5f,
        )
    }
    val annotations = remember(renderWords, annotationStyle, measurer) {
        measureWordAnnotations(
            words = renderWords,
            measurer = measurer,
            annotationStyle = annotationStyle,
        )
    }
    val chunkWidthsPx = remember(renderPlan.chunks, style, measurer) {
        renderPlan.chunks.map { chunk ->
            if (chunk.text.isEmpty()) {
                0f
            } else {
                measurer.measure(
                    text = chunk.text,
                    style = style,
                    softWrap = false,
                    maxLines = 1,
                ).size.width.toFloat()
            }
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val widthPx = with(density) { maxWidth.roundToPx() }.coerceAtLeast(1)
        val balanced = remember(renderPlan, chunkWidthsPx, widthPx) {
            buildBalancedLyricLayout(
                plan = renderPlan,
                chunkWidthsPx = chunkWidthsPx,
                containerWidthPx = widthPx.toFloat(),
            )
        }
        val text = balanced.text.ifEmpty { " " }
        val layout = remember(text, style, widthPx, measurer) {
            measurer.measure(
                text = text,
                style = style,
                softWrap = false,
                maxLines = Int.MAX_VALUE,
                constraints = Constraints(maxWidth = widthPx),
            )
        }
        val fontSizePx = with(density) { style.fontSize.toPx() }
        val fadeWidthPx = fontSizePx * fadeWidthEm.coerceAtLeast(0.0001f)
        val annotationGapPx = fontSizePx * 0.05f
        val rubyHeightPx = annotations.maxOfOrNull { it.ruby?.size?.height ?: 0 }?.toFloat() ?: 0f
        val romanHeightPx = annotations.maxOfOrNull { it.roman?.size?.height ?: 0 }?.toFloat() ?: 0f
        val rubyReservePx = if (rubyHeightPx > 0f) rubyHeightPx + annotationGapPx else 0f
        val romanReservePx = if (romanHeightPx > 0f) romanHeightPx + annotationGapPx else 0f
        val baseOffsetY = rubyReservePx
        val totalHeightPx = layout.size.height + rubyReservePx + romanReservePx
        val totalHeight = with(density) { totalHeightPx.toDp() }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(max(minimumHeight.value, totalHeight.value).dp)
        ) {
            if (renderWords.isEmpty() || balanced.text.isEmpty()) {
                withTransform({ translate(top = baseOffsetY) }) {
                    drawText(layout, color = inactiveColor)
                }
                return@Canvas
            }

            renderWords.forEachIndexed { wordIndex, word ->
                val geometry = buildWordGeometry(
                    layout = layout,
                    word = word,
                    wordOffset = balanced.wordOffsets.getOrElse(wordIndex) { 0 },
                    positionMs = positionMs,
                    fadeWidthPx = fadeWidthPx,
                )

                val wordMotion = if (active) {
                    wordMotionAt(
                        word = word,
                        positionMs = positionMs,
                        isBackground = line.isBackground,
                    )
                } else {
                    WordMotion()
                }

                if (active && shouldEmphasize(word) && geometry.characters.isNotEmpty()) {
                    geometry.characters.forEachIndexed { characterIndex, character ->
                        val characterMotion = characterMotionAt(
                            word = word,
                            positionMs = positionMs,
                            characterIndex = characterIndex,
                            totalCharacters = geometry.characters.size,
                            isBackground = line.isBackground,
                            isLastWord = wordIndex == renderWords.lastIndex,
                        )
                        drawCharacter(
                            layout = layout,
                            geometry = character,
                            wordMotion = wordMotion,
                            characterMotion = characterMotion,
                            fontSizePx = fontSizePx,
                            baseOffsetY = baseOffsetY,
                            activeColor = activeColor,
                            inactiveColor = inactiveColor,
                        )
                    }
                } else {
                    drawWord(
                        layout = layout,
                        geometry = geometry,
                        motion = wordMotion,
                        fontSizePx = fontSizePx,
                        baseOffsetY = baseOffsetY,
                        active = active,
                        activeColor = activeColor,
                        inactiveColor = inactiveColor,
                    )
                }

                geometry.bounds?.let { bounds ->
                    drawWordAnnotations(
                        word = word,
                        layout = annotations.getOrElse(wordIndex) { WordAnnotationLayout() },
                        baseBounds = bounds,
                        baseOffsetY = baseOffsetY,
                        wordTranslateY = wordMotion.translateYEm * fontSizePx,
                        annotationGapPx = annotationGapPx,
                        positionMs = positionMs,
                        active = active,
                        activeColor = activeColor,
                        inactiveColor = inactiveColor,
                    )
                }
            }
        }
    }
}

private data class CharacterGeometry(
    val full: Path,
    val solid: Path,
    val midFade: Path,
    val edgeFade: Path,
    val bounds: Rect,
)

private data class WordGeometry(
    val full: Path,
    val solid: Path,
    val midFade: Path,
    val edgeFade: Path,
    val bounds: Rect?,
    val characters: List<CharacterGeometry>,
)

private data class LocalTextRange(
    val start: Int,
    val endExclusive: Int,
)

private fun DrawScope.drawWord(
    layout: TextLayoutResult,
    geometry: WordGeometry,
    motion: WordMotion,
    fontSizePx: Float,
    baseOffsetY: Float,
    active: Boolean,
    activeColor: Color,
    inactiveColor: Color,
) {
    if (geometry.bounds == null) return
    val yOffset = baseOffsetY + motion.translateYEm * fontSizePx

    withTransform({ translate(top = yOffset) }) {
        clipPath(geometry.full) {
            drawText(layout, color = inactiveColor)
        }

        if (!active) return@withTransform
        drawHighlight(layout, geometry.solid, geometry.midFade, geometry.edgeFade, activeColor)
    }
}

private fun DrawScope.drawCharacter(
    layout: TextLayoutResult,
    geometry: CharacterGeometry,
    wordMotion: WordMotion,
    characterMotion: CharacterMotion,
    fontSizePx: Float,
    baseOffsetY: Float,
    activeColor: Color,
    inactiveColor: Color,
) {
    val xOffset = characterMotion.translateXEm * fontSizePx
    val yOffset = baseOffsetY +
        (wordMotion.translateYEm + characterMotion.translateYEm) * fontSizePx
    val pivot = geometry.bounds.center

    if (characterMotion.glowAlpha > 0.001f && characterMotion.glowRadiusEm > 0f) {
        val radiusPx = characterMotion.glowRadiusEm * fontSizePx * 0.22f
        val haloAlpha = activeColor.alpha * characterMotion.glowAlpha
        val haloScale = characterMotion.scale + characterMotion.glowRadiusEm * 0.025f
        val offsets = arrayOf(
            Offset.Zero,
            Offset(radiusPx, 0f),
            Offset(-radiusPx, 0f),
            Offset(0f, radiusPx),
            Offset(0f, -radiusPx),
        )

        offsets.forEachIndexed { index, offset ->
            withTransform({
                translate(left = xOffset + offset.x, top = yOffset + offset.y)
                scale(scaleX = haloScale, scaleY = haloScale, pivot = pivot)
            }) {
                clipPath(geometry.full) {
                    drawText(
                        layout,
                        color = activeColor.copy(
                            alpha = haloAlpha * if (index == 0) 0.18f else 0.10f,
                        ),
                    )
                }
            }
        }
    }

    withTransform({
        translate(left = xOffset, top = yOffset)
        scale(
            scaleX = characterMotion.scale,
            scaleY = characterMotion.scale,
            pivot = pivot,
        )
    }) {
        clipPath(geometry.full) {
            drawText(layout, color = inactiveColor)
        }
        drawHighlight(layout, geometry.solid, geometry.midFade, geometry.edgeFade, activeColor)
    }
}

private fun DrawScope.drawHighlight(
    layout: TextLayoutResult,
    solid: Path,
    midFade: Path,
    edgeFade: Path,
    activeColor: Color,
) {
    clipPath(solid) {
        drawText(layout, color = activeColor)
    }
    clipPath(midFade) {
        drawText(layout, color = activeColor.copy(alpha = activeColor.alpha * 0.62f))
    }
    clipPath(edgeFade) {
        drawText(layout, color = activeColor.copy(alpha = activeColor.alpha * 0.28f))
    }
}

private fun buildWordGeometry(
    layout: TextLayoutResult,
    word: LyricWord,
    wordOffset: Int,
    positionMs: Long,
    fadeWidthPx: Float,
): WordGeometry {
    val wordGeometry = buildGeometryRange(
        layout = layout,
        word = word,
        wordOffset = wordOffset,
        range = LocalTextRange(0, word.text.length),
        positionMs = positionMs,
        fadeWidthPx = fadeWidthPx,
    )

    val characters = graphemeRanges(word.text).mapNotNull { range ->
        val geometry = buildGeometryRange(
            layout = layout,
            word = word,
            wordOffset = wordOffset,
            range = range,
            positionMs = positionMs,
            fadeWidthPx = fadeWidthPx,
        )
        val bounds = geometry.bounds ?: return@mapNotNull null
        CharacterGeometry(
            full = geometry.full,
            solid = geometry.solid,
            midFade = geometry.midFade,
            edgeFade = geometry.edgeFade,
            bounds = bounds,
        )
    }

    return WordGeometry(
        full = wordGeometry.full,
        solid = wordGeometry.solid,
        midFade = wordGeometry.midFade,
        edgeFade = wordGeometry.edgeFade,
        bounds = wordGeometry.bounds,
        characters = characters,
    )
}

private data class RangeGeometry(
    val full: Path,
    val solid: Path,
    val midFade: Path,
    val edgeFade: Path,
    val bounds: Rect?,
)

private fun buildGeometryRange(
    layout: TextLayoutResult,
    word: LyricWord,
    wordOffset: Int,
    range: LocalTextRange,
    positionMs: Long,
    fadeWidthPx: Float,
): RangeGeometry {
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

    if (wordText.isEmpty() || range.start >= range.endExclusive) {
        return RangeGeometry(full, solid, midFade, edgeFade, null)
    }

    for (localIndex in range.start until range.endExclusive) {
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
    return RangeGeometry(full, solid, midFade, edgeFade, bounds)
}

private fun graphemeRanges(text: String): List<LocalTextRange> {
    if (text.isEmpty()) return emptyList()

    var trimStart = 0
    while (trimStart < text.length && text[trimStart].isWhitespace()) trimStart++
    var trimEnd = text.length
    while (trimEnd > trimStart && text[trimEnd - 1].isWhitespace()) trimEnd--
    if (trimStart >= trimEnd) return emptyList()

    val iterator = BreakIterator.getCharacterInstance(Locale.ROOT)
    iterator.setText(text)
    val ranges = mutableListOf<LocalTextRange>()

    var start = iterator.first()
    var end = iterator.next()
    while (end != BreakIterator.DONE) {
        val clippedStart = max(start, trimStart)
        val clippedEnd = min(end, trimEnd)
        if (clippedStart < clippedEnd) {
            ranges += LocalTextRange(clippedStart, clippedEnd)
        }
        start = end
        end = iterator.next()
    }
    return ranges
}
