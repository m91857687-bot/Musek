package com.musicplayer.promax.presentation.equalizer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.musicplayer.promax.R
import com.musicplayer.promax.playback.AudioEffectsBridge
import com.musicplayer.promax.playback.AudioEffectsManager
import com.musicplayer.promax.presentation.components.PremiumScreenHeader
import com.musicplayer.promax.presentation.settings.SettingsUiState
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EqualizerScreen(
    settingsState: SettingsUiState = SettingsUiState(),
    onEffectsEnabledChanged: (Boolean) -> Unit = {},
    onPresetSelected: (String?) -> Unit = {},
    onBassChanged: (Int) -> Unit = {},
    onVirtualizerChanged: (Int) -> Unit = {},
    onLoudnessChanged: (Int) -> Unit = {},
    onPlaybackSpeedChanged: (Float) -> Unit = {},
    onCrossfadeSecondsChanged: (Int) -> Unit = {},
    onOpenSettings: (() -> Unit)? = null
) {
    val manager by AudioEffectsBridge.manager.collectAsState()
    var bands by remember { mutableStateOf(manager?.bands().orEmpty()) }
    var bass by remember(settingsState.bassBoostStrength) { mutableIntStateOf(settingsState.bassBoostStrength) }
    var virtualizer by remember(settingsState.virtualizerStrength) { mutableIntStateOf(settingsState.virtualizerStrength) }
    var loudness by remember(settingsState.loudnessGainMb) { mutableIntStateOf(settingsState.loudnessGainMb) }
    var enabled by remember(settingsState.audioEffectsEnabled) { mutableStateOf(settingsState.audioEffectsEnabled) }

    LaunchedEffect(manager) {
        manager?.let { activeManager ->
            bands = activeManager.bands()
            bass = activeManager.bassStrength()
            virtualizer = activeManager.virtualizerStrength()
            loudness = activeManager.loudnessGain()
            enabled = activeManager.effectsEnabled
        }
    }

    val presetOptions = listOf(
        AudioEffectsManager.Preset.FLAT to R.string.preset_flat,
        AudioEffectsManager.Preset.BASS to R.string.preset_bass,
        AudioEffectsManager.Preset.VOCAL to R.string.preset_vocal,
        AudioEffectsManager.Preset.TREBLE to R.string.preset_treble,
        AudioEffectsManager.Preset.POP to R.string.preset_pop,
        AudioEffectsManager.Preset.ROCK to R.string.preset_rock,
        AudioEffectsManager.Preset.JAZZ to R.string.preset_jazz,
        AudioEffectsManager.Preset.DANCE to R.string.preset_dance
    )

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 680.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            PremiumScreenHeader(
                eyebrow = stringResource(R.string.sound_control),
                title = stringResource(R.string.equalizer),
                subtitle = stringResource(R.string.equalizer_screen_subtitle),
                trailingAction = onOpenSettings?.let { openSettings ->
                    {
                        IconButton(
                            onClick = openSettings,
                            modifier = Modifier.testTag("equalizer_settings_button")
                        ) {
                            Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.settings))
                        }
                    }
                }
            )

            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.audio_effects), fontWeight = FontWeight.SemiBold)
                            Text(
                                stringResource(R.string.audio_effects_description),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = enabled,
                            onCheckedChange = { checked ->
                                enabled = checked
                                manager?.setEnabled(checked)
                                onEffectsEnabledChanged(checked)
                            }
                        )
                    }
                    HorizontalDivider()
                    Text(stringResource(R.string.presets), fontWeight = FontWeight.SemiBold)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        presetOptions.forEach { (preset, labelRes) ->
                            val isSelected = settingsState.eqPresetName == preset.name
                            FilterChip(
                                selected = isSelected,
                                enabled = enabled,
                                onClick = {
                                    manager?.applyPreset(preset)
                                    bands = manager?.bands().orEmpty()
                                    onPresetSelected(preset.name)
                                },
                                label = { Text(stringResource(labelRes)) }
                            )
                        }
                    }
                }
            }

            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(stringResource(R.string.playback_speed), fontWeight = FontWeight.SemiBold)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                String.format(Locale.US, "%.2gx", settingsState.playbackSpeed),
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                            if (abs(settingsState.playbackSpeed - 1.0f) >= 0.03f) {
                                TextButton(onClick = { onPlaybackSpeedChanged(1.0f) }) {
                                    Text(stringResource(R.string.reset_speed))
                                }
                            }
                        }
                    }
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { speed ->
                            FilterChip(
                                selected = abs(settingsState.playbackSpeed - speed) < 0.03f,
                                onClick = { onPlaybackSpeedChanged(speed) },
                                label = { Text(String.format(Locale.US, "%.2gx", speed)) }
                            )
                        }
                    }
                    Slider(
                        value = settingsState.playbackSpeed.coerceIn(0.5f, 2.0f),
                        onValueChange = { raw ->
                            val stepped = ((raw * 20f).toInt() / 20f).coerceIn(0.5f, 2.0f)
                            onPlaybackSpeedChanged(stepped)
                        },
                        valueRange = 0.5f..2.0f
                    )
                    HorizontalDivider()
                    Text(stringResource(R.string.smooth_transition), fontWeight = FontWeight.SemiBold)
                    Text(
                        stringResource(R.string.crossfade_description),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(0, 2, 4, 6, 8).forEach { seconds ->
                            FilterChip(
                                selected = settingsState.crossfadeSeconds == seconds,
                                onClick = { onCrossfadeSecondsChanged(seconds) },
                                label = {
                                    Text(
                                        if (seconds == 0) stringResource(R.string.crossfade_off)
                                        else stringResource(R.string.crossfade_seconds, seconds)
                                    )
                                }
                            )
                        }
                    }
                }
            }

            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.equalizer_bands), fontWeight = FontWeight.SemiBold)
                    if (bands.isEmpty()) {
                        Text(
                            stringResource(R.string.audio_effects_unavailable),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    } else {
                        bands.forEach { band ->
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        if (band.centerHz >= 1000) "${band.centerHz / 1000f} kHz" else "${band.centerHz} Hz",
                                        modifier = Modifier.weight(1f),
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                    Text(
                                        "${if (band.levelMb >= 0) "+" else ""}${"%.1f".format(band.levelMb / 100f)} dB",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Slider(
                                    value = band.levelMb.toFloat(),
                                    onValueChange = { value ->
                                        manager?.setBandLevel(band.index, value.roundToInt())
                                        bands = bands.map { if (it.index == band.index) it.copy(levelMb = value.roundToInt()) else it }
                                        onPresetSelected(null)
                                    },
                                    valueRange = band.minMb.toFloat()..band.maxMb.toFloat(),
                                    enabled = enabled
                                )
                            }
                        }
                    }
                    Text(
                        stringResource(R.string.equalizer_device_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            EffectSliderCard(
                title = stringResource(R.string.bass_boost),
                description = stringResource(R.string.bass_boost_description),
                value = bass,
                maxValue = 1000f,
                displayLabel = "${(bass / 10f).roundToInt()}%",
                available = manager?.isBassBoostAvailable == true,
                enabled = enabled,
                onValueChange = {
                    bass = it
                    manager?.setBassStrength(it)
                    onBassChanged(it)
                }
            )
            EffectSliderCard(
                title = stringResource(R.string.virtualizer),
                description = stringResource(R.string.virtualizer_description),
                value = virtualizer,
                maxValue = 1000f,
                displayLabel = "${(virtualizer / 10f).roundToInt()}%",
                available = manager?.isVirtualizerAvailable == true,
                enabled = enabled,
                onValueChange = {
                    virtualizer = it
                    manager?.setVirtualizerStrength(it)
                    onVirtualizerChanged(it)
                }
            )
            EffectSliderCard(
                title = stringResource(R.string.loudness_enhancer),
                description = stringResource(R.string.loudness_enhancer_description),
                value = loudness,
                maxValue = 1500f,
                displayLabel = "+${"%.1f".format(loudness / 100f)} dB",
                available = manager?.isLoudnessAvailable == true,
                enabled = enabled,
                onValueChange = {
                    loudness = it
                    manager?.setLoudnessGain(it)
                    onLoudnessChanged(it)
                }
            )
            if (manager == null) {
                Text(stringResource(R.string.audio_engine_starting), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun EffectSliderCard(
    title: String,
    description: String,
    value: Int,
    maxValue: Float,
    displayLabel: String,
    available: Boolean,
    enabled: Boolean,
    onValueChange: (Int) -> Unit
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                Text(displayLabel, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium)
            }
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Slider(
                value = value.toFloat(),
                onValueChange = { onValueChange(it.roundToInt()) },
                valueRange = 0f..maxValue,
                enabled = available && enabled
            )
            if (!available) {
                Text(
                    stringResource(R.string.effect_not_supported),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
