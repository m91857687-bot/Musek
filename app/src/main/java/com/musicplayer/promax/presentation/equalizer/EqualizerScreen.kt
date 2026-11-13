package com.musicplayer.promax.presentation.equalizer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.musicplayer.promax.R
import com.musicplayer.promax.playback.AudioEffectsBridge
import com.musicplayer.promax.playback.AudioEffectsManager
import com.musicplayer.promax.presentation.components.PremiumScreenHeader
import kotlin.math.roundToInt

@Composable
fun EqualizerScreen() {
    val manager by AudioEffectsBridge.manager.collectAsState()
    var bands by remember { mutableStateOf(manager?.bands().orEmpty()) }
    var bass by remember { mutableStateOf(manager?.bassStrength() ?: 0) }
    var virtualizer by remember { mutableStateOf(manager?.virtualizerStrength() ?: 0) }
    var enabled by remember { mutableStateOf(manager?.effectsEnabled ?: true) }

    LaunchedEffect(manager) {
        bands = manager?.bands().orEmpty()
        bass = manager?.bassStrength() ?: 0
        virtualizer = manager?.virtualizerStrength() ?: 0
        enabled = manager?.effectsEnabled ?: true
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 22.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        PremiumScreenHeader(
            eyebrow = stringResource(R.string.sound_control),
            title = stringResource(R.string.equalizer),
            subtitle = stringResource(R.string.equalizer_screen_subtitle)
        )

        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.audio_effects), fontWeight = FontWeight.SemiBold)
                        Text(stringResource(R.string.audio_effects_description), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = enabled, enabled = manager != null, onCheckedChange = {
                        enabled = it
                        manager?.setEnabled(it)
                    })
                }
                HorizontalDivider()
                Text(stringResource(R.string.presets), fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PresetChip(stringResource(R.string.preset_flat), manager != null && enabled) { manager?.applyPreset(AudioEffectsManager.Preset.FLAT); bands = manager?.bands().orEmpty() }
                    PresetChip(stringResource(R.string.preset_bass), manager != null && enabled) { manager?.applyPreset(AudioEffectsManager.Preset.BASS); bands = manager?.bands().orEmpty() }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PresetChip(stringResource(R.string.preset_vocal), manager != null && enabled) { manager?.applyPreset(AudioEffectsManager.Preset.VOCAL); bands = manager?.bands().orEmpty() }
                    PresetChip(stringResource(R.string.preset_treble), manager != null && enabled) { manager?.applyPreset(AudioEffectsManager.Preset.TREBLE); bands = manager?.bands().orEmpty() }
                }
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.equalizer_bands), fontWeight = FontWeight.SemiBold)
                if (bands.isEmpty()) {
                    Text(stringResource(R.string.audio_effects_unavailable), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                } else {
                    bands.forEach { band ->
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(if (band.centerHz >= 1000) "${band.centerHz / 1000f} kHz" else "${band.centerHz} Hz", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelMedium)
                                Text("${if (band.levelMb >= 0) "+" else ""}${"%.1f".format(band.levelMb / 100f)} dB", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            }
                            Slider(
                                value = band.levelMb.toFloat(),
                                onValueChange = { value ->
                                    manager?.setBandLevel(band.index, value.roundToInt())
                                    bands = bands.map { if (it.index == band.index) it.copy(levelMb = value.roundToInt()) else it }
                                },
                                valueRange = band.minMb.toFloat()..band.maxMb.toFloat(),
                                enabled = enabled
                            )
                        }
                    }
                }
                Text(stringResource(R.string.equalizer_device_note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        EffectSliderCard(
            title = stringResource(R.string.bass_boost),
            description = stringResource(R.string.bass_boost_description),
            value = bass,
            available = manager?.isBassBoostAvailable == true,
            enabled = enabled,
            onValueChange = { bass = it; manager?.setBassStrength(it) }
        )
        EffectSliderCard(
            title = stringResource(R.string.virtualizer),
            description = stringResource(R.string.virtualizer_description),
            value = virtualizer,
            available = manager?.isVirtualizerAvailable == true,
            enabled = enabled,
            onValueChange = { virtualizer = it; manager?.setVirtualizerStrength(it) }
        )
        if (manager == null) Text(stringResource(R.string.audio_engine_starting), color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun PresetChip(label: String, available: Boolean, onClick: () -> Unit) {
    AssistChip(onClick = onClick, enabled = available, label = { Text(label) })
}

@Composable
private fun EffectSliderCard(
    title: String,
    description: String,
    value: Int,
    available: Boolean,
    enabled: Boolean,
    onValueChange: (Int) -> Unit
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                Text("${(value / 10f).roundToInt()}%", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium)
            }
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Slider(value = value.toFloat(), onValueChange = { onValueChange(it.roundToInt()) }, valueRange = 0f..1000f, enabled = available && enabled)
            if (!available) Text(stringResource(R.string.effect_not_supported), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
