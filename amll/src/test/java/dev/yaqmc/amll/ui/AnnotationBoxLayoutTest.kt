package dev.yaqmc.amll.ui

import dev.yaqmc.amll.model.LyricWord
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class AnnotationBoxLayoutTest {
    @Test fun annotationBoxUsesWidestStackLayer() {
        assertEquals(
            60f,
            resolveWordBoxWidthPx(
                baseWidthPx = 40f,
                romanWidthPx = 50f,
                rubyWidthPx = 55f,
                romanEndPaddingPx = 10f,
            ),
            0.001f,
        )
        assertEquals(
            72f,
            resolveWordBoxWidthPx(
                baseWidthPx = 40f,
                romanWidthPx = 20f,
                rubyWidthPx = 72f,
                romanEndPaddingPx = 10f,
            ),
            0.001f,
        )
    }

    @Test fun rubyFlexRowWidthIsSumOfIndependentSegments() {
        assertEquals(42f, resolveRubyRowWidthPx(listOf(12f, 20f, 10f)), 0.001f)
        assertEquals(12f, resolveRubyRowWidthPx(listOf(12f, -8f)), 0.001f)
    }

    @Test fun rubySegmentsArePlacedAsOneCenteredFlexRow() {
        assertArrayEquals(
            floatArrayOf(79f, 91f, 111f),
            resolveRubySegmentLeftOffsetsPx(
                centerXPx = 100f,
                segmentWidthsPx = listOf(12f, 20f, 10f),
            ),
            0.001f,
        )
    }

    @Test fun maskHeightStacksRubyBaseAndRomanWithoutExtraGap() {
        assertEquals(
            68f,
            resolveWordMaskContentHeightPx(
                baseHeightPx = 40f,
                rubyBandHeightPx = 14f,
                romanBandHeightPx = 14f,
            ),
            0.001f,
        )
    }

    @Test fun romanEndPaddingShiftsVisibleTextTowardInlineStart() {
        assertEquals(
            65f,
            resolveCenteredAnnotationLeftPx(
                centerXPx = 100f,
                contentWidthPx = 60f,
                endPaddingPx = 10f,
            ),
            0.001f,
        )
        assertEquals(
            70f,
            resolveCenteredAnnotationLeftPx(
                centerXPx = 100f,
                contentWidthPx = 60f,
            ),
            0.001f,
        )
    }

    @Test fun chunkWidthIsSumOfChildVisualBoxes() {
        val plan = RenderWordPlan(
            listOf(
                RenderWordChunk(
                    listOf(
                        LyricWord(0, 100, "su"),
                        LyricWord(100, 200, "gar"),
                    )
                ),
                RenderWordChunk(listOf(LyricWord(200, 200, " "))),
            )
        )

        assertEquals(listOf(75f, 8f), resolveChunkWidthsPx(plan, listOf(30f, 45f, 8f)))
    }

    @Test fun startAlignedRowsAccumulateExtraWidthForward() {
        val plan = planForTranslations()
        val translations = resolveWordBoxTranslationsPx(
            plan = plan,
            breaks = emptyList(),
            baseWidthsPx = listOf(30f, 30f, 20f),
            wordBoxWidthsPx = listOf(50f, 40f, 20f),
            alignment = WordBoxAlignment.Start,
        )

        assertArrayEquals(floatArrayOf(10f, 25f, 30f), translations, 0.001f)
    }

    @Test fun endAlignedRowsPreserveVisualRightEdge() {
        val plan = planForTranslations()
        val translations = resolveWordBoxTranslationsPx(
            plan = plan,
            breaks = emptyList(),
            baseWidthsPx = listOf(30f, 30f, 20f),
            wordBoxWidthsPx = listOf(50f, 40f, 20f),
            alignment = WordBoxAlignment.End,
        )

        assertArrayEquals(floatArrayOf(-20f, -5f, 0f), translations, 0.001f)
    }

    @Test fun hardBreakResetsAccumulatedExtraForNextRow() {
        val plan = RenderWordPlan(
            listOf(
                RenderWordChunk(listOf(LyricWord(0, 100, "a"))),
                RenderWordChunk(listOf(LyricWord(100, 200, "b"))),
            )
        )
        val translations = resolveWordBoxTranslationsPx(
            plan = plan,
            breaks = listOf(1),
            baseWidthsPx = listOf(20f, 20f),
            wordBoxWidthsPx = listOf(40f, 50f),
            alignment = WordBoxAlignment.Start,
        )

        assertArrayEquals(floatArrayOf(10f, 15f), translations, 0.001f)
    }

    private fun planForTranslations() = RenderWordPlan(
        listOf(
            RenderWordChunk(listOf(LyricWord(0, 100, "a"))),
            RenderWordChunk(listOf(LyricWord(100, 200, "b"))),
            RenderWordChunk(listOf(LyricWord(200, 300, "c"))),
        )
    )
}
