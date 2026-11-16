package com.musicplayer.promax

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
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
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import com.musicplayer.promax.data.local.MusicTrackEntity
import com.musicplayer.promax.data.metadata.ArtworkColorExtractor
import com.musicplayer.promax.data.metadata.ArtworkPalette
import com.musicplayer.promax.data.work.MusicIndexScheduler
import com.musicplayer.promax.domain.model.AppLanguage
import com.musicplayer.promax.navigation.AppDestination
import com.musicplayer.promax.playback.PlaybackController
import com.musicplayer.promax.presentation.components.PlaylistCheckboxDialog
import com.musicplayer.promax.presentation.components.TrackArtworkBox
import com.musicplayer.promax.presentation.equalizer.EqualizerScreen
import com.musicplayer.promax.presentation.home.HomeScreen
import com.musicplayer.promax.presentation.library.LIBRARY_TRACK_LIST_HEADER_COUNT
import com.musicplayer.promax.presentation.library.LibraryBrowserTab
import com.musicplayer.promax.presentation.library.LibraryScreen
import com.musicplayer.promax.presentation.library.MusicLibraryViewModel
import com.musicplayer.promax.presentation.player.NowPlayingScreen
import com.musicplayer.promax.presentation.playlists.PlaylistsScreen
import com.musicplayer.promax.presentation.settings.SettingsScreen
import com.musicplayer.promax.presentation.settings.SettingsViewModel
import com.musicplayer.promax.presentation.statistics.StatisticsScreen
import com.musicplayer.promax.presentation.statistics.StatisticsViewModel
import com.musicplayer.promax.ui.theme.MidnightBlack
import com.musicplayer.promax.ui.theme.MusicPlayerProMaxTheme
import com.musicplayer.promax.ui.theme.SubtleBorder
import kotlinx.coroutines.delay
import java.io.File
import java.util.Locale
import kotlin.math.abs

@Composable
fun MusicPlayerProMaxApp(
    settingsViewModel: SettingsViewModel,
    musicLibraryViewModel: MusicLibraryViewModel,
    statisticsViewModel: StatisticsViewModel,
    playbackController: MediaController?,
    openNowPlayingSignal: Int = 0
) {
    val context = LocalContext.current
    val permissionName = if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO
    else Manifest.permission.READ_EXTERNAL_STORAGE
    val permissionInitiallyGranted = ContextCompat.checkSelfPermission(context, permissionName) == PackageManager.PERMISSION_GRANTED
    val permissionState = remember { mutableStateOf(permissionInitiallyGranted) }
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
                Toast.makeText(
                    context,
                    if (result.isSuccess) context.getString(R.string.lyrics_imported) else context.getString(R.string.lyrics_import_failed),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        lyricsTrackKey.value = null
    }

    val libraryState = musicLibraryViewModel.uiState.collectAsStateWithLifecycle().value
    val settingsState = settingsViewModel.uiState.collectAsStateWithLifecycle().value
    val language = settingsState.language
    val theme = settingsState.theme
    val accent = settingsState.accent
    val statisticsState = statisticsViewModel.state.collectAsStateWithLifecycle().value

    val coverTargetTrackKey = remember { mutableStateOf<String?>(null) }
    val coverPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        val trackKey = coverTargetTrackKey.value
        if (uri != null && trackKey != null) {
            musicLibraryViewModel.updateTrackArtwork(trackKey, uri) { result ->
                Toast.makeText(
                    context,
                    if (result.isSuccess) context.getString(R.string.cover_updated) else context.getString(R.string.cover_update_failed),
                    Toast.LENGTH_SHORT
                ).show()
                val newPath = result.getOrNull()
                if (newPath != null && playbackController != null) {
                    val base = libraryState.homeTracks.firstOrNull { it.trackKey == trackKey }
                        ?: libraryState.tracks.firstOrNull { it.trackKey == trackKey }
                    if (base != null) {
                        PlaybackController.refreshTrackMetadata(playbackController, base.copy(artworkFilePath = newPath))
                    }
                }
            }
        }
        coverTargetTrackKey.value = null
    }
    val launchCoverPicker: (String) -> Unit = { trackKey ->
        coverTargetTrackKey.value = trackKey
        coverPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
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

    val m3uImportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            val fallbackName = uri.lastPathSegment
                ?.substringAfterLast('/')
                ?.substringBeforeLast('.')
                ?.ifBlank { context.getString(R.string.playlists) }
                ?: context.getString(R.string.playlists)
            musicLibraryViewModel.importM3uPlaylist(fallbackName, uri) { result ->
                val count = result.getOrNull()
                val msg = if (count != null && count > 0) {
                    context.getString(R.string.m3u_imported, count)
                } else {
                    context.getString(R.string.m3u_import_failed)
                }
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            }
        }
    }

    var pendingM3uExport by remember { mutableStateOf<Pair<String, List<MusicTrackEntity>>?>(null) }
    val m3uExportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("audio/x-mpegurl")) { uri: Uri? ->
        val payload = pendingM3uExport
        if (uri != null && payload != null) {
            musicLibraryViewModel.exportM3uPlaylist(payload.first, payload.second, uri) { ok ->
                Toast.makeText(
                    context,
                    if (ok) context.getString(R.string.m3u_exported) else context.getString(R.string.m3u_export_failed),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        pendingM3uExport = null
    }

    val selectedDestinationState = rememberSaveable { mutableStateOf(AppDestination.HOME) }
    val selectedDestination = selectedDestinationState.value
    val showNowPlaying = rememberSaveable { mutableStateOf(false) }
    val showInsights = rememberSaveable { mutableStateOf(false) }
    val showSettings = rememberSaveable { mutableStateOf(false) }
    val focusSearchRequest = remember { mutableStateOf(false) }
    var requestedLibraryTab by remember { mutableStateOf<LibraryBrowserTab?>(null) }
    var requestedLibraryGroup by remember { mutableStateOf<String?>(null) }
    var trackForPlaylistDialog by remember { mutableStateOf<MusicTrackEntity?>(null) }

    val homeListState = rememberLazyListState()
    val libraryListState = rememberLazyListState()
    val playlistsListState = rememberLazyListState()
    val playlistTracksListState = rememberLazyListState()

    var pendingLocateTrackKey by remember { mutableStateOf<String?>(null) }
    var highlightedTrackKey by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(openNowPlayingSignal) {
        if (openNowPlayingSignal > 0) {
            showSettings.value = false
            showInsights.value = false
            showNowPlaying.value = true
        }
    }

    val activeTrackKeyState = remember(playbackController) {
        mutableStateOf(playbackController?.currentMediaItem?.mediaId)
    }
    val repeatModeState = remember(playbackController) {
        mutableIntStateOf(playbackController?.repeatMode ?: Player.REPEAT_MODE_OFF)
    }
    DisposableEffect(playbackController) {
        val controller = playbackController ?: return@DisposableEffect onDispose {}
        activeTrackKeyState.value = controller.currentMediaItem?.mediaId
        repeatModeState.intValue = controller.repeatMode
        val listener = object : Player.Listener {
            override fun onEvents(player: Player, events: Player.Events) {
                activeTrackKeyState.value = player.currentMediaItem?.mediaId
                repeatModeState.intValue = player.repeatMode
            }
        }
        controller.addListener(listener)
        onDispose { controller.removeListener(listener) }
    }

    val repeatingTrackKey = if (repeatModeState.intValue == Player.REPEAT_MODE_ONE) activeTrackKeyState.value else null

    val toggleRepeatForTrack: (List<MusicTrackEntity>, MusicTrackEntity) -> Unit = { queue, track ->
        playbackController?.let { controller ->
            if (controller.currentMediaItem?.mediaId == track.trackKey) {
                controller.repeatMode = if (controller.repeatMode == Player.REPEAT_MODE_ONE) {
                    Player.REPEAT_MODE_OFF
                } else {
                    Player.REPEAT_MODE_ONE
                }
            } else {
                PlaybackController.playQueue(controller, queue, track)
                controller.repeatMode = Player.REPEAT_MODE_ONE
            }
        }
    }

    val enqueuePlayNext: (MusicTrackEntity) -> Unit = { track ->
        playbackController?.let { controller ->
            PlaybackController.playNext(controller, track)
            Toast.makeText(context, context.getString(R.string.added_to_play_next), Toast.LENGTH_SHORT).show()
        }
    }

    val openArtistGroup: (String) -> Unit = { artistName ->
        showNowPlaying.value = false
        showInsights.value = false
        showSettings.value = false
        requestedLibraryTab = LibraryBrowserTab.ARTISTS
        requestedLibraryGroup = artistName
        selectedDestinationState.value = AppDestination.LIBRARY
    }

    val openAlbumGroup: (String) -> Unit = { albumName ->
        showNowPlaying.value = false
        showInsights.value = false
        showSettings.value = false
        requestedLibraryTab = LibraryBrowserTab.ALBUMS
        requestedLibraryGroup = albumName
        selectedDestinationState.value = AppDestination.LIBRARY
    }

    val locateTrackInList: (String) -> Unit = { trackKey ->
        showNowPlaying.value = false
        showInsights.value = false
        showSettings.value = false
        requestedLibraryTab = LibraryBrowserTab.SONGS
        requestedLibraryGroup = null
        selectedDestinationState.value = AppDestination.LIBRARY
        if (libraryState.tracks.none { it.trackKey == trackKey }) {
            if (libraryState.searchQuery.isNotEmpty()) musicLibraryViewModel.setSearchQuery("")
            if (libraryState.favoritesOnly) musicLibraryViewModel.setFavoritesOnly(false)
        }
        pendingLocateTrackKey = trackKey
    }

    LaunchedEffect(pendingLocateTrackKey, selectedDestination, libraryState.tracks) {
        val targetKey = pendingLocateTrackKey ?: return@LaunchedEffect
        if (selectedDestination != AppDestination.LIBRARY) return@LaunchedEffect
        val trackIndex = libraryState.tracks.indexOfFirst { it.trackKey == targetKey }
        if (trackIndex >= 0) {
            val lazyIndex = LIBRARY_TRACK_LIST_HEADER_COUNT + trackIndex
            libraryListState.animateScrollToItem(lazyIndex)
            highlightedTrackKey = targetKey
            pendingLocateTrackKey = null
        }
    }

    LaunchedEffect(highlightedTrackKey) {
        if (highlightedTrackKey != null) {
            delay(2_400L)
            highlightedTrackKey = null
        }
    }

    val hasScanAccess = permissionState.value ||
        (!settingsState.internalMusicEnabled && settingsState.externalMusicEnabled && settingsState.externalTreeUri != null)

    BackHandler(enabled = showNowPlaying.value) { showNowPlaying.value = false }
    BackHandler(enabled = !showNowPlaying.value && showInsights.value) { showInsights.value = false }
    BackHandler(enabled = !showNowPlaying.value && !showInsights.value && showSettings.value) { showSettings.value = false }

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

    val activeTrack = libraryState.homeTracks.firstOrNull { it.trackKey == activeTrackKeyState.value }
        ?: libraryState.tracks.firstOrNull { it.trackKey == activeTrackKeyState.value }
    val activeArtworkPath = activeTrack?.artworkFilePath?.takeIf { File(it).isFile }

    var dynamicPalette by remember { mutableStateOf<ArtworkPalette?>(null) }
    LaunchedEffect(activeArtworkPath, settingsState.dynamicArtworkThemeEnabled) {
        dynamicPalette = if (settingsState.dynamicArtworkThemeEnabled && activeArtworkPath != null) {
            ArtworkColorExtractor.extractFromFile(activeArtworkPath)
        } else {
            null
        }
    }

    MusicPlayerProMaxTheme(
        appTheme = theme,
        appAccent = accent,
        dynamicPrimaryOverride = dynamicPalette?.let { Color(it.primaryArgb) },
        dynamicSecondaryOverride = dynamicPalette?.let { Color(it.secondaryArgb) }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                containerColor = MaterialTheme.colorScheme.background,
                bottomBar = {
                    Column {
                        playbackController?.let { controller ->
                            MiniPlayer(
                                controller = controller,
                                artworkPath = activeArtworkPath,
                                playbackSpeed = settingsState.playbackSpeed,
                                onSpeedBadgeClick = {
                                    showSettings.value = false
                                    showInsights.value = false
                                    selectedDestinationState.value = AppDestination.EQUALIZER
                                },
                                onExpand = { showNowPlaying.value = true }
                            )
                        }
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface,
                            tonalElevation = 0.dp
                        ) {
                            AppDestination.entries.forEach { destination ->
                                NavigationBarItem(
                                    selected = !showInsights.value && !showSettings.value && selectedDestination == destination,
                                    onClick = {
                                        showInsights.value = false
                                        showSettings.value = false
                                        selectedDestinationState.value = destination
                                    },
                                    icon = {
                                        Icon(
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
                                        indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                when {
                    showInsights.value -> {
                        Box(modifier = Modifier.padding(innerPadding)) {
                            StatisticsScreen(
                                state = statisticsState,
                                onBack = { showInsights.value = false }
                            )
                        }
                    }
                    showSettings.value -> {
                        Box(modifier = Modifier.padding(innerPadding)) {
                            SettingsScreen(
                                selectedLanguage = language,
                                selectedTheme = theme,
                                selectedAccent = accent,
                                dynamicArtworkThemeEnabled = settingsState.dynamicArtworkThemeEnabled,
                                sleepTimerEndMillis = settingsState.sleepTimerEndMillis,
                                onLanguageSelected = settingsViewModel::selectLanguage,
                                onThemeSelected = settingsViewModel::selectTheme,
                                onAccentSelected = settingsViewModel::selectAccent,
                                onDynamicArtworkThemeChanged = settingsViewModel::setDynamicArtworkThemeEnabled,
                                onSetSleepTimer = settingsViewModel::setSleepTimer,
                                internalMusicEnabled = settingsState.internalMusicEnabled,
                                externalMusicEnabled = settingsState.externalMusicEnabled,
                                externalTreeUri = settingsState.externalTreeUri,
                                minTrackDurationSeconds = settingsState.minTrackDurationSeconds,
                                shakeToSkipEnabled = settingsState.shakeToSkipEnabled,
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
                                onChooseExternalFolder = { folderPicker.launch(null) },
                                onMinTrackDurationChanged = settingsViewModel::setMinTrackDurationSeconds,
                                onShakeToSkipChanged = settingsViewModel::setShakeToSkipEnabled,
                                onBack = { showSettings.value = false }
                            )
                        }
                    }
                    else -> Crossfade(
                        targetState = selectedDestination,
                        label = "main_navigation",
                        modifier = Modifier.padding(innerPadding)
                    ) { destination ->
                        when (destination) {
                            AppDestination.HOME -> HomeScreen(
                                allTracks = libraryState.homeTracks,
                                totalTracksCount = libraryState.trackCount,
                                favoritesCount = libraryState.favoriteKeys.size,
                                playlistsCount = libraryState.playlists.size,
                                favoriteKeys = libraryState.favoriteKeys,
                                activeTrackKey = activeTrackKeyState.value,
                                repeatingTrackKey = repeatingTrackKey,
                                highlightedTrackKey = highlightedTrackKey,
                                listState = homeListState,
                                recentTracks = libraryState.recentTracks,
                                favoriteTracks = libraryState.favoriteTracks,
                                onPlayTrack = { tracks, selected ->
                                    playbackController?.let { controller ->
                                        PlaybackController.playQueue(controller, tracks, selected)
                                        showNowPlaying.value = true
                                    }
                                },
                                onPlayNext = enqueuePlayNext,
                                onGoToArtist = openArtistGroup,
                                onGoToAlbum = openAlbumGroup,
                                onToggleRepeatTrack = toggleRepeatForTrack,
                                onAddToPlaylist = { track -> trackForPlaylistDialog = track },
                                onToggleFavorite = musicLibraryViewModel::toggleFavorite,
                                onChangeCover = { track -> launchCoverPicker(track.trackKey) },
                                onResetCover = { track -> musicLibraryViewModel.resetTrackArtwork(track.trackKey) },
                                onShufflePlay = {
                                    val tracks = libraryState.homeTracks
                                    if (tracks.isNotEmpty()) {
                                        val shuffled = tracks.shuffled()
                                        playbackController?.let { controller ->
                                            controller.shuffleModeEnabled = true
                                            PlaybackController.playQueue(controller, shuffled, shuffled.first())
                                            showNowPlaying.value = true
                                        }
                                    }
                                },
                                onScanMusic = {
                                    if (hasScanAccess) musicLibraryViewModel.scan()
                                    else permissionLauncher.launch(permissionName)
                                },
                                onOpenLibrary = {
                                    musicLibraryViewModel.setFavoritesOnly(false)
                                    selectedDestinationState.value = AppDestination.LIBRARY
                                },
                                onOpenSearch = {
                                    musicLibraryViewModel.setFavoritesOnly(false)
                                    focusSearchRequest.value = true
                                    selectedDestinationState.value = AppDestination.LIBRARY
                                },
                                onOpenFavorites = {
                                    musicLibraryViewModel.setFavoritesOnly(true)
                                    selectedDestinationState.value = AppDestination.LIBRARY
                                },
                                onOpenPlaylists = { selectedDestinationState.value = AppDestination.PLAYLISTS },
                                onOpenInsights = { showInsights.value = true },
                                onOpenSettings = { showSettings.value = true }
                            )
                            AppDestination.LIBRARY -> LibraryScreen(
                                state = libraryState,
                                activeTrackKey = activeTrackKeyState.value,
                                repeatingTrackKey = repeatingTrackKey,
                                highlightedTrackKey = highlightedTrackKey,
                                listState = libraryListState,
                                focusSearchRequest = focusSearchRequest.value,
                                onSearchFocusConsumed = { focusSearchRequest.value = false },
                                requestedTab = requestedLibraryTab,
                                requestedGroupName = requestedLibraryGroup,
                                onNavigationRequestConsumed = {
                                    requestedLibraryTab = null
                                    requestedLibraryGroup = null
                                },
                                hasAudioPermission = hasScanAccess,
                                onTrackClick = { track ->
                                    playbackController?.let { controller ->
                                        PlaybackController.playQueue(controller, libraryState.tracks, track)
                                        showNowPlaying.value = true
                                    }
                                },
                                onPlayGroupTracks = { tracks, selected ->
                                    playbackController?.let { controller ->
                                        PlaybackController.playQueue(controller, tracks, selected)
                                        showNowPlaying.value = true
                                    }
                                },
                                onPlayNext = enqueuePlayNext,
                                onToggleRepeatTrack = { track -> toggleRepeatForTrack(libraryState.tracks, track) },
                                onToggleRepeatGroupTrack = toggleRepeatForTrack,
                                onScanClick = {
                                    if (hasScanAccess) musicLibraryViewModel.scan()
                                    else permissionLauncher.launch(permissionName)
                                },
                                onSearchChanged = musicLibraryViewModel::setSearchQuery,
                                onSortChanged = musicLibraryViewModel::setSortMode,
                                onFavoritesOnlyChanged = musicLibraryViewModel::setFavoritesOnly,
                                onToggleFavorite = musicLibraryViewModel::toggleFavorite,
                                onBatchAddFavorites = musicLibraryViewModel::addFavoritesBatch,
                                onBatchAddToPlaylist = musicLibraryViewModel::addTracksToPlaylistBatch,
                                onCreatePlaylist = musicLibraryViewModel::createPlaylist,
                                onAddToPlaylistRequest = { track -> trackForPlaylistDialog = track },
                                onUpdateMetadata = musicLibraryViewModel::updateTrackMetadata,
                                onImportLyrics = { trackKey ->
                                    lyricsTrackKey.value = trackKey
                                    lyricsPicker.launch(arrayOf("text/*", "application/octet-stream"))
                                },
                                onChangeCover = { track -> launchCoverPicker(track.trackKey) },
                                onResetCover = { track -> musicLibraryViewModel.resetTrackArtwork(track.trackKey) },
                                onOpenSettings = { showSettings.value = true }
                            )
                            AppDestination.PLAYLISTS -> PlaylistsScreen(
                                state = libraryState,
                                viewModel = musicLibraryViewModel,
                                activeTrackKey = activeTrackKeyState.value,
                                repeatingTrackKey = repeatingTrackKey,
                                listState = playlistsListState,
                                detailListState = playlistTracksListState,
                                onPlayTrack = { tracks, selected ->
                                    playbackController?.let { controller ->
                                        PlaybackController.playQueue(controller, tracks, selected)
                                        showNowPlaying.value = true
                                    }
                                },
                                onToggleRepeatTrack = toggleRepeatForTrack,
                                onAddToPlaylistRequest = { track -> trackForPlaylistDialog = track },
                                onToggleFavorite = musicLibraryViewModel::toggleFavorite,
                                onChangeCover = { track -> launchCoverPicker(track.trackKey) },
                                onResetCover = { track -> musicLibraryViewModel.resetTrackArtwork(track.trackKey) },
                                onImportM3uClick = {
                                    m3uImportLauncher.launch(arrayOf("audio/x-mpegurl", "audio/mpegurl", "application/vnd.apple.mpegurl", "*/*"))
                                },
                                onExportM3uClick = { name, tracks ->
                                    pendingM3uExport = name to tracks
                                    val safeFileName = name.replace(Regex("[^\\p{L}\\p{Nd}_ -]+"), "_").ifBlank { "playlist" }
                                    m3uExportLauncher.launch("$safeFileName.m3u")
                                },
                                onOpenSettings = { showSettings.value = true }
                            )
                            AppDestination.EQUALIZER -> EqualizerScreen(
                                settingsState = settingsState,
                                onEffectsEnabledChanged = settingsViewModel::setAudioEffectsEnabled,
                                onPresetSelected = settingsViewModel::setEqPresetName,
                                onBandLevelsChanged = settingsViewModel::setEqBandLevelsMb,
                                onSaveCustomProfile = settingsViewModel::saveCustomEqProfile,
                                onDeleteCustomProfile = settingsViewModel::deleteCustomEqProfile,
                                onApplyCustomProfile = settingsViewModel::applyCustomEqProfile,
                                onBassChanged = settingsViewModel::setBassBoostStrength,
                                onVirtualizerChanged = settingsViewModel::setVirtualizerStrength,
                                onLoudnessChanged = settingsViewModel::setLoudnessGainMb,
                                onStereoBalanceChanged = settingsViewModel::setStereoBalance,
                                onSkipSilenceChanged = settingsViewModel::setSkipSilenceEnabled,
                                onPlaybackSpeedChanged = settingsViewModel::setPlaybackSpeed,
                                onCrossfadeSecondsChanged = settingsViewModel::setCrossfadeSeconds,
                                onOpenSettings = { showSettings.value = true }
                            )
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = showNowPlaying.value && playbackController != null,
                enter = slideInVertically(initialOffsetY = { it / 3 }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it / 3 }) + fadeOut()
            ) {
                playbackController?.let { controller ->
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MidnightBlack
                    ) {
                        NowPlayingScreen(
                            controller = controller,
                            onBack = { showNowPlaying.value = false },
                            playbackSpeed = settingsState.playbackSpeed,
                            onPlaybackSpeedChange = settingsViewModel::setPlaybackSpeed,
                            crossfadeSeconds = settingsState.crossfadeSeconds,
                            onCrossfadeSecondsChange = settingsViewModel::setCrossfadeSeconds,
                            onChangeCoverArt = launchCoverPicker,
                            onResetCoverArt = { trackKey ->
                                musicLibraryViewModel.resetTrackArtwork(trackKey)
                            },
                            onLocateInList = locateTrackInList,
                            dynamicGlowColor = dynamicPalette?.let { Color(it.glowArgb) },
                            modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing)
                        )
                    }
                }
            }
        }

        trackForPlaylistDialog?.let { track ->
            val playlistIdsFlow = remember(track.trackKey) {
                musicLibraryViewModel.playlistIdsForTrack(track.trackKey)
            }
            val includedIds by playlistIdsFlow.collectAsStateWithLifecycle(initialValue = emptyList())
            PlaylistCheckboxDialog(
                track = track,
                playlists = libraryState.playlists,
                includedPlaylistIds = includedIds.toSet(),
                onTogglePlaylist = { playlistId, included ->
                    musicLibraryViewModel.setTrackInPlaylist(playlistId, track.trackKey, included)
                },
                onCreatePlaylist = { name, onResult ->
                    musicLibraryViewModel.createPlaylist(name, onResult)
                },
                onDismiss = { trackForPlaylistDialog = null }
            )
        }
    }
}

@Composable
private fun MiniPlayer(
    controller: MediaController,
    artworkPath: String?,
    playbackSpeed: Float = 1.0f,
    onSpeedBadgeClick: () -> Unit = {},
    onExpand: () -> Unit
) {
    val playerState = remember(controller) { mutableStateOf(readPlayerState(controller)) }
    val positionMs = remember(controller) { mutableLongStateOf(controller.currentPosition.coerceAtLeast(0L)) }

    DisposableEffect(controller) {
        val listener = object : Player.Listener {
            override fun onEvents(player: Player, events: Player.Events) {
                playerState.value = readPlayerState(player)
                positionMs.longValue = player.currentPosition.coerceAtLeast(0L)
            }
        }
        controller.addListener(listener)
        onDispose { controller.removeListener(listener) }
    }

    val state = playerState.value
    LaunchedEffect(controller, state.isPlaying) {
        while (state.isPlaying) {
            positionMs.longValue = controller.currentPosition.coerceAtLeast(0L)
            delay(500L)
        }
    }

    if (state.title.isBlank()) return
    val progress = if (state.durationMs > 0L) (positionMs.longValue.toFloat() / state.durationMs.toFloat()).coerceIn(0f, 1f) else 0f
    var horizontalSwipeAccum by remember { mutableFloatStateOf(0f) }

    Surface(
        onClick = onExpand,
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, SubtleBorder),
        tonalElevation = 4.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .pointerInput(controller) {
                detectHorizontalDragGestures(
                    onDragStart = { horizontalSwipeAccum = 0f },
                    onDragEnd = {
                        if (horizontalSwipeAccum <= -90f) controller.seekToNextMediaItem()
                        else if (horizontalSwipeAccum >= 90f) controller.seekToPreviousMediaItem()
                        horizontalSwipeAccum = 0f
                    },
                    onHorizontalDrag = { _, dragAmount -> horizontalSwipeAccum += dragAmount }
                )
            }
            .testTag("mini_player_card")
    ) {
        Column {
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.5.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = SubtleBorder
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TrackArtworkBox(
                    artworkPath = artworkPath,
                    size = 38.dp,
                    cornerRadius = 10.dp,
                    isCurrentTrack = state.isPlaying
                )
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = state.title,
                        maxLines = 1,
                        modifier = Modifier.basicMarquee(),
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        state.artist.ifBlank { stringResource(R.string.unknown_artist) },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (abs(playbackSpeed - 1.0f) >= 0.03f) {
                    Surface(
                        onClick = onSpeedBadgeClick,
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                        modifier = Modifier.padding(end = 2.dp).testTag("mini_player_speed_badge")
                    ) {
                        Text(
                            text = String.format(Locale.US, "%.2gx", playbackSpeed),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                        )
                    }
                }
                IconButton(
                    onClick = {
                        controller.repeatMode = if (controller.repeatMode == Player.REPEAT_MODE_ONE) {
                            Player.REPEAT_MODE_OFF
                        } else {
                            Player.REPEAT_MODE_ONE
                        }
                    },
                    modifier = Modifier.testTag("mini_player_repeat_one")
                ) {
                    Icon(
                        imageVector = if (state.repeatOneEnabled) Icons.Filled.RepeatOne else Icons.Filled.Repeat,
                        contentDescription = stringResource(R.string.repeat_this_song),
                        tint = if (state.repeatOneEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(
                    onClick = { if (controller.isPlaying) controller.pause() else controller.play() },
                    modifier = Modifier.testTag("mini_player_play_pause")
                ) {
                    Icon(
                        if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = stringResource(R.string.accessibility_play_pause),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(
                    onClick = controller::seekToNextMediaItem,
                    modifier = Modifier.testTag("mini_player_next")
                ) {
                    Icon(
                        Icons.Filled.SkipNext,
                        contentDescription = stringResource(R.string.accessibility_next_track),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

private data class MiniPlayerState(
    val title: String,
    val artist: String,
    val isPlaying: Boolean,
    val repeatOneEnabled: Boolean,
    val durationMs: Long
)

private fun readPlayerState(player: Player): MiniPlayerState {
    val metadata = player.currentMediaItem?.mediaMetadata
    val duration = player.duration.takeUnless { it == C.TIME_UNSET } ?: 0L
    return MiniPlayerState(
        title = metadata?.title?.toString().orEmpty(),
        artist = metadata?.artist?.toString().orEmpty(),
        isPlaying = player.isPlaying,
        repeatOneEnabled = player.repeatMode == Player.REPEAT_MODE_ONE,
        durationMs = duration.coerceAtLeast(0L)
    )
}
