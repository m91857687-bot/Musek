# Decisions Log

- Product name: Music Player Pro Max.
- Visual identity: premium black + electric blue.
- Home page: personal, calm and minimal; recently played and favorites sections with a library shortcut.
- Storage scope for later indexing: internal storage and SD card; user can toggle each source in settings.
- No user-configurable excluded folders.
- Artwork source: real embedded album art and genuine related cover images; no random placeholder artwork.
- Background artwork: show current track cover in media notification and lock screen where Android/device support it.
- Languages: Arabic, English, Spanish; detect device language initially, English fallback for unsupported languages, persist manual choice.
- Offline: no internet dependency for core product functionality.
- Audio equalizer target: 10 bands; effects are constrained by device capabilities.
- Minimum Android target: Android 8.0 / API 26, provisional pending compatibility verification.
- Current application ID: `com.musicplayer.promax`, provisional.
- Phase 1 build tooling: AGP 8.13.2, Gradle 8.13, Kotlin 2.3.21, Compose BOM 2026.09.00, JDK 17.
- Phase 2 Home visual direction: quiet Luxury Minimalism; personal landing screen with a branded header, decorative vinyl-inspired identity art, recently played and Favorites sections, and shortcuts to Songs/Albums/Artists.
- The vinyl illustration on Home is decorative brand artwork only; it must never be presented as an extracted album cover.
- Empty states are intentional until real library/playback data is implemented; no demo tracks or simulated usage metrics.
- Phase 2 screen shell uses reusable premium headers and empty-state components; actual functionality remains deferred to the corresponding implementation phases.

## Phase 3 decisions
- Room is the persistent catalog store; MediaStore content URIs are retained instead of copying audio files.
- MediaStore metadata updates use `DATE_MODIFIED` with a small overlap; deletion reconciliation reads IDs only.
- Internal and external source toggles default to enabled and are persisted in DataStore.
- Android 10+ removable volumes use MediaStore when exposed. SAF is the fallback for media not exposed as a separate volume; Android 8/9 requires explicit SD folder selection to separate removable storage.
- WorkManager runs an initial unique task, a 12-hour periodic update, and debounced foreground MediaStore-change scans.
- The catalog displays metadata only; actual playback is reserved for Phase 4.

## Phase 8 metadata safety decision
- Track metadata edits are app-local display overrides stored in Room. Do not claim that original audio tags are rewritten. This avoids format-specific tag-writing dependencies and broad storage write access.
- Embedded artwork is extracted on demand and cached in app-private storage.
- Lyrics are local-only imported LRC text; no online lyric lookup is allowed in this offline-first player.
