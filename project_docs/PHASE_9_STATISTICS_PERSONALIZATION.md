# Phase 9 — Statistics and Personalization

## Scope implemented

### Listening statistics
- Added `playback_history` Room entity and `PlaybackStatsDao`.
- Added Room migration 3 → 4 without destructive migration.
- Added `PlaybackHistoryRecorder` to the playback service. It records audible segments when playback pauses, changes tracks, ends, or the service is destroyed.
- A visit contributes one play count on its first audible segment; resumed segments add listening time without incrementing the play count again.
- History is retained for a rolling 365-day window and old rows are pruned during writes.
- Added a Statistics destination with total plays, distinct tracks, total listening time, and top tracks.
- Statistics are local-only and do not require network access.

### Sleep timer
- Added a persisted end timestamp to DataStore.
- Added 15, 30, 45, and 60-minute presets plus cancellation.
- The playback service observes the persisted timer and pauses the player at expiry. A newly created service can resume a pending timer from the stored end timestamp.

### Personalization
- Added persisted accent choices: Electric Blue, Violet, and Emerald.
- Routed Compose accent references through MaterialTheme color roles so the chosen accent is applied throughout the app.
- Existing Classic and Cinematic surface themes remain available.

### Home-screen widget
- Added a resizable home-screen widget showing current title and artist.
- Added play/pause and next-track actions through a MediaController connection.
- Playback service refreshes widget content when playback metadata or state changes.
- Widget actions use explicit internal broadcasts; no network permission or external service is introduced.

## Files added or changed
- `data/stats/PlaybackHistoryEntity.kt`
- `data/stats/PlaybackStatsDao.kt`
- `playback/PlaybackHistoryRecorder.kt`
- `presentation/statistics/StatisticsViewModel.kt`
- `presentation/statistics/StatisticsScreen.kt`
- `domain/model/AppAccent.kt`
- `widget/MusicPlayerWidgetProvider.kt`
- `res/layout/widget_music_player.xml`
- `res/xml/music_player_widget_info.xml`
- `res/values*/strings.xml`
- Updated Room database, preferences repository, settings screen/view model, navigation, app theme, playback service, manifest, and README.

## Known limitations
- Listening segments are persisted when a segment closes. A sudden process kill before the callback is flushed can lose the currently open segment.
- The statistics screen summarizes the rolling last 365 days, not lifetime history.
- The widget is a compact text-and-controls widget; it does not yet show album artwork or a progress bar.
- Widget play/pause and next actions depend on the playback MediaSession being connectable. If connection fails, tapping the widget's main body still opens the app.
- Accent color is applied through the app's Material theme roles; artwork-derived colors are not generated dynamically.
- Sleep timer counts wall-clock time, including periods when playback is paused.

## Verification boundary
- XML parsing, translation-key parity, string-resource reference checks, SQLite query/migration smoke checks, and Kotlin syntax parsing were performed.
- No Gradle build, APK generation, emulator test, or physical-device test was performed.
