package com.musicplayer.promax.playback

import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.Virtualizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Android platform audio effects attached to the active ExoPlayer audio session. */
class AudioEffectsManager(audioSessionId: Int) {
    data class Band(val index: Short, val centerHz: Int, val minMb: Int, val maxMb: Int, val levelMb: Int)

    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null
    @Volatile var effectsEnabled: Boolean = true
        private set

    val isEqualizerAvailable: Boolean get() = equalizer != null
    val isBassBoostAvailable: Boolean get() = bassBoost != null
    val isVirtualizerAvailable: Boolean get() = virtualizer != null

    init {
        if (audioSessionId > 0) {
            equalizer = runCatching { Equalizer(0, audioSessionId).apply { enabled = true } }.getOrNull()
            bassBoost = runCatching { BassBoost(0, audioSessionId).apply { enabled = true } }.getOrNull()
            virtualizer = runCatching { Virtualizer(0, audioSessionId).apply { enabled = true } }.getOrNull()
        }
    }

    fun bands(): List<Band> {
        val eq = equalizer ?: return emptyList()
        val range = runCatching { eq.bandLevelRange }.getOrNull() ?: return emptyList()
        return runCatching {
            (0 until eq.numberOfBands.toInt()).map { rawIndex ->
                val index = rawIndex.toShort()
                Band(index, eq.getCenterFreq(index) / 1000, range[0].toInt(), range[1].toInt(), eq.getBandLevel(index).toInt())
            }
        }.getOrDefault(emptyList())
    }

    fun setBandLevel(index: Short, levelMb: Int) {
        val eq = equalizer ?: return
        runCatching {
            val range = eq.bandLevelRange
            eq.setBandLevel(index, levelMb.coerceIn(range[0].toInt(), range[1].toInt()).toShort())
        }
    }

    fun setBassStrength(value: Int) {
        runCatching { bassBoost?.setStrength(value.coerceIn(0, 1000).toShort()) }
    }

    fun bassStrength(): Int = runCatching { bassBoost?.roundedStrength?.toInt() ?: 0 }.getOrDefault(0)

    fun setVirtualizerStrength(value: Int) {
        runCatching { virtualizer?.setStrength(value.coerceIn(0, 1000).toShort()) }
    }

    fun virtualizerStrength(): Int = runCatching { virtualizer?.roundedStrength?.toInt() ?: 0 }.getOrDefault(0)

    fun setEnabled(enabled: Boolean) {
        effectsEnabled = enabled
        runCatching { equalizer?.enabled = enabled }
        runCatching { bassBoost?.enabled = enabled }
        runCatching { virtualizer?.enabled = enabled }
    }

    fun applyPreset(preset: Preset) {
        runCatching {
            val eq = equalizer ?: return
            val count = eq.numberOfBands.toInt()
            if (count <= 0) return
            val values = when (preset) {
                Preset.FLAT -> listOf(0, 0, 0, 0, 0)
                Preset.BASS -> listOf(650, 500, 150, 0, -100)
                Preset.VOCAL -> listOf(-150, 100, 450, 350, 100)
                Preset.TREBLE -> listOf(-100, 0, 150, 450, 650)
            }
            val range = eq.bandLevelRange
            for (i in 0 until count) {
                val normalized = if (count == 1) 2 else (i * 4f / (count - 1)).toInt().coerceIn(0, 4)
                val value = values[normalized].coerceIn(range[0].toInt(), range[1].toInt())
                eq.setBandLevel(i.toShort(), value.toShort())
            }
        }
    }

    fun release() {
        effectsEnabled = false
        runCatching { equalizer?.release() }
        runCatching { bassBoost?.release() }
        runCatching { virtualizer?.release() }
        equalizer = null
        bassBoost = null
        virtualizer = null
    }

    enum class Preset { FLAT, BASS, VOCAL, TREBLE }
}

/** Same-process bridge: the playback service owns the effects and the UI only controls them. */
object AudioEffectsBridge {
    private val _manager = MutableStateFlow<AudioEffectsManager?>(null)
    val manager: StateFlow<AudioEffectsManager?> = _manager.asStateFlow()

    fun attach(value: AudioEffectsManager) { _manager.value = value }
    fun detach(value: AudioEffectsManager) {
        if (_manager.value === value) _manager.value = null
    }
}
