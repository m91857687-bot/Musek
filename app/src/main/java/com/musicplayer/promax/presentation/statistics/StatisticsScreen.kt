package com.musicplayer.promax.presentation.statistics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.musicplayer.promax.R
import com.musicplayer.promax.data.stats.TopTrackStat
import com.musicplayer.promax.presentation.components.PremiumScreenHeader

@Composable
fun StatisticsScreen(state: StatisticsUiState) {
    LazyColumn(
        modifier = Modifier.padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            PremiumScreenHeader(
                eyebrow = stringResource(R.string.listening_insights),
                title = stringResource(R.string.your_listening),
                subtitle = stringResource(R.string.listening_insights_description)
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard(stringResource(R.string.total_plays), state.totalPlays.toString(), Modifier.weight(1f))
                StatCard(stringResource(R.string.unique_tracks), state.uniqueTracks.toString(), Modifier.weight(1f))
            }
        }
        item {
            val minutes = state.listeningMs / 60_000L
            val duration = if (minutes >= 60) "${minutes / 60}h ${minutes % 60}m" else "${minutes}m"
            StatCard(stringResource(R.string.listening_time), duration, Modifier.fillMaxWidth())
        }
        item {
            Text(stringResource(R.string.most_played), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
        if (state.topTracks.isEmpty()) {
            item { Text(stringResource(R.string.statistics_empty), color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 20.dp)) }
        } else {
            items(state.topTracks, key = { it.trackKey }) { track -> TopTrackRow(track) }
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun TopTrackRow(track: TopTrackStat) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(track.title.ifBlank { stringResource(R.string.unknown_track) }, maxLines = 1, fontWeight = FontWeight.Medium)
            Text(track.artist.ifBlank { stringResource(R.string.unknown_artist) }, maxLines = 1, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text("${track.plays}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
    }
}
