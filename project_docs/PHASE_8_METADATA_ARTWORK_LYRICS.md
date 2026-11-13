# Phase 8 — Metadata, Artwork & Lyrics

## Scope implemented

### Metadata
- Reads embedded title, artist, album, and duration from the selected audio file through `MediaMetadataRetriever` when the track is opened for playback.
- Supports app-local display overrides for title, artist, and album.
- The original audio file is not rewritten. This avoids unsafe or format-dependent tag rewriting and avoids requiring broad storage write permissions.
- Library search and sorting use the effective display metadata.

### Album artwork
- Extracts embedded artwork from the actual audio file on demand when it is played.
- Uses bounded image decoding (20 MB embedded-image input cap, dimension sanity checks, downsampling, RGB_565) to reduce memory risk.
- Saves JPEG artwork under app-private `filesDir/album_artwork`, not in the user's music folders.
- Stores the cached file path in Room, displays the real artwork in Now Playing, and uses a blurred/dimmed version as the cinematic screen background. If no embedded cover exists or extraction fails, the existing vinyl visual remains a fallback and is not presented as a real cover.

### Lyrics
- Imports a local LRC file through Android's document picker.
- Validates that at least one timestamped lyric line exists and limits imported text to 1 MB.
- Parses fractional timestamps and multiple timestamps on a line.
- Synchronizes the current and next lyric line to the playback position.
- Stores lyric text locally in Room; no network access or online lyrics service is used.

## Data and migration
- Room database version advanced from 2 to 3.
- Migration 2 → 3 adds nullable columns for title/artist/album overrides, artwork file path, and LRC text.
- Migration 1 → 2 remains registered, so older installs can migrate sequentially.
- Catalog refresh writes update only indexed source fields and preserve app-local metadata overrides. If the audio file size or modification time changes, cached artwork and imported lyrics are invalidated so stale art/lyrics are not shown.

## Important boundaries
- Metadata editing changes the labels shown by this app only; it does not rewrite ID3/Vorbis/MP4 tags inside the original audio file.
- Artwork extraction is on-demand for a track when it is played, not a full-library background extraction.
- Lyrics are imported manually from local `.lrc` files; automatic sidecar discovery is not included.
- The active notification/lock-screen MediaItem is not refreshed mid-playback after first-time extraction; cached artwork is attached when a queue is built on a later play.
- Actual artwork extraction, SAF URI compatibility, and lyric timing still require device testing.
