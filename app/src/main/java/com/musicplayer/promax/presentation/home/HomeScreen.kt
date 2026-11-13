package com.musicplayer.promax.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.History
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.musicplayer.promax.R
import com.musicplayer.promax.presentation.components.FeatureEmptyState
import com.musicplayer.promax.ui.theme.ElevatedBlack
import com.musicplayer.promax.ui.theme.SecondaryText
import com.musicplayer.promax.ui.theme.SubtleBorder

import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.ui.text.style.TextOverflow
import com.musicplayer.promax.data.local.MusicTrackEntity

@Composable
fun HomeScreen(
    recentTracks: List<MusicTrackEntity> = emptyList(),
    favoriteTracks: List<MusicTrackEntity> = emptyList(),
    onPlayTrack: (List<MusicTrackEntity>, MusicTrackEntity) -> Unit = { _, _ -> },
    onOpenLibrary: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(25.dp)
    ) {
        HomeHeader(onOpenSettings = onOpenSettings)
        ListeningHero(onOpenLibrary = onOpenLibrary)

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            HomeSectionHeading(
                title = stringResource(R.string.recently_played),
                icon = Icons.Filled.History
            )
            if (recentTracks.isEmpty()) {
                FeatureEmptyState(
                    icon = Icons.Filled.GraphicEq,
                    title = stringResource(R.string.no_recent_title),
                    description = stringResource(R.string.no_recent_music)
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    recentTracks.forEach { track ->
                        HomeTrackItem(
                            track = track,
                            onClick = { onPlayTrack(recentTracks, track) }
                        )
                    }
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            HomeSectionHeading(
                title = stringResource(R.string.favorites),
                icon = Icons.Filled.FavoriteBorder
            )
            if (favoriteTracks.isEmpty()) {
                FeatureEmptyState(
                    icon = Icons.Filled.FavoriteBorder,
                    title = stringResource(R.string.no_favorites_title),
                    description = stringResource(R.string.no_favorites)
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    favoriteTracks.forEach { track ->
                        HomeTrackItem(
                            track = track,
                            onClick = { onPlayTrack(favoriteTracks, track) }
                        )
                    }
                }
            }
        }

        LibraryShortcuts(onOpenLibrary = onOpenLibrary)
        Spacer(modifier = Modifier.height(4.dp))
    }
}

@Composable
private fun HomeTrackItem(track: MusicTrackEntity, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, SubtleBorder)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.13f)) {
                Icon(Icons.Filled.MusicNote, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(8.dp))
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(track.displayTitle, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(track.displayArtist.ifBlank { stringResource(R.string.unknown_artist) }, style = MaterialTheme.typography.bodySmall, color = SecondaryText, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Icon(Icons.Filled.PlayArrow, contentDescription = stringResource(R.string.play), tint = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun HomeHeader(onOpenSettings: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(11.dp)) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary))),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.GraphicEq, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
            }
            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(
                    text = stringResource(R.string.app_brand_name),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(R.string.premium_audio),
                    style = MaterialTheme.typography.labelSmall,
                    color = SecondaryText,
                    letterSpacing = 1.1.sp
                )
            }
        }
        IconButton(onClick = onOpenSettings) {
            Icon(
                imageVector = Icons.Filled.Settings,
                contentDescription = stringResource(R.string.settings),
                tint = SecondaryText
            )
        }
    }
}

@Composable
private fun ListeningHero(onOpenLibrary: () -> Unit) {
    Surface(
        onClick = onOpenLibrary,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = ElevatedBlack,
        border = BorderStroke(1.dp, SubtleBorder)
    ) {
        Box(
            modifier = Modifier
                .background(
                    Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.surfaceVariant,
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
                .padding(21.dp)
        ) {
            VinylArtwork(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .size(148.dp)
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.66f)
                    .align(Alignment.CenterStart),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = stringResource(R.string.your_sound),
                    color = MaterialTheme.colorScheme.secondary,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp
                )
                Text(
                    text = stringResource(R.string.listening_starts_here),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 29.sp
                )
                Text(
                    text = stringResource(R.string.browse_collection_hint),
                    color = SecondaryText,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun VinylArtwork(modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(MaterialTheme.colorScheme.primary.copy(alpha = 0.20f), MaterialTheme.colorScheme.primary.copy(alpha = 0.02f), ElevatedBlack)
                    )
                )
        )
        Box(
            modifier = Modifier
                .size(132.dp)
                .clip(CircleShape)
                .background(Brush.sweepGradient(listOf(ElevatedBlack, MaterialTheme.colorScheme.background, ElevatedBlack)))
                .border(1.dp, SubtleBorder, CircleShape)
        )
        Box(
            modifier = Modifier
                .size(92.dp)
                .clip(CircleShape)
                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f), CircleShape)
        )
        Box(
            modifier = Modifier
                .size(53.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary))),
            contentAlignment = Alignment.Center
        ) {
            Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(MaterialTheme.colorScheme.background))
        }
    }
}

@Composable
private fun HomeSectionHeading(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun LibraryShortcuts(onOpenLibrary: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = stringResource(R.string.explore_library),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ShortcutTile(
                modifier = Modifier.weight(1f),
                icon = Icons.Filled.LibraryMusic,
                label = stringResource(R.string.songs),
                onClick = onOpenLibrary
            )
            ShortcutTile(
                modifier = Modifier.weight(1f),
                icon = Icons.Filled.Album,
                label = stringResource(R.string.albums),
                onClick = onOpenLibrary
            )
            ShortcutTile(
                modifier = Modifier.weight(1f),
                icon = Icons.Filled.Person,
                label = stringResource(R.string.artists),
                onClick = onOpenLibrary
            )
        }
    }
}

@Composable
private fun ShortcutTile(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
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
            modifier = Modifier.padding(vertical = 15.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(22.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Medium)
        }
    }
}
