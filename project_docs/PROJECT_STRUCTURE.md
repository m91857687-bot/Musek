# Project Structure

```text
MusicPlayerProMax/
├── app/
│   ├── schemas/                         # Room schema export destination
│   ├── src/main/
│   │   ├── AndroidManifest.xml
│   │   ├── java/com/musicplayer/promax/
│   │   │   ├── MainActivity.kt
│   │   │   ├── MusicPlayerProMaxApp.kt
│   │   │   ├── data/
│   │   │   │   ├── local/               # Room database, entities, DAOs
│   │   │   │   ├── media/               # MediaStore and SAF discovery
│   │   │   │   ├── preferences/         # DataStore preferences
│   │   │   │   └── work/                # WorkManager scan worker/scheduler
│   │   │   ├── domain/
│   │   │   │   ├── model/
│   │   │   │   └── repository/
│   │   │   ├── navigation/
│   │   │   ├── presentation/
│   │   │   │   ├── components/
│   │   │   │   ├── equalizer/
│   │   │   │   ├── home/
│   │   │   │   ├── library/
│   │   │   │   ├── playlists/
│   │   │   │   └── settings/
│   │   │   └── ui/theme/
│   │   └── res/                         # Localized resources and launcher art
│   └── src/test/                        # Unit tests
├── gradle/
│   ├── libs.versions.toml
│   └── wrapper/gradle-wrapper.properties
└── project_docs/                        # Project specifications and status
```

## Main Phase 3 files
- `data/local/MusicLibraryDatabase.kt`: Room database singleton.
- `data/local/MusicTrackEntity.kt`: indexed audio metadata record.
- `data/local/ScanCheckpointEntity.kt`: per-volume incremental scan timestamp.
- `data/local/MusicTrackDao.kt`: catalog reads/upserts/deletions.
- `data/local/ScanCheckpointDao.kt`: scan checkpoint persistence.
- `data/media/MediaStoreMusicScanner.kt`: incremental MediaStore scanner and SAF fallback traversal.
- `data/work/MusicIndexWorker.kt`: background scan execution.
- `data/work/MusicIndexScheduler.kt`: unique and periodic work scheduling.
- `presentation/library/MusicLibraryViewModel.kt`: Room/WorkManager state for Library UI.

## Phase 7 additions
- `data/local/PlaylistEntities.kt` — playlist, playlist membership, favorites, and playlist count projection.
- `data/local/PlaylistDao.kt` — persistent playlist/favorite operations and reactive queries.
- `presentation/library/MusicLibraryViewModel.kt` — search, sort, favorites, playlist actions, and library state.
- `presentation/playlists/PlaylistsScreen.kt` — playlist list/detail, create, rename, delete, play, and remove actions.


## Phase 9 additions
- `data/stats/` — playback history entity and aggregate DAO.
- `presentation/statistics/` — statistics state and insights screen.
- `widget/` — home-screen widget provider and internal action receiver.
- `res/layout/widget_music_player.xml` and `res/xml/music_player_widget_info.xml` — widget layout and provider metadata.
