package com.musicplayer.promax.presentation.playlists

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.musicplayer.promax.R
import com.musicplayer.promax.data.local.MusicTrackEntity
import com.musicplayer.promax.presentation.components.FeatureEmptyState
import com.musicplayer.promax.presentation.components.PremiumScreenHeader
import com.musicplayer.promax.presentation.library.LibraryUiState
import com.musicplayer.promax.presentation.library.MusicLibraryViewModel
import com.musicplayer.promax.ui.theme.SecondaryText
import com.musicplayer.promax.ui.theme.SubtleBorder

@Composable
fun PlaylistsScreen(
    state: LibraryUiState,
    viewModel: MusicLibraryViewModel,
    onPlayTrack: (List<MusicTrackEntity>, MusicTrackEntity) -> Unit
) {
    var selectedPlaylistId by rememberSaveable { mutableStateOf<Long?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    val selectedPlaylist = state.playlists.firstOrNull { it.playlist.playlistId == selectedPlaylistId }
    val selectedId = selectedPlaylistId
    val playlistTracksFlow = remember(selectedId) {
        if (selectedId == null) kotlinx.coroutines.flow.flowOf(emptyList()) else viewModel.playlistTracks(selectedId)
    }
    val playlistTracks by playlistTracksFlow.collectAsStateWithLifecycle(initialValue = emptyList())

    LaunchedEffect(selectedPlaylistId, state.playlists) {
        if (selectedPlaylistId != null && state.playlists.none { it.playlist.playlistId == selectedPlaylistId }) {
            selectedPlaylistId = null
        }
    }

    BackHandler(enabled = selectedPlaylist != null) { selectedPlaylistId = null }

    if (selectedPlaylist != null) {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 22.dp, vertical = 20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { selectedPlaylistId = null }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                }
                Column(Modifier.weight(1f)) {
                    Text(selectedPlaylist.playlist.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(stringResource(R.string.playlist_track_count, selectedPlaylist.trackCount), color = SecondaryText, style = MaterialTheme.typography.labelMedium)
                }
                IconButton(onClick = { showRenameDialog = true }) { Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.rename)) }
                IconButton(onClick = { showDeleteDialog = true }) { Icon(Icons.Filled.DeleteOutline, contentDescription = stringResource(R.string.delete)) }
            }
            if (playlistTracks.isEmpty()) {
                FeatureEmptyState(
                    icon = Icons.Filled.QueueMusic,
                    title = stringResource(R.string.playlist_empty_title),
                    description = stringResource(R.string.playlist_empty_body)
                )
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(playlistTracks, key = { it.trackKey }) { track ->
                        PlaylistTrackRow(
                            track = track,
                            onPlay = { onPlayTrack(playlistTracks, track) },
                            onRemove = { viewModel.removeTrackFromPlaylist(selectedPlaylist.playlist.playlistId, track.trackKey) }
                        )
                    }
                }
            }
        }
    } else {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 22.dp, vertical = 24.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            PremiumScreenHeader(
                eyebrow = stringResource(R.string.your_music_your_way),
                title = stringResource(R.string.playlists),
                subtitle = stringResource(R.string.playlists_screen_subtitle)
            )
            Button(onClick = { showCreateDialog = true }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary), modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.create_playlist))
            }
            if (state.playlists.isEmpty()) {
                FeatureEmptyState(
                    icon = Icons.Filled.QueueMusic,
                    title = stringResource(R.string.playlists_empty_title),
                    description = stringResource(R.string.playlists_empty_body)
                )
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(state.playlists, key = { it.playlist.playlistId }) { item ->
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable { selectedPlaylistId = item.playlist.playlistId },
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, SubtleBorder)
                        ) {
                            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.13f)) {
                                    Icon(Icons.Filled.QueueMusic, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(12.dp))
                                }
                                Column(Modifier.weight(1f).padding(start = 12.dp)) {
                                    Text(item.playlist.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 1)
                                    Text(stringResource(R.string.playlist_track_count, item.trackCount), color = SecondaryText, style = MaterialTheme.typography.labelSmall)
                                }
                                Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        PlaylistNameDialog(
            title = stringResource(R.string.create_playlist),
            initialName = "",
            confirmLabel = stringResource(R.string.create),
            onDismiss = { showCreateDialog = false },
            onConfirm = { name, result -> viewModel.createPlaylist(name, result) }
        )
    }
    if (showRenameDialog && selectedPlaylist != null) {
        PlaylistNameDialog(
            title = stringResource(R.string.rename_playlist),
            initialName = selectedPlaylist.playlist.name,
            confirmLabel = stringResource(R.string.save),
            onDismiss = { showRenameDialog = false },
            onConfirm = { name, result -> viewModel.renamePlaylist(selectedPlaylist.playlist.playlistId, name, result) }
        )
    }
    if (showDeleteDialog && selectedPlaylist != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.delete_playlist)) },
            text = { Text(stringResource(R.string.delete_playlist_confirmation, selectedPlaylist.playlist.name)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deletePlaylist(selectedPlaylist.playlist.playlistId)
                    selectedPlaylistId = null
                    showDeleteDialog = false
                }) { Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text(stringResource(R.string.cancel)) } }
        )
    }
}

@Composable
private fun PlaylistNameDialog(
    title: String,
    initialName: String,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (String, (Boolean) -> Unit) -> Unit
) {
    var name by remember(initialName) { mutableStateOf(initialName) }
    var nameError by remember(initialName) { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it.take(50); nameError = false },
                singleLine = true,
                label = { Text(stringResource(R.string.playlist_name)) },
                isError = nameError,
                supportingText = if (nameError) ({ Text(stringResource(R.string.playlist_name_duplicate)) }) else null
            )
        },
        confirmButton = {
            TextButton(onClick = {
                if (name.isNotBlank()) onConfirm(name.trim()) { success ->
                    if (success) onDismiss() else nameError = true
                }
            }, enabled = name.isNotBlank()) { Text(confirmLabel) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } }
    )
}

@Composable
private fun PlaylistTrackRow(track: MusicTrackEntity, onPlay: () -> Unit, onRemove: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onPlay),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, SubtleBorder)
    ) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.MusicNote, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Text(track.displayTitle, fontWeight = FontWeight.SemiBold, maxLines = 1)
                Text(track.displayArtist, color = SecondaryText, style = MaterialTheme.typography.labelSmall, maxLines = 1)
            }
            IconButton(onClick = onRemove) { Icon(Icons.Filled.DeleteOutline, contentDescription = stringResource(R.string.remove_from_playlist), tint = SecondaryText) }
        }
    }
}
