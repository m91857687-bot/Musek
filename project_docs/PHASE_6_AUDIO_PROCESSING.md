# Phase 6 — Audio Processing

## Implemented
- Added `AudioEffectsManager`, owned by `MusicPlaybackService` and attached to the active ExoPlayer audio session.
- Added platform Equalizer controls using the actual band count and frequency centers exposed by the Android audio effect implementation.
- Added Flat, Bass, Vocal, and Treble presets. Preset values are mapped across the device's available bands and clamped to the device's supported millibel range.
- Added BassBoost and Virtualizer controls when the platform/device exposes those effects.
- Added a same-process bridge for the Compose screen to control effects without creating a second audio session.
- Replaced the previous Equalizer placeholder screen with functional sliders, preset controls, and capability-aware unavailable states.
- Added `MODIFY_AUDIO_SETTINGS` permission and English, Arabic, and Spanish strings.

## Platform constraints
- Android audio effect availability and band count are implementation/device dependent. The UI deliberately reports actual available bands rather than claiming every device has ten hardware bands.
- Effects are attached to the current ExoPlayer audio session. They may be unavailable until the playback service has created an audio session.
- These controls are session-level. Persisted user presets, per-track profiles, and device-by-device tuning are not implemented in this phase.
- BassBoost and Virtualizer may be unavailable or may be provided by the device's audio effect chain; unsupported controls are disabled in the UI.

## Verification performed
- Source-level review of the new manager, service integration, and Compose screen.
- XML parsing and translation-key parity checks are recorded in `VERIFICATION_REPORT_PHASE_6.md`.
- Gradle and emulator/device execution were intentionally not run.
