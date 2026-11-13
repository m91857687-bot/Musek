# Phase 7 — Library Management

## Scope delivered in source
- Persistent playlists stored in Room.
- Playlist membership stored by stable `trackKey`; membership rows cascade when a playlist is deleted. Track references are intentionally not foreign-keyed to `music_tracks`, so a temporary media scan deletion does not destroy a user's playlist membership.
- Persistent favorites stored independently from the media catalog.
- Search by title, artist, and album.
- Sort by title, artist, album, duration, and newest modified date.
- Favorites-only library filter.
- Add tracks to a playlist from the library; remove tracks from a playlist.
- Create, rename, and delete playlists; duplicate names are rejected case-insensitively.
- Playlist track count and playback queue integration.

## Database
- Room schema version increased from 1 to 2.
- Explicit `MIGRATION_1_2` creates `playlists`, `playlist_tracks`, and `favorite_tracks` and their indexes.
- No destructive migration fallback was added.

## Important behavior
- Search, sorting, favorites, and playlist lists are observed as Room Flows; the UI updates when database rows change.
- Search query is applied in SQLite rather than filtering only the first 200 tracks in memory.
- Playlist rows reference the catalog's stable `trackKey`; playlist queries show only tracks currently present in the catalog. If a file temporarily disappears and is later re-indexed with the same key, its playlist membership can become visible again.
- Adding the same track to the same playlist is idempotent.
- Playlist membership order is append order; drag-and-drop reordering is not part of this phase.

## Static verification
- XML parsing and translation-key parity checked across English, Arabic, and Spanish.
- Android resource references checked against the default strings resource.
- SQLite migration statements and the search/sort/favorites query checked against a local SQLite database fixture.
- ZIP integrity checked after packaging.

## Not verified
- No Gradle build, APK generation, emulator run, or physical-device test was performed. This report describes source changes and static checks only.
