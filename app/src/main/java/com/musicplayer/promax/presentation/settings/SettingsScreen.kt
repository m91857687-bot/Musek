package com.musicplayer.promax.presentation.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.musicplayer.promax.R
import com.musicplayer.promax.domain.model.AppLanguage
import com.musicplayer.promax.domain.model.AppAccent
import com.musicplayer.promax.domain.model.AppTheme
import com.musicplayer.promax.presentation.components.PremiumScreenHeader
import com.musicplayer.promax.ui.theme.SecondaryText
import com.musicplayer.promax.ui.theme.SubtleBorder

@Composable
fun SettingsScreen(
    selectedLanguage: AppLanguage,
    selectedTheme: AppTheme,
    selectedAccent: AppAccent,
    sleepTimerEndMillis: Long?,
    onLanguageSelected: (AppLanguage) -> Unit,
    onThemeSelected: (AppTheme) -> Unit,
    onAccentSelected: (AppAccent) -> Unit,
    onSetSleepTimer: (Int?) -> Unit,
    internalMusicEnabled: Boolean,
    externalMusicEnabled: Boolean,
    externalTreeUri: String?,
    onInternalMusicEnabledChanged: (Boolean) -> Unit,
    onExternalMusicEnabledChanged: (Boolean) -> Unit,
    onChooseExternalFolder: () -> Unit,
    onBack: (() -> Unit)? = null
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 680.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp)
        ) {
            if (onBack != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("settings_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.personalize),
                            color = MaterialTheme.colorScheme.secondary,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = stringResource(R.string.settings),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Text(
                    text = stringResource(R.string.settings_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = SecondaryText
                )
            } else {
                PremiumScreenHeader(
                    eyebrow = stringResource(R.string.personalize),
                    title = stringResource(R.string.settings),
                    subtitle = stringResource(R.string.settings_description)
                )
            }

        SettingsGroup(title = stringResource(R.string.language)) {
            SettingChoice(
                title = stringResource(R.string.english),
                subtitle = "English",
                selected = selectedLanguage == AppLanguage.ENGLISH,
                onClick = { onLanguageSelected(AppLanguage.ENGLISH) }
            )
            SettingChoice(
                title = stringResource(R.string.arabic),
                subtitle = "العربية",
                selected = selectedLanguage == AppLanguage.ARABIC,
                onClick = { onLanguageSelected(AppLanguage.ARABIC) }
            )
            SettingChoice(
                title = stringResource(R.string.spanish),
                subtitle = "Español",
                selected = selectedLanguage == AppLanguage.SPANISH,
                onClick = { onLanguageSelected(AppLanguage.SPANISH) }
            )
            SettingChoice(
                title = stringResource(R.string.system_default),
                subtitle = stringResource(R.string.system_language_description),
                selected = selectedLanguage == AppLanguage.SYSTEM,
                onClick = { onLanguageSelected(AppLanguage.SYSTEM) }
            )
        }

        SettingsGroup(title = stringResource(R.string.music_sources)) {
            SettingSwitch(
                title = stringResource(R.string.internal_storage),
                subtitle = stringResource(R.string.internal_storage_description),
                checked = internalMusicEnabled,
                onCheckedChange = onInternalMusicEnabledChanged
            )
            SettingSwitch(
                title = stringResource(R.string.external_storage),
                subtitle = stringResource(R.string.external_storage_description),
                checked = externalMusicEnabled,
                onCheckedChange = onExternalMusicEnabledChanged
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onChooseExternalFolder)
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.choose_sd_folder), style = MaterialTheme.typography.bodyLarge)
                    Text(
                        text = if (externalTreeUri == null) stringResource(R.string.sd_folder_not_selected)
                        else stringResource(R.string.sd_folder_selected),
                        style = MaterialTheme.typography.bodySmall,
                        color = SecondaryText
                    )
                }
                Text(stringResource(R.string.choose), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
            }
        }

        SettingsGroup(title = stringResource(R.string.appearance)) {
            SettingChoice(
                title = stringResource(R.string.classic_theme),
                subtitle = stringResource(R.string.classic_theme_description),
                selected = selectedTheme == AppTheme.CLASSIC,
                onClick = { onThemeSelected(AppTheme.CLASSIC) }
            )
            SettingChoice(
                title = stringResource(R.string.cinematic_theme),
                subtitle = stringResource(R.string.cinematic_theme_description),
                selected = selectedTheme == AppTheme.CINEMATIC,
                onClick = { onThemeSelected(AppTheme.CINEMATIC) }
            )
            SettingChoice(stringResource(R.string.accent_blue), stringResource(R.string.accent_blue_description), selectedAccent == AppAccent.ELECTRIC_BLUE) { onAccentSelected(AppAccent.ELECTRIC_BLUE) }
            SettingChoice(stringResource(R.string.accent_violet), stringResource(R.string.accent_violet_description), selectedAccent == AppAccent.VIOLET) { onAccentSelected(AppAccent.VIOLET) }
            SettingChoice(stringResource(R.string.accent_emerald), stringResource(R.string.accent_emerald_description), selectedAccent == AppAccent.EMERALD) { onAccentSelected(AppAccent.EMERALD) }
        }

        SettingsGroup(title = stringResource(R.string.sleep_timer)) {
            var nowMillis by androidx.compose.runtime.remember { androidx.compose.runtime.mutableLongStateOf(System.currentTimeMillis()) }
            androidx.compose.runtime.LaunchedEffect(sleepTimerEndMillis) {
                nowMillis = System.currentTimeMillis()
                while (sleepTimerEndMillis != null && nowMillis < sleepTimerEndMillis) {
                    kotlinx.coroutines.delay(1_000L)
                    nowMillis = System.currentTimeMillis()
                }
            }
            val activeEndMillis = sleepTimerEndMillis?.takeIf { it > nowMillis }
            val remaining = ((activeEndMillis ?: 0L) - nowMillis).coerceAtLeast(0L)
            Text(
                text = if (activeEndMillis == null) stringResource(R.string.sleep_timer_off)
                else stringResource(R.string.sleep_timer_remaining, remaining / 60_000L, (remaining % 60_000L) / 1_000L),
                style = MaterialTheme.typography.bodyMedium,
                color = SecondaryText
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(15, 30, 45, 60).forEach { minutes ->
                    androidx.compose.material3.OutlinedButton(modifier = Modifier.weight(1f), onClick = { onSetSleepTimer(minutes) }) {
                        Text("${minutes}m")
                    }
                }
            }
            androidx.compose.material3.TextButton(
                onClick = { onSetSleepTimer(null) },
                enabled = activeEndMillis != null
            ) {
                Text(
                    stringResource(R.string.cancel_sleep_timer),
                    color = if (activeEndMillis != null) MaterialTheme.colorScheme.primary else SecondaryText
                )
            }
        }
        }
    }
}

@Composable
private fun SettingsGroup(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(11.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, SubtleBorder)
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp)) {
                content()
            }
        }
    }
}

@Composable
private fun SettingChoice(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
            )
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = SecondaryText)
        }
        RadioButton(
            selected = selected,
            onClick = onClick,
            colors = androidx.compose.material3.RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
        )
    }
}


@Composable
private fun SettingSwitch(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = if (checked) FontWeight.SemiBold else FontWeight.Normal)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = SecondaryText)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
