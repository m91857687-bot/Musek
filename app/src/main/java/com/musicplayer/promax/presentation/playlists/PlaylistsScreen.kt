package com.musicplayer.promax.presentation.playlists

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.musicplayer.promax.R
import com.musicplayer.promax.data.local.MusicTrackEntity
import com.musicplayer.promax.presentation.components.EditorialTrackRow
import com.musicplayer.promax.presentation.components.FeatureEmptyState
import com.musicplayer.promax.presentation.components.PremiumScreenHeader
import com.musicplayer.promax.presentation.components.TrackArtworkBox
import com.musicplayer.promax.presentation.library.LibraryUiState
import com.musicplayer.promax.presentation.library.MusicLibraryViewModel
import com.musicplayer.promax.ui.theme.SecondaryText
import com.musicplayer.promax.ui.theme.SubtleBorder

enum class SmartPlaylistType {
    MOST_PLAYED,
    RECENTLY_PLAYED,
    RECENTLY_ADDED,
    FAVORITES
}

@Composable
fun PlaylistsScreen(
    state: LibraryUiState,
    viewModel: MusicLibraryViewModel,
    activeTrackKey: String? = null,
    repeatingTrackKey: String? = null,
    listState: LazyListState = rememberLazyListState(),
    detailListState: LazyListState = rememberLazyListState(),
    onPlayTrack: (List<MusicTrackEntity>, MusicTrackEntity) -> Unit,
    onToggleRepeatTrack: (List<MusicTrackEntity>, MusicTrackEntity) -> Unit = { _, _ -> },
    onAddToPlaylistRequest: (MusicTrackEntity) -> Unit = {},
    onToggleFavorite: (String) -> Unit = {},
    onChangeCover: (MusicTrackEntity) -> Unit = {},
    onResetCover: (MusicTrackEntity) -> Unit = {},
    onOpenSettings: (() -> Unit)? = null
) {
    var selectedPlaylistId by rememberSaveable { mutableStateOf<Long?>(null) }
    var selectedSmartTypeName by rememberSaveable { mutableStateOf<String?>(null) }
    val selectedSmartType = selectedSmartTypeName?.let { runCatching { SmartPlaylistType.valueOf(it) }.getOrNull() }
    var showCreateDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showAddSongsDialog by remember { mutableStateOf(false) }
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

    BackHandler(enabled = selectedSmartType != null || selectedPlaylist != null) {
        if (selectedSmartType != null) selectedSmartTypeName = null
        else selectedPlaylistId = null
    }

    val smartTracksForType: (SmartPlaylistType) -> List<MusicTrackEntity> = { type ->
        when (type) {
            SmartPlaylistType.MOST_PLAYED -> state.smartMostPlayedTracks
            SmartPlaylistType.RECENTLY_PLAYED -> state.smartRecentTracks
            SmartPlaylistType.RECENTLY_ADDED -> state.homeTracks.take(30)
            SmartPlaylistType.FAVORITES -> state.favoriteTracks
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        when {
            selectedSmartType != null -> {
                val tracks = smartTracksForType(selectedSmartType)
                val title = when (selectedSmartType) {
                    SmartPlaylistType.MOST_PLAYED -> stringResource(R.string.most_played)
                    SmartPlaylistType.RECENTLY_PLAYED -> stringResource(R.string.recently_played)
                    SmartPlaylistType.RECENTLY_ADDED -> stringResource(R.string.smart_recently_added)
                    SmartPlaylistType.FAVORITES -> stringResource(R.string.favorites)
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 680.dp)
                        .fillMaxSize()
                        .padding(horizontal = 20.dp, vertical = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { selectedSmartTypeName = null }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                        }
                        Column(Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    title,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.16f)
                                ) {
                                    Text(
                                        text = stringResource(R.string.smart_auto_badge),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                            Text(
                                stringResource(R.string.playlist_track_count, tracks.size),
                                color = SecondaryText,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }

                    if (tracks.isNotEmpty()) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = { onPlayTrack(tracks, tracks.first()) },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Icon(Icons.Filled.PlayArrow, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text(stringResource(R.string.play_all))
                            }
                            OutlinedButton(
                                onClick = {
                                    val shuffled = tracks.shuffled()
                                    onPlayTrack(shuffled, shuffled.first())
                                },
                                modifier = Modifier.weight(1f),
                                border = BorderStroke(1.dp, SubtleBorder)
                            ) {
                                Icon(Icons.Filled.Shuffle, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text(stringResource(R.string.shuffle))
                            }
                        }
                    }

                    if (tracks.isEmpty()) {
                        FeatureEmptyState(
                            icon = Icons.Filled.AutoAwesome,
                            title = stringResource(R.string.playlist_empty_title),
                            description = stringResource(R.string.statistics_empty)
                        )
                    } else {
                        LazyColumn(
                            state = detailListState,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(tracks, key = { "smart_${selectedSmartType.name}_${it.trackKey}" }) { track ->
                                EditorialTrackRow(
                                    track = track,
                                    isCurrentTrack = track.trackKey == activeTrackKey,
                                    isRepeatingTrack = track.trackKey == repeatingTrackKey,
                                    isFavorite = track.trackKey in state.favoriteKeys,
                                    onClick = { onPlayTrack(tracks, track) },
                                    onToggleRepeatTrack = { onToggleRepeatTrack(tracks, track) },
                                    onToggleFavorite = { onToggleFavorite(track.trackKey) },
                                    onAddToPlaylist = { onAddToPlaylistRequest(track) },
                                    onChangeCover = { onChangeCover(track) },
                                    onResetCover = { onResetCover(track) }
                                )
                            }
                        }
                    }
                }
            }

            selectedPlaylist != null -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 680.dp)
                        .fillMaxSize()
                        .padding(horizontal = 20.dp, vertical = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { selectedPlaylistId = null }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                        }
                        Column(Modifier.weight(1f)) {
                            Text(
                                selectedPlaylist.playlist.name,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                stringResource(R.string.playlist_track_count, selectedPlaylist.trackCount),
                                color = SecondaryText,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                        IconButton(
                            onClick = { showAddSongsDialog = true },
                            modifier = Modifier.testTag("playlist_add_songs_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = stringResource(R.string.add_songs),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(onClick = { showRenameDialog = true }) {
                            Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.rename))
                        }
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Filled.DeleteOutline, contentDescription = stringResource(R.string.delete))
                        }
                    }
                    if (playlistTracks.isEmpty()) {
                        FeatureEmptyState(
                            icon = Icons.AutoMirrored.Filled.QueueMusic,
                            title = stringResource(R.string.playlist_empty_title),
                            description = stringResource(R.string.playlist_empty_body)
                        )
                        Button(
                            onClick = { showAddSongsDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("playlist_empty_add_songs_button")
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.add_songs))
                        }
                    } else {
                        LazyColumn(
                            state = detailListState,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(playlistTracks, key = { it.trackKey }) { track ->
                                PlaylistTrackRow(
                                    track = track,
                                    isCurrentTrack = track.trackKey == activeTrackKey,
                                    isRepeatingTrack = track.trackKey == repeatingTrackKey,
                                    onPlay = { onPlayTrack(playlistTracks, track) },
                                    onToggleRepeat = { onToggleRepeatTrack(playlistTracks, track) },
                                    onRemove = { viewModel.removeTrackFromPlaylist(selectedPlaylist.playlist.playlistId, track.trackKey) }
                                )
                            }
                        }
                    }
                }
            }

            else -> {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 680.dp)
                        .fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        PremiumScreenHeader(
                            eyebrow = stringResource(R.string.your_music_your_way),
                            title = stringResource(R.string.playlists),
                            subtitle = stringResource(R.string.playlists_screen_subtitle),
                            trailingAction = onOpenSettings?.let { openSettings ->
                                {
                                    IconButton(
                                        onClick = openSettings,
                                        modifier = Modifier.testTag("playlists_settings_button")
                                    ) {
                                        Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.settings))
                                    }
                                }
                            }
                        )
                    }

                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.AutoAwesome,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = stringResource(R.string.smart_playlists),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                SmartPlaylistCard(
                                    icon = Icons.Filled.TrendingUp,
                                    title = stringResource(R.string.most_played),
                                    count = state.smartMostPlayedTracks.size,
                                    modifier = Modifier.weight(1f).testTag("smart_playlist_most_played"),
                                    onClick = { selectedSmartTypeName = SmartPlaylistType.MOST_PLAYED.name }
                                )
                                SmartPlaylistCard(
                                    icon = Icons.Filled.History,
                                    title = stringResource(R.string.recently_played),
                                    count = state.smartRecentTracks.size,
                                    modifier = Modifier.weight(1f).testTag("smart_playlist_recently_played"),
                                    onClick = { selectedSmartTypeName = SmartPlaylistType.RECENTLY_PLAYED.name }
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                SmartPlaylistCard(
                                    icon = Icons.Filled.NewReleases,
                                    title = stringResource(R.string.smart_recently_added),
                                    count = state.homeTracks.take(30).size,
                                    modifier = Modifier.weight(1f).testTag("smart_playlist_recently_added"),
                                    onClick = { selectedSmartTypeName = SmartPlaylistType.RECENTLY_ADDED.name }
                                )
                                SmartPlaylistCard(
                                    icon = Icons.Filled.Favorite,
                                    title = stringResource(R.string.favorites),
                                    count = state.favoriteTracks.size,
                                    modifier = Modifier.weight(1f).testTag("smart_playlist_favorites"),
                                    onClick = { selectedSmartTypeName = SmartPlaylistType.FAVORITES.name }
                                )
                            }
                        }
                    }

                    item {
                        Button(
                            onClick = { showCreateDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.create_playlist))
                        }
                    }

                    if (state.playlists.isEmpty()) {
                        item {
                            FeatureEmptyState(
                                icon = Icons.AutoMirrored.Filled.QueueMusic,
                                title = stringResource(R.string.playlists_empty_title),
                                description = stringResource(R.string.playlists_empty_body)
                            )
                        }
                    } else {
                        items(state.playlists, key = { it.playlist.playlistId }) { item ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedPlaylistId = item.playlist.playlistId },
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, SubtleBorder)
                            ) {
                                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.13f)
                                    ) {
                                        Icon(
                                            Icons.AutoMirrored.Filled.QueueMusic,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(12.dp)
                                        )
                                    }
                                    Column(Modifier.weight(1f).padding(start = 12.dp)) {
                                        Text(
                                            item.playlist.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            stringResource(R.string.playlist_track_count, item.trackCount),
                                            color = SecondaryText,
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                    Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddSongsDialog && selectedPlaylist != null) {
        val currentTrackKeys = remember(playlistTracks) { playlistTracks.map { it.trackKey }.toSet() }
        AddSongsToPlaylistDialog(
            allTracks = state.homeTracks,
            selectedKeys = currentTrackKeys,
            onToggleTrack = { trackKey, included ->
                viewModel.setTrackInPlaylist(selectedPlaylist.playlist.playlistId, trackKey, included)
            },
            onDismiss = { showAddSongsDialog = false }
        )
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
private fun SmartPlaylistCard(
    icon: ImageVector,
    title: String,
    count: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, SubtleBorder)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(8.dp).size(18.dp)
                    )
                }
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.14f)
                ) {
                    Text(
                        text = stringResource(R.string.smart_auto_badge),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                    )
                }
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = stringResource(R.string.playlist_track_count, count),
                style = MaterialTheme.typography.labelSmall,
                color = SecondaryText
            )
        }
    }
}

@Composable
private fun AddSongsToPlaylistDialog(
    allTracks: List<MusicTrackEntity>,
    selectedKeys: Set<String>,
    onToggleTrack: (String, Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var filterQuery by remember { mutableStateOf("") }
    val filteredTracks = remember(allTracks, filterQuery) {
        val clean = filterQuery.trim()
        if (clean.isEmpty()) allTracks
        else allTracks.filter {
            it.displayTitle.contains(clean, ignoreCase = true) ||
                it.displayArtist.contains(clean, ignoreCase = true) ||
                it.displayAlbum.contains(clean, ignoreCase = true)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_songs)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (allTracks.isEmpty()) {
                    Text(
                        text = stringResource(R.string.library_empty_body),
                        style = MaterialTheme.typography.bodyMedium,
                        color = SecondaryText
                    )
                } else {
                    OutlinedTextField(
                        value = filterQuery,
                        onValueChange = { filterQuery = it },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                        placeholder = { Text(stringResource(R.string.search_music)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 320.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(filteredTracks, key = { it.trackKey }) { track ->
                            val isIncluded = track.trackKey in selectedKeys
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onToggleTrack(track.trackKey, !isIncluded) }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                TrackArtworkBox(
                                    artworkPath = track.artworkFilePath,
                                    size = 38.dp,
                                    cornerRadius = 8.dp
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = track.displayTitle,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = track.displayArtist.ifBlank { stringResource(R.string.unknown_artist) },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = SecondaryText,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Checkbox(
                                    checked = isIncluded,
                                    onCheckedChange = { checked -> onToggleTrack(track.trackKey, checked) }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.done))
            }
        }
    )
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
private fun PlaylistTrackRow(
    track: MusicTrackEntity,
    isCurrentTrack: Boolean,
    isRepeatingTrack: Boolean,
    onPlay: () -> Unit,
    onToggleRepeat: () -> Unit,
    onRemove: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onPlay),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, SubtleBorder)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TrackArtworkBox(
                    artworkPath = track.artworkFilePath,
                    size = 48.dp,
                    cornerRadius = 10.dp,
                    isCurrentTrack = isCurrentTrack
                )
                Column(Modifier.weight(1f)) {
                    Text(
                        track.displayTitle,
                        fontWeight = if (isCurrentTrack) FontWeight.Bold else FontWeight.SemiBold,
                        color = if (isCurrentTrack) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        track.displayArtist.ifBlank { stringResource(R.string.unknown_artist) },
                        color = SecondaryText,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onToggleRepeat) {
                    Icon(
                        imageVector = if (isRepeatingTrack) Icons.Filled.RepeatOne else Icons.Filled.Repeat,
                        contentDescription = stringResource(R.string.repeat_this_song),
                        tint = if (isRepeatingTrack) MaterialTheme.colorScheme.primary else SecondaryText
                    )
                }
                IconButton(onClick = onRemove) {
                    Icon(
                        Icons.Filled.DeleteOutline,
                        contentDescription = stringResource(R.string.remove_from_playlist),
                        tint = SecondaryText
                    )
                }
            }
        }
    }
}
