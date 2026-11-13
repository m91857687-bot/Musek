# Phase 4 — Playback Engine

## Scope implemented in source
- Added AndroidX Media3 ExoPlayer and MediaSession dependencies.
- Added `MusicPlaybackService`, a `MediaSessionService` that owns the player outside the Activity lifecycle.
- Configured music audio attributes, automatic audio-focus handling, and pause-on-audio-becoming-noisy behavior.
- Registered the media playback foreground service and required foreground-service permissions in the manifest.
- Added a MediaController connection from `MainActivity` to the service.
- Connected real indexed library rows to playback. Selecting a row builds a queue from the currently exposed library page, seeks to the selected item, prepares and starts playback.
- Added a compact mini-player above the bottom navigation with current title/artist, play/pause, and next controls.
- Playback uses the stored MediaStore or SAF content URI. Audio files are not copied and no network access is added.

## Important boundaries
- This phase does not include the cinematic full-screen Now Playing screen; that belongs to Phase 5.
- Artwork extraction and notification artwork are not implemented yet; they belong to Phase 8/artwork integration. MediaSession metadata currently includes title, artist and album only.
- Queue is currently formed from the first 200 catalog rows exposed by the existing Phase 3 DAO. Full-library paging/queue management belongs to later library-management work.
- Supported formats depend on Media3/device decoders and the actual file/container codec.

## Verification status
- Source and manifest review performed; XML/resource/static checks are to be recorded in the phase delivery report.
- Gradle was not run, per user instruction. No APK build or Android runtime test is claimed.
