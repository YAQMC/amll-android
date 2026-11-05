package dev.yaqmc.amll.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.graphics.graphicsLayer

@Composable
internal fun InterludeDots(
    interlude: LyricInterlude,
    positionMs: () -> Long,
    style: AMLLStyle,
    textAlign: TextAlign,
    modifier: Modifier = Modifier,
) {
    // Derived so the dots only update while the interlude visual actually changes, and read only
    // inside graphicsLayer blocks so those updates skip recomposition and relayout.
    val visualState = remember(interlude, positionMs) {
        derivedStateOf { interludeVisualStateAt(interlude, positionMs()) }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(style.interludeItemHeight)
            .graphicsLayer {
                val visual = visualState.value
                scaleX = visual.scale
                scaleY = visual.scale
            },
        horizontalArrangement = Arrangement.spacedBy(style.interludeDotGap, textAlign.horizontalAlignment()),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(3) { index ->
            Box(
                modifier = Modifier
                    .size(style.interludeDotSize)
                    .graphicsLayer { alpha = visualState.value.dotAlphas[index] }
                    .background(style.activeColor, CircleShape),
            )
        }
    }
}
