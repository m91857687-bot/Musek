package com.musicplayer.promax.presentation.library

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.musicplayer.promax.R
import com.musicplayer.promax.data.local.MusicTrackEntity
import com.musicplayer.promax.data.local.PlaylistWithCount
import com.musicplayer.promax.presentation.components.EditorialTrackRow
import com.musicplayer.promax.presentation.components.FeatureEmptyState
import com.musicplayer.promax.presentation.components.PremiumScreenHeader
import com.musicplayer.promax.ui.theme.SecondaryText
import com.musicplayer.promax.ui.theme.SubtleBorder

@Composable
fun LibraryScreen(
    state: LibraryUiState,
    activeTrackKey: String? = null,
    focusSearchRequest: Boolean = false,
    onSearchFocusConsumed: () -> Unit = {},
    onScanClick: () -> Unit,
    hasAudioPermission: Boolean,
    onTrackClick: (MusicTrackEntity) -> Unit,
    onSearchChanged: (String) -> Unit,
    onSortChanged: (String) -> Unit,
    onFavoritesOnlyChanged: (Boolean) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onAddToPlaylist: (Long, String) -> Unit,
    onUpdateMetadata: (String, String, String, String) -> Unit,
    onImportLyrics: (String) -> Unit
) {
    var sortMenuExpanded by remember { mutableStateOf(false) }
    var trackForPlaylist by remember { mutableStateOf<MusicTrackEntity?>(null) }
    var trackForEdit by remember { mutableStateOf<MusicTrackEntity?>(null) }
    val searchFocusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    LaunchedEffect(focusSearchRequest, state.trackCount) {
        if (focusSearchRequest && state.trackCount > 0) {
            runCatching { searchFocusRequester.requestFocus() }
            onSearchFocusConsumed()
        }
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 22.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            PremiumScreenHeader(
                eyebrow = stringResource(R.string.your_collection),
                title = stringResource(R.string.library),
                subtitle = stringResource(R.string.library_screen_subtitle)
            )
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, SubtleBorder)
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.indexed_tracks), style = MaterialTheme.typography.labelLarge, color = SecondaryText)
                    Text(state.trackCount.toString(), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                    if (state.isScanning) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            Text(stringResource(R.string.indexing_in_background), color = SecondaryText)
                        }
                    } else {
                        Button(onClick = onScanClick, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)) {
                            Text(stringResource(if (state.trackCount == 0) R.string.scan_music else R.string.update_library))
                        }
                        if (!hasAudioPermission) Text(stringResource(R.string.audio_permission_required), color = SecondaryText)
                    }
                    if (state.scanFailed) Text(stringResource(R.string.indexing_failed), color = MaterialTheme.colorScheme.error)
                }
            }
        }
        if (state.trackCount > 0) {
            item {
                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = onSearchChanged,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(searchFocusRequester)
                        .testTag("library_search_input"),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    trailingIcon = {
                        if (state.searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { onSearchChanged("") },
                                modifier = Modifier.testTag("library_search_clear")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = stringResource(R.string.cancel)
                                )
                            }
                        }
                    },
                    placeholder = { Text(stringResource(R.string.search_music)) },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                    shape = RoundedCornerShape(16.dp)
                )
            }
            item {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterPill(text = stringResource(R.string.all_songs), selected = !state.favoritesOnly) { onFavoritesOnlyChanged(false) }
                    FilterPill(text = stringResource(R.string.favorites), selected = state.favoritesOnly) { onFavoritesOnlyChanged(true) }
                    androidx.compose.foundation.layout.Box {
                        androidx.compose.material3.OutlinedButton(onClick = { sortMenuExpanded = true }) {
                            Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = null)
                            Text(stringResource(R.string.sort))
                        }
                        DropdownMenu(expanded = sortMenuExpanded, onDismissRequest = { sortMenuExpanded = false }) {
                            SortOption(R.string.sort_title, MusicLibraryViewModel.SORT_TITLE, state.sortMode, { onSortChanged(it); sortMenuExpanded = false })
                            SortOption(R.string.sort_artist, MusicLibraryViewModel.SORT_ARTIST, state.sortMode, { onSortChanged(it); sortMenuExpanded = false })
                            SortOption(R.string.sort_album, MusicLibraryViewModel.SORT_ALBUM, state.sortMode, { onSortChanged(it); sortMenuExpanded = false })
                            SortOption(R.string.sort_duration, MusicLibraryViewModel.SORT_DURATION, state.sortMode, { onSortChanged(it); sortMenuExpanded = false })
                            SortOption(R.string.sort_newest, MusicLibraryViewModel.SORT_NEWEST, state.sortMode, { onSortChanged(it); sortMenuExpanded = false })
                        }
                    }
                }
            }
        }
        if (state.tracks.isEmpty()) {
            item {
                FeatureEmptyState(
                    icon = if (state.favoritesOnly) Icons.Filled.FavoriteBorder else Icons.Filled.LibraryMusic,
                    title = stringResource(
                        when {
                            state.trackCount == 0 -> R.string.library_empty_title
                            state.searchQuery.isNotBlank() -> R.string.no_search_results
                            state.favoritesOnly -> R.string.no_favorite_results
                            else -> R.string.no_search_results
                        }
                    ),
                    description = stringResource(
                        when {
                            state.trackCount == 0 -> R.string.library_empty_body
                            state.searchQuery.isNotBlank() -> R.string.no_search_results_body
                            state.favoritesOnly -> R.string.no_favorite_results_body
                            else -> R.string.no_search_results_body
                        }
                    )
                )
            }
        } else {
            item {
                Text(
                    text = stringResource(if (state.favoritesOnly) R.string.favorites else R.string.songs),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            items(state.tracks, key = { it.trackKey }) { track ->
                EditorialTrackRow(
                    track = track,
                    isCurrentTrack = track.trackKey == activeTrackKey,
                    isFavorite = track.trackKey in state.favoriteKeys,
                    onClick = { onTrackClick(track) },
                    onToggleFavorite = { onToggleFavorite(track.trackKey) },
                    onAddToPlaylist = { trackForPlaylist = track },
                    onEditMetadata = { trackForEdit = track }
                )
            }
        }
    }

    trackForEdit?.let { track ->
        TrackMetadataDialog(
            track = track,
            onDismiss = { trackForEdit = null },
            onSave = { title, artist, album ->
                onUpdateMetadata(track.trackKey, title, artist, album)
                trackForEdit = null
            },
            onImportLyrics = { onImportLyrics(track.trackKey) }
        )
    }
    trackForPlaylist?.let { track ->
        AddToPlaylistDialog(
            track = track,
            playlists = state.playlists,
            onDismiss = { trackForPlaylist = null },
            onSelect = { playlistId -> onAddToPlaylist(playlistId, track.trackKey); trackForPlaylist = null }
        )
    }
}

@Composable
private fun FilterPill(text: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.primary else SubtleBorder)
    ) {
        Text(text, modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp), color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun SortOption(label: Int, value: String, selected: String, onClick: (String) -> Unit) {
    DropdownMenuItem(text = { Text(stringResource(label)) }, onClick = { onClick(value) }, trailingIcon = {
        if (selected == value) Text("✓", color = MaterialTheme.colorScheme.primary)
    })
}

@Composable
private fun TrackMetadataDialog(
    track: MusicTrackEntity,
    onDismiss: () -> Unit,
    onSave: (String, String, String) -> Unit,
    onImportLyrics: () -> Unit
) {
    var title by remember(track.trackKey) { mutableStateOf(track.displayTitle) }
    var artist by remember(track.trackKey) { mutableStateOf(track.displayArtist) }
    var album by remember(track.trackKey) { mutableStateOf(track.displayAlbum) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.edit_track_metadata)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(title, { title = it }, label = { Text(stringResource(R.string.track_title)) }, singleLine = true)
                OutlinedTextField(artist, { artist = it }, label = { Text(stringResource(R.string.track_artist)) }, singleLine = true)
                OutlinedTextField(album, { album = it }, label = { Text(stringResource(R.string.track_album)) }, singleLine = true)
                Text(stringResource(R.string.metadata_override_note), style = MaterialTheme.typography.bodySmall, color = SecondaryText)
                TextButton(onClick = onImportLyrics) { Text(stringResource(R.string.import_lrc_lyrics)) }
            }
        },
        confirmButton = { TextButton(onClick = { onSave(title, artist, album) }) { Text(stringResource(R.string.save)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } }
    )
}

@Composable
private fun AddToPlaylistDialog(
    track: MusicTrackEntity,
    playlists: List<PlaylistWithCount>,
    onDismiss: () -> Unit,
    onSelect: (Long) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_to_playlist)) },
        text = {
            if (playlists.isEmpty()) Text(stringResource(R.string.create_playlist_first))
            else Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(track.displayTitle, color = SecondaryText, style = MaterialTheme.typography.labelMedium)
                playlists.forEach { item ->
                    TextButton(onClick = { onSelect(item.playlist.playlistId) }, modifier = Modifier.fillMaxWidth()) {
                        Text(item.playlist.name, modifier = Modifier.weight(1f))
                        Text(item.trackCount.toString(), color = SecondaryText)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) } }
    )
}

private fun formatDuration(durationMs: Long): String {
    val totalSeconds = durationMs / 1000
    return "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}
