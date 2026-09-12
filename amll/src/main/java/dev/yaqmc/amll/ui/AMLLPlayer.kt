package dev.yaqmc.amll.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.yaqmc.amll.model.LyricLine
import dev.yaqmc.amll.state.AMLLPlayerState

@Composable
fun AMLLPlayer(
    state: AMLLPlayerState,
    modifier: Modifier = Modifier,
    style: AMLLStyle = AMLLStyle(),
    onLineClick: ((LyricLine) -> Unit)? = null,
) {
    val listState = rememberLazyListState()
    val activeIndex = state.activeLineIndex

    LaunchedEffect(activeIndex, state.lyricLines.size) {
        if (activeIndex >= 0 && activeIndex < state.lyricLines.size) {
            listState.animateScrollToItem(
                index = activeIndex,
                scrollOffset = -96,
            )
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            horizontal = style.horizontalPadding,
            vertical = style.verticalPadding,
        ),
        verticalArrangement = Arrangement.spacedBy(style.lineSpacing),
    ) {
        itemsIndexed(
            items = state.lyricLines,
            key = { index, line -> "${line.startTimeMs}:$index" },
        ) { index, line ->
            val active = index == activeIndex || (line.isBackground && index - 1 == activeIndex)
            val scale by animateFloatAsState(
                targetValue = if (active) style.activeScale else style.inactiveScale,
                animationSpec = spring(stiffness = 360f, dampingRatio = 0.86f),
                label = "lyric-scale",
            )
            val alpha by animateFloatAsState(
                targetValue = if (active) style.activeAlpha else style.inactiveAlpha,
                animationSpec = spring(stiffness = 420f, dampingRatio = 0.9f),
                label = "lyric-alpha",
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        this.alpha = alpha
                        transformOrigin = if (line.isDuet) {
                            androidx.compose.ui.graphics.TransformOrigin(1f, 0.5f)
                        } else {
                            androidx.compose.ui.graphics.TransformOrigin(0f, 0.5f)
                        }
                    }
                    .pointerInput(line, onLineClick) {
                        if (onLineClick != null) {
                            detectTapGestures { onLineClick(line) }
                        }
                    },
                horizontalAlignment = if (line.isDuet) Alignment.End else Alignment.Start,
            ) {
                KaraokeText(
                    line = line,
                    positionMs = state.positionMs,
                    style = TextStyle(
                        fontSize = style.lineFontSize,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.SemiBold,
                        textAlign = if (line.isDuet) TextAlign.End else TextAlign.Start,
                    ),
                    active = active,
                    activeColor = style.activeColor,
                    inactiveColor = style.inactiveColor,
                )

                if (line.translatedLyric.isNotBlank()) {
                    androidx.compose.material3.Text(
                        text = line.translatedLyric,
                        modifier = Modifier.padding(top = 4.dp),
                        color = if (active) style.secondaryActiveColor else style.secondaryInactiveColor,
                        fontSize = style.secondaryFontSize,
                        textAlign = if (line.isDuet) TextAlign.End else TextAlign.Start,
                    )
                }

                if (line.romanLyric.isNotBlank()) {
                    androidx.compose.material3.Text(
                        text = line.romanLyric,
                        modifier = Modifier.padding(top = 2.dp),
                        color = if (active) style.secondaryActiveColor else style.secondaryInactiveColor,
                        fontSize = style.secondaryFontSize,
                        textAlign = if (line.isDuet) TextAlign.End else TextAlign.Start,
                    )
                }
            }
        }
    }
}
