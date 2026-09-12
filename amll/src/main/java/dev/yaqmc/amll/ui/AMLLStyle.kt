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
    val activeScale: Float = 1.055f,
    val inactiveScale: Float = 0.94f,
    val activeAlpha: Float = 1f,
    val inactiveAlpha: Float = 0.52f,
    val lineSpacing: Dp = 18.dp,
    val horizontalPadding: Dp = 28.dp,
    val verticalPadding: Dp = 96.dp,
)
