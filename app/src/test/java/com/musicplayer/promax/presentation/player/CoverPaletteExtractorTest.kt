package com.musicplayer.promax.presentation.player

import android.graphics.Color
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class CoverPaletteExtractorTest {

    @Test
    fun extractFromPixelsSelectsVibrantDominantColors() {
        val fallbackPrimary = Color.rgb(30, 120, 240)
        val fallbackSecondary = Color.rgb(200, 100, 50)
        val crimson = Color.rgb(220, 42, 68)
        val emerald = Color.rgb(34, 198, 120)
        val pixels = IntArray(40) { index ->
            if (index < 28) crimson else emerald
        }

        val extracted = CoverPaletteExtractor.extractFromPixels(
            pixels = pixels,
            fallbackPrimaryArgb = fallbackPrimary,
            fallbackSecondaryArgb = fallbackSecondary
        )

        assertNotEquals(fallbackPrimary, extracted.primaryArgb)
        assertNotEquals(Color.BLACK, extracted.backdropArgb)
    }

    @Test
    fun extractFromPixelsFallsBackGracefullyOnTransparentOrNeutralPixels() {
        val fallbackPrimary = Color.rgb(66, 133, 244)
        val fallbackSecondary = Color.rgb(52, 168, 83)
        val transparentPixels = IntArray(16) { Color.TRANSPARENT }

        val extracted = CoverPaletteExtractor.extractFromPixels(
            pixels = transparentPixels,
            fallbackPrimaryArgb = fallbackPrimary,
            fallbackSecondaryArgb = fallbackSecondary
        )

        assertEquals(fallbackPrimary, extracted.primaryArgb)
        assertEquals(fallbackSecondary, extracted.secondaryArgb)
    }
}
