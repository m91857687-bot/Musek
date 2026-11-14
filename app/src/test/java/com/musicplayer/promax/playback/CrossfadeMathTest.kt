package com.musicplayer.promax.playback

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CrossfadeMathTest {

    @Test
    fun clampDurationSecondsEnforcesZeroToTwelveRange() {
        assertEquals(0, CrossfadeMath.clampDurationSeconds(-5))
        assertEquals(4, CrossfadeMath.clampDurationSeconds(4))
        assertEquals(12, CrossfadeMath.clampDurationSeconds(30))
    }

    @Test
    fun fadeOutAndFadeInProgressSmoothlyWithinBounds() {
        assertEquals(1f, CrossfadeMath.fadeOutVolume(remainingMs = 4000L, fadeWindowMs = 4000L))
        assertEquals(0.08f, CrossfadeMath.fadeOutVolume(remainingMs = 0L, fadeWindowMs = 4000L))
        val midOut = CrossfadeMath.fadeOutVolume(remainingMs = 2000L, fadeWindowMs = 4000L)
        assertTrue(midOut in 0.45f..0.60f)

        assertEquals(0.08f, CrossfadeMath.fadeInVolume(elapsedMs = 0L, fadeWindowMs = 4000L))
        assertEquals(1f, CrossfadeMath.fadeInVolume(elapsedMs = 4000L, fadeWindowMs = 4000L))
        val midIn = CrossfadeMath.fadeInVolume(elapsedMs = 2000L, fadeWindowMs = 4000L)
        assertTrue(midIn in 0.45f..0.60f)
    }

    @Test
    fun nonPositiveWindowDefaultsToFullVolume() {
        assertEquals(1f, CrossfadeMath.fadeOutVolume(remainingMs = 500L, fadeWindowMs = 0L))
        assertEquals(1f, CrossfadeMath.fadeInVolume(elapsedMs = 500L, fadeWindowMs = 0L))
    }
}
