# Phase 3 — Source Code Review

## Scope

Reviewed the Phase 3 implementation directly: Room entities/DAOs/database, MediaStore scanner, SAF scanner, WorkManager worker/scheduler, DataStore source preferences, permission and folder-picker integration, Library UI/ViewModel, manifest, and Gradle dependency declarations. No Gradle build or emulator run was performed as requested.

## Corrections made during review

1. **SAF source replacement safety:** the scanner previously deleted the previously selected SAF tree before traversing the newly selected tree. If the new provider/tree failed during enumeration, the previous library source could already have been removed. Deletion of other SAF trees now happens only after the selected tree has been fully traversed and reconciled successfully.
2. **Dropped scan requests:** one-time indexing used `ExistingWorkPolicy.KEEP`. A request arriving while a scan was active could be silently discarded, including a request caused by a source-setting change or a MediaStore update. It now uses `APPEND_OR_REPLACE` so a follow-up scan is queued rather than lost.

## Findings and remaining limitations

- **Permission and SAF selection need device validation.** The runtime permission flow and persisted tree grant are present, but denial, revocation, SD-card removal, and vendor-specific document providers have not been exercised on a device.
- **SAF tags are incomplete by design in this phase.** SAF entries currently use the filename and file facts; artist, album, and duration remain empty/zero until a later metadata-reading phase.
- **SAF enumeration is not truly incremental.** The tree is traversed on each scan. Unchanged rows avoid database writes, but large SD cards may still take time.
- **MediaStore reconciliation reads every ID in each scanned volume.** Metadata reads are incremental, but deletion reconciliation still scales with the number of indexed audio items.
- **The Library currently displays only the first 200 tracks.** The database query is capped at 200 and no paging/infinite-scroll mechanism is wired yet.
- **The scanner's `fullMetadataScan` option currently has no UI or worker path that requests a forced full refresh.** It is retained as an internal capability, but a user-facing recovery/rescan policy should be designed later.
- **No end-to-end test exists for scan correctness.** Unit/instrumentation coverage should be added for additions, edits, deletions, source toggles, SAF replacement failure, and permission loss.

## Static review result

The reviewed source is internally consistent at the source-inspection level after the two corrections above. This is not a Kotlin/Android compilation result and does not establish runtime correctness. Gradle and emulator testing were intentionally not attempted in this review.
