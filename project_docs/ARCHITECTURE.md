# Architecture

## Application layers
- `presentation`: Jetpack Compose screens and ViewModels.
- `domain`: app models and repository contracts.
- `data/preferences`: DataStore-backed user preferences.
- `data/local`: Room database, entities and DAOs.
- `data/media`: MediaStore and Storage Access Framework metadata discovery.
- `data/work`: WorkManager scheduling and background indexing worker.
- `navigation`: destination definitions.
- `ui/theme`: design tokens and Material theme.

## Music indexing data flow
1. The user grants `READ_MEDIA_AUDIO` (Android 13+) or `READ_EXTERNAL_STORAGE` (Android 8–12), or explicitly selects an SD-card folder using SAF.
2. Source choices are stored in DataStore.
3. `MusicIndexScheduler` enqueues unique background work. A 12-hour periodic task is registered when an indexing permission/source is granted.
4. `MusicIndexWorker` reads the selected sources and calls `MediaStoreMusicScanner`.
5. MediaStore metadata is queried incrementally using `DATE_MODIFIED`; a lightweight ID-only query reconciles deletions.
6. SAF fallback traverses the selected document tree and compares URI, last-modified time and size before writing changed metadata.
7. Room publishes the catalog count and searchable catalog queries as Flows. Search, sorting, and favorites filtering execute in SQLite; the UI does not perform filesystem queries.
8. Playlist, playlist-membership, and favorite records are stored in Room. Playlist membership uses the stable track key and is retained if a catalog scan temporarily removes a track row.
9. Library and playlist screens observe Room Flows and render lazy lists. Playback from a playlist sends its current ordered tracks to the Media3 queue.

## Offline/privacy properties
- No INTERNET permission is declared.
- Audio file bytes are not copied during indexing.
- Persisted SAF read access is limited to the folder selected by the user.
- WorkManager indexing does not require network connectivity.

## Phase 7 library management data flow
1. Search text is debounced for 250 ms and combined with the selected sort mode and favorites filter.
2. The ViewModel switches to the corresponding Room `observeLibraryTracks` query.
3. Favorite toggles write to `favorite_tracks`; playlist actions write to `playlists` and `playlist_tracks`.
4. Room invalidation emits updated Flows to the UI. Playlist deletion cascades to its membership rows.
5. The playlist detail view obtains tracks by joining membership rows with the current music catalog; playback uses that ordered list.

## Phase 8 metadata pipeline
- `TrackMetadataRepository` uses `MediaMetadataRetriever` on an IO dispatcher to read embedded audio tags and extract embedded artwork.
- Album artwork is decoded with bounds checks/downsampling and cached in app-private files; Room stores only its path.
- `MusicTrackDao` keeps source metadata separate from app-local display overrides, artwork path, and imported LRC text.
- LRC text is parsed by `LrcParser` and the active line is selected from the Media3 playback position.
- Library rescans update source/index fields without replacing user-added metadata, artwork paths, or lyrics.


## Phase 9 additions
- Playback history is stored locally in Room (`playback_history`, database version 4).
- The playback service records audible segments asynchronously and owns the persisted sleep timer.
- DataStore stores accent selection and the sleep timer end timestamp.
- Statistics are derived from local playback history; no analytics endpoint or network access is used.
- The home-screen widget uses an internal AppWidgetProvider and MediaController actions.

## Phase 10 quality corrections
- Statistics DAO queries enforce a rolling 365-day window at query time; history pruning remains a storage cleanup task rather than the only correctness boundary.
- Playlist/favorite write workflows use Room `@Transaction` methods to keep duplicate checks and mutations together.
- Artwork sampling is isolated in a pure Kotlin helper so image-memory bounds can be unit tested without Android runtime dependencies.
- The Activity checks its lifecycle before publishing a late MediaController connection result.
