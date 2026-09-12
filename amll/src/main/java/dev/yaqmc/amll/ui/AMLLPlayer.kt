package dev.yaqmc.amll.ui

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
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
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
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
import kotlin.math.roundToInt

@Composable
fun AMLLPlayer(
    state: AMLLPlayerState,
    modifier: Modifier = Modifier,
    style: AMLLStyle = AMLLStyle(),
    onLineClick: ((LyricLine) -> Unit)? = null,
) {
    val listState = rememberLazyListState()
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val isNarrowViewport = configuration.screenWidthDp <= 1024

    val groups = remember(state.lyricLines) { groupLyricLines(state.lyricLines) }
    val interludes = remember(groups) { calculateInterludes(groups) }
    val listItems = remember(groups, interludes) { buildLyricListItems(groups, interludes) }

    val activeLineIndex = state.activeLineIndex
    val activeGroupIndex = remember(groups, activeLineIndex) {
        groups.indexOfFirst { it.mainIndex == activeLineIndex }
    }
    val activeInterlude = activeInterludeAt(interludes, state.positionMs)
    val focusItemIndex = lyricFocusItemIndex(
        items = listItems,
        activeGroupIndex = activeGroupIndex,
        activeInterlude = activeInterlude,
    )

    val isUserDragging by listState.interactionSource.collectIsDraggedAsState()
    var autoAlignSuspended by remember { mutableStateOf(false) }

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
        focusItemIndex,
        listItems.size,
        style.focusOffset,
        autoAlignSuspended,
    ) {
        if (autoAlignSuspended) return@LaunchedEffect
        if (focusItemIndex !in listItems.indices) return@LaunchedEffect

        val focusOffsetPx = with(density) { style.focusOffset.roundToPx() }
        val visible = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == focusItemIndex }
        if (visible == null) {
            listState.animateScrollToItem(
                index = focusItemIndex,
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
            items = listItems,
            key = { _, item -> item.stableKey },
        ) { _, item ->
            when (item) {
                is LyricListItem.Interlude -> {
                    val anchor = item.interlude.anchorGroupIndex
                    val alignEnd = anchor in groups.indices && groups[anchor].main.isDuet
                    InterludeDots(
                        interlude = item.interlude,
                        positionMs = state.positionMs,
                        style = style,
                        alignEnd = alignEnd,
                    )
                }

                is LyricListItem.Group -> {
                    val groupIndex = item.groupIndex
                    val group = item.group
                    val active = activeInterlude == null && groupIndex == activeGroupIndex
                    val referenceGroupIndex = when {
                        activeInterlude != null && activeInterlude.anchorGroupIndex >= 0 -> {
                            activeInterlude.anchorGroupIndex
                        }
                        activeGroupIndex >= 0 -> activeGroupIndex
                        else -> 0
                    }
                    val distance = abs(groupIndex - referenceGroupIndex)
                    val targetScale = if (active || !state.isPlaying) {
                        style.activeScale
                    } else {
                        style.inactiveScale
                    }
                    val targetAlpha = if (active) {
                        style.activeAlpha
                    } else {
                        inactiveAlphaForDistance(distance, style)
                    }

                    val blurScrollToIndex = when {
                        activeInterlude != null -> {
                            (activeInterlude.anchorGroupIndex + 1).coerceIn(0, groups.lastIndex)
                        }
                        activeGroupIndex >= 0 -> activeGroupIndex
                        else -> 0
                    }
                    val latestHighlightedIndex = when {
                        activeInterlude != null -> activeInterlude.anchorGroupIndex
                        activeGroupIndex >= 0 -> activeGroupIndex
                        else -> 0
                    }
                    val blurRadiusPx = resolveBlurRadiusPx(
                        index = groupIndex,
                        scrollToIndex = blurScrollToIndex,
                        latestHighlightedIndex = latestHighlightedIndex,
                        isFocused = active,
                        autoAlignSuspended = autoAlignSuspended,
                        isNarrowViewport = isNarrowViewport,
                        enabled = style.enableBlur,
                    )

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
                        isPlaying = state.isPlaying,
                        positionMs = state.positionMs,
                        style = style,
                        scale = scale,
                        alpha = alpha,
                        blurRadiusPx = blurRadiusPx,
                        onLineClick = onLineClick,
                    )
                }
            }
        }
    }
}

@Composable
private fun LyricGroup(
    group: LyricLineGroup,
    active: Boolean,
    isPlaying: Boolean,
    positionMs: Long,
    style: AMLLStyle,
    scale: Float,
    alpha: Float,
    blurRadiusPx: Float,
    onLineClick: ((LyricLine) -> Unit)?,
) {
    val main = group.main
    val mainAlignment = if (main.isDuet) Alignment.End else Alignment.Start
    val mainTextAlign = if (main.isDuet) TextAlign.End else TextAlign.Start
    val backgroundFirst = shouldPlaceBackgroundFirst(
        group = group,
        alwaysPostpositionBackground = style.alwaysPostpositionBackground,
    )

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

                renderEffect = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && blurRadiusPx > 0.01f) {
                    BlurEffect(radiusX = blurRadiusPx, radiusY = blurRadiusPx)
                } else {
                    null
                }
            }
            .pointerInput(main, onLineClick) {
                if (onLineClick != null) {
                    detectTapGestures { onLineClick(main) }
                }
            },
        horizontalAlignment = mainAlignment,
    ) {
        val background = group.background
        if (background != null && backgroundFirst) {
            BackgroundVocal(
                line = background,
                mainIsDuet = main.isDuet,
                active = active,
                isPlaying = isPlaying,
                placedFirst = true,
                positionMs = positionMs,
                style = style,
            )
        }

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

        if (background != null && !backgroundFirst) {
            BackgroundVocal(
                line = background,
                mainIsDuet = main.isDuet,
                active = active,
                isPlaying = isPlaying,
                placedFirst = false,
                positionMs = positionMs,
                style = style,
            )
        }
    }
}

@Composable
private fun BackgroundVocal(
    line: LyricLine,
    mainIsDuet: Boolean,
    active: Boolean,
    isPlaying: Boolean,
    placedFirst: Boolean,
    positionMs: Long,
    style: AMLLStyle,
) {
    val alignment = if (mainIsDuet) Alignment.End else Alignment.Start
    val textAlign = if (mainIsDuet) TextAlign.End else TextAlign.Start
    val hiddenDirection = if (placedFirst) 1f else -1f
    val visible = active || !isPlaying

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(220)) +
            slideInVertically(
                animationSpec = spring(dampingRatio = 0.82f, stiffness = 180f),
                initialOffsetY = { height -> (height * 0.8f * hiddenDirection).roundToInt() },
            ) +
            scaleIn(
                initialScale = 0.8f,
                animationSpec = spring(dampingRatio = 0.82f, stiffness = 180f),
            ),
        exit = fadeOut(animationSpec = tween(180)) +
            slideOutVertically(
                animationSpec = spring(dampingRatio = 0.9f, stiffness = 240f),
                targetOffsetY = { height -> (height * 0.8f * hiddenDirection).roundToInt() },
            ) +
            scaleOut(
                targetScale = 0.8f,
                animationSpec = spring(dampingRatio = 0.9f, stiffness = 240f),
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    top = if (placedFirst) 0.dp else 4.dp,
                    bottom = if (placedFirst) 4.dp else 0.dp,
                )
                .graphicsLayer { alpha = style.backgroundAlpha },
            horizontalAlignment = alignment,
        ) {
            KaraokeText(
                line = line,
                positionMs = positionMs,
                style = TextStyle(
                    fontSize = style.lineFontSize * style.backgroundLineScale,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = textAlign,
                ),
                active = active,
                activeColor = style.activeColor,
                inactiveColor = style.inactiveColor,
                minimumHeight = 28.dp,
                fadeWidthEm = style.wordFadeWidthEm,
            )
        }
    }
}

private fun inactiveAlphaForDistance(distance: Int, style: AMLLStyle): Float {
    if (distance <= 1) return style.inactiveAlpha
    val t = ((distance - 1) / 3f).coerceIn(0f, 1f)
    return style.inactiveAlpha + (style.farInactiveAlpha - style.inactiveAlpha) * t
}
