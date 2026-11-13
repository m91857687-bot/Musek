package com.musicplayer.promax.presentation.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
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

private const val HOME_PREVIEW_SONG_LIMIT = 15

@Composable
fun HomeScreen(
    allTracks: List<MusicTrackEntity> = emptyList(),
    totalTracksCount: Int = allTracks.size,
    favoritesCount: Int = 0,
    playlistsCount: Int = 0,
    favoriteKeys: Set<String> = emptySet(),
    activeTrackKey: String? = null,
    recentTracks: List<MusicTrackEntity> = emptyList(),
    favoriteTracks: List<MusicTrackEntity> = emptyList(),
    onPlayTrack: (List<MusicTrackEntity>, MusicTrackEntity) -> Unit = { _, _ -> },
    onToggleFavorite: (String) -> Unit = {},
    onShufflePlay: () -> Unit = {},
    onScanMusic: () -> Unit = {},
    onOpenLibrary: () -> Unit,
    onOpenSearch: () -> Unit = onOpenLibrary,
    onOpenFavorites: () -> Unit = onOpenLibrary,
    onOpenPlaylists: () -> Unit = onOpenLibrary,
    onOpenInsights: () -> Unit = {},
    onOpenSettings: () -> Unit
) {
    val homePreviewTracks = allTracks.take(HOME_PREVIEW_SONG_LIMIT)

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 680.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item(contentType = "header") {
                HomeTopBar(
                    showSearchAction = allTracks.isNotEmpty(),
                    onOpenSearch = onOpenSearch,
                    onOpenInsights = onOpenInsights,
                    onOpenSettings = onOpenSettings
                )
            }

            if (allTracks.isEmpty()) {
                item {
                    EmptyHomePrompt(
                        onScanMusic = onScanMusic,
                        onOpenLibrary = onOpenLibrary,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                }
            } else {
                item {
                    QuickActionChipsBar(
                        totalTracksCount = totalTracksCount,
                        favoritesCount = favoritesCount,
                        playlistsCount = playlistsCount,
                        onShufflePlay = onShufflePlay,
                        onOpenLibrary = onOpenLibrary,
                        onOpenFavorites = onOpenFavorites,
                        onOpenPlaylists = onOpenPlaylists
                    )
                }

                if (recentTracks.isNotEmpty()) {
                    item {
                        HomeSectionHeader(
                            title = stringResource(R.string.recently_played),
                            actionLabel = null,
                            onAction = null
                        )
                    }
                    item {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(recentTracks, key = { "recent_${it.trackKey}" }) { track ->
                                RecentCoverCard(
                                    track = track,
                                    isCurrentTrack = track.trackKey == activeTrackKey,
                                    onClick = { onPlayTrack(recentTracks, track) }
                                )
                            }
                        }
                    }
                }

                if (favoriteTracks.isNotEmpty()) {
                    item {
                        HomeSectionHeader(
                            title = stringResource(R.string.favorites),
                            actionLabel = stringResource(R.string.see_all),
                            onAction = onOpenFavorites
                        )
                    }
                    item {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(favoriteTracks, key = { "fav_${it.trackKey}" }) { track ->
                                FavoriteShelfTile(
                                    track = track,
                                    isCurrentTrack = track.trackKey == activeTrackKey,
                                    onClick = { onPlayTrack(favoriteTracks, track) }
                                )
                            }
                        }
                    }
                }

                item {
                    HomeSectionHeader(
                        title = stringResource(R.string.songs),
                        actionLabel = stringResource(R.string.see_all),
                        onAction = onOpenLibrary
                    )
                }

                itemsIndexed(
                    items = homePreviewTracks,
                    key = { _, it -> "home_${it.trackKey}" },
                    contentType = { _, _ -> "track_row" }
                ) { index, track ->
                    Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                        EditorialTrackRow(
                            track = track,
                            isCurrentTrack = track.trackKey == activeTrackKey,
                            isFavorite = track.trackKey in favoriteKeys,
                            onClick = { onPlayTrack(allTracks, track) },
                            onToggleFavorite = { onToggleFavorite(track.trackKey) },
                            showDivider = index < homePreviewTracks.lastIndex
                        )
                    }
                }

                if (allTracks.size > HOME_PREVIEW_SONG_LIMIT) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            OutlinedButton(
                                onClick = onOpenLibrary,
                                shape = RoundedCornerShape(50),
                                border = BorderStroke(1.dp, SubtleBorder)
                            ) {
                                Text(
                                    text = "${stringResource(R.string.see_all)} (${allTracks.size})",
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeTopBar(
    showSearchAction: Boolean,
    onOpenSearch: () -> Unit,
    onOpenInsights: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = stringResource(R.string.app_brand_name),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = stringResource(R.string.welcome_back),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            if (showSearchAction) {
                IconButton(
                    onClick = onOpenSearch,
                    modifier = Modifier.testTag("home_search_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = stringResource(R.string.search_music),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            IconButton(
                onClick = onOpenInsights,
                modifier = Modifier.testTag("home_insights_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.BarChart,
                    contentDescription = stringResource(R.string.statistics),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            IconButton(
                onClick = onOpenSettings,
                modifier = Modifier.testTag("home_settings_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = stringResource(R.string.settings),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun QuickActionChipsBar(
    totalTracksCount: Int,
    favoritesCount: Int,
    playlistsCount: Int,
    onShufflePlay: () -> Unit,
    onOpenLibrary: () -> Unit,
    onOpenFavorites: () -> Unit,
    onOpenPlaylists: () -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        item {
            Surface(
                onClick = onShufflePlay,
                modifier = Modifier.testTag("home_shuffle_button"),
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.primary
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Shuffle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = stringResource(R.string.shuffle_play),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
        item {
            HomeFilterChip(
                icon = Icons.Filled.LibraryMusic,
                label = stringResource(R.string.track_count_short, totalTracksCount),
                onClick = onOpenLibrary,
                modifier = Modifier.testTag("home_hub_songs")
            )
        }
        item {
            HomeFilterChip(
                icon = Icons.Filled.Favorite,
                label = "${stringResource(R.string.favorites)} · $favoritesCount",
                onClick = onOpenFavorites,
                modifier = Modifier.testTag("home_hub_favorites")
            )
        }
        item {
            HomeFilterChip(
                icon = Icons.AutoMirrored.Filled.QueueMusic,
                label = stringResource(R.string.playlist_count_short, playlistsCount),
                onClick = onOpenPlaylists,
                modifier = Modifier.testTag("home_hub_playlists")
            )
        }
    }
}

@Composable
private fun HomeFilterChip(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, SubtleBorder)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(15.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun HomeSectionHeader(
    title: String,
    actionLabel: String?,
    onAction: (() -> Unit)?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        if (actionLabel != null && onAction != null) {
            TextButton(onClick = onAction) {
                Text(
                    text = actionLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}

@Composable
private fun RecentCoverCard(
    track: MusicTrackEntity,
    isCurrentTrack: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(136.dp)
            .clickable(onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(modifier = Modifier.size(136.dp)) {
            TrackArtworkBox(
                artworkPath = track.artworkFilePath,
                size = 136.dp,
                cornerRadius = 14.dp,
                isCurrentTrack = isCurrentTrack
            )
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.background.copy(alpha = 0.78f),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
                    .size(32.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = stringResource(R.string.play),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = track.displayTitle,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (isCurrentTrack) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = track.displayArtist.ifBlank { stringResource(R.string.unknown_artist) },
                style = MaterialTheme.typography.bodySmall,
                color = SecondaryText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun FavoriteShelfTile(
    track: MusicTrackEntity,
    isCurrentTrack: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, SubtleBorder),
        modifier = Modifier.width(214.dp)
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            TrackArtworkBox(
                artworkPath = track.artworkFilePath,
                size = 44.dp,
                cornerRadius = 10.dp,
                isCurrentTrack = isCurrentTrack
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = track.displayTitle,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isCurrentTrack) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
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
        }
    }
}

@Composable
private fun EmptyHomePrompt(
    onScanMusic: () -> Unit,
    onOpenLibrary: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, SubtleBorder)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.LibraryMusic,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(30.dp)
                )
            }
            Text(
                text = stringResource(R.string.library_empty_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(R.string.ready_to_scan),
                style = MaterialTheme.typography.bodyMedium,
                color = SecondaryText
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onScanMusic,
                    modifier = Modifier.testTag("home_scan_button"),
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(stringResource(R.string.scan_music))
                }
                OutlinedButton(
                    onClick = onOpenLibrary,
                    modifier = Modifier.testTag("home_open_library_button"),
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(1.dp, SubtleBorder)
                ) {
                    Text(stringResource(R.string.library))
                }
            }
        }
    }
}
