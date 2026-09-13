package dev.yaqmc.amll.ui

import android.os.Build
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle

internal const val AMLL_BOTTOM_LINE_IDLE_ALPHA = 0.20f
internal const val AMLL_BOTTOM_LINE_FOCUSED_ALPHA = 0.85f
internal const val AMLL_BOTTOM_LINE_FONT_SCALE = 0.70f
internal const val AMLL_BOTTOM_LINE_LINE_HEIGHT_EM = 1.26f

/**
 * Native equivalent of AMLL's optional bottom-line container.
 *
 * The slot stays at the end of the measured lyric list for the whole song. At end-of-song it becomes
 * the focus target and transitions from 0.2 to 0.85 opacity, matching `.bottomLine.gradientMask`.
 */
@Composable
internal fun AMLLBottomLine(
    focused: Boolean,
    blurRadiusPx: Float,
    style: AMLLStyle,
    content: @Composable () -> Unit,
) {
    val alpha by animateLyricGroupOpacity(
        if (focused) AMLL_BOTTOM_LINE_FOCUSED_ALPHA else AMLL_BOTTOM_LINE_IDLE_ALPHA
    )
    val density = LocalDensity.current
    val lineLayout = with(density) {
        resolveAMLLLineLayoutPx(style.lineFontSize.toPx())
    }
    val verticalPadding = with(density) { lineLayout.groupVerticalPaddingPx.toDp() }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = verticalPadding)
            .graphicsLayer {
                this.alpha = alpha
                renderEffect = if (
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && blurRadiusPx > 0.01f
                ) {
                    BlurEffect(radiusX = blurRadiusPx, radiusY = blurRadiusPx)
                } else {
                    null
                }
            },
    ) {
        CompositionLocalProvider(LocalContentColor provides style.activeColor) {
            ProvideTextStyle(
                TextStyle(
                    fontSize = style.lineFontSize * AMLL_BOTTOM_LINE_FONT_SCALE,
                    lineHeight = style.lineFontSize * AMLL_BOTTOM_LINE_LINE_HEIGHT_EM,
                    fontWeight = style.lyricFontWeight,
                ),
                content = content,
            )
        }
    }
}
