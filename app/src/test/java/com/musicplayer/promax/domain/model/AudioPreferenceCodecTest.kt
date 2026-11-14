package com.musicplayer.promax.domain.model

import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AudioPreferenceCodecTest {

    @Test
    fun bandLevelsRoundTripPreservesOrderAndValues() {
        val levels = listOf(-400, 0, 350, 1200, -150)
        val encoded = AudioPreferenceCodec.encodeBandLevels(levels)
        assertEquals(levels, AudioPreferenceCodec.decodeBandLevels(encoded))
        assertTrue(AudioPreferenceCodec.decodeBandLevels(null).isEmpty())
        assertTrue(AudioPreferenceCodec.decodeBandLevels("   ").isEmpty())
    }

    @Test
    fun customPresetsRoundTripClampsEffectsAndSkipsInvalidEntries() {
        val presets = listOf(
            CustomEqPreset(
                name = "Late Night",
                bandLevelsMb = listOf(300, 150, 0, 100, 250),
                bassStrength = 640,
                virtualizerStrength = 420
            ),
            CustomEqPreset(
                name = "Studio Flat",
                bandLevelsMb = listOf(0, 0, 0, 0, 0),
                bassStrength = 0,
                virtualizerStrength = 0
            )
        )
        val encoded = AudioPreferenceCodec.encodeCustomPresets(presets)
        val decoded = AudioPreferenceCodec.decodeCustomPresets(encoded)
        assertEquals(presets, decoded)
        assertTrue(AudioPreferenceCodec.decodeCustomPresets("not-json").isEmpty())
    }
}
