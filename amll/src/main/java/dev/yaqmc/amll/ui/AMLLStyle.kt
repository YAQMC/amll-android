package dev.yaqmc.amll.ui

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Immutable
data class AMLLStyle(
    // AMLL gradient-mask endpoints: bright=1.0 and dark=0.4. SOLID derives the same RGB at 0.2.
    val activeColor: Color = Color.White,
    val inactiveColor: Color = Color.White.copy(alpha = AMLL_GRADIENT_DARK_MASK_ALPHA),
    // Upstream .lyricSubLine stays at 0.3 opacity; group opacity is applied separately.
    val secondaryActiveColor: Color = Color.White.copy(alpha = 0.30f),
    val secondaryInactiveColor: Color = Color.White.copy(alpha = 0.30f),
    val lineFontSize: TextUnit = 34.sp,
    // react-full exposes lyric font-weight on the player root and defaults it to CSS weight 600.
    // Main, sub and background lyric content therefore inherit the same stable metric weight.
    val lyricFontWeight: FontWeight = FontWeight.SemiBold,
    // Upstream sub-lines are 0.5em of the base lyric font: 17sp for the native 34sp default.
    val secondaryFontSize: TextUnit = 17.sp,
    // Upstream AMLL keeps the active main line at 100% and scales inactive main lines to 97%.
    val activeScale: Float = 1f,
    val inactiveScale: Float = 0.97f,
    // Mirrors setEnableScale(). Disabling scale only affects the main-line 97% treatment; AMLL's
    // background-vocal 75% inactive transform is a separate effect and remains enabled.
    val enableScale: Boolean = true,
    // Dynamic AMLL group-opacity targets: highlighted groups are 0.85, ordinary rows stay at 1.0.
    val activeAlpha: Float = AMLL_HIGHLIGHTED_GROUP_ALPHA,
    val inactiveAlpha: Float = AMLL_DYNAMIC_GROUP_ALPHA,
    // Legacy bootstrap field kept for source compatibility. Upstream does not distance-fade dynamic
    // lyric opacity; v21 therefore no longer uses a separate far-row target.
    val farInactiveAlpha: Float = AMLL_DYNAMIC_GROUP_ALPHA,
    // Mirrors setHidePassedLines(). Passed rows are nearly transparent only while playback is active;
    // pausing restores them, matching upstream behavior.
    val hidePassedLines: Boolean = false,
    // Upstream background vocals render at ~0.7em. Their lyric-line transform additionally
    // scales inactive playing rows to 75%, independently from the wrapper's 0.8 slide scale.
    val backgroundLineScale: Float = 0.70f,
    val backgroundInactiveScale: Float = 0.75f,
    val backgroundAlpha: Float = 0.40f,
    // Kept for source compatibility with the bootstrap; v8 derives actual slide distance from
    // the background row height like AMLL (80 slide units == 80% of row height).
    val backgroundSlide: Dp = 80.dp,
    // Mirrors AMLL's setAlwaysPostpositionBackground option.
    val alwaysPostpositionBackground: Boolean = false,
    // Additional spacing outside AMLL's measured group box. Upstream has no separate inter-group
    // gap: its visual rhythm comes from the wrapper's 0.4em vertical padding, so the native default
    // is zero while retaining this property for source-compatible custom spacing.
    val lineSpacing: Dp = 0.dp,
    // Unspecified follows AMLL's responsive wrapper padding: 20dp at <=500dp viewport width,
    // otherwise 1em of the measured main lyric font. Supplying a Dp value forces a fixed override.
    val horizontalPadding: Dp = Dp.Unspecified,
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
    // Appended for source compatibility. Upstream setEnableSpring(false) swaps physical springs for
    // ordinary CSS transform transitions rather than disabling animation outright.
    val enableSpring: Boolean = true,
)
