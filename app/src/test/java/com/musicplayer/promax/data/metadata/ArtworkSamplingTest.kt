package com.musicplayer.promax.data.metadata

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ArtworkSamplingTest {
    @Test
    fun keepsBothDecodedDimensionsWithinLimit() {
        val sample = ArtworkSampling.calculateSampleSize(12_000, 8_000, 1_200)
        assertTrue(12_000 / sample <= 1_200)
        assertTrue(8_000 / sample <= 1_200)
    }

    @Test
    fun doesNotUpsampleSmallArtwork() {
        assertEquals(1, ArtworkSampling.calculateSampleSize(640, 480, 1_200))
    }

    @Test
    fun rejectsInvalidDimensions() {
        assertFailsWith<IllegalArgumentException> {
            ArtworkSampling.calculateSampleSize(0, 100, 1_200)
        }
    }
}
