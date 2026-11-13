# Next Task

Phase 10 remains in progress. The code-level integration review pass is complete, but do not mark the project release-ready yet.

Required next validation in an Android-capable environment (after the user's approval to run build tooling):
1. Build debug and release variants and resolve compile/resource/manifest issues.
2. Generate Room schema snapshots and verify migrations from database versions 1 through 4 against real Room-generated schemas.
3. Run unit and instrumentation tests.
4. Test permissions, MediaStore/SAF/SD card, playback service, media notification/lock screen, audio effects, sleep timer, widget controls, and lyrics/artwork on representative Android versions.
5. Profile large-library memory/performance; consider Paging 3 before release.
6. Confirm final application ID, signing, Play listing, privacy policy, data safety, and store assets.

Do not start a new feature phase. Keep the next work within Phase 10 and preserve the user's instruction not to run Gradle until explicitly changed.
