package dev.yaqmc.amll.model

/**
 * Mirrors upstream AMLL's obscene-word masking modes.
 *
 * Masking changes only the base lyric text. Ruby and romanization stay untouched so their timing
 * and pronunciation metadata remain available exactly like upstream.
 */
enum class MaskObsceneWordsMode {
    Disabled,
    FullMask,
    PartialMask,
}
