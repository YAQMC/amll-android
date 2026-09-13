package dev.yaqmc.amll.ui

import dev.yaqmc.amll.model.LyricRuby
import dev.yaqmc.amll.model.LyricWord
import org.junit.Assert.assertEquals
import org.junit.Test

class WordMaskGradientTest {
    @Test fun fadeWidthUsesMeasuredWordHeight() {
        val gradient = resolveWordMaskGradientPx(
            wordLeftPx = 10f,
            wordWidthPx = 100f,
            wordHeightPx = 40f,
            startTimeMs = 1_000L,
            endTimeMs = 2_000L,
            positionMs = 1_500L,
            fadeWidthFactor = 1f,
        )

        assertEquals(40f, gradient.fadeEndX - gradient.fadeStartX, 0.0001f)
        assertEquals(40f, gradient.fadeStartX, 0.0001f)
        assertEquals(80f, gradient.fadeEndX, 0.0001f)
    }

    @Test fun fadeIsHalfEnteredAtWordStartAndHalfExitedAtWordEnd() {
        val atStart = resolveWordMaskGradientPx(
            wordLeftPx = 0f,
            wordWidthPx = 100f,
            wordHeightPx = 20f,
            startTimeMs = 1_000L,
            endTimeMs = 2_000L,
            positionMs = 1_000L,
            fadeWidthFactor = 1f,
        )
        val atEnd = resolveWordMaskGradientPx(
            wordLeftPx = 0f,
            wordWidthPx = 100f,
            wordHeightPx = 20f,
            startTimeMs = 1_000L,
            endTimeMs = 2_000L,
            positionMs = 2_000L,
            fadeWidthFactor = 1f,
        )

        assertEquals(-10f, atStart.fadeStartX, 0.0001f)
        assertEquals(10f, atStart.fadeEndX, 0.0001f)
        assertEquals(90f, atEnd.fadeStartX, 0.0001f)
        assertEquals(110f, atEnd.fadeEndX, 0.0001f)
    }

    @Test fun farOutsideTimingClampsToFullyDarkOrFullyBright() {
        val before = resolveWordMaskGradientPx(
            wordLeftPx = 30f,
            wordWidthPx = 80f,
            wordHeightPx = 20f,
            startTimeMs = 1_000L,
            endTimeMs = 2_000L,
            positionMs = 0L,
            fadeWidthFactor = 1f,
        )
        val after = resolveWordMaskGradientPx(
            wordLeftPx = 30f,
            wordWidthPx = 80f,
            wordHeightPx = 20f,
            startTimeMs = 1_000L,
            endTimeMs = 2_000L,
            positionMs = 3_000L,
            fadeWidthFactor = 1f,
        )

        assertEquals(10f, before.fadeStartX, 0.0001f)
        assertEquals(30f, before.fadeEndX, 0.0001f)
        assertEquals(110f, after.fadeStartX, 0.0001f)
        assertEquals(130f, after.fadeEndX, 0.0001f)
    }

    @Test fun rubyTimingDrivesWholeWordSweepAndHoldsAcrossSegmentGap() {
        val word = LyricWord(
            startTimeMs = 1_000L,
            endTimeMs = 2_000L,
            text = "東京",
            ruby = listOf(
                LyricRuby(1_000L, 1_300L, "とう"),
                LyricRuby(1_500L, 2_000L, "きょう"),
            ),
        )

        // Five UTF-16 ruby code units: the first two consume 40% of the measured word width.
        assertEquals(0.20f, resolveWordMaskSweepProgress(word, 1_150L), 0.0001f)
        assertEquals(0.40f, resolveWordMaskSweepProgress(word, 1_300L), 0.0001f)
        assertEquals(0.40f, resolveWordMaskSweepProgress(word, 1_400L), 0.0001f)
        assertEquals(0.70f, resolveWordMaskSweepProgress(word, 1_750L), 0.0001f)
        assertEquals(1.00f, resolveWordMaskSweepProgress(word, 2_000L), 0.0001f)
    }

    @Test fun rubyTimingIsClampedToWordBoundsLikeUpstream() {
        val word = LyricWord(
            startTimeMs = 1_000L,
            endTimeMs = 2_000L,
            text = "愛",
            ruby = listOf(LyricRuby(800L, 2_200L, "あい")),
        )

        assertEquals(0f, resolveWordMaskSweepProgress(word, 1_000L), 0.0001f)
        assertEquals(0.5f, resolveWordMaskSweepProgress(word, 1_500L), 0.0001f)
        assertEquals(1f, resolveWordMaskSweepProgress(word, 2_000L), 0.0001f)
    }

    @Test fun timingOutsideRubyWordStillKeepsFadeLeadInAndTail() {
        val word = LyricWord(
            startTimeMs = 1_000L,
            endTimeMs = 2_000L,
            text = "愛",
            ruby = listOf(LyricRuby(1_000L, 2_000L, "あい")),
        )

        assertEquals(-0.5f, resolveWordMaskSweepProgress(word, 500L), 0.0001f)
        assertEquals(1.5f, resolveWordMaskSweepProgress(word, 2_500L), 0.0001f)
    }

    @Test fun untransformedCharacterKeepsParentMaskCoordinates() {
        val mask = WordMaskGradientPx(20f, 60f)
        val local = resolveCharacterLocalWordMask(
            wordMask = mask,
            characterTranslateXPx = 0f,
            characterScale = 1f,
            pivotXPx = 40f,
        )

        assertEquals(mask.fadeStartX, local.fadeStartX, 0.0001f)
        assertEquals(mask.fadeEndX, local.fadeEndX, 0.0001f)
    }

    @Test fun translatedCharacterInverseMapsMaskToFixedParentPosition() {
        val local = resolveCharacterLocalWordMask(
            wordMask = WordMaskGradientPx(20f, 60f),
            characterTranslateXPx = 8f,
            characterScale = 1f,
            pivotXPx = 40f,
        )

        assertEquals(12f, local.fadeStartX, 0.0001f)
        assertEquals(52f, local.fadeEndX, 0.0001f)
    }

    @Test fun scaledCharacterInverseMapsMaskAroundGlyphPivot() {
        val local = resolveCharacterLocalWordMask(
            wordMask = WordMaskGradientPx(20f, 60f),
            characterTranslateXPx = 0f,
            characterScale = 2f,
            pivotXPx = 40f,
        )

        assertEquals(30f, local.fadeStartX, 0.0001f)
        assertEquals(50f, local.fadeEndX, 0.0001f)
    }

    @Test fun translatedAndScaledCharacterStillResolvesToParentMaskAfterTransform() {
        val translate = 8f
        val scale = 1.25f
        val pivot = 40f
        val parent = WordMaskGradientPx(20f, 60f)
        val local = resolveCharacterLocalWordMask(
            wordMask = parent,
            characterTranslateXPx = translate,
            characterScale = scale,
            pivotXPx = pivot,
        )

        fun forward(localX: Float): Float = translate + pivot + scale * (localX - pivot)

        assertEquals(parent.fadeStartX, forward(local.fadeStartX), 0.0001f)
        assertEquals(parent.fadeEndX, forward(local.fadeEndX), 0.0001f)
    }
}
