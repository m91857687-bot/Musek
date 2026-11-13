# Changelog

## 2026-11-13 — Final Engineering Audit, Bug Fixes, CI & Verification (Phase 10)
- Fixed build/SDK/toolchain compatibility (`compileSdk = 36`, `kotlin = 2.0.21`, Java 17 target, `WorkManager` `LiveData.asFlow()`, Compose `Column`/`BorderStroke` imports) and added `.github/workflows/build_apk.yml` for GitHub APK builds.
- Fixed `PlaybackHistoryRecorder` recording false plays/duration while paused and added 365-day retention filtering to `PlaybackStatsDao.observeRecentTracks()`.
- Chunked `MusicTrackDao.deleteTracks()` into 500-item batches (`@Transaction`) to prevent SQLite 999-parameter crashes, and fixed `PlaylistDao.observePlaylists()` to exclude deleted catalog tracks from counts.
- Connected `HomeScreen` Recent/Favorites sections to live Room data, eliminated duplicate `NowPlayingScreen` artwork bitmap decodes, handled `onAudioSessionIdChanged` and sleep timer drift in `MusicPlaybackService`, and fixed `EqualizerScreen`, `SettingsScreen`, `PlaylistsScreen` (`BackHandler` + AutoMirrored RTL back icon), `MainActivity` (`enableEdgeToEdge()`), widget strings/icons, and adaptive round icon background.
- Removed leftover `com.example` template files and `/extracted_proj`.
- Added Robolectric tests (`MusicLibraryDatabaseTest`, `PlaybackHistoryRecorderTest`) verifying Room v1→v4 migrations, >999 batch deletes, 365-day stats queries, and segment play-count accuracy; all 14/14 tests and `:app:assembleDebug` passed (`BUILD SUCCESSFUL`).
- Updated `FINAL_AUDIT_REPORT.md`, `PROJECT_STATUS.md`, and `KNOWN_ISSUES.md`.

## 0.1.0 — Phase 1 foundation
- Created initial Android application structure.
- Added Compose Material 3 shell and five top-level destinations.
- Added Arabic, English and Spanish string resources.
- Added DataStore persistence for language and theme choices.
- Added Classic and Cinematic black/blue color schemes.
- Added project documentation and initial launcher artwork.
- No playback or indexing functionality included in this phase.

## 2026-11-13 — Phase 1 verification retry
- Added the missing `QueueMusic` icon import in `AppDestination.kt`.
- Retried Gradle bootstrap; distribution download was blocked by DNS resolution failure for `services.gradle.org`.
- Updated verification report with the exact retry result and remaining verification limits.

## Phase 2 — Premium UI and Navigation Shell
- Refined Home screen to the approved premium-minimal direction.
- Added reusable screen header and feature empty-state components.
- Improved Library, Playlists, Equalizer and Settings destination layouts.
- Added Home shortcuts to Library and Settings.
- Refined bottom navigation selected/unselected colors and saved destination state.
- Added English, Arabic and Spanish copy for all new UI elements.
- Kept all unimplemented media features explicitly empty; no mock music or playback was introduced.

## Phase 3 — Music discovery and indexing
- Added Room entities, DAO and database for the local music catalog and scan checkpoints.
- Added MediaStore incremental scanning and lightweight ID reconciliation.
- Added SD-card SAF folder selection and persisted access.
- Added internal/external source controls in Settings.
- Added WorkManager one-time and periodic indexing plus foreground MediaStore change observation.
- Added a Room-backed catalog count and lazy track list in Library.
- Added audio permission handling for Android 8 through current Android versions.
- Static checks passed; Gradle build remains unverified due DNS failure reaching services.gradle.org.


## Phase 3 code review
- Delayed deletion of other SAF trees until the newly selected tree has completed traversal and reconciliation.
- Changed unique scan work policy to `APPEND_OR_REPLACE` so requests arriving during an active scan are not dropped.
- Added `PHASE_3_CODE_REVIEW.md` with review scope, corrections, limitations, and verification boundaries.


## Phase 4 — Playback Engine
- Added Media3 ExoPlayer and MediaSession dependencies.
- Added a background MediaSessionService with automatic audio-focus handling and noisy-route pause behavior.
- Connected the library list to real playback through MediaController.
- Added compact mini-player controls for play/pause and next.
- Registered media playback foreground service permissions and manifest service declaration.


## Phase 5 — Now Playing Experience
- Added a dedicated full-screen player connected to the existing Media3 controller.
- Opened the full player from the mini-player and automatically after selecting a library track.
- Added live metadata, elapsed/total time, seeking, play/pause, previous/next, shuffle and repeat controls.
- Added a slowly rotating vinyl-inspired visual and play/pause transition.
- Added English, Arabic and Spanish labels.
- Documented genuine artwork extraction as a Phase 8 dependency; no fake album art is presented as real metadata.
- Static resource and source-structure checks passed; Gradle and runtime tests were not run.


## Phase 6 — Audio Processing (2026-11-13)
- Added service-owned Android Equalizer, BassBoost, and Virtualizer controls tied to the active ExoPlayer audio session.
- Added Flat, Bass, Vocal, and Treble presets and replaced the equalizer placeholder UI with functional controls.
- Added capability-aware UI and Arabic/English/Spanish translations.
- Added `MODIFY_AUDIO_SETTINGS` permission.
- Static verification only; no Gradle build or device test was run.


## Phase 6 code review corrections
- Replaced the plain audio-effects singleton bridge with a `StateFlow` observable by the Compose UI.
- Synchronized the master switch with the manager's actual enabled state and disabled controls until an audio session is attached.
- Guarded effect adjustment and preset operations against platform implementation exceptions.
- Corrected outdated equalizer placeholder descriptions in all three locales.
- Added `PHASE_6_CODE_REVIEW.md`; static checks repeated.

## Phase 7 — Library Management
- Added Room-backed playlists, playlist membership, and favorites.
- Added a version 1 → 2 Room migration without destructive database fallback.
- Added live library search across title, artist, and album; sorting by title, artist, album, duration, and modified date; and a favorites-only filter.
- Added playlist creation, rename, deletion, duplicate-name validation, add/remove track actions, playlist counts, and playback from a playlist queue.
- Added Arabic, English, and Spanish strings for all new controls and empty/error states.


## Phase 8 — Metadata, Artwork & Lyrics (2026-11-13)
- Added Room v2 → v3 migration for in-app metadata overrides, cached artwork paths, and imported LRC text; earlier migration paths remain registered.
- Added embedded album-art extraction with `MediaMetadataRetriever`, bounded image decoding, and app-private persistent artwork cache.
- Added real artwork presentation on the Now Playing screen, a blurred album-art background, and artwork URI metadata for newly constructed playback queue items.
- Added editable in-app title/artist/album overrides. The original audio file tags are intentionally not rewritten.
- Added LRC import with a 1 MB input limit, timestamp parsing (including multiple timestamps per line and fractional seconds), and live lyric-line synchronization to playback position.
- Changed catalog refresh writes to update indexed columns without overwriting user metadata. If a source file changes (size or modified time), its cached artwork and imported lyrics are invalidated.
- Added Arabic, English, and Spanish translations and LRC parser unit tests.
- No Gradle build or device test was run.

## Phase 9 — Statistics and personalization
- Added local playback history, 365-day listening insights, and a statistics screen.
- Added a persisted sleep timer controlled by the playback service.
- Added three selectable accent colors and applied Material theme accent roles throughout Compose UI.
- Added a resizable home-screen playback widget with play/pause and next actions.
- Added Room migration 3 → 4 and English, Arabic, and Spanish strings.

## Phase 10 — Quality & Release review pass (2026-11-13)
- Applied rolling 365-day filters to all listening-statistics aggregate queries.
- Moved playlist creation/rename, favorite toggles, and playlist-track removal into Room transaction methods.
- Guarded late MediaController connection completion against Activity teardown.
- Re-check audio-library permission when returning from Android Settings to the app.
- Tightened artwork bitmap downsampling so neither decoded dimension exceeds 1200 px.
- Added localized accessibility descriptions for mini-player playback controls.
- Added pure Kotlin unit tests for artwork sampling bounds and invalid dimensions.
- Added `PHASE_10_FINAL_REVIEW.md` with static checks and outstanding release validation.
- Gradle intentionally not run; no APK/AAB or device test claimed.
