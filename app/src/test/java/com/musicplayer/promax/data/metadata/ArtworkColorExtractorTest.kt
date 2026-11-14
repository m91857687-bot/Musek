package com.musicplayer.promax.data.metadata

import android.graphics.Color
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ArtworkColorExtractorTest {
    @Test
    fun returnsNullForEmptyPixels() {
        assertNull(ArtworkColorExtractor.extractAccentPalette(IntArray(0)))
    }

    @Test
    fun returnsNullWhenImageHasOnlyNeutralOrDarkPixels() {
        val neutralPixels = intArrayOf(
            Color.BLACK,
            Color.WHITE,
            Color.DKGRAY,
            Color.LTGRAY,
            Color.argb(60, 200, 30, 30) // translucent pixel skipped
        )
        assertNull(ArtworkColorExtractor.extractAccentPalette(neutralPixels))
    }

    @Test
    fun extractsDominantAndDistinctSecondaryHueFromArtworkPixels() {
        val pixels = IntArray(40) { index ->
            if (index < 26) Color.rgb(25, 110, 235) // dominant vivid blue
            else Color.rgb(245, 95, 50) // secondary warm orange
        }
        val palette = ArtworkColorExtractor.extractAccentPalette(pixels)
        assertNotNull(palette)
        assertNotEquals(palette.primaryArgb, palette.secondaryArgb)
        val primaryHsv = FloatArray(3)
        Color.colorToHSV(palette.primaryArgb, primaryHsv)
        assertTrue(primaryHsv[0] in 190f..245f, "Expected dominant blue hue, got ${primaryHsv[0]}")
    }
}
