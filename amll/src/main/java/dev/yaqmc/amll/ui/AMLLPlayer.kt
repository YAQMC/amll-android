package dev.yaqmc.amll.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.yaqmc.amll.model.LyricLine
import dev.yaqmc.amll.state.AMLLPlayerState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlin.math.abs

@Composable
fun AMLLPlayer(
    state: AMLLPlayerState,
    modifier: Modifier = Modifier,
    style: AMLLStyle = AMLLStyle(),
    onLineClick: ((LyricLine) -> Unit)? = null,
) {
    val listState = rememberLazyListState()
    val density = LocalDensity.current
    val groups = remember(state.lyricLines) { groupLyricLines(state.lyricLines) }
    val activeLineIndex = state.activeLineIndex
    val activeGroupIndex = remember(groups, activeLineIndex) {
        groups.indexOfFirst { it.mainIndex == activeLineIndex }
    }

    val isUserDragging by listState.interactionSource.collectIsDraggedAsState()
    var autoAlignSuspended by remember { mutableStateOf(false) }

    // Match AMLL's interaction model: once the user takes control of the lyric list, automatic
    // focus movement is suspended. Compose owns the actual fling physics; the five-second resume
    // timer begins only after both the finger drag and native inertia have fully stopped.
    LaunchedEffect(isUserDragging, style.autoAlignResumeDelayMs) {
        if (isUserDragging) {
            autoAlignSuspended = true
            return@LaunchedEffect
        }
        if (!autoAlignSuspended) return@LaunchedEffect

        snapshotFlow { listState.isScrollInProgress }
            .filter { scrolling -> !scrolling }
            .first()

        delay(style.autoAlignResumeDelayMs.coerceAtLeast(0L))

        if (!isUserDragging && !listState.isScrollInProgress) {
            autoAlignSuspended = false
        }
    }

    LaunchedEffect(
        activeGroupIndex,
        groups.size,
        style.focusOffset,
        autoAlignSuspended,
    ) {
        if (autoAlignSuspended) return@LaunchedEffect
        if (activeGroupIndex !in groups.indices) return@LaunchedEffect

        val focusOffsetPx = with(density) { style.focusOffset.roundToPx() }
        val visible = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == activeGroupIndex }
        if (visible == null) {
            // Large seek / first layout: establish the destination immediately enough that the
            // spring does not spend frames traversing dozens of off-screen items.
            listState.animateScrollToItem(
                index = activeGroupIndex,
                scrollOffset = -focusOffsetPx,
            )
        } else {
            val targetOffset = listState.layoutInfo.viewportStartOffset + focusOffsetPx
            val delta = (visible.offset - targetOffset).toFloat()
            if (abs(delta) > 0.5f) {
                listState.animateScrollBy(
                    value = delta,
                    animationSpec = spring(
                        dampingRatio = 0.82f,
                        stiffness = 165f,
                    ),
                )
            }
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
            items = groups,
            key = { index, group -> "${group.main.startTimeMs}:$index" },
        ) { index, group ->
            val active = index == activeGroupIndex
            val distance = if (activeGroupIndex >= 0) abs(index - activeGroupIndex) else Int.MAX_VALUE
            val targetScale = if (active) style.activeScale else style.inactiveScale
            val targetAlpha = if (active) {
                style.activeAlpha
            } else {
                inactiveAlphaForDistance(distance, style)
            }

            val scale by animateFloatAsState(
                targetValue = targetScale,
                animationSpec = spring(stiffness = 260f, dampingRatio = 0.82f),
                label = "amll-line-scale",
            )
            val alpha by animateFloatAsState(
                targetValue = targetAlpha,
                animationSpec = spring(stiffness = 310f, dampingRatio = 0.88f),
                label = "amll-line-alpha",
            )

            LyricGroup(
                group = group,
                active = active,
                positionMs = state.positionMs,
                style = style,
                scale = scale,
                alpha = alpha,
                onLineClick = onLineClick,
            )
        }
    }
}

@Composable
private fun LyricGroup(
    group: LyricLineGroup,
    active: Boolean,
    positionMs: Long,
    style: AMLLStyle,
    scale: Float,
    alpha: Float,
    onLineClick: ((LyricLine) -> Unit)?,
) {
    val main = group.main
    val mainAlignment = if (main.isDuet) Alignment.End else Alignment.Start
    val mainTextAlign = if (main.isDuet) TextAlign.End else TextAlign.Start
    val density = LocalDensity.current
    val backgroundSlidePx = with(density) { style.backgroundSlide.roundToPx() }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                this.alpha = alpha
                transformOrigin = if (main.isDuet) {
                    TransformOrigin(1f, 0.5f)
                } else {
                    TransformOrigin(0f, 0.5f)
                }
            }
            .pointerInput(main, onLineClick) {
                if (onLineClick != null) {
                    detectTapGestures { onLineClick(main) }
                }
            },
        horizontalAlignment = mainAlignment,
    ) {
        KaraokeText(
            line = main,
            positionMs = positionMs,
            style = TextStyle(
                fontSize = style.lineFontSize,
                fontWeight = if (active) FontWeight.Bold else FontWeight.SemiBold,
                textAlign = mainTextAlign,
            ),
            active = active,
            activeColor = style.activeColor,
            inactiveColor = style.inactiveColor,
            fadeWidthEm = style.wordFadeWidthEm,
        )

        if (main.translatedLyric.isNotBlank()) {
            Text(
                text = main.translatedLyric,
                modifier = Modifier.padding(top = 4.dp),
                color = if (active) style.secondaryActiveColor else style.secondaryInactiveColor,
                fontSize = style.secondaryFontSize,
                textAlign = mainTextAlign,
            )
        }

        if (main.romanLyric.isNotBlank()) {
            Text(
                text = main.romanLyric,
                modifier = Modifier.padding(top = 2.dp),
                color = if (active) style.secondaryActiveColor else style.secondaryInactiveColor,
                fontSize = style.secondaryFontSize,
                textAlign = mainTextAlign,
            )
        }

        val background = group.background
        if (background != null) {
            AnimatedVisibility(
                visible = active,
                enter = fadeIn(animationSpec = tween(220)) + slideInVertically(
                    animationSpec = spring(dampingRatio = 0.82f, stiffness = 180f),
                    initialOffsetY = { -backgroundSlidePx },
                ),
                exit = fadeOut(animationSpec = tween(180)) + slideOutVertically(
                    animationSpec = spring(dampingRatio = 0.9f, stiffness = 240f),
                    targetOffsetY = { -backgroundSlidePx },
                ),
            ) {
                val backgroundTextAlign = if (background.isDuet) TextAlign.End else TextAlign.Start
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                        .graphicsLayer { this.alpha = style.backgroundAlpha },
                    horizontalAlignment = if (background.isDuet) Alignment.End else Alignment.Start,
                ) {
                    KaraokeText(
                        line = background,
                        positionMs = positionMs,
                        style = TextStyle(
                            fontSize = style.lineFontSize * style.backgroundLineScale,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = backgroundTextAlign,
                        ),
                        active = true,
                        activeColor = style.activeColor,
                        inactiveColor = style.inactiveColor,
                        minimumHeight = 28.dp,
                        fadeWidthEm = style.wordFadeWidthEm,
                    )
                }
            }
        }
    }
}

private fun inactiveAlphaForDistance(distance: Int, style: AMLLStyle): Float {
    if (distance <= 1) return style.inactiveAlpha
    val t = ((distance - 1) / 3f).coerceIn(0f, 1f)
    return style.inactiveAlpha + (style.farInactiveAlpha - style.inactiveAlpha) * t
}
