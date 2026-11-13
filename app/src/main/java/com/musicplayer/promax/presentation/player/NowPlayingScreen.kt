package com.musicplayer.promax.presentation.player

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import com.musicplayer.promax.R
import com.musicplayer.promax.data.local.MusicLibraryDatabase
import com.musicplayer.promax.data.local.MusicTrackEntity
import com.musicplayer.promax.data.metadata.LrcParser
import com.musicplayer.promax.data.metadata.TrackMetadataRepository
import com.musicplayer.promax.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.flowOf
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun NowPlayingScreen(controller: MediaController, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val database = remember(context) { MusicLibraryDatabase.getInstance(context) }
    val metadataRepository = remember(context) { TrackMetadataRepository(context) }
    var playerState by remember(controller) { mutableStateOf(readNowPlayingState(controller)) }
    val trackFlow = remember(playerState.mediaId) {
        playerState.mediaId.takeIf(String::isNotBlank)?.let(database.musicTrackDao()::observeTrack) ?: flowOf<com.musicplayer.promax.data.local.MusicTrackEntity?>(null)
    }
    val currentTrack = trackFlow.collectAsStateWithLifecycle(initialValue = null).value
    LaunchedEffect(currentTrack?.trackKey, currentTrack?.artworkFilePath) {
        val track = currentTrack ?: return@LaunchedEffect
        if (track.artworkFilePath.isNullOrBlank() || !File(track.artworkFilePath).isFile) metadataRepository.extractArtwork(track.copy(artworkFilePath = null))
    }
    val lyricLines = remember(currentTrack?.lyricsLrc) { currentTrack?.lyricsLrc?.let(LrcParser::parse).orEmpty() }
    var positionMs by remember(controller) { mutableLongStateOf(controller.currentPosition.coerceAtLeast(0L)) }
    var isSeeking by remember { mutableStateOf(false) }
    val seekingState by rememberUpdatedState(isSeeking)

    DisposableEffect(controller) {
        val listener = object : Player.Listener {
            override fun onEvents(player: Player, events: Player.Events) {
                playerState = readNowPlayingState(player)
                if (!seekingState) positionMs = player.currentPosition.coerceAtLeast(0L)
            }
        }
        controller.addListener(listener)
        onDispose { controller.removeListener(listener) }
    }
    LaunchedEffect(controller, playerState.isPlaying, isSeeking) {
        while (playerState.isPlaying && !isSeeking) {
            positionMs = controller.currentPosition.coerceAtLeast(0L)
            delay(500L)
        }
    }

    val currentLyricIndex = LrcParser.currentIndex(lyricLines, positionMs)
    val duration = playerState.durationMs.coerceAtLeast(0L)
    val progress = if (duration > 0L) (positionMs.toFloat() / duration.toFloat()).coerceIn(0f, 1f) else 0f

    val artworkPath = currentTrack?.artworkFilePath?.takeIf { File(it).isFile }
    val artworkBitmap by produceState<Bitmap?>(initialValue = null, artworkPath) {
        value = if (artworkPath == null) null
        else withContext(Dispatchers.IO) { runCatching { BitmapFactory.decodeFile(artworkPath) }.getOrNull() }
    }

    Box(modifier.fillMaxSize().background(MidnightBlack)) {
        artworkBitmap?.let { image ->
            ArtworkBackdrop(bitmap = image)
        }
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    listOf(MidnightBlack.copy(alpha = 0.68f), MaterialTheme.colorScheme.background.copy(alpha = 0.91f), MidnightBlack.copy(alpha = 0.96f))
                )
            )
        )
        Column(Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back), tint = MaterialTheme.colorScheme.onBackground)
                }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(stringResource(R.string.now_playing), color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.labelMedium, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
                    Text((currentTrack?.displayAlbum ?: playerState.album).ifBlank { stringResource(R.string.app_brand_name) }, color = SecondaryText, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Spacer(Modifier.size(48.dp))
            }
            Spacer(Modifier.weight(0.55f))
            artworkBitmap?.let { image ->
                ArtworkCover(bitmap = image, modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp))
            } ?: VinylArtworkLarge(Modifier.fillMaxWidth().padding(horizontal = 8.dp))
            Spacer(Modifier.weight(0.6f))
            Text((currentTrack?.displayTitle ?: playerState.title).ifBlank { stringResource(R.string.no_track_selected) }, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(7.dp))
            Text((currentTrack?.displayArtist ?: playerState.artist).ifBlank { stringResource(R.string.unknown_artist) }, style = MaterialTheme.typography.bodyLarge, color = SecondaryText, textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (lyricLines.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = lyricLines.getOrNull(currentLyricIndex)?.text ?: stringResource(R.string.lyrics_waiting),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.secondary,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                lyricLines.getOrNull(currentLyricIndex + 1)?.let { nextLine ->
                    Text(nextLine.text, style = MaterialTheme.typography.bodySmall, color = SecondaryText, textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            Spacer(Modifier.height(22.dp))
            Slider(
                value = progress,
                onValueChange = { value -> isSeeking = true; if (duration > 0L) positionMs = (value * duration).toLong() },
                onValueChangeFinished = { if (duration > 0L) controller.seekTo(positionMs); isSeeking = false },
                enabled = duration > 0L,
                colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.secondary, activeTrackColor = MaterialTheme.colorScheme.primary, inactiveTrackColor = SubtleBorder)
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(formatTime(positionMs), color = SecondaryText, style = MaterialTheme.typography.labelSmall)
                Text(formatTime(duration), color = SecondaryText, style = MaterialTheme.typography.labelSmall)
            }
            Spacer(Modifier.height(18.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { controller.shuffleModeEnabled = !controller.shuffleModeEnabled }) {
                    Icon(Icons.Filled.Shuffle, contentDescription = stringResource(R.string.shuffle), tint = if (playerState.shuffleEnabled) MaterialTheme.colorScheme.secondary else SecondaryText)
                }
                IconButton(onClick = controller::seekToPreviousMediaItem, modifier = Modifier.size(52.dp)) {
                    Icon(Icons.Filled.SkipPrevious, contentDescription = stringResource(R.string.previous_track), tint = MaterialTheme.colorScheme.onBackground, modifier = Modifier.size(32.dp))
                }
                Surface(onClick = { if (controller.isPlaying) controller.pause() else controller.play() }, shape = CircleShape, color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(76.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Crossfade(playerState.isPlaying, label = "now_playing_toggle") { playing ->
                            Icon(if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow, contentDescription = stringResource(if (playing) R.string.pause else R.string.play), tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(38.dp))
                        }
                    }
                }
                IconButton(onClick = controller::seekToNextMediaItem, modifier = Modifier.size(52.dp)) {
                    Icon(Icons.Filled.SkipNext, contentDescription = stringResource(R.string.next_track), tint = MaterialTheme.colorScheme.onBackground, modifier = Modifier.size(32.dp))
                }
                IconButton(onClick = {
                    controller.repeatMode = when (controller.repeatMode) {
                        Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                        Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                        else -> Player.REPEAT_MODE_OFF
                    }
                }) {
                    val icon = if (playerState.repeatMode == Player.REPEAT_MODE_ONE) Icons.Filled.RepeatOne else Icons.Filled.Repeat
                    Icon(icon, contentDescription = stringResource(R.string.repeat), tint = if (playerState.repeatMode == Player.REPEAT_MODE_OFF) SecondaryText else MaterialTheme.colorScheme.secondary)
                }
            }
            Spacer(Modifier.weight(0.6f))
        }
    }
}

@Composable
private fun VinylArtworkLarge(modifier: Modifier = Modifier) {
    val rotation by rememberInfiniteTransition(label = "vinyl_rotation").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(animation = tween(durationMillis = 24_000, easing = LinearEasing)),
        label = "vinyl_rotation_angle"
    )
    Box(modifier, contentAlignment = Alignment.Center) {
        Box(
            Modifier.fillMaxWidth(0.9f).height(250.dp).clip(RoundedCornerShape(30.dp))
                .background(Brush.radialGradient(listOf(MaterialTheme.colorScheme.primary.copy(alpha = 0.22f), MidnightBlack, MidnightBlack))),
            contentAlignment = Alignment.Center
        ) {
            Box(
                Modifier.size(226.dp).graphicsLayer { rotationZ = rotation }.clip(CircleShape)
                    .background(Brush.sweepGradient(listOf(MidnightBlack, SubtleBorder, MidnightBlack, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), MidnightBlack)))
                    .border(1.dp, SubtleBorder, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                repeat(5) { index ->
                    Box(Modifier.size((190 - index * 25).dp).clip(CircleShape).border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.12f + index * 0.035f), CircleShape))
                }
                Box(Modifier.size(92.dp).clip(CircleShape).background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary))), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.GraphicEq, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(44.dp))
                }
            }
        }
    }
}

@Composable
private fun ArtworkBackdrop(bitmap: Bitmap) {
    Image(
        bitmap = bitmap.asImageBitmap(),
        contentDescription = null,
        modifier = Modifier.fillMaxSize().blur(30.dp).graphicsLayer { alpha = 0.32f },
        contentScale = ContentScale.Crop
    )
}

@Composable
private fun ArtworkCover(bitmap: Bitmap, modifier: Modifier = Modifier) {
    Image(
        bitmap = bitmap.asImageBitmap(),
        contentDescription = stringResource(R.string.album_artwork),
        modifier = modifier.height(250.dp).clip(RoundedCornerShape(30.dp)),
        contentScale = ContentScale.Crop
    )
}

private data class NowPlayingState(val mediaId: String, val title: String, val artist: String, val album: String, val isPlaying: Boolean, val shuffleEnabled: Boolean, val repeatMode: Int, val durationMs: Long)

private fun readNowPlayingState(player: Player): NowPlayingState {
    val metadata = player.currentMediaItem?.mediaMetadata
    val duration = player.duration.takeUnless { it == C.TIME_UNSET } ?: 0L
    return NowPlayingState(player.currentMediaItem?.mediaId.orEmpty(), metadata?.title?.toString().orEmpty(), metadata?.artist?.toString().orEmpty(), metadata?.albumTitle?.toString().orEmpty(), player.isPlaying, player.shuffleModeEnabled, player.repeatMode, duration)
}

private fun formatTime(milliseconds: Long): String {
    val seconds = milliseconds.coerceAtLeast(0L) / 1000L
    return "%d:%02d".format(seconds / 60L, seconds % 60L)
}
