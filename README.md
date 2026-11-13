# Music Player Pro Max

An offline-first Android local music player built with Kotlin, Jetpack Compose, Material 3, Room, WorkManager, and Media3.

## Implemented source phases

1. Foundation, localization, design tokens, settings persistence, and navigation.
2. Premium UI foundation and main app screens.
3. Local music discovery and indexing through MediaStore and SAF, with Room-backed catalog and background work.
4. Media3 playback service, MediaSession, background playback, audio focus handling, and mini-player.
5. Full-screen Now Playing experience with seeking, transport controls, shuffle/repeat, and vinyl-inspired motion.
6. Device-supported Equalizer, BassBoost, Virtualizer, and presets.
7. Room-backed playlists and favorites, search, sorting, and playlist playback.
8. Embedded artwork extraction/cache, app-local metadata overrides, imported LRC lyrics, and lyric synchronization.
9. Listening statistics, sleep timer, accent customization, and home-screen widget.
10. Code-level integration review and static quality corrections; Android build/device/release validation remains pending.

## GitHub Actions CI/CD (Automatic APK Build)

This repository includes a GitHub Actions workflow (`.github/workflows/build_apk.yml`) that automatically builds the Android Debug APK upon pushing code to GitHub.

### How to get your APK from GitHub:
1. Push your code to your GitHub repository (`https://github.com/m91857687-bot/Moshagela` or your fork).
2. Go to the **Actions** tab in your GitHub repository.
3. Click on the latest **Build Android APK** workflow run.
4. Scroll down to the **Artifacts** section at the bottom of the summary page and download `MusicPlayerProMax-Debug-APK`.
5. Extract the ZIP file to get `app-debug.apk` and install it on your Android device.

See `FINAL_AUDIT_REPORT.md`, `project_docs/PROJECT_STATUS.md`, and `KNOWN_ISSUES.md` for full audit and status details.
