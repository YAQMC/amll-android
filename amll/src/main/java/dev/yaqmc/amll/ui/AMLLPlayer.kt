package dev.yaqmc.amll.ui

import android.os.Build
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.animateScrollBy
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import dev.yaqmc.amll.model.LyricLine
import dev.yaqmc.amll.state.AMLLPlayerState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlin.math.abs
import kotlin.math.roundToInt

private data class PlaybackClockSample(
    val positionMs: Long,
    val isPlaying: Boolean,
    val positionUpdateVersion: Long,
    val explicitSeekVersion: Long,
)

@Composable
fun AMLLPlayer(
    state: AMLLPlayerState,
    modifier: Modifier = Modifier,
    style: AMLLStyle = AMLLStyle(),
    onLineClick: ((LyricLine) -> Unit)? = null,
) {
    val listState = rememberLazyListState()
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val isNarrowViewport = configuration.screenWidthDp <= 1024
    var measuredViewportWidthPx by remember { mutableIntStateOf(0) }
    var measuredViewportHeightPx by remember { mutableIntStateOf(0) }

    val fixedHorizontalPaddingPx = if (style.horizontalPadding.value.isNaN()) {
        null
    } else {
        with(density) { style.horizontalPadding.toPx() }
    }
    val lineFontSizePx = with(density) { style.lineFontSize.toPx() }
    val horizontalPaddingPx = resolveAMLLHorizontalPaddingPx(
        viewportWidthPx = measuredViewportWidthPx,
        density = density.density,
        lineFontSizePx = lineFontSizePx,
        fixedPaddingPx = fixedHorizontalPaddingPx,
    )
    val horizontalPadding = with(density) { horizontalPaddingPx.toDp() }
    val horizontalPaddingIntPx = horizontalPaddingPx.roundToInt()

    val minimumVerticalPaddingPx = with(density) { style.verticalPadding.toPx() }
    val focusEdgePaddingPx = remember(
        measuredViewportHeightPx,
        style.alignPosition,
        minimumVerticalPaddingPx,
    ) {
        resolveFocusEdgePaddingPx(
            viewportHeightPx = measuredViewportHeightPx,
            alignPosition = style.alignPosition,
            minimumPaddingPx = minimumVerticalPaddingPx,
        )
    }
    val beforePaddingPx = focusEdgePaddingPx.before.roundToInt()
    val afterPaddingPx = focusEdgePaddingPx.after.roundToInt()
    val beforePadding = with(density) { beforePaddingPx.toDp() }
    val afterPadding = with(density) { afterPaddingPx.toDp() }

    val groups = remember(state.lyricLines) { groupLyricLines(state.lyricLines) }
    val hasDuetLine = remember(groups) { groups.any { it.main.isDuet } }
    val interludes = remember(groups) { calculateInterludes(groups) }
    val listItems = remember(groups, interludes) { buildLyricListItems(groups, interludes) }

    val activeLineIndex = state.activeLineIndex
    val activeGroupIndex = remember(groups, activeLineIndex) {
        groups.indexOfFirst { it.mainIndex == activeLineIndex }
    }
    val activeInterlude = activeInterludeAt(interludes, state.positionMs)
    val passedBoundary = resolvePassedBoundary(
        activeGroupIndex = activeGroupIndex,
        interludeAnchorGroupIndex = activeInterlude?.anchorGroupIndex,
    )
    val focusItemIndex = lyricFocusItemIndex(
        items = listItems,
        activeGroupIndex = activeGroupIndex,
        activeInterlude = activeInterlude,
    )
    val focusIntervalMs = remember(groups, activeGroupIndex) {
        if (activeGroupIndex > 0 && activeGroupIndex < groups.size) {
            (groups[activeGroupIndex].main.startTimeMs - groups[activeGroupIndex - 1].main.startTimeMs)
                .coerceAtLeast(0L)
        } else {
            null
        }
    }

    val seekDetector = remember { PlaybackSeekDetector() }
    var lastSeekPositionMs by remember { mutableStateOf<Long?>(null) }
    var seekEpoch by remember { mutableStateOf(0) }

    LaunchedEffect(state, state.lyricLines, state.autoSeekDetectionEnabled) {
        seekDetector.reset()
        lastSeekPositionMs = null
        var lastExplicitSeekVersion = state.explicitSeekVersion

        snapshotFlow {
            PlaybackClockSample(
                positionMs = state.positionMs,
                isPlaying = state.isPlaying,
                positionUpdateVersion = state.positionUpdateVersion,
                explicitSeekVersion = state.explicitSeekVersion,
            )
        }.collect { sample ->
            val explicitSeek = sample.explicitSeekVersion != lastExplicitSeekVersion
            lastExplicitSeekVersion = sample.explicitSeekVersion
            val detectedSeek = if (state.autoSeekDetectionEnabled) {
                seekDetector.detect(sample.positionMs, sample.isPlaying)
            } else {
                false
            }

            if (explicitSeek || detectedSeek) {
                lastSeekPositionMs = sample.positionMs
                seekEpoch += 1
            }
        }
    }

    val linePosSpring = focusSpringSpec(
        isSeeking = lastSeekPositionMs == state.positionMs,
        isInterludeActive = activeInterlude != null,
        intervalMs = focusIntervalMs,
    )

    var autoAlignSuspended by remember { mutableStateOf(false) }
    var touchPointerDown by remember { mutableStateOf(false) }
    var lastManualInput by remember { mutableStateOf<ManualScrollInputType?>(null) }
    var manualInteractionEpoch by remember { mutableStateOf(0) }

    LaunchedEffect(
        autoAlignSuspended,
        manualInteractionEpoch,
        style.autoAlignResumeDelayMs,
    ) {
        if (!autoAlignSuspended) return@LaunchedEffect

        if (touchPointerDown) {
            snapshotFlow { touchPointerDown }
                .filter { pressed -> !pressed }
                .first()
        }

        if (lastManualInput == ManualScrollInputType.Wheel) {
            delay(WHEEL_IDLE_TIMEOUT_MS)
        }

        snapshotFlow { listState.isScrollInProgress }
            .filter { scrolling -> !scrolling }
            .first()

        delay(style.autoAlignResumeDelayMs.coerceAtLeast(0L))

        if (!touchPointerDown && !listState.isScrollInProgress) {
            autoAlignSuspended = false
            lastManualInput = null
        }
    }

    LaunchedEffect(
        focusItemIndex,
        listItems.size,
        style.alignPosition,
        style.alignAnchor,
        autoAlignSuspended,
        focusIntervalMs,
        activeInterlude,
        seekEpoch,
        measuredViewportHeightPx,
        beforePaddingPx,
        afterPaddingPx,
        horizontalPaddingIntPx,
    ) {
        if (autoAlignSuspended) return@LaunchedEffect
        if (focusItemIndex !in listItems.indices) return@LaunchedEffect
        if (measuredViewportHeightPx <= 0) return@LaunchedEffect

        if (listState.layoutInfo.beforeContentPadding != beforePaddingPx) {
            snapshotFlow { listState.layoutInfo.beforeContentPadding }
                .filter { appliedPadding -> appliedPadding == beforePaddingPx }
                .first()
        }

        if (
            listState.layoutInfo.viewportEndOffset -
            listState.layoutInfo.viewportStartOffset <= 0
        ) {
            snapshotFlow {
                listState.layoutInfo.viewportEndOffset - listState.layoutInfo.viewportStartOffset
            }.filter { viewportHeight -> viewportHeight > 0 }.first()
        }

        var layoutInfo = listState.layoutInfo
        var viewportHeightPx =
            (layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset).coerceAtLeast(1)
        var visible = layoutInfo.visibleItemsInfo.firstOrNull { it.index == focusItemIndex }

        if (visible == null) {
            val coarseOffsetFromStart = viewportHeightPx * style.alignPosition
            listState.animateScrollToItem(
                index = focusItemIndex,
                scrollOffset = -coarseOffsetFromStart.roundToInt(),
            )
            layoutInfo = listState.layoutInfo
            viewportHeightPx =
                (layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset).coerceAtLeast(1)
            visible = layoutInfo.visibleItemsInfo.firstOrNull { it.index == focusItemIndex }
        }

        visible?.let { target ->
            val targetTop = resolveFocusItemTopPx(
                viewportStartPx = layoutInfo.viewportStartOffset,
                viewportHeightPx = viewportHeightPx,
                targetHeightPx = target.size,
                alignPosition = style.alignPosition,
                alignAnchor = style.alignAnchor,
            )
            val delta = target.offset - targetTop
            if (abs(delta) > 0.5f) {
                listState.animateScrollBy(
                    value = delta,
                    animationSpec = spring(
                        dampingRatio = linePosSpring.dampingRatio,
                        stiffness = linePosSpring.stiffness,
                    ),
                )
            }
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { size ->
                measuredViewportWidthPx = size.width
                measuredViewportHeightPx = size.height
            }
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    val touchIntent = TouchScrollIntentTracker()
                    var hasTouch = false

                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)

                        if (
                            event.type == PointerEventType.Scroll &&
                            event.changes.any { change ->
                                change.scrollDelta.x != 0f || change.scrollDelta.y != 0f
                            }
                        ) {
                            lastManualInput = ManualScrollInputType.Wheel
                            autoAlignSuspended = true
                            manualInteractionEpoch += 1
                        }

                        val touchChanges = event.changes.filter { change ->
                            change.type == PointerType.Touch
                        }
                        val pressedTouches = touchChanges.filter { change -> change.pressed }
                        val justPressed = touchChanges.firstOrNull { change ->
                            change.pressed && !change.previousPressed
                        }
                        val justReleased = touchChanges.any { change ->
                            !change.pressed && change.previousPressed
                        }

                        if (!hasTouch && justPressed != null) {
                            hasTouch = true
                            touchPointerDown = true
                            lastManualInput = ManualScrollInputType.Touch
                            touchIntent.onDown(justPressed.position.x, justPressed.position.y)
                            manualInteractionEpoch += 1
                        } else if (hasTouch && justPressed != null && pressedTouches.size > 1) {
                            val anchor = pressedTouches.first()
                            touchIntent.reanchor(anchor.position.x, anchor.position.y)
                        }

                        if (hasTouch && justReleased && pressedTouches.isNotEmpty()) {
                            val anchor = pressedTouches.first()
                            touchIntent.reanchor(anchor.position.x, anchor.position.y)
                        }

                        if (hasTouch && pressedTouches.isNotEmpty()) {
                            val anchor = pressedTouches.first()
                            if (touchIntent.onMove(anchor.position.x, anchor.position.y)) {
                                lastManualInput = ManualScrollInputType.Touch
                                autoAlignSuspended = true
                                manualInteractionEpoch += 1
                            }
                        }

                        if (hasTouch && pressedTouches.isEmpty()) {
                            hasTouch = false
                            touchPointerDown = false
                            touchIntent.onUpOrCancel()
                            manualInteractionEpoch += 1
                        }
                    }
                }
            },
        contentPadding = PaddingValues(
            start = horizontalPadding,
            top = beforePadding,
            end = horizontalPadding,
            bottom = afterPadding,
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
                    val isPassed = groupIndex < passedBoundary
                    val targetAlpha = if (
                        style.hidePassedLines && state.isPlaying && isPassed
                    ) {
                        AMLL_HIDDEN_PASSED_GROUP_ALPHA
                    } else if (active) {
                        style.activeAlpha
                    } else {
                        style.inactiveAlpha
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

                    val alpha by animateLyricGroupOpacity(targetAlpha)

                    LyricGroup(
                        group = group,
                        active = active,
                        isPlaying = state.isPlaying,
                        positionMs = state.positionMs,
                        style = style,
                        hasDuetLine = hasDuetLine,
                        linePosSpring = linePosSpring,
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
    hasDuetLine: Boolean,
    linePosSpring: FocusSpringSpec,
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
    val density = LocalDensity.current
    val lineLayout = with(density) {
        resolveAMLLLineLayoutPx(style.lineFontSize.toPx())
    }
    val groupVerticalPadding = with(density) { lineLayout.groupVerticalPaddingPx.toDp() }
    val subLineHeight = style.lineFontSize * AMLL_SUBLINE_LINE_HEIGHT_EM
    val mainScaleSpec = mainLineScaleSpringSpec()
    val mainScale by animateFloatAsState(
        targetValue = resolveMainLineScaleTarget(
            isActive = active,
            isPlaying = isPlaying,
            enableScale = style.enableScale,
            activeScale = style.activeScale,
            inactiveScale = style.inactiveScale,
        ),
        animationSpec = spring(
            stiffness = mainScaleSpec.stiffness,
            dampingRatio = mainScaleSpec.dampingRatio,
        ),
        label = "amll-main-line-scale",
    )

    Column(
        modifier = Modifier
            .amllSpeakerInset(
                hasDuetLine = hasDuetLine,
                isDuet = main.isDuet,
            )
            .fillMaxWidth()
            .padding(vertical = groupVerticalPadding)
            .graphicsLayer {
                this.alpha = alpha
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
            AMLLBackgroundVocal(
                line = background,
                mainIsDuet = main.isDuet,
                active = active,
                isPlaying = isPlaying,
                placedFirst = true,
                positionMs = positionMs,
                style = style,
                springSpec = linePosSpring,
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    scaleX = mainScale
                    scaleY = mainScale
                    transformOrigin = if (main.isDuet) {
                        TransformOrigin(1f, 0.5f)
                    } else {
                        TransformOrigin(0f, 0.5f)
                    }
                },
            horizontalAlignment = mainAlignment,
        ) {
            KaraokeText(
                line = main,
                positionMs = positionMs,
                style = TextStyle(
                    fontSize = style.lineFontSize,
                    fontWeight = style.lyricFontWeight,
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
                    color = if (active) style.secondaryActiveColor else style.secondaryInactiveColor,
                    fontSize = style.secondaryFontSize,
                    fontWeight = style.lyricFontWeight,
                    lineHeight = subLineHeight,
                    textAlign = mainTextAlign,
                )
            }

            if (main.romanLyric.isNotBlank()) {
                Text(
                    text = main.romanLyric,
                    color = if (active) style.secondaryActiveColor else style.secondaryInactiveColor,
                    fontSize = style.secondaryFontSize,
                    fontWeight = style.lyricFontWeight,
                    lineHeight = subLineHeight,
                    textAlign = mainTextAlign,
                )
            }
        }

        if (background != null && !backgroundFirst) {
            AMLLBackgroundVocal(
                line = background,
                mainIsDuet = main.isDuet,
                active = active,
                isPlaying = isPlaying,
                placedFirst = false,
                positionMs = positionMs,
                style = style,
                springSpec = linePosSpring,
            )
        }
    }
}
