# Tasks — prune-stored-history

Order: the cutoff and the three deletes first (an off-by-one there would delete the oldest day the
UI still shows), then the count, then the verification pass. Lands after `consolidated-day-marker`.

## 1. One policy: prune everything outside the window (riskiest)

- [x] In `data/SessionHistoryStore.kt`, stop sharing one number: `SIX_DAYS` keeps meaning the read window (`week`, `:39-41`); add a guard constant for pruning so the cutoff is today minus seven days (keep today and the previous seven; delete today minus eight and older).
- [x] Add `deleteBefore(day: String)` to `DailyAggregateDao` and `ConsolidatedDayDao` — `DELETE ... WHERE day < :cutoff`, the ISO text order `totalsFor` already relies on.
- [x] In `consolidate` (`:32-37`), keep the order *store first, prune last*: after the pending loop, prune `session_record` at the millisecond cutoff **and** the two day-keyed tables at the same day, so a failed run still prunes nothing.
- [x] Update the stale headers: `SessionRecordDao` ("seven-day window"), `DailyAggregateDao` ("survives pruning"), `ConsolidatedDayDao` ("No delete: retention keeps it").
- [x] Extend `HistoryRetentionTest`: totals and markers older than the guard are deleted; the guard day survives in all three tables; pre-existing rows outside the window disappear on the next run.
- [x] Extend `ConsolidationRetryTest`: a run that fails midway still deletes nothing.

## 2. The unlock count leaves with its day

- [x] Extend `ConsolidatedDayTest`: a count inside the window reads back; after its day leaves the window and a run prunes, reading it answers "none stored".
- [x] Since this lands after `consolidated-day-marker`, take its archived requirement text as the base and replace only the "never pruned" clause (keep its migration scenarios and its `[@test]` paths).

## 3. Verification

- [x] Correct `docs/mechanisms/daily-unlock-count.md:126-129` ("never deleted; pruning only touches `session_record`") — it is stale the moment this lands.
- [x] `gradlew.bat test` green (retention, consolidation, day selection, week): 119 tests, 0 failed.
- [ ] Emulator check: seed rows older than the guard, run a consolidation, confirm all three tables shrink and the ring still shows seven days with their totals and counts.
- [x] `openspec validate prune-stored-history --strict`, `node scripts/gates/gate-check.mjs`, `node scripts/gates/build-board.mjs`.

No schema change: version stays 3, no migration, no new schema JSON.
