package dev.yaqmc.amll.state

import dev.yaqmc.amll.model.LyricLine
import dev.yaqmc.amll.model.MaskObsceneWordsMode

internal fun applyObsceneWordMask(
    lines: List<LyricLine>,
    mode: MaskObsceneWordsMode,
    maskChar: Char,
): List<LyricLine> {
    if (mode == MaskObsceneWordsMode.Disabled) return lines

    return lines.map { line ->
        line.copy(
            words = line.words.map { word ->
                if (!word.obscene) {
                    word
                } else {
                    word.copy(
                        text = maskObsceneText(
                            text = word.text,
                            mode = mode,
                            maskChar = maskChar,
                        ),
                    )
                }
            },
        )
    }
}

internal fun maskObsceneText(
    text: String,
    mode: MaskObsceneWordsMode,
    maskChar: Char,
): String = when (mode) {
    MaskObsceneWordsMode.Disabled -> text
    MaskObsceneWordsMode.FullMask -> text.maskNonWhitespace(maskChar)
    MaskObsceneWordsMode.PartialMask -> {
        val trimmed = text.trim()
        if (trimmed.length <= 2) {
            text.maskNonWhitespace(maskChar)
        } else {
            val firstVisibleIndex = text.indexOf(trimmed)
            val lastVisibleIndex = firstVisibleIndex + trimmed.length - 1
            buildString(text.length) {
                text.forEachIndexed { index, char ->
                    append(
                        if (
                            index > firstVisibleIndex &&
                            index < lastVisibleIndex &&
                            !char.isWhitespace()
                        ) {
                            maskChar
                        } else {
                            char
                        },
                    )
                }
            }
        }
    }
}

private fun String.maskNonWhitespace(maskChar: Char): String =
    buildString(length) {
        this@maskNonWhitespace.forEach { char ->
            append(if (char.isWhitespace()) char else maskChar)
        }
    }
