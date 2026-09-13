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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
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
    val solidColor = inactiveColor.copy(
        alpha = inactiveColor.alpha * AMLL_SOLID_MASK_ALPHA / AMLL_GRADIENT_DARK_MASK_ALPHA,
    )
    val maskColors = animateLyricMaskColors(
        active = active,
        solidColor = solidColor,
        brightColor = activeColor,
        darkColor = inactiveColor,
    )
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
    val baseWordWidthsPx = remember(renderWords, style, measurer) {
        renderWords.map { word ->
            if (word.text.isEmpty()) {
                0f
            } else {
                measurer.measure(
                    text = word.text,
                    style = style,
                    softWrap = false,
                    maxLines = 1,
                ).size.width.toFloat()
            }
        }
    }
    val characterShadowLayouts = remember(renderWords, style, measurer) {
        renderWords.map { word ->
            graphemeRanges(word.text).map { range ->
                val characterLayout = measurer.measure(
                    text = word.text.substring(range.start, range.endExclusive),
                    style = style,
                    softWrap = false,
                    maxLines = 1,
                )
                CharacterShadowLayout(
                    layout = characterLayout,
                    inkBounds = textLayoutInkBounds(characterLayout),
                )
            }
        }
    }
    val annotationFontSizePx = with(density) { annotationStyle.fontSize.toPx() }
    val romanEndPaddingPx = annotationFontSizePx * 0.3f
    val wordBoxWidthsPx = remember(baseWordWidthsPx, annotations, romanEndPaddingPx) {
        baseWordWidthsPx.indices.map { index ->
            val annotation = annotations.getOrElse(index) { WordAnnotationLayout() }
            resolveWordBoxWidthPx(
                baseWidthPx = baseWordWidthsPx[index],
                romanWidthPx = annotation.roman?.size?.width?.toFloat(),
                rubyWidthPx = annotation.ruby?.size?.width?.toFloat(),
                romanEndPaddingPx = romanEndPaddingPx,
            )
        }
    }
    val chunkWidthsPx = remember(renderPlan, wordBoxWidthsPx) {
        resolveChunkWidthsPx(renderPlan, wordBoxWidthsPx)
    }
    val wordBoxAlignment = when (style.textAlign) {
        TextAlign.End, TextAlign.Right -> WordBoxAlignment.End
        TextAlign.Center -> WordBoxAlignment.Center
        else -> WordBoxAlignment.Start
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
        val wordBoxTranslationsPx = remember(
            renderPlan,
            balanced.breaks,
            baseWordWidthsPx,
            wordBoxWidthsPx,
            wordBoxAlignment,
        ) {
            resolveWordBoxTranslationsPx(
                plan = renderPlan,
                breaks = balanced.breaks,
                baseWidthsPx = baseWordWidthsPx,
                wordBoxWidthsPx = wordBoxWidthsPx,
                alignment = wordBoxAlignment,
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
        val rubyReservePx = annotations.maxOfOrNull { it.ruby?.size?.height ?: 0 }
            ?.toFloat() ?: 0f
        val romanReservePx = annotations.maxOfOrNull { it.roman?.size?.height ?: 0 }
            ?.toFloat() ?: 0f
        val annotationRowExtraPx = rubyReservePx + romanReservePx
        val totalHeightPx = layout.size.height + layout.lineCount * annotationRowExtraPx
        val totalHeight = with(density) { totalHeightPx.toDp() }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(max(minimumHeight.value, totalHeight.value).dp)
        ) {
            if (renderWords.isEmpty() || balanced.text.isEmpty()) {
                withTransform({ translate(top = rubyReservePx) }) {
                    drawText(layout, color = maskColors.dark)
                }
                return@Canvas
            }

            var atomIndex = 0
            renderPlan.chunks.forEachIndexed { chunkIndex, chunk ->
                val chunkStartAtom = atomIndex
                val geometries = chunk.words.mapIndexed { localIndex, word ->
                    val globalIndex = chunkStartAtom + localIndex
                    buildWordGeometry(
                        layout = layout,
                        word = word,
                        wordOffset = balanced.wordOffsets.getOrElse(globalIndex) { 0 },
                        wordBoxWidthPx = wordBoxWidthsPx.getOrElse(globalIndex) { 0f },
                        rubyBandHeightPx = rubyReservePx,
                        romanBandHeightPx = romanReservePx,
                        positionMs = positionMs,
                        fadeWidthFactor = fadeWidthEm,
                    )
                }
                val totalChunkCharacters = geometries.sumOf { it.characters.size }
                var chunkCharacterIndex = 0

                chunk.words.forEachIndexed { localIndex, word ->
                    val globalIndex = chunkStartAtom + localIndex
                    val geometry = geometries[localIndex]
                    val wordOffset = balanced.wordOffsets.getOrElse(globalIndex) { 0 }
                    val lineIndex = lineIndexForOffset(layout, wordOffset)
                    val baseTranslateX = wordBoxTranslationsPx.getOrElse(globalIndex) { 0f }
                    val baseTranslateY = rubyReservePx + lineIndex * annotationRowExtraPx
                    val wordMotion = if (active) {
                        wordMotionAt(
                            word = word,
                            positionMs = positionMs,
                            isBackground = line.isBackground,
                        )
                    } else {
                        WordMotion()
                    }

                    if (active && chunk.emphasized && geometry.characters.isNotEmpty()) {
                        geometry.characters.forEachIndexed { localCharacterIndex, character ->
                            val characterMotion = characterMotionAt(
                                word = chunk.emphasisWord,
                                positionMs = positionMs,
                                characterIndex = chunkCharacterIndex + localCharacterIndex,
                                totalCharacters = totalChunkCharacters,
                                isBackground = line.isBackground,
                                isLastWord = chunkIndex == renderPlan.lastContentChunkIndex,
                                forceEmphasize = true,
                            )
                            drawCharacter(
                                layout = layout,
                                geometry = character,
                                shadowLayout = characterShadowLayouts
                                    .getOrNull(globalIndex)
                                    ?.getOrNull(localCharacterIndex),
                                wordMask = geometry.mask,
                                wordMotion = wordMotion,
                                characterMotion = characterMotion,
                                fontSizePx = fontSizePx,
                                baseTranslateX = baseTranslateX,
                                baseTranslateY = baseTranslateY,
                                activeColor = maskColors.bright,
                                inactiveColor = maskColors.dark,
                            )
                        }
                    } else {
                        drawWord(
                            layout = layout,
                            geometry = geometry,
                            motion = wordMotion,
                            fontSizePx = fontSizePx,
                            baseTranslateX = baseTranslateX,
                            baseTranslateY = baseTranslateY,
                            activeColor = maskColors.bright,
                            inactiveColor = maskColors.dark,
                        )
                    }

                    geometry.bounds?.let { bounds ->
                        drawWordAnnotations(
                            layout = annotations.getOrElse(globalIndex) { WordAnnotationLayout() },
                            baseBounds = bounds,
                            baseTranslateX = baseTranslateX,
                            baseTranslateY = baseTranslateY,
                            wordTranslateY = wordMotion.translateYEm * fontSizePx,
                            romanEndPaddingPx = romanEndPaddingPx,
                            wordMask = geometry.mask,
                            activeColor = maskColors.bright,
                            inactiveColor = maskColors.dark,
                        )
                    }
                    chunkCharacterIndex += geometry.characters.size
                }
                atomIndex += chunk.words.size
            }
        }
    }
}

private fun lineIndexForOffset(layout: TextLayoutResult, offset: Int): Int {
    val length = layout.layoutInput.text.length
    if (length <= 0) return 0
    return layout.getLineForOffset(offset.coerceIn(0, length - 1))
}

private data class CharacterGeometry(
    val full: Path,
    val bounds: Rect,
)

private data class CharacterShadowLayout(
    val layout: TextLayoutResult,
    val inkBounds: Rect?,
)

private data class WordGeometry(
    val full: Path,
    val bounds: Rect?,
    val mask: WordMaskGradientPx?,
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
    baseTranslateX: Float,
    baseTranslateY: Float,
    activeColor: Color,
    inactiveColor: Color,
) {
    if (geometry.bounds == null) return
    val yOffset = baseTranslateY + motion.translateYEm * fontSizePx

    withTransform({ translate(left = baseTranslateX, top = yOffset) }) {
        drawWordMask(
            layout = layout,
            path = geometry.full,
            mask = geometry.mask,
            brightColor = activeColor,
            darkColor = inactiveColor,
        )
    }
}

private fun DrawScope.drawCharacter(
    layout: TextLayoutResult,
    geometry: CharacterGeometry,
    shadowLayout: CharacterShadowLayout?,
    wordMask: WordMaskGradientPx?,
    wordMotion: WordMotion,
    characterMotion: CharacterMotion,
    fontSizePx: Float,
    baseTranslateX: Float,
    baseTranslateY: Float,
    activeColor: Color,
    inactiveColor: Color,
) {
    val xOffset = baseTranslateX + characterMotion.translateXEm * fontSizePx
    val yOffset = baseTranslateY +
        (wordMotion.translateYEm + characterMotion.translateYEm) * fontSizePx
    val pivot = geometry.bounds.center

    val shadowSpec = resolveCharacterTextShadowSpec(characterMotion, fontSizePx)
    val localInkBounds = shadowLayout?.inkBounds
    if (shadowSpec != null && shadowLayout != null && localInkBounds != null) {
        val shadowTopLeft = Offset(
            x = geometry.bounds.left - localInkBounds.left,
            y = geometry.bounds.top - localInkBounds.top,
        )
        withTransform({
            translate(left = xOffset, top = yOffset)
            scale(
                scaleX = characterMotion.scale,
                scaleY = characterMotion.scale,
                pivot = pivot,
            )
        }) {
            drawText(
                textLayoutResult = shadowLayout.layout,
                color = Color.Transparent,
                topLeft = shadowTopLeft,
                shadow = Shadow(
                    color = Color.White.copy(alpha = shadowSpec.alpha),
                    offset = Offset.Zero,
                    blurRadius = shadowSpec.blurRadiusPx,
                ),
            )
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
        drawWordMask(
            layout = layout,
            path = geometry.full,
            mask = wordMask,
            brightColor = activeColor,
            darkColor = inactiveColor,
        )
    }
}

private fun DrawScope.drawWordMask(
    layout: TextLayoutResult,
    path: Path,
    mask: WordMaskGradientPx?,
    brightColor: Color,
    darkColor: Color,
) {
    clipPath(path) {
        if (mask == null) {
            drawText(layout, color = darkColor)
            return@clipPath
        }

        drawText(
            layout,
            brush = Brush.horizontalGradient(
                colors = listOf(brightColor, darkColor),
                startX = mask.fadeStartX,
                endX = mask.fadeEndX,
            ),
        )
    }
}

private fun buildWordGeometry(
    layout: TextLayoutResult,
    word: LyricWord,
    wordOffset: Int,
    wordBoxWidthPx: Float,
    rubyBandHeightPx: Float,
    romanBandHeightPx: Float,
    positionMs: Long,
    fadeWidthFactor: Float,
): WordGeometry {
    val wordGeometry = buildGeometryRange(
        layout = layout,
        wordOffset = wordOffset,
        range = LocalTextRange(0, word.text.length),
    )
    val mask = wordGeometry.bounds?.let { bounds ->
        val measuredWordWidth = max(bounds.width, wordBoxWidthPx.coerceAtLeast(0f))
        val measuredWordLeft = bounds.center.x - measuredWordWidth / 2f
        val measuredWordHeight = resolveWordMaskContentHeightPx(
            baseHeightPx = bounds.height,
            rubyBandHeightPx = rubyBandHeightPx,
            romanBandHeightPx = romanBandHeightPx,
        )
        resolveWordMaskGradientPx(
            wordLeftPx = measuredWordLeft,
            wordWidthPx = measuredWordWidth,
            wordHeightPx = measuredWordHeight,
            word = word,
            positionMs = positionMs,
            fadeWidthFactor = fadeWidthFactor,
        )
    }

    val characters = graphemeRanges(word.text).mapNotNull { range ->
        val geometry = buildGeometryRange(
            layout = layout,
            wordOffset = wordOffset,
            range = range,
        )
        val bounds = geometry.bounds ?: return@mapNotNull null
        CharacterGeometry(
            full = geometry.full,
            bounds = bounds,
        )
    }

    return WordGeometry(
        full = wordGeometry.full,
        bounds = wordGeometry.bounds,
        mask = mask,
        characters = characters,
    )
}

private data class RangeGeometry(
    val full: Path,
    val bounds: Rect?,
)

private fun buildGeometryRange(
    layout: TextLayoutResult,
    wordOffset: Int,
    range: LocalTextRange,
): RangeGeometry {
    val full = Path()

    var minLeft = Float.POSITIVE_INFINITY
    var minTop = Float.POSITIVE_INFINITY
    var maxRight = Float.NEGATIVE_INFINITY
    var maxBottom = Float.NEGATIVE_INFINITY

    if (range.start >= range.endExclusive) {
        return RangeGeometry(full, null)
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
    }

    val bounds = if (minLeft.isFinite()) {
        Rect(minLeft, minTop, maxRight, maxBottom)
    } else {
        null
    }
    return RangeGeometry(full, bounds)
}

private fun textLayoutInkBounds(layout: TextLayoutResult): Rect? {
    val textLength = layout.layoutInput.text.length
    if (textLength <= 0) return null

    var minLeft = Float.POSITIVE_INFINITY
    var minTop = Float.POSITIVE_INFINITY
    var maxRight = Float.NEGATIVE_INFINITY
    var maxBottom = Float.NEGATIVE_INFINITY

    for (index in 0 until textLength) {
        val box = layout.getBoundingBox(index)
        minLeft = min(minLeft, box.left)
        minTop = min(minTop, box.top)
        maxRight = max(maxRight, box.right)
        maxBottom = max(maxBottom, box.bottom)
    }

    return if (minLeft.isFinite()) {
        Rect(minLeft, minTop, maxRight, maxBottom)
    } else {
        null
    }
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
