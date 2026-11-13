# Phase Plan

1. **Foundation and design system** — project configuration, localization, theme tokens, settings persistence and navigation shell. Source created; build verification still pending.
2. **Premium UI completion** — responsive home/library/player layouts and interaction design foundation. Source created; emulator verification still pending.
3. **Music discovery and indexing** — MediaStore/SAF sources, internal + SD card selection, Room catalog, incremental scanning and background work. Source implemented; static checks passed; Android build/runtime verification pending.
4. **Playback engine** — Media3 playback, background service, media session, notification, lock-screen controls and audio focus. Source implemented; build/runtime verification pending.
5. **Now Playing experience** — full-screen player, live progress/seeking, transport controls, shuffle/repeat, vinyl-inspired motion and transitions. Source implemented; static checks passed; build/runtime verification pending.
6. **Audio processing** — equalizer and supported device effects, presets and per-track settings. Implemented in source; device verification pending.
7. **Library management** — Room-backed playlists and favorites, live search, sorting, favorites filter, and playlist playback. Implemented in source; static verification complete; Android build/runtime verification pending.
8. **Metadata and lyrics** — genuine embedded artwork extraction, in-app metadata overrides, imported local LRC lyrics and playback synchronization. Implemented in source; static checks passed; Android build/runtime verification pending.
9. **Statistics and personalization** — listening insights, widgets, sleep timer and advanced appearance controls. Implemented in source; static verification performed; Android build/runtime verification pending.
10. **Quality and release** — integration review, compatibility, performance, accessibility, security, tests and Play release preparation. Code-level review pass completed; Android build, schema/migration verification, runtime/device tests and release preparation remain pending.

Phase transitions require user approval. Phase numbers and boundaries may be refined only with approval.


## Phase 6 status
Implemented in source: session-attached equalizer, presets, BassBoost, Virtualizer, and the equalizer UI. Device support varies; see `PHASE_6_AUDIO_PROCESSING.md`.
