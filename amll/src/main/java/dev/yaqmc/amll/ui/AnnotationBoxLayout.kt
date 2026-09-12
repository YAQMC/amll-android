package dev.yaqmc.amll.ui

import kotlin.math.max

internal enum class WordBoxAlignment {
    Start,
    Center,
    End,
}

/**
 * Resolves the horizontal footprint of one AMLL word box.
 *
 * The DOM renderer stacks base + roman inside `wordBody`, then stacks ruby above that body. Both
 * stacks are center-aligned, so the outer width is the maximum participating width rather than a
 * sum. `romanWidthPx` should include the blank NBSP layout when the line has romanWord enabled.
 */
internal fun resolveWordBoxWidthPx(
    baseWidthPx: Float,
    romanWidthPx: Float?,
    rubyWidthPx: Float?,
    romanEndPaddingPx: Float,
): Float {
    val safeBase = baseWidthPx.coerceAtLeast(0f)
    val romanBox = romanWidthPx?.let { it.coerceAtLeast(0f) + romanEndPaddingPx.coerceAtLeast(0f) }
        ?: 0f
    val wordBody = max(safeBase, romanBox)
    return max(wordBody, rubyWidthPx?.coerceAtLeast(0f) ?: 0f)
}

/** Sum child word-box widths for each indivisible render chunk. */
internal fun resolveChunkWidthsPx(
    plan: RenderWordPlan,
    wordBoxWidthsPx: List<Float>,
): List<Float> {
    require(plan.words.size == wordBoxWidthsPx.size) {
        "render words and wordBoxWidthsPx must have the same size"
    }

    var atomIndex = 0
    return plan.chunks.map { chunk ->
        var width = 0f
        repeat(chunk.words.size) {
            width += wordBoxWidthsPx[atomIndex++].coerceAtLeast(0f)
        }
        width
    }
}

/**
 * Computes the draw-time X translation for each child word after balanced hard breaks are known.
 *
 * TextLayoutResult still packs base glyphs using only base widths. AMLL's annotation containers can
 * be wider than those glyphs, so each base word is centered inside its visual box and later words
 * are shifted by accumulated extra width. End/center aligned rows compensate from the left so the
 * row's visual right/center anchor remains stable.
 */
internal fun resolveWordBoxTranslationsPx(
    plan: RenderWordPlan,
    breaks: List<Int>,
    baseWidthsPx: List<Float>,
    wordBoxWidthsPx: List<Float>,
    alignment: WordBoxAlignment,
): FloatArray {
    require(plan.words.size == baseWidthsPx.size && plan.words.size == wordBoxWidthsPx.size) {
        "render words, baseWidthsPx and wordBoxWidthsPx must have the same size"
    }
    if (plan.words.isEmpty()) return FloatArray(0)

    val translations = FloatArray(plan.words.size)
    val rowBreaks = (breaks + plan.chunks.size).distinct().sorted()
    var rowStartChunk = 0
    var rowStartAtom = 0

    rowBreaks.forEach { rowEndChunk ->
        var rowAtomCount = 0
        for (chunkIndex in rowStartChunk until rowEndChunk) {
            rowAtomCount += plan.chunks[chunkIndex].words.size
        }
        val rowEndAtom = rowStartAtom + rowAtomCount
        var totalExtra = 0f
        for (atomIndex in rowStartAtom until rowEndAtom) {
            totalExtra += (
                wordBoxWidthsPx[atomIndex].coerceAtLeast(0f) -
                    baseWidthsPx[atomIndex].coerceAtLeast(0f)
                ).coerceAtLeast(0f)
        }

        var accumulatedExtra = when (alignment) {
            WordBoxAlignment.Start -> 0f
            WordBoxAlignment.Center -> -totalExtra / 2f
            WordBoxAlignment.End -> -totalExtra
        }

        for (atomIndex in rowStartAtom until rowEndAtom) {
            val extra = (
                wordBoxWidthsPx[atomIndex].coerceAtLeast(0f) -
                    baseWidthsPx[atomIndex].coerceAtLeast(0f)
                ).coerceAtLeast(0f)
            translations[atomIndex] = accumulatedExtra + extra / 2f
            accumulatedExtra += extra
        }

        rowStartChunk = rowEndChunk
        rowStartAtom = rowEndAtom
    }

    return translations
}
