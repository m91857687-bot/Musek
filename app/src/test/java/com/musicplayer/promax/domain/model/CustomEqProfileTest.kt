package com.musicplayer.promax.domain.model

import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class CustomEqProfileTest {
    @Test
    fun encodeAndDecodeBandLevels_roundTripsValues() {
        val raw = listOf(450, 200, -150, 300, 550)
        val encoded = CustomEqProfileCodec.encodeBandLevels(raw)
        val decoded = CustomEqProfileCodec.decodeBandLevels(encoded)
        assertEquals(raw, decoded)
    }

    @Test
    fun encodeAndDecodeProfiles_roundTripsAndClampsValues() {
        val profiles = listOf(
            CustomEqProfile(
                id = "id-1",
                name = "Studio Warm",
                bandsMb = listOf(300, 150, 0, -100, 250),
                bassStrength = 600,
                virtualizerStrength = 400,
                loudnessMb = 750,
                stereoBalance = -0.35f
            ),
            CustomEqProfile(
                id = "id-2",
                name = "Night Bass",
                bandsMb = listOf(700, 400, 0, 100, 200),
                bassStrength = 900,
                virtualizerStrength = 250,
                loudnessMb = 500,
                stereoBalance = 0.25f
            )
        )

        val json = CustomEqProfileCodec.encodeProfiles(profiles)
        val restored = CustomEqProfileCodec.decodeProfiles(json)
        assertEquals(2, restored.size)
        assertEquals("Studio Warm", restored[0].name)
        assertEquals(listOf(300, 150, 0, -100, 250), restored[0].bandsMb)
        assertEquals(600, restored[0].bassStrength)
        assertEquals(-0.35f, restored[0].stereoBalance, 0.001f)
        assertEquals("Night Bass", restored[1].name)
    }

    @Test
    fun decodeProfiles_invalidJson_returnsEmpty() {
        assertTrue(CustomEqProfileCodec.decodeProfiles("not-json").isEmpty())
        assertTrue(CustomEqProfileCodec.decodeProfiles(null).isEmpty())
    }
}
