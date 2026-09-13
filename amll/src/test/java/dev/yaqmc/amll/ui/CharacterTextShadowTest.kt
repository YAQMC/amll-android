package dev.yaqmc.amll.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CharacterTextShadowTest {
    @Test fun zeroGlowProducesNoShadow() {
        assertNull(
            resolveCharacterTextShadowSpec(
                motion = CharacterMotion(glowAlpha = 0f, glowRadiusEm = 0.2f),
                fontSizePx = 40f,
            )
        )
    }

    @Test fun upstreamEmRadiusMapsDirectlyToPixels() {
        val spec = requireNotNull(
            resolveCharacterTextShadowSpec(
                motion = CharacterMotion(glowAlpha = 0.6f, glowRadiusEm = 0.24f),
                fontSizePx = 50f,
            )
        )

        assertEquals(12f, spec.blurRadiusPx, 0.0001f)
        assertEquals(0.6f, spec.alpha, 0.0001f)
    }

    @Test fun alphaIsNotAttenuatedByRenderer() {
        val spec = requireNotNull(
            resolveCharacterTextShadowSpec(
                motion = CharacterMotion(glowAlpha = 0.8f, glowRadiusEm = 0.3f),
                fontSizePx = 34f,
            )
        )

        assertEquals(0.8f, spec.alpha, 0.0001f)
        assertEquals(10.2f, spec.blurRadiusPx, 0.0001f)
    }
}
