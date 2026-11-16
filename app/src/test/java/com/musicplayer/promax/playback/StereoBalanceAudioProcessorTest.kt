package com.musicplayer.promax.playback

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor.AudioFormat
import androidx.media3.common.util.UnstableApi
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class StereoBalanceAudioProcessorTest {
    @Test
    fun computeChannelGains_handlesCenterLeftAndRight() {
        val center = StereoBalanceAudioProcessor.computeChannelGains(0f)
        assertEquals(1f, center.first, 0.001f)
        assertEquals(1f, center.second, 0.001f)

        val fullLeft = StereoBalanceAudioProcessor.computeChannelGains(-1f)
        assertEquals(1f, fullLeft.first, 0.001f)
        assertEquals(0f, fullLeft.second, 0.001f)

        val halfRight = StereoBalanceAudioProcessor.computeChannelGains(0.5f)
        assertEquals(0.5f, halfRight.first, 0.001f)
        assertEquals(1f, halfRight.second, 0.001f)
    }

    @OptIn(UnstableApi::class)
    @Test
    fun queueInput_scalesStereo16BitPcmCorrectly() {
        val processor = StereoBalanceAudioProcessor()
        processor.configure(AudioFormat(44_100, 2, C.ENCODING_PCM_16BIT))
        processor.flush()
        processor.setBalance(-0.5f) // Left = 1.0, Right = 0.5

        val input = ByteBuffer.allocateDirect(4).order(ByteOrder.nativeOrder()).apply {
            putShort(10_000)
            putShort(10_000)
            flip()
        }
        processor.queueInput(input)
        val output = processor.output
        assertEquals(4, output.remaining())
        assertEquals(10_000.toShort(), output.getShort())
        assertEquals(5_000.toShort(), output.getShort())
    }
}
