package dev.yaqmc.amll.ui

import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import kotlin.math.roundToInt

/**
 * Mirrors upstream `.hasDuetLine` geometry: once a song contains duet lines, every lyric line keeps
 * 15% of the opposite side free. Measuring the child at 85% width is important because it also
 * changes wrapping and therefore the line/group height, just like the DOM renderer.
 */
internal fun Modifier.amllSpeakerInset(
    hasDuetLine: Boolean,
    isDuet: Boolean,
): Modifier {
    if (!hasDuetLine) return this

    return layout { measurable, constraints ->
        if (constraints.maxWidth == Constraints.Infinity) {
            val placeable = measurable.measure(constraints)
            layout(placeable.width, placeable.height) {
                placeable.place(0, 0)
            }
        } else {
            val outerWidth = constraints.maxWidth
            val contentWidth = (outerWidth * lyricContentWidthFraction(hasDuetLine))
                .roundToInt()
                .coerceIn(0, outerWidth)
            val placeable = measurable.measure(
                constraints.copy(
                    minWidth = contentWidth,
                    maxWidth = contentWidth,
                )
            )
            val x = if (isDuet) outerWidth - contentWidth else 0

            layout(outerWidth, placeable.height) {
                placeable.place(x, 0)
            }
        }
    }
}
