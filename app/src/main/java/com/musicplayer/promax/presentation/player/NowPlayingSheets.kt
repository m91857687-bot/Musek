package com.musicplayer.promax.presentation.player

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.musicplayer.promax.R
import com.musicplayer.promax.data.metadata.TimedLyric
import com.musicplayer.promax.playback.QueueItem
import com.musicplayer.promax.presentation.components.TrackArtworkBox
import com.musicplayer.promax.ui.theme.MidnightBlack
import com.musicplayer.promax.ui.theme.SecondaryText
import com.musicplayer.promax.ui.theme.SubtleBorder
import kotlin.math.abs
import kotlin.math.sin

@Composable
fun WaveformSeekbar(
    trackKeySeed: String,
    progress: Float,
    enabled: Boolean,
    primaryColor: Color,
    secondaryColor: Color,
    onSeekFraction: (Float) -> Unit,
    onSeekFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val barHeights = remember(trackKeySeed) {
        val seed = trackKeySeed.hashCode()
        List(44) { idx ->
            val wave = abs(sin((idx * 0.42 + (seed % 37) * 0.13)))
            val mod = (((idx * 31 + seed) and 0xF) / 15.0)
            (0.24f + 0.56f * wave.toFloat() + 0.20f * mod.toFloat()).coerceIn(0.18f, 1.0f)
        }
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                detectTapGestures { offset ->
                    val fraction = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                    onSeekFraction(fraction)
                    onSeekFinished()
                }
            }
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                detectDragGestures(
                    onDragEnd = { onSeekFinished() },
                    onDragCancel = { onSeekFinished() },
                    onDrag = { change, _ ->
                        change.consume()
                        val fraction = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                        onSeekFraction(fraction)
                    }
                )
            }
            .testTag("waveform_seekbar")
    ) {
        val count = barHeights.size
        val step = size.width / count.toFloat()
        val barWidth = (step * 0.54f).coerceAtLeast(3f)
        val centerY = size.height / 2f
        val activeCutoff = progress.coerceIn(0f, 1f) * count

        barHeights.forEachIndexed { index, amp ->
            val barHeight = size.height * amp
            val left = index * step + (step - barWidth) / 2f
            val top = centerY - barHeight / 2f
            val active = index <= activeCutoff
            val barColor = if (active) {
                if (index >= activeCutoff - 1.5f) secondaryColor else primaryColor
            } else {
                SubtleBorder.copy(alpha = 0.72f)
            }
            drawRoundRect(
                color = barColor,
                topLeft = Offset(left, top),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueBottomSheet(
    queueItems: List<QueueItem>,
    onJumpToIndex: (Int) -> Unit,
    onMoveItem: (Int, Int) -> Unit,
    onRemoveItem: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 520.dp)
                .padding(horizontal = 18.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.AutoMirrored.Filled.QueueMusic, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(
                        text = stringResource(R.string.queue_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = stringResource(R.string.playlist_track_count, queueItems.size),
                    style = MaterialTheme.typography.labelMedium,
                    color = SecondaryText
                )
            }
            if (queueItems.isEmpty()) {
                Text(
                    text = stringResource(R.string.queue_empty),
                    color = SecondaryText,
                    modifier = Modifier.padding(vertical = 24.dp)
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    itemsIndexed(queueItems, key = { idx, item -> "${idx}_${item.mediaId}" }) { index, item ->
                        Surface(
                            onClick = { onJumpToIndex(index) },
                            shape = RoundedCornerShape(14.dp),
                            color = if (item.isCurrent) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f) else MaterialTheme.colorScheme.background.copy(alpha = 0.45f),
                            border = BorderStroke(1.dp, if (item.isCurrent) MaterialTheme.colorScheme.primary.copy(alpha = 0.55f) else SubtleBorder)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                TrackArtworkBox(
                                    artworkPath = item.artworkPath,
                                    size = 42.dp,
                                    cornerRadius = 10.dp,
                                    isCurrentTrack = item.isCurrent
                                )
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        text = item.title.ifBlank { stringResource(R.string.no_track_selected) },
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (item.isCurrent) FontWeight.Bold else FontWeight.SemiBold,
                                        color = if (item.isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = item.artist.ifBlank { stringResource(R.string.unknown_artist) },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = SecondaryText,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                IconButton(
                                    onClick = { onMoveItem(index, index - 1) },
                                    enabled = index > 0
                                ) {
                                    Icon(Icons.Filled.ArrowUpward, contentDescription = stringResource(R.string.move_up), modifier = Modifier.size(18.dp))
                                }
                                IconButton(
                                    onClick = { onMoveItem(index, index + 1) },
                                    enabled = index < queueItems.lastIndex
                                ) {
                                    Icon(Icons.Filled.ArrowDownward, contentDescription = stringResource(R.string.move_down), modifier = Modifier.size(18.dp))
                                }
                                IconButton(onClick = { onRemoveItem(index) }) {
                                    Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.remove_from_queue), modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FullscreenLyricsOverlay(
    title: String,
    artist: String,
    artworkBitmap: Bitmap?,
    lyrics: List<TimedLyric>,
    currentIndex: Int,
    onSeekToLine: (Long) -> Unit,
    onClose: () -> Unit
) {
    BackHandler(onBack = onClose)
    val listState = rememberLazyListState()
    LaunchedEffect(currentIndex) {
        if (currentIndex >= 0 && currentIndex < lyrics.size) {
            listState.animateScrollToItem((currentIndex - 2).coerceAtLeast(0))
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MidnightBlack)
            .testTag("fullscreen_lyrics_overlay")
    ) {
        artworkBitmap?.let { bmp ->
            Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize().blur(36.dp).graphicsLayer { alpha = 0.28f },
                contentScale = ContentScale.Crop
            )
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = artist.ifBlank { stringResource(R.string.unknown_artist) },
                        style = MaterialTheme.typography.labelMedium,
                        color = SecondaryText
                    )
                }
                IconButton(onClick = onClose, modifier = Modifier.testTag("close_lyrics_overlay")) {
                    Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.back))
                }
            }
            Spacer(Modifier.height(16.dp))
            if (lyrics.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = stringResource(R.string.no_lyrics_for_track),
                        style = MaterialTheme.typography.bodyLarge,
                        color = SecondaryText,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                    contentPadding = PaddingValues(vertical = 36.dp)
                ) {
                    itemsIndexed(lyrics, key = { idx, line -> "${idx}_${line.timeMs}" }) { index, line ->
                        val isActive = index == currentIndex
                        Text(
                            text = line.text,
                            style = if (isActive) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleMedium,
                            fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.Medium,
                            color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onSeekToLine(line.timeMs) }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CarModeOverlay(
    title: String,
    artist: String,
    isPlaying: Boolean,
    progress: Float,
    onPrevious: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onNext: () -> Unit,
    onExit: () -> Unit
) {
    BackHandler(onBack = onExit)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MidnightBlack)
            .padding(24.dp)
            .testTag("car_mode_overlay")
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Filled.DirectionsCar, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(
                        text = stringResource(R.string.car_mode),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                TextButton(onClick = onExit, modifier = Modifier.testTag("exit_car_mode_button")) {
                    Text(stringResource(R.string.exit_car_mode))
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = title.ifBlank { stringResource(R.string.no_track_selected) },
                    style = MaterialTheme.typography.headlineMedium,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = artist.ifBlank { stringResource(R.string.unknown_artist) },
                    style = MaterialTheme.typography.titleLarge,
                    color = SecondaryText,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = { progress.coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(50)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = SubtleBorder
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    onClick = onPrevious,
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.5.dp, SubtleBorder),
                    modifier = Modifier.size(84.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Filled.SkipPrevious,
                            contentDescription = stringResource(R.string.previous_track),
                            modifier = Modifier.size(44.dp)
                        )
                    }
                }
                Surface(
                    onClick = onTogglePlayPause,
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(116.dp).testTag("car_mode_play_pause")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = stringResource(if (isPlaying) R.string.pause else R.string.play),
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(58.dp)
                        )
                    }
                }
                Surface(
                    onClick = onNext,
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.5.dp, SubtleBorder),
                    modifier = Modifier.size(84.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Filled.SkipNext,
                            contentDescription = stringResource(R.string.next_track),
                            modifier = Modifier.size(44.dp)
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}
