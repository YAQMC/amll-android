package dev.yaqmc.amll.ui

internal data class CharacterTextShadowSpec(
    val blurRadiusPx: Float,
    val alpha: Float,
)

/**
 * Converts AMLL's emphasized-character `text-shadow` values from em-space into pixels.
 *
 * Upstream already stores the exact CSS blur radius in [CharacterMotion.glowRadiusEm] and the
 * animated rgba alpha in [CharacterMotion.glowAlpha]. Do not attenuate either value again here.
 */
internal fun resolveCharacterTextShadowSpec(
    motion: CharacterMotion,
    fontSizePx: Float,
): CharacterTextShadowSpec? {
    if (motion.glowAlpha <= 0f || motion.glowRadiusEm <= 0f || fontSizePx <= 0f) return null

    return CharacterTextShadowSpec(
        blurRadiusPx = motion.glowRadiusEm * fontSizePx,
        alpha = motion.glowAlpha.coerceIn(0f, 1f),
    )
}
