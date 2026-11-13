package com.musicplayer.promax

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import com.musicplayer.promax.data.work.MusicIndexScheduler
import com.musicplayer.promax.domain.model.AppLanguage
import com.musicplayer.promax.navigation.AppDestination
import com.musicplayer.promax.playback.PlaybackController
import com.musicplayer.promax.presentation.equalizer.EqualizerScreen
import com.musicplayer.promax.presentation.home.HomeScreen
import com.musicplayer.promax.presentation.library.LibraryScreen
import com.musicplayer.promax.presentation.library.MusicLibraryViewModel
import com.musicplayer.promax.presentation.playlists.PlaylistsScreen
import com.musicplayer.promax.presentation.player.NowPlayingScreen
import com.musicplayer.promax.presentation.settings.SettingsScreen
import com.musicplayer.promax.presentation.settings.SettingsViewModel
import com.musicplayer.promax.presentation.statistics.StatisticsViewModel
import com.musicplayer.promax.presentation.statistics.StatisticsScreen
import com.musicplayer.promax.ui.theme.MusicPlayerProMaxTheme

@Composable
fun MusicPlayerProMaxApp(
    settingsViewModel: SettingsViewModel,
    musicLibraryViewModel: MusicLibraryViewModel,
    statisticsViewModel: StatisticsViewModel,
    playbackController: MediaController?
) {
    val context = LocalContext.current
    val permissionName = if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO
    else Manifest.permission.READ_EXTERNAL_STORAGE
    val permissionInitiallyGranted = ContextCompat.checkSelfPermission(context, permissionName) == PackageManager.PERMISSION_GRANTED
    val permissionState = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(permissionInitiallyGranted) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        permissionState.value = granted
        if (granted) {
            MusicIndexScheduler.ensurePeriodicUpdates(context)
            musicLibraryViewModel.scan()
        }
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val granted = ContextCompat.checkSelfPermission(context, permissionName) == PackageManager.PERMISSION_GRANTED
                val wasGranted = permissionState.value
                permissionState.value = granted
                if (granted && !wasGranted) {
                    MusicIndexScheduler.ensurePeriodicUpdates(context)
                    musicLibraryViewModel.scan()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val lyricsTrackKey = remember { mutableStateOf<String?>(null) }
    val lyricsPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        val trackKey = lyricsTrackKey.value
        if (uri != null && trackKey != null) {
            musicLibraryViewModel.importLyrics(trackKey, uri) { result ->
                android.widget.Toast.makeText(
                    context,
                    if (result.isSuccess) context.getString(R.string.lyrics_imported) else context.getString(R.string.lyrics_import_failed),
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
        }
        lyricsTrackKey.value = null
    }
    val folderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri: Uri? ->
        if (uri != null) {
            val flags = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
            runCatching { context.contentResolver.takePersistableUriPermission(uri, flags) }
            settingsViewModel.setExternalTreeUri(uri.toString()) {
                MusicIndexScheduler.ensurePeriodicUpdates(context)
                musicLibraryViewModel.scan()
            }
        }
    }
    val libraryState = musicLibraryViewModel.uiState.collectAsStateWithLifecycle().value
    val settingsState = settingsViewModel.uiState.collectAsStateWithLifecycle().value
    val language = settingsState.language
    val theme = settingsState.theme
    val accent = settingsState.accent
    val statisticsState = statisticsViewModel.state.collectAsStateWithLifecycle().value
    val selectedDestinationState = rememberSaveable {
        androidx.compose.runtime.mutableStateOf(AppDestination.HOME)
    }
    val selectedDestination = selectedDestinationState.value
    val showNowPlaying = rememberSaveable { mutableStateOf(false) }

    BackHandler(enabled = showNowPlaying.value) { showNowPlaying.value = false }

    LaunchedEffect(settingsState.isLoaded, language) {
        if (settingsState.isLoaded) {
            val locales = if (language == AppLanguage.SYSTEM) {
                LocaleListCompat.getEmptyLocaleList()
            } else {
                LocaleListCompat.forLanguageTags(language.languageTag)
            }
            if (AppCompatDelegate.getApplicationLocales() != locales) {
                AppCompatDelegate.setApplicationLocales(locales)
            }
        }
    }

    MusicPlayerProMaxTheme(appTheme = theme, appAccent = accent) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                if (!showNowPlaying.value) Column {
                    playbackController?.let { controller ->
                        MiniPlayer(controller = controller, onExpand = { showNowPlaying.value = true })
                    }
                    NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 0.dp
                ) {
                    AppDestination.entries.forEach { destination ->
                        NavigationBarItem(
                            selected = selectedDestination == destination,
                            onClick = { selectedDestinationState.value = destination },
                            icon = {
                                androidx.compose.material3.Icon(
                                    imageVector = destination.icon,
                                    contentDescription = stringResource(destination.labelRes)
                                )
                            },
                            label = {
                                Text(
                                    text = stringResource(destination.labelRes),
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            alwaysShowLabel = true,
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.onSurface,
                                indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.13f),
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                    }
                }
            }
        ) { innerPadding ->
            if (showNowPlaying.value && playbackController != null) {
                NowPlayingScreen(controller = playbackController, onBack = { showNowPlaying.value = false }, modifier = Modifier.padding(innerPadding))
            } else Crossfade(
                targetState = selectedDestination,
                label = "main_navigation",
                modifier = Modifier.padding(innerPadding)
            ) { destination ->
                when (destination) {
                    AppDestination.HOME -> HomeScreen(
                        recentTracks = libraryState.recentTracks,
                        favoriteTracks = libraryState.favoriteTracks,
                        onPlayTrack = { tracks, selected ->
                            playbackController?.let { controller ->
                                PlaybackController.playQueue(controller, tracks, selected)
                                showNowPlaying.value = true
                            }
                        },
                        onOpenLibrary = { selectedDestinationState.value = AppDestination.LIBRARY },
                        onOpenSettings = { selectedDestinationState.value = AppDestination.SETTINGS }
                    )
                    AppDestination.LIBRARY -> LibraryScreen(
                        state = libraryState,
                        hasAudioPermission = permissionState.value ||
                            (!settingsState.internalMusicEnabled && settingsState.externalMusicEnabled && settingsState.externalTreeUri != null),
                        onTrackClick = { track ->
                            playbackController?.let { controller ->
                                PlaybackController.playQueue(controller, libraryState.tracks, track)
                                showNowPlaying.value = true
                            }
                        },
                        onScanClick = {
                            if (permissionState.value ||
                                (!settingsState.internalMusicEnabled && settingsState.externalMusicEnabled && settingsState.externalTreeUri != null)
                            ) musicLibraryViewModel.scan()
                            else permissionLauncher.launch(permissionName)
                        },
                        onSearchChanged = musicLibraryViewModel::setSearchQuery,
                        onSortChanged = musicLibraryViewModel::setSortMode,
                        onFavoritesOnlyChanged = musicLibraryViewModel::setFavoritesOnly,
                        onToggleFavorite = musicLibraryViewModel::toggleFavorite,
                        onAddToPlaylist = musicLibraryViewModel::addTrackToPlaylist,
                        onUpdateMetadata = musicLibraryViewModel::updateTrackMetadata,
                        onImportLyrics = { trackKey ->
                            lyricsTrackKey.value = trackKey
                            lyricsPicker.launch(arrayOf("text/*", "application/octet-stream"))
                        }
                    )
                    AppDestination.PLAYLISTS -> PlaylistsScreen(
                        state = libraryState,
                        viewModel = musicLibraryViewModel,
                        onPlayTrack = { tracks, selected ->
                            playbackController?.let { controller ->
                                PlaybackController.playQueue(controller, tracks, selected)
                                showNowPlaying.value = true
                            }
                        }
                    )
                    AppDestination.EQUALIZER -> EqualizerScreen()
                    AppDestination.STATISTICS -> StatisticsScreen(statisticsState)
                    AppDestination.SETTINGS -> SettingsScreen(
                        selectedLanguage = language,
                        selectedTheme = theme,
                        selectedAccent = accent,
                        sleepTimerEndMillis = settingsState.sleepTimerEndMillis,
                        onLanguageSelected = settingsViewModel::selectLanguage,
                        onThemeSelected = settingsViewModel::selectTheme,
                        onAccentSelected = settingsViewModel::selectAccent,
                        onSetSleepTimer = settingsViewModel::setSleepTimer,
                        internalMusicEnabled = settingsState.internalMusicEnabled,
                        externalMusicEnabled = settingsState.externalMusicEnabled,
                        externalTreeUri = settingsState.externalTreeUri,
                        onInternalMusicEnabledChanged = { enabled ->
                            settingsViewModel.setInternalMusicEnabled(enabled) {
                                musicLibraryViewModel.scan()
                            }
                        },
                        onExternalMusicEnabledChanged = { enabled ->
                            settingsViewModel.setExternalMusicEnabled(enabled) {
                                musicLibraryViewModel.scan()
                            }
                        },
                        onChooseExternalFolder = { folderPicker.launch(null) }
                    )
                }
            }
        }
    }
}


@androidx.compose.runtime.Composable
private fun MiniPlayer(controller: MediaController, onExpand: () -> Unit) {
    val playerState = remember(controller) { mutableStateOf(readPlayerState(controller)) }
    DisposableEffect(controller) {
        val listener = object : Player.Listener {
            override fun onEvents(player: Player, events: Player.Events) {
                playerState.value = readPlayerState(player)
            }
        }
        controller.addListener(listener)
        onDispose { controller.removeListener(listener) }
    }
    val state = playerState.value
    if (state.title.isBlank()) return
    Surface(
        onClick = onExpand,
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(18.dp),
        tonalElevation = 4.dp,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(state.title, maxLines = 1, fontWeight = FontWeight.SemiBold)
                Text(state.artist, maxLines = 1, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = { if (controller.isPlaying) controller.pause() else controller.play() }) {
                Icon(
                    if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = stringResource(R.string.accessibility_play_pause),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            IconButton(onClick = controller::seekToNext) {
                Icon(
                    Icons.Filled.SkipNext,
                    contentDescription = stringResource(R.string.accessibility_next_track),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

private data class MiniPlayerState(val title: String, val artist: String, val isPlaying: Boolean)

private fun readPlayerState(player: Player): MiniPlayerState {
    val metadata = player.currentMediaItem?.mediaMetadata
    return MiniPlayerState(
        title = metadata?.title?.toString().orEmpty(),
        artist = metadata?.artist?.toString().orEmpty(),
        isPlaying = player.isPlaying
    )
}
