package dev.yaqmc.amll.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TransformAnimationPolicyTest {
    @Test fun enabledUsesPhysicalSpring() {
        val policy = resolveTransformAnimationPolicy(enableSpring = true)

        assertTrue(policy.usePhysicalSpring)
        assertEquals(500, policy.fallbackDurationMs)
    }

    @Test fun disabledUsesFiveHundredMillisecondTransformTransition() {
        val policy = resolveTransformAnimationPolicy(enableSpring = false)

        assertFalse(policy.usePhysicalSpring)
        assertEquals(AMLL_DISABLED_SPRING_TRANSFORM_DURATION_MS, policy.fallbackDurationMs)
        assertEquals(500, policy.fallbackDurationMs)
    }
}
