package com.musicplayer.promax.presentation.components

import android.graphics.Bitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.musicplayer.promax.R
import com.musicplayer.promax.data.local.MusicTrackEntity
import com.musicplayer.promax.data.local.PlaylistWithCount
import com.musicplayer.promax.data.metadata.ArtworkThumbnailCache
import com.musicplayer.promax.ui.theme.SecondaryText
import com.musicplayer.promax.ui.theme.SubtleBorder
import java.io.File

@Composable
fun PremiumScreenHeader(
    eyebrow: String? = null,
    title: String,
    subtitle: String? = null,
    trailingAction: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            if (eyebrow != null) {
                Text(
                    text = eyebrow,
                    color = MaterialTheme.colorScheme.secondary,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = SecondaryText
                )
            }
        }
        if (trailingAction != null) {
            trailingAction()
        }
    }
}

@Composable
fun FeatureEmptyState(
    icon: ImageVector,
    title: String,
    description: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, SubtleBorder)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 22.dp, vertical = 26.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(11.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(62.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(MaterialTheme.colorScheme.primary.copy(alpha = 0.22f), MaterialTheme.colorScheme.secondary.copy(alpha = 0.08f))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(28.dp)
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = SecondaryText
            )
            Spacer(modifier = Modifier.size(1.dp))
        }
    }
}

@Composable
fun TrackArtworkBox(
    artworkPath: String?,
    size: Dp,
    cornerRadius: Dp = 10.dp,
    isCurrentTrack: Boolean = false,
    modifier: Modifier = Modifier
) {
    val validPath = artworkPath?.takeIf { it.isNotEmpty() && File(it).isFile }
    val targetPx = with(LocalDensity.current) { size.roundToPx().coerceAtLeast(64) }
    val bitmap by produceState<Bitmap?>(initialValue = null, validPath, targetPx) {
        value = ArtworkThumbnailCache.load(validPath, targetPx)
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(cornerRadius))
            .background(
                Brush.linearGradient(
                    listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = if (isCurrentTrack) 0.28f else 0.16f),
                        MaterialTheme.colorScheme.surfaceVariant
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap!!.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            if (isCurrentTrack) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background.copy(alpha = 0.45f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.GraphicEq,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(size * 0.42f)
                    )
                }
            }
        } else {
            Icon(
                imageVector = if (isCurrentTrack) Icons.Filled.GraphicEq else Icons.Filled.MusicNote,
                contentDescription = null,
                tint = if (isCurrentTrack) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(size * 0.44f)
            )
        }
    }
}

@Composable
fun EditorialTrackRow(
    track: MusicTrackEntity,
    isCurrentTrack: Boolean = false,
    isRepeatingTrack: Boolean = false,
    isHighlighted: Boolean = false,
    isFavorite: Boolean? = null,
    onClick: () -> Unit,
    onToggleRepeatTrack: (() -> Unit)? = null,
    onToggleFavorite: (() -> Unit)? = null,
    onAddToPlaylist: (() -> Unit)? = null,
    onEditMetadata: (() -> Unit)? = null,
    onChangeCover: (() -> Unit)? = null,
    onResetCover: (() -> Unit)? = null,
    showDivider: Boolean = true
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val hasMenu = onEditMetadata != null || onChangeCover != null || (onResetCover != null && !track.artworkFilePath.isNullOrEmpty())
    val hasQuickActions = onToggleRepeatTrack != null || onAddToPlaylist != null || (onToggleFavorite != null && isFavorite != null)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = if (isHighlighted) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f) else Color.Transparent,
        border = if (isHighlighted) BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)) else null
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onClick)
                    .padding(horizontal = if (isHighlighted) 8.dp else 0.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TrackArtworkBox(
                        artworkPath = track.artworkFilePath,
                        size = 54.dp,
                        cornerRadius = 12.dp,
                        isCurrentTrack = isCurrentTrack
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            text = track.displayTitle,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (isCurrentTrack) FontWeight.Bold else FontWeight.SemiBold,
                            color = if (isCurrentTrack) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                        val subtitle = buildList {
                            add(track.displayArtist.ifBlank { stringResource(R.string.unknown_artist) })
                            if (track.displayAlbum.isNotBlank()) add(track.displayAlbum)
                            if (track.durationMs > 0L) add(formatEditorialDuration(track.durationMs))
                        }.joinToString(" • ")
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = SecondaryText,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (hasMenu) {
                        Box {
                            IconButton(
                                onClick = { menuExpanded = true },
                                modifier = Modifier.testTag("track_more_${track.trackKey}")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.MoreVert,
                                    contentDescription = stringResource(R.string.more_options),
                                    tint = SecondaryText
                                )
                            }
                            DropdownMenu(
                                expanded = menuExpanded,
                                onDismissRequest = { menuExpanded = false }
                            ) {
                                if (onEditMetadata != null) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.edit_track_metadata)) },
                                        leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null) },
                                        onClick = {
                                            menuExpanded = false
                                            onEditMetadata()
                                        }
                                    )
                                }
                                if (onChangeCover != null) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.change_cover_art)) },
                                        leadingIcon = { Icon(Icons.Filled.PhotoLibrary, contentDescription = null) },
                                        onClick = {
                                            menuExpanded = false
                                            onChangeCover()
                                        }
                                    )
                                }
                                if (onResetCover != null && !track.artworkFilePath.isNullOrEmpty()) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.reset_cover_art)) },
                                        leadingIcon = { Icon(Icons.Filled.RestartAlt, contentDescription = null) },
                                        onClick = {
                                            menuExpanded = false
                                            onResetCover()
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
                if (hasQuickActions) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 60.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (onToggleRepeatTrack != null) {
                            IconButton(
                                onClick = onToggleRepeatTrack,
                                modifier = Modifier.testTag("track_repeat_${track.trackKey}")
                            ) {
                                Icon(
                                    imageVector = if (isRepeatingTrack) Icons.Filled.RepeatOne else Icons.Filled.Repeat,
                                    contentDescription = stringResource(R.string.repeat_this_song),
                                    tint = if (isRepeatingTrack) MaterialTheme.colorScheme.primary else SecondaryText
                                )
                            }
                        }
                        if (onAddToPlaylist != null) {
                            IconButton(
                                onClick = onAddToPlaylist,
                                modifier = Modifier.testTag("track_add_playlist_${track.trackKey}")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.PlaylistAdd,
                                    contentDescription = stringResource(R.string.add_to_playlist),
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                        if (onToggleFavorite != null && isFavorite != null) {
                            IconButton(onClick = onToggleFavorite) {
                                Icon(
                                    imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                    contentDescription = stringResource(R.string.favorites),
                                    tint = if (isFavorite) MaterialTheme.colorScheme.primary else SecondaryText
                                )
                            }
                        }
                    }
                }
            }
            if (showDivider) {
                HorizontalDivider(
                    modifier = Modifier.padding(start = 66.dp),
                    thickness = 0.5.dp,
                    color = SubtleBorder.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@Composable
fun PlaylistCheckboxDialog(
    track: MusicTrackEntity,
    playlists: List<PlaylistWithCount>,
    includedPlaylistIds: Set<Long>,
    onTogglePlaylist: (Long, Boolean) -> Unit,
    onCreatePlaylist: (String, (Boolean) -> Unit) -> Unit,
    onDismiss: () -> Unit
) {
    var showInlineCreate by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }
    var duplicateError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_to_playlist)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = track.displayTitle,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.secondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (playlists.isEmpty() && !showInlineCreate) {
                    Text(
                        text = stringResource(R.string.create_playlist_first),
                        style = MaterialTheme.typography.bodyMedium,
                        color = SecondaryText
                    )
                } else if (playlists.isNotEmpty()) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 260.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        items(playlists, key = { it.playlist.playlistId }) { item ->
                            val isChecked = item.playlist.playlistId in includedPlaylistIds
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onTogglePlaylist(item.playlist.playlistId, !isChecked)
                                    }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = { checked ->
                                        onTogglePlaylist(item.playlist.playlistId, checked)
                                    }
                                )
                                Text(
                                    text = item.playlist.name,
                                    modifier = Modifier.weight(1f),
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = item.trackCount.toString(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SecondaryText
                                )
                            }
                        }
                    }
                }
                if (showInlineCreate) {
                    OutlinedTextField(
                        value = newPlaylistName,
                        onValueChange = {
                            newPlaylistName = it.take(50)
                            duplicateError = false
                        },
                        singleLine = true,
                        label = { Text(stringResource(R.string.playlist_name)) },
                        isError = duplicateError,
                        supportingText = if (duplicateError) {
                            { Text(stringResource(R.string.playlist_name_duplicate)) }
                        } else null,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(
                            onClick = { showInlineCreate = false },
                            enabled = true
                        ) {
                            Text(stringResource(R.string.cancel))
                        }
                        TextButton(
                            onClick = {
                                val clean = newPlaylistName.trim()
                                if (clean.isNotEmpty()) {
                                    onCreatePlaylist(clean) { created ->
                                        if (created) {
                                            newPlaylistName = ""
                                            showInlineCreate = false
                                        } else {
                                            duplicateError = true
                                        }
                                    }
                                }
                            },
                            enabled = newPlaylistName.isNotBlank()
                        ) {
                            Text(stringResource(R.string.create))
                        }
                    }
                } else {
                    TextButton(
                        onClick = { showInlineCreate = true },
                        modifier = Modifier.testTag("playlist_dialog_create_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.size(6.dp))
                        Text(stringResource(R.string.create_playlist))
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

private fun formatEditorialDuration(durationMs: Long): String {
    val totalSeconds = (durationMs / 1000L).coerceAtLeast(0L)
    return "%d:%02d".format(totalSeconds / 60L, totalSeconds % 60L)
}
