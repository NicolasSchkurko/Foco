# Tasks

## 1. The stored datum

- [x] 1.1 `data/db/DailyUnlock.kt`: entity `daily_unlock` — the day as an ISO text primary key (the shape `DailyAggregate` uses) and the count. One type per file, named after it.
- [x] 1.2 `data/db/DailyUnlockDao.kt`: the `REPLACE` upsert and the read for one day; **no delete**, so pruning cannot touch it.
- [x] 1.3 `data/db/FocoDatabase.kt`: `version = 2`, register the entity and the DAO, and a `Migration(1, 2)` that creates the table; `exportSchema` keeps the baseline and no destructive fallback is configured.
- [x] 1.4 The exported schema `app/schemas/com.foco.app.data.db.FocoDatabase/2.json` is produced by the build and committed next to `1.json`.

## 2. The writer and the reader

- [x] 2.1 `SessionHistoryStore.consolidateDay`: store the day's unlock count with its totals — the count comes from the same `readDay(day)` it already calls (`DayData.unlockTimes.size`), so there is no second platform read.
- [x] 2.2 `SessionHistoryStore`: a read for one day's count, answering **null when the day has no stored count** (a day consolidated before this change, or never consolidated) and the number otherwise (`0` is a real answer). No requirement forces a name; keep it next to `sessionsFor`.

## 3. Tests

- [x] 3.1 `StoredUnlocksTest` (JVM, over the DAO fakes): consolidating a day stores its count; reading it back answers it; `0` unlocks answers `0`; a day that was never consolidated answers null; a second consolidation of the same day does not duplicate or lose the count.
- [x] 3.2 `UnlockMigrationTest` (device, `MigrationTestHelper`): v1 → v2 keeps the sessions and totals written by version 1 and adds the new table (empty, so the day reads as not stored). This is the first bump, and `schema-drift-guard` recorded that the migration's data-survival test belongs to it.
- [x] 3.3 `HistoryFakes.kt`: a fake for the new DAO, so the store's logic keeps running on the JVM (the same reason the other two fakes exist).
- [x] 3.4 The three existing store tests (`HistoryConsolidationTest`, `HistoryRetentionTest`, `HistoryWeekTest`) gain the new fake: the store's constructor gains a DAO, so every caller must compile.

## 4. Verify

- [x] 4.1 `openspec validate --all --strict` and `node scripts/gates/gate-check.mjs --run-tests`.
- [x] 4.2 The migration test on device (`connectedDebugAndroidTest`; the known `UnlockCountTest` flake stays unrelated). `room-testing` is a test-only dependency, pinned to the Room version already in `gradle/libs.versions.toml`.
- [x] 4.3 The device test could not start at first: Room 2.8's schema bundles need `kotlinx.serialization` 1.8 and the classpath resolved 1.7.3 (via lifecycle, copied into androidTest by AGP's consistent resolution) -> `AbstractMethodError: typeParametersSerializers()`. Aligned with `kotlinx-serialization-bom` 1.8.1; `UnlockMigrationTest` now passes on API 36 (`OK (1 test)`). Recorded in `DECISIONS.md` and in the walkthrough, since it is a shipped-classpath version bump.

## Workflow follow-up

- `tech_brief: true`: `walkthrough.md` + comprehension sign-off before archiving (a second table, a version bump, a write riding the existing consolidation, and a test dependency).
- This change **reverses** the non-goal recorded by the archived `unlock-counter`; the reversal is the human's decision of 2026-10-19, recorded in `DECISIONS.md`.
- `week-screen` declares a dep on this change and lands after it is `verified`; showing a past day's count belongs there.
