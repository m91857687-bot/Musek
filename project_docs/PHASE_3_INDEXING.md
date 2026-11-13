# Phase 3 — Music Discovery and Indexing

## Implemented in source

- Added a Room database for indexed audio records and per-volume scan checkpoints.
- Added MediaStore scanning for the primary shared-storage volume and removable volumes exposed by Android 10+.
- Added a Storage Access Framework tree picker for removable storage that is not exposed as a separate MediaStore volume; Android 8/9 uses this explicit route for SD-card access.
- Added runtime audio permission handling (`READ_MEDIA_AUDIO` on Android 13+, `READ_EXTERNAL_STORAGE` through Android 12).
- Added source switches in Settings for internal and external storage. Both default to enabled.
- Added a persistent SAF URI permission for the selected SD-card folder.
- Added WorkManager one-time indexing, a 12-hour periodic update, and a foreground MediaStore `ContentObserver` that debounces media changes while the app is open.
- Added incremental MediaStore metadata queries using `DATE_MODIFIED` with a two-second overlap. A lightweight ID-only reconciliation removes deleted entries without rereading all metadata.
- Added SAF traversal that compares last-modified time and file size before updating cached rows. SAF still requires directory enumeration because the provider does not expose a reliable global incremental-change query.
- Added a Room-backed track count and a lazy list of up to 200 catalog entries in Library.
- Source toggles remove catalog entries from disabled sources. Unmounted removable MediaStore volumes are removed from the visible catalog.
- The worker uses no network requirement and the app does not request INTERNET permission.

## Explicit limitations

- This phase indexes metadata and content URIs only. It does not decode or play audio.
- Album-art extraction, artwork caching, playback, playlists, metadata editing, and lyrics are later phases.
- SAF fallback records currently use the document filename and file facts; full embedded tag parsing is not included in this phase.
- On Android 8/9, selecting the SD-card folder through the system picker is required to distinguish removable storage from the aggregate MediaStore volume.
- Periodic indexing is scheduled every 12 hours. MediaStore changes are also observed while the app is in the foreground; changes while the app is closed are reconciled on the next periodic or app-start scan.

## Verification status

- XML parsing: passed.
- Version-catalog TOML parsing: passed.
- English/Arabic/Spanish string-key parity: passed (74 keys each).
- Kotlin `R.string` references: passed (62 references, none missing).
- Gradle version-catalog alias cross-check: passed.
- Manifest permission check: passed; no INTERNET permission present.
- Gradle `:app:assembleDebug`: attempted, but did not start because this environment could not resolve `services.gradle.org` (DNS error). No APK build success is claimed.
- Android emulator test: not run in this environment.
