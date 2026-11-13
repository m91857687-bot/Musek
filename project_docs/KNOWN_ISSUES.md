# Known Issues / Verification Gaps

1. **Build & Test Status**: Successfully built `app/build/outputs/apk/debug/app-debug.apk` and passed all 14 unit & Robolectric tests (`MusicLibraryDatabaseTest`, `PlaybackHistoryRecorderTest`, `ArtworkSamplingTest`, `LrcParserTest`, `AppLanguageTest`).
2. **Android Hardware & Device Testing**: On-device behavior (runtime permission dialogs, physical SD-card SAF tree selection, background playback audio focus, headset unplug pause, lock-screen controls, and hardware Equalizer/BassBoost/Virtualizer support across OEM devices) requires manual validation on a physical Android phone.
3. **Cold Widget Play Action**: When `MusicPlaybackService` is stopped and has no queued items (`mediaItemCount == 0`), tapping Play on the home-screen widget cannot start playback until the user opens the app (tapping the widget opens `MainActivity`).
4. **Gradle Wrapper vs Container Build**: The repository includes `./gradlew` (Gradle 8.13) for GitHub Actions CI (`/.github/workflows/build_apk.yml`) and standard developer machines, while container verification ran using preinstalled `gradle` 9.3.1 due to offline DNS limits on `services.gradle.org`.
5. **SAF Fallback**: SAF fallback rows use filename and file facts; embedded tag/artwork extraction behavior for SAF URIs requires device validation.
6. **Application ID**: Application ID `com.musicplayer.promax` is preserved across all builds.
7. **Large Libraries**: Library queries materialize matching tracks into a `List`; Paging 3 is not integrated. Very large libraries (>10k tracks) may increase memory usage.
8. **Room Schema & Migrations**: Migrations 1→2, 2→3, and 3→4 are verified by `MusicLibraryDatabaseTest` (`migrationsFromVersion1To4PreserveTracksAndOpenCleanly`).
9. **Metadata Overrides & LRC Import**: App-local metadata overrides do not rewrite embedded tags in original audio files (by design), and LRC files are imported manually per track.
10. **Google Play Release**: Production release keystore, R8 release minification testing, store listing assets, and privacy policy configuration remain outstanding before Google Play publication.
