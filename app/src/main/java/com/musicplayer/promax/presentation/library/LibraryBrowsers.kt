package com.musicplayer.promax.presentation.library

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.musicplayer.promax.R
import com.musicplayer.promax.data.local.MusicTrackEntity
import com.musicplayer.promax.presentation.components.EditorialTrackRow
import com.musicplayer.promax.presentation.components.TrackArtworkBox
import com.musicplayer.promax.ui.theme.SecondaryText
import com.musicplayer.promax.ui.theme.SubtleBorder
import kotlinx.coroutines.launch

enum class LibraryBrowserTab {
    SONGS,
    ALBUMS,
    ARTISTS,
    FOLDERS
}

@Composable
fun MultiSelectActionBar(
    selectedCount: Int,
    onSelectAll: () -> Unit,
    onAddFavorites: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onCancel: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth().testTag("multi_select_bar"),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onCancel) {
                Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.cancel))
            }
            Text(
                text = stringResource(R.string.multi_select_count, selectedCount),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onSelectAll, modifier = Modifier.testTag("multi_select_all_button")) {
                Icon(Icons.Filled.SelectAll, contentDescription = stringResource(R.string.select_all))
            }
            IconButton(
                onClick = onAddFavorites,
                enabled = selectedCount > 0,
                modifier = Modifier.testTag("multi_select_favorites_button")
            ) {
                Icon(
                    Icons.Filled.Favorite,
                    contentDescription = stringResource(R.string.favorites),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            IconButton(
                onClick = onAddToPlaylist,
                enabled = selectedCount > 0,
                modifier = Modifier.testTag("multi_select_playlist_button")
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.PlaylistAdd,
                    contentDescription = stringResource(R.string.add_to_playlist),
                    tint = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}

@Composable
fun AlbumsGridView(
    albums: List<AlbumGroup>,
    onAlbumClick: (AlbumGroup) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 152.dp),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(albums, key = { it.name }) { album ->
            Surface(
                onClick = { onAlbumClick(album) },
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, SubtleBorder)
            ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        TrackArtworkBox(
                            artworkPath = album.artworkPath,
                            size = 132.dp,
                            cornerRadius = 14.dp
                        )
                    }
                    Text(
                        text = album.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = buildString {
                            if (album.artist.isNotBlank()) {
                                append(album.artist)
                                append(" • ")
                            }
                            append(stringResource(R.string.playlist_track_count, album.tracks.size))
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = SecondaryText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun ArtistsListView(
    artists: List<ArtistGroup>,
    onArtistClick: (ArtistGroup) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(artists, key = { it.name }) { artist ->
            Surface(
                onClick = { onArtistClick(artist) },
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, SubtleBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (artist.artworkPath != null) {
                        TrackArtworkBox(artworkPath = artist.artworkPath, size = 48.dp, cornerRadius = 24.dp)
                    } else {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = artist.name,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = stringResource(R.string.playlist_track_count, artist.tracks.size),
                            style = MaterialTheme.typography.labelSmall,
                            color = SecondaryText
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FoldersListView(
    folders: List<FolderGroup>,
    onFolderClick: (FolderGroup) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(folders, key = { it.folderPath }) { folder ->
            Surface(
                onClick = { onFolderClick(folder) },
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, SubtleBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                    }
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = folder.displayName.ifBlank { stringResource(R.string.folder_root) },
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${folder.folderPath} • ${stringResource(R.string.playlist_track_count, folder.tracks.size)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = SecondaryText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GroupTracksDetailView(
    title: String,
    subtitle: String,
    tracks: List<MusicTrackEntity>,
    activeTrackKey: String?,
    repeatingTrackKey: String?,
    favoriteKeys: Set<String>,
    onBack: () -> Unit,
    onPlayTrack: (List<MusicTrackEntity>, MusicTrackEntity) -> Unit,
    onPlayNext: (MusicTrackEntity) -> Unit,
    onToggleRepeatTrack: (List<MusicTrackEntity>, MusicTrackEntity) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onAddToPlaylist: (MusicTrackEntity) -> Unit,
    onChangeCover: (MusicTrackEntity) -> Unit,
    onResetCover: (MusicTrackEntity) -> Unit
) {
    BackHandler(onBack = onBack)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack, modifier = Modifier.testTag("group_detail_back")) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
            }
            Column(Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelMedium,
                    color = SecondaryText
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

        LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            items(tracks, key = { "group_${it.trackKey}" }) { track ->
                EditorialTrackRow(
                    track = track,
                    isCurrentTrack = track.trackKey == activeTrackKey,
                    isRepeatingTrack = track.trackKey == repeatingTrackKey,
                    isFavorite = track.trackKey in favoriteKeys,
                    onClick = { onPlayTrack(tracks, track) },
                    onPlayNext = { onPlayNext(track) },
                    onToggleRepeatTrack = { onToggleRepeatTrack(tracks, track) },
                    onToggleFavorite = { onToggleFavorite(track.trackKey) },
                    onAddToPlaylist = { onAddToPlaylist(track) },
                    onChangeCover = { onChangeCover(track) },
                    onResetCover = { onResetCover(track) }
                )
            }
        }
    }
}

@Composable
fun LibraryFastScroller(
    tracks: List<MusicTrackEntity>,
    listState: LazyListState,
    headerOffset: Int,
    modifier: Modifier = Modifier
) {
    if (tracks.size < 18) return
    val scope = rememberCoroutineScope()
    var isDragging by remember { mutableStateOf(false) }
    var activeLetter by remember { mutableStateOf("") }
    var dragFraction by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = modifier
            .fillMaxHeight()
            .width(34.dp)
            .padding(vertical = 84.dp, horizontal = 4.dp)
            .pointerInput(tracks.size) {
                detectVerticalDragGestures(
                    onDragStart = { offset ->
                        isDragging = true
                        dragFraction = (offset.y / size.height.toFloat()).coerceIn(0f, 1f)
                    },
                    onDragEnd = { isDragging = false },
                    onDragCancel = { isDragging = false },
                    onVerticalDrag = { change, _ ->
                        change.consume()
                        val fraction = (change.position.y / size.height.toFloat()).coerceIn(0f, 1f)
                        dragFraction = fraction
                        val idx = (fraction * (tracks.size - 1)).toInt().coerceIn(0, tracks.size - 1)
                        val titleChar = tracks[idx].displayTitle.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "#"
                        activeLetter = titleChar
                        scope.launch {
                            listState.scrollToItem(headerOffset + idx)
                        }
                    }
                )
            },
        contentAlignment = Alignment.CenterEnd
    ) {
        if (isDragging && activeLetter.isNotEmpty()) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(top = (dragFraction * 220f).dp)
                    .size(32.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = activeLetter,
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        Box(
            modifier = Modifier
                .width(5.dp)
                .height(64.dp)
                .clip(RoundedCornerShape(50))
                .background(
                    if (isDragging) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                )
        )
    }
}
