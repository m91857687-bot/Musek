# MASTER CONTEXT — Music Player Pro Max

## Product
- Display name: Music Player Pro Max
- Arabic display name: مشغل الأغاني برو ماكس
- Category: offline local music player for Android
- Monetization direction: paid one-time purchase; final price is not set
- Package/application ID currently provisional: `com.musicplayer.promax`

## Product principles
- Offline-first: core functionality must not depend on an internet connection.
- Premium visual identity: luxury black with electric blue.
- Main screen philosophy: quiet, personal, minimal, and polished rather than crowded.
- Performance: music indexing must not block the UI and must avoid full rescans on every launch.
- Real functionality only: never label a placeholder as implemented functionality.
- Build in explicit phases; review and approve each phase before starting the next.

## Confirmed features for the full product
- Internal storage and external SD card music sources, controllable from settings.
- Automatic indexing with persistent local metadata and incremental updates.
- Real album-art extraction from embedded artwork and genuine related cover files.
- Background playback, lock-screen/media notification artwork and transport controls.
- Audio formats to support where the decoder/device allows: MP3, FLAC, WAV, AAC, OGG, and other suitable local formats.
- 10-band equalizer, Bass Booster, Virtualizer and left/right balance subject to device capabilities.
- General audio profile, per-track settings and user presets.
- Gapless playback where supported by the format and playback engine.
- Playlists, metadata editing, local lyrics display (including timed LRC when available), listening statistics.
- Bluetooth/headset controls, sleep timer, home-screen widget, customizable player appearance and backgrounds.
- Languages: Arabic, English and Spanish; system-language detection on first launch, English fallback for unsupported device languages, and persistent manual selection.

## Phase boundaries
Work proceeds in explicit phases. Do not implement the next phase without explicit user approval.

## Current implementation status (updated 2026-11-13)
- Phase 1 foundation and Phase 2 UI source are present in the repository.
- Phase 3 music discovery/indexing source has now been added: Room catalog, MediaStore and SAF scanning, internal/external source preferences, WorkManager, and a Library catalog list.
- Phase 4 playback engine source has now been added: Media3 ExoPlayer, MediaSessionService, background playback integration, audio focus/noisy-route handling, library track playback, and a compact mini-player.
- Phase 5 Now Playing source has now been added: full-screen player, live progress/seek, play/pause/previous/next, shuffle/repeat, animated vinyl-inspired presentation, and English/Arabic/Spanish labels.
- Phase 3 has passed static resource/catalog checks. Gradle assembleDebug remains unverified because this environment cannot resolve services.gradle.org; emulator/runtime behavior has not been tested.
- Phases 4, 5, and 6 source implementations are complete. Phase 6 was statically reviewed and corrected before Phase 7 began with user approval.

## Phase 7 — Library management
- Implemented Room-backed playlists, playlist memberships, and favorites with a non-destructive Room 1→2 migration.
- Library now supports debounced database search by title/artist/album, sorting by title/artist/album/duration/newest, and a favorites-only filter.
- Users can create, rename, and delete playlists; add tracks from the library; remove tracks; and play a playlist queue.
- New UI strings are localized in English, Arabic, and Spanish.
- Static XML/resource/SQLite checks passed. Gradle, APK, emulator, and device verification have not been run.

## Phase 8 — Metadata, artwork, and lyrics
- Added Room v2→v3 migration for app-local metadata overrides, cached artwork paths, and imported LRC text.
- Added embedded metadata reading and real embedded album-art extraction on demand during playback, with bounded image decoding and app-private caching.
- Added in-app title/artist/album overrides; original audio file tags are not rewritten.
- Added manual local LRC import, timestamp parsing, and playback-position lyric synchronization.
- Catalog refresh preserves user metadata overrides and invalidates artwork/LRC cache when the underlying audio file changes.
- Static checks passed; Gradle, APK, emulator, and device verification have not been run.
- Phase 9 must wait for explicit user approval.


## Latest status — Phase 9
Phase 9 source is implemented: local 365-day listening insights, playback history in Room v4, a persisted service-owned sleep timer, selectable accent colors, and a home-screen playback widget. Static checks were performed; do not claim Gradle build or device verification. Phase 10 remains unstarted and requires explicit user approval.
