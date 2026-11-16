package com.musicplayer.promax.playback

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor.AudioFormat
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Real-time stereo L/R balance DSP processor for 16-bit stereo PCM audio.
 * balance = -1f -> full Left, 0f -> Center, +1f -> full Right.
 */
@OptIn(UnstableApi::class)
class StereoBalanceAudioProcessor : BaseAudioProcessor() {
    @Volatile
    private var balance: Float = 0f

    fun setBalance(value: Float) {
        balance = value.coerceIn(-1f, 1f)
    }

    fun getBalance(): Float = balance

    override fun onConfigure(inputAudioFormat: AudioFormat): AudioFormat {
        return if (inputAudioFormat.encoding == C.ENCODING_PCM_16BIT && inputAudioFormat.channelCount == 2) {
            inputAudioFormat
        } else {
            AudioFormat.NOT_SET
        }
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val remaining = inputBuffer.remaining()
        if (remaining == 0) return

        val (leftGain, rightGain) = computeChannelGains(balance)
        val out = replaceOutputBuffer(remaining)

        if (abs(balance) < 0.005f) {
            out.put(inputBuffer)
            out.flip()
            return
        }

        // Frame size = 4 bytes (2 channels * 16-bit signed LE)
        while (inputBuffer.remaining() >= 4) {
            val leftSample = inputBuffer.getShort()
            val rightSample = inputBuffer.getShort()
            val scaledLeft = (leftSample * leftGain).roundToInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            val scaledRight = (rightSample * rightGain).roundToInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            out.putShort(scaledLeft)
            out.putShort(scaledRight)
        }
        while (inputBuffer.hasRemaining()) {
            out.put(inputBuffer.get())
        }
        out.flip()
    }

    companion object {
        fun computeChannelGains(balance: Float): Pair<Float, Float> {
            val clamped = balance.coerceIn(-1f, 1f)
            val leftGain = if (clamped <= 0f) 1f else (1f - clamped).coerceIn(0f, 1f)
            val rightGain = if (clamped >= 0f) 1f else (1f + clamped).coerceIn(0f, 1f)
            return leftGain to rightGain
        }
    }
}
