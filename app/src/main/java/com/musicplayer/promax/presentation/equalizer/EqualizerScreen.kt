package com.musicplayer.promax.presentation.equalizer

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.musicplayer.promax.R
import com.musicplayer.promax.domain.model.CustomEqProfile
import com.musicplayer.promax.playback.AudioEffectsBridge
import com.musicplayer.promax.playback.AudioEffectsManager
import com.musicplayer.promax.presentation.components.PremiumScreenHeader
import com.musicplayer.promax.presentation.settings.SettingsUiState
import com.musicplayer.promax.ui.theme.SecondaryText
import com.musicplayer.promax.ui.theme.SubtleBorder
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EqualizerScreen(
    settingsState: SettingsUiState = SettingsUiState(),
    onEffectsEnabledChanged: (Boolean) -> Unit = {},
    onPresetSelected: (String?) -> Unit = {},
    onBandLevelsChanged: (List<Int>) -> Unit = {},
    onSaveCustomProfile: (name: String, bandsMb: List<Int>, bass: Int, virt: Int, loud: Int, balance: Float) -> Unit = { _, _, _, _, _, _ -> },
    onDeleteCustomProfile: (String) -> Unit = {},
    onApplyCustomProfile: (CustomEqProfile) -> Unit = {},
    onBassChanged: (Int) -> Unit = {},
    onVirtualizerChanged: (Int) -> Unit = {},
    onLoudnessChanged: (Int) -> Unit = {},
    onStereoBalanceChanged: (Float) -> Unit = {},
    onSkipSilenceChanged: (Boolean) -> Unit = {},
    onPlaybackSpeedChanged: (Float) -> Unit = {},
    onCrossfadeSecondsChanged: (Int) -> Unit = {},
    onOpenSettings: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val manager by AudioEffectsBridge.manager.collectAsState()
    var bands by remember { mutableStateOf(manager?.bands().orEmpty()) }
    var bass by remember(settingsState.bassBoostStrength) { mutableIntStateOf(settingsState.bassBoostStrength) }
    var virtualizer by remember(settingsState.virtualizerStrength) { mutableIntStateOf(settingsState.virtualizerStrength) }
    var loudness by remember(settingsState.loudnessGainMb) { mutableIntStateOf(settingsState.loudnessGainMb) }
    var balance by remember(settingsState.stereoBalance) { mutableFloatStateOf(settingsState.stereoBalance) }
    var enabled by remember(settingsState.audioEffectsEnabled) { mutableStateOf(settingsState.audioEffectsEnabled) }
    var showSaveProfileDialog by remember { mutableStateOf(false) }

    LaunchedEffect(manager, settingsState.eqBandLevelsMb, settingsState.eqPresetName) {
        manager?.let { activeManager ->
            bands = activeManager.bands()
            bass = activeManager.bassStrength()
            virtualizer = activeManager.virtualizerStrength()
            loudness = activeManager.loudnessGain()
            enabled = activeManager.effectsEnabled
        }
    }

    val displayBands = remember(bands, settingsState.eqBandLevelsMb, settingsState.eqPresetName) {
        if (bands.isNotEmpty()) {
            bands
        } else {
            val fallbackFreqs = listOf(60, 230, 910, 3600, 14000)
            val fallbackLevels = settingsState.eqBandLevelsMb.takeIf { it.isNotEmpty() }
                ?: presetFallbackLevels(settingsState.eqPresetName)
            fallbackFreqs.mapIndexed { idx, freq ->
                AudioEffectsManager.Band(
                    index = idx.toShort(),
                    centerHz = freq,
                    minMb = -1500,
                    maxMb = 1500,
                    levelMb = fallbackLevels.getOrElse(idx) { 0 }.coerceIn(-1500, 1500)
                )
            }
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
        AudioEffectsManager.Preset.DANCE to R.string.preset_dance,
        AudioEffectsManager.Preset.CLASSICAL to R.string.preset_classical,
        AudioEffectsManager.Preset.ACOUSTIC to R.string.preset_acoustic
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

            // Master Engine + Presets Card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, SubtleBorder),
                shape = RoundedCornerShape(22.dp)
            ) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.audio_effects), fontWeight = FontWeight.Bold)
                            Text(
                                stringResource(R.string.audio_effects_description),
                                style = MaterialTheme.typography.bodySmall,
                                color = SecondaryText
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
                    HorizontalDivider(color = SubtleBorder.copy(alpha = 0.6f))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(stringResource(R.string.presets), fontWeight = FontWeight.SemiBold)
                        TextButton(
                            onClick = { showSaveProfileDialog = true },
                            enabled = enabled,
                            modifier = Modifier.testTag("eq_save_profile_button")
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(stringResource(R.string.save_eq_profile))
                        }
                    }
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
                                    val updatedBands = manager?.bands()?.also { bands = it }
                                    val levels = updatedBands?.map { it.levelMb } ?: presetFallbackLevels(preset.name)
                                    onBandLevelsChanged(levels)
                                    onPresetSelected(preset.name)
                                },
                                label = { Text(stringResource(labelRes)) }
                            )
                        }
                    }

                    if (settingsState.customEqProfiles.isNotEmpty()) {
                        HorizontalDivider(color = SubtleBorder.copy(alpha = 0.5f))
                        Text(
                            stringResource(R.string.custom_eq_profiles),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(vertical = 2.dp)
                        ) {
                            items(settingsState.customEqProfiles, key = { it.id }) { profile ->
                                Surface(
                                    onClick = {
                                        if (enabled) {
                                            manager?.applyBandLevels(profile.bandsMb)
                                            manager?.setBassStrength(profile.bassStrength)
                                            manager?.setVirtualizerStrength(profile.virtualizerStrength)
                                            manager?.setLoudnessGain(profile.loudnessMb)
                                            bands = manager?.bands().orEmpty()
                                            onApplyCustomProfile(profile)
                                        }
                                    },
                                    shape = RoundedCornerShape(14.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                                    border = BorderStroke(1.dp, SubtleBorder)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Equalizer,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = profile.name,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        IconButton(
                                            onClick = { onDeleteCustomProfile(profile.id) },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Close,
                                                contentDescription = stringResource(R.string.delete),
                                                modifier = Modifier.size(14.dp),
                                                tint = SecondaryText
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Interactive Studio Frequency Curve + Vertical EQ Faders
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, SubtleBorder),
                shape = RoundedCornerShape(22.dp)
            ) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Filled.GraphicEq, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text(stringResource(R.string.equalizer_bands), fontWeight = FontWeight.Bold)
                        }
                        TextButton(
                            enabled = enabled,
                            onClick = {
                                manager?.applyPreset(AudioEffectsManager.Preset.FLAT)
                                val updated = manager?.bands()?.also { bands = it }
                                val flatLevels = updated?.map { it.levelMb } ?: List(displayBands.size) { 0 }
                                onBandLevelsChanged(flatLevels)
                                onPresetSelected(AudioEffectsManager.Preset.FLAT.name)
                            }
                        ) {
                            Text(stringResource(R.string.preset_flat))
                        }
                    }

                    EqFrequencyCurveCanvas(
                        bands = displayBands,
                        enabled = enabled,
                        primaryColor = MaterialTheme.colorScheme.primary,
                        secondaryColor = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(126.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                            .testTag("eq_frequency_curve_canvas")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        displayBands.forEach { band ->
                            VerticalEqFader(
                                band = band,
                                enabled = enabled,
                                onLevelChange = { newMb ->
                                    manager?.setBandLevel(band.index, newMb)
                                    val nextBands = displayBands.map {
                                        if (it.index == band.index) it.copy(levelMb = newMb) else it
                                    }
                                    if (bands.isNotEmpty()) bands = nextBands
                                    onBandLevelsChanged(nextBands.map { it.levelMb })
                                    onPresetSelected(null)
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    if (manager == null) {
                        Text(
                            stringResource(R.string.audio_engine_starting),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    } else {
                        Text(
                            stringResource(R.string.equalizer_device_note),
                            style = MaterialTheme.typography.bodySmall,
                            color = SecondaryText
                        )
                    }
                }
            }

            // Stereo L/R Balance + Skip Silence Card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, SubtleBorder),
                shape = RoundedCornerShape(22.dp)
            ) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(stringResource(R.string.stereo_balance), fontWeight = FontWeight.SemiBold)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            val balanceLabel = when {
                                abs(balance) < 0.03f -> stringResource(R.string.stereo_balance_center)
                                balance < 0f -> stringResource(R.string.stereo_balance_left, (-balance * 100f).roundToInt())
                                else -> stringResource(R.string.stereo_balance_right, (balance * 100f).roundToInt())
                            }
                            Text(
                                text = balanceLabel,
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                            if (abs(balance) >= 0.03f) {
                                TextButton(onClick = {
                                    balance = 0f
                                    onStereoBalanceChanged(0f)
                                }) {
                                    Text(stringResource(R.string.stereo_balance_center))
                                }
                            }
                        }
                    }
                    Slider(
                        value = balance.coerceIn(-1f, 1f),
                        onValueChange = { raw ->
                            val snapped = if (abs(raw) < 0.05f) 0f else raw
                            balance = snapped
                            onStereoBalanceChanged(snapped)
                        },
                        valueRange = -1f..1f,
                        modifier = Modifier.testTag("eq_stereo_balance_slider")
                    )
                    HorizontalDivider(color = SubtleBorder.copy(alpha = 0.5f))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.skip_silence), fontWeight = FontWeight.SemiBold)
                            Text(
                                stringResource(R.string.skip_silence_description),
                                style = MaterialTheme.typography.bodySmall,
                                color = SecondaryText
                            )
                        }
                        Switch(
                            checked = settingsState.skipSilenceEnabled,
                            onCheckedChange = onSkipSilenceChanged,
                            modifier = Modifier.testTag("eq_skip_silence_switch")
                        )
                    }
                }
            }

            // Playback Speed & Smooth Crossfade Card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, SubtleBorder),
                shape = RoundedCornerShape(22.dp)
            ) {
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
                    HorizontalDivider(color = SubtleBorder.copy(alpha = 0.5f))
                    Text(stringResource(R.string.smooth_transition), fontWeight = FontWeight.SemiBold)
                    Text(
                        stringResource(R.string.crossfade_description),
                        style = MaterialTheme.typography.bodySmall,
                        color = SecondaryText
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

            // Hardware DSP Sliders
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
        }
    }

    if (showSaveProfileDialog) {
        SaveEqProfileDialog(
            onDismiss = { showSaveProfileDialog = false },
            onSave = { name ->
                onSaveCustomProfile(
                    name,
                    displayBands.map { it.levelMb },
                    bass,
                    virtualizer,
                    loudness,
                    balance
                )
                showSaveProfileDialog = false
                android.widget.Toast.makeText(
                    context,
                    context.getString(R.string.eq_profile_saved),
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
        )
    }
}

@Composable
private fun EqFrequencyCurveCanvas(
    bands: List<AudioEffectsManager.Band>,
    enabled: Boolean,
    primaryColor: Color,
    secondaryColor: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.padding(horizontal = 12.dp, vertical = 12.dp)) {
        val w = size.width
        val h = size.height
        val gridColor = SubtleBorder.copy(alpha = 0.7f)

        // Horizontal dB lines (+max, 0dB, -min)
        listOf(0.15f, 0.5f, 0.85f).forEachIndexed { idx, yFraction ->
            drawLine(
                color = if (idx == 1) primaryColor.copy(alpha = 0.35f) else gridColor,
                start = Offset(0f, h * yFraction),
                end = Offset(w, h * yFraction),
                strokeWidth = if (idx == 1) 2f else 1f
            )
        }

        if (bands.isEmpty()) return@Canvas

        val points = bands.mapIndexed { index, band ->
            val x = if (bands.size == 1) w / 2f else (index.toFloat() / (bands.size - 1)) * w
            val span = (band.maxMb - band.minMb).coerceAtLeast(1).toFloat()
            val norm = ((band.levelMb - band.minMb).toFloat() / span).coerceIn(0f, 1f)
            val y = h * 0.88f - norm * (h * 0.76f)
            Offset(x, y)
        }

        val linePath = Path().apply {
            moveTo(points.first().x, points.first().y)
            for (i in 0 until points.lastIndex) {
                val p0 = points[i]
                val p1 = points[i + 1]
                val cx = (p0.x + p1.x) / 2f
                cubicTo(cx, p0.y, cx, p1.y, p1.x, p1.y)
            }
        }

        val fillPath = Path().apply {
            addPath(linePath)
            lineTo(points.last().x, h)
            lineTo(points.first().x, h)
            close()
        }

        val strokeAlpha = if (enabled) 1f else 0.38f
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                listOf(
                    primaryColor.copy(alpha = 0.32f * strokeAlpha),
                    secondaryColor.copy(alpha = 0.08f * strokeAlpha),
                    Color.Transparent
                )
            )
        )
        drawPath(
            path = linePath,
            brush = Brush.horizontalGradient(
                listOf(
                    primaryColor.copy(alpha = strokeAlpha),
                    secondaryColor.copy(alpha = strokeAlpha)
                )
            ),
            style = Stroke(width = 5f, cap = StrokeCap.Round)
        )

        points.forEach { pt ->
            drawCircle(
                color = primaryColor.copy(alpha = 0.25f * strokeAlpha),
                radius = 12f,
                center = pt
            )
            drawCircle(
                color = secondaryColor.copy(alpha = strokeAlpha),
                radius = 6f,
                center = pt
            )
        }
    }
}

@Composable
private fun VerticalEqFader(
    band: AudioEffectsManager.Band,
    enabled: Boolean,
    onLevelChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val span = (band.maxMb - band.minMb).coerceAtLeast(1).toFloat()
    val norm = ((band.levelMb - band.minMb).toFloat() / span).coerceIn(0f, 1f)
    val dbText = "${if (band.levelMb >= 0) "+" else ""}${"%.0f".format(band.levelMb / 100f)}"
    val freqLabel = if (band.centerHz >= 1000) {
        val khz = band.centerHz / 1000f
        if (khz % 1f == 0f) "${khz.toInt()}k" else "${"%.1f".format(khz)}k"
    } else {
        "${band.centerHz}"
    }
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary

    Column(
        modifier = modifier.padding(horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = dbText,
            style = MaterialTheme.typography.labelSmall,
            color = if (enabled) primary else SecondaryText,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
        Box(
            modifier = Modifier
                .width(48.dp)
                .height(152.dp)
                .pointerInput(band.index, band.minMb, band.maxMb, enabled) {
                    if (!enabled) return@pointerInput
                    detectTapGestures { offset ->
                        val fraction = (1f - (offset.y / size.height.toFloat())).coerceIn(0f, 1f)
                        val newMb = (band.minMb + fraction * span).roundToInt().coerceIn(band.minMb, band.maxMb)
                        onLevelChange(newMb)
                    }
                }
                .pointerInput(band.index, band.minMb, band.maxMb, enabled) {
                    if (!enabled) return@pointerInput
                    detectDragGestures { change, _ ->
                        change.consume()
                        val fraction = (1f - (change.position.y / size.height.toFloat())).coerceIn(0f, 1f)
                        val newMb = (band.minMb + fraction * span).roundToInt().coerceIn(band.minMb, band.maxMb)
                        onLevelChange(newMb)
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize().padding(vertical = 10.dp)) {
                val trackWidth = 8.dp.toPx()
                val centerX = size.width / 2f
                val trackTop = 0f
                val trackHeight = size.height
                val midY = trackHeight / 2f
                val thumbY = trackHeight * (1f - norm)

                drawRoundRect(
                    color = SubtleBorder.copy(alpha = 0.7f),
                    topLeft = Offset(centerX - trackWidth / 2f, trackTop),
                    size = Size(trackWidth, trackHeight),
                    cornerRadius = CornerRadius(trackWidth / 2f, trackWidth / 2f)
                )

                val fillTop = minOf(midY, thumbY)
                val fillHeight = abs(thumbY - midY).coerceAtLeast(2f)
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        listOf(
                            primary.copy(alpha = if (enabled) 0.95f else 0.35f),
                            secondary.copy(alpha = if (enabled) 0.85f else 0.35f)
                        )
                    ),
                    topLeft = Offset(centerX - trackWidth / 2f, fillTop),
                    size = Size(trackWidth, fillHeight),
                    cornerRadius = CornerRadius(trackWidth / 2f, trackWidth / 2f)
                )

                drawCircle(
                    color = if (enabled) primary else SecondaryText,
                    radius = 9.dp.toPx(),
                    center = Offset(centerX, thumbY)
                )
                drawCircle(
                    color = Color.White.copy(alpha = if (enabled) 0.9f else 0.4f),
                    radius = 4.dp.toPx(),
                    center = Offset(centerX, thumbY)
                )
            }
        }
        Text(
            text = freqLabel,
            style = MaterialTheme.typography.labelSmall,
            color = SecondaryText,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

@Composable
private fun SaveEqProfileDialog(
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.save_eq_profile)) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it.take(40) },
                singleLine = true,
                label = { Text(stringResource(R.string.eq_profile_name)) },
                modifier = Modifier.fillMaxWidth().testTag("eq_profile_name_input")
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(name.trim()) },
                enabled = name.isNotBlank()
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
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
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, SubtleBorder),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                Text(displayLabel, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium)
            }
            Text(description, style = MaterialTheme.typography.bodySmall, color = SecondaryText)
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

private fun presetFallbackLevels(presetName: String?): List<Int> {
    val preset = presetName?.let { runCatching { AudioEffectsManager.Preset.valueOf(it) }.getOrNull() }
    return when (preset) {
        AudioEffectsManager.Preset.BASS -> listOf(650, 500, 150, 0, -100)
        AudioEffectsManager.Preset.VOCAL -> listOf(-150, 100, 450, 350, 100)
        AudioEffectsManager.Preset.TREBLE -> listOf(-100, 0, 150, 450, 650)
        AudioEffectsManager.Preset.POP -> listOf(-100, 250, 450, 250, -50)
        AudioEffectsManager.Preset.ROCK -> listOf(450, 200, -100, 300, 550)
        AudioEffectsManager.Preset.JAZZ -> listOf(350, 150, -150, 250, 400)
        AudioEffectsManager.Preset.DANCE -> listOf(550, 350, 0, 300, 450)
        AudioEffectsManager.Preset.CLASSICAL -> listOf(350, 200, -100, 250, 400)
        AudioEffectsManager.Preset.ACOUSTIC -> listOf(250, 150, 200, 350, 200)
        else -> listOf(0, 0, 0, 0, 0)
    }
}
