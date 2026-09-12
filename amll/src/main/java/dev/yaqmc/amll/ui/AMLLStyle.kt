package dev.yaqmc.amll.ui

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Immutable
data class AMLLStyle(
    val activeColor: Color = Color.White,
    val inactiveColor: Color = Color.White.copy(alpha = 0.38f),
    val secondaryActiveColor: Color = Color.White.copy(alpha = 0.72f),
    val secondaryInactiveColor: Color = Color.White.copy(alpha = 0.28f),
    val lineFontSize: TextUnit = 34.sp,
    val secondaryFontSize: TextUnit = 16.sp,
    // Upstream AMLL keeps the active main line at 100% and scales inactive main lines to 97%.
    val activeScale: Float = 1f,
    val inactiveScale: Float = 0.97f,
    val activeAlpha: Float = 1f,
    val inactiveAlpha: Float = 0.52f,
    val farInactiveAlpha: Float = 0.24f,
    // Upstream background vocals are rendered at ~0.7em and stay intentionally subdued.
    val backgroundLineScale: Float = 0.70f,
    val backgroundAlpha: Float = 0.40f,
    // Kept for source compatibility with the bootstrap; v8 derives actual slide distance from
    // the background row height like AMLL (80 slide units == 80% of row height).
    val backgroundSlide: Dp = 80.dp,
    // Mirrors AMLL's setAlwaysPostpositionBackground option.
    val alwaysPostpositionBackground: Boolean = false,
    val lineSpacing: Dp = 18.dp,
    val horizontalPadding: Dp = 28.dp,
    val verticalPadding: Dp = 120.dp,
    // Legacy bootstrap option retained for source compatibility. v16 uses viewport-relative
    // alignPosition + alignAnchor for automatic focus, matching upstream AMLL.
    val focusOffset: Dp = 112.dp,
    // AMLL documents 1em as the Android-like word-mask fade width (0.5em for iPad-like rendering).
    val wordFadeWidthEm: Float = 1f,
    // Upstream waits five seconds after manual scrolling + inertia becomes idle before resuming auto-align.
    val autoAlignResumeDelayMs: Long = 5_000L,
    // AMLL enables distance-based lyric blur by default; native RenderEffect is used where supported.
    val enableBlur: Boolean = true,
    // AMLL interlude dots are compact, widely spaced and occupy their own focusable gap item.
    val interludeDotSize: Dp = 10.dp,
    val interludeDotGap: Dp = 8.dp,
    val interludeItemHeight: Dp = 56.dp,
    // Upstream default layoutConfig: Center anchor at 35% of the viewport height.
    val alignPosition: Float = 0.35f,
    val alignAnchor: AMLLAlignAnchor = AMLLAlignAnchor.Center,
)
