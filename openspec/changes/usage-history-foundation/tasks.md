# Tasks

## 1. Prove the build first (the risk is the dependency, not the logic)

- [ ] 1.1 Add `room-runtime` + `room-ktx` and the KSP plugin to `gradle/libs.versions.toml` and `app/build.gradle.kts`, pinning versions compatible with AGP 9.4 / Kotlin 2.4. Nothing else yet.
- [ ] 1.2 Configure `room.schemaLocation` (KSP arg) to `app/schemas/` and `exportSchema = true`; run `gradlew.bat assembleDebug` with **no** database class yet, to prove KSP runs.
- [ ] 1.3 Stop here if KSP does not run: the alternatives (raw `SQLiteOpenHelper`) are in the proposal, and the change does not proceed on a broken toolchain.

## 2. Schema (one type per file, as `OneTypePerFileTest` requires)

- [ ] 2.1 `data/db/SessionRecord.kt`: `@Entity("session_record", primaryKeys = ["packageName", "start"], indices = [Index("start")])`.
- [ ] 2.2 `data/db/DailyAggregate.kt`: `@Entity("daily_aggregate", primaryKeys = ["packageName", "day"])`, day as an ISO date string (readable in queries, sorts correctly).
- [ ] 2.3 `data/db/SessionRecordDao.kt`: `upsert` (`REPLACE`), `sessionsBetween` (half-open on `start`), `pruneBefore`.
- [ ] 2.4 `data/db/DailyAggregateDao.kt`: `upsert`, `storedDays`, `totalsFor`.
- [ ] 2.5 `data/db/FocoDatabase.kt`: `@Database(version = 1)`, the two entities, the two DAOs, `exportSchema = true`.
- [ ] 2.6 Commit `app/schemas/…/1.json`: it is the migration baseline, and `fallbackToDestructiveMigration` is never used (it would erase exactly the history this change exists to keep).

## 3. The store (an adapter, not a policy owner)

- [ ] 3.1 `data/SessionHistoryStore.kt`: takes `SessionRecordDao`, `DailyAggregateDao` and a `(LocalDate) -> DayData` reader, so the logic is JVM-testable with fakes and no Android runtime.
- [ ] 3.2 `consolidate(today)`: offer `availableDays(today)` **without today** (its data is not final), skip the days that already have totals, map the fold's sessions to records, upsert both tables, then prune raw rows older than seven days.
- [ ] 3.3 `week(until)`: read the totals of the last seven days as the shape the Week screen will consume (no UI yet, so nothing else changes).
- [ ] 3.4 Idempotence: a second consolidation of the same day writes the same rows (the composite key does the work).

## 4. Trigger and hardening

- [ ] 4.1 `MainActivity.onCreate`: launch the consolidation on `Dispatchers.IO` once per start (`lifecycleScope`), guarded so it cannot run twice concurrently.
- [ ] 4.2 `AndroidManifest.xml`: `android:allowBackup="false"` on `<application>`.
- [ ] 4.3 `app/src/test/java/com/foco/app/BackupDisabledManifestTest.kt`: same pattern as `BackInvokedCallbackManifestTest` (reads `src/main/AndroidManifest.xml`).

## 5. Tests

- [ ] 5.1 `HistoryConsolidationTest` (JVM, fakes): a day is stored and read back; a day the platform no longer serves still yields its data; consolidating twice leaves identical rows.
- [ ] 5.2 `HistoryRetentionTest` (JVM, fakes): pruning deletes raw rows older than seven days and keeps their totals; a day inside the window is untouched.
- [ ] 5.3 `FocoDatabaseTest` (instrumented): the real SQLite applies both primary keys and the `REPLACE` semantics — the part the fakes cannot prove.
- [ ] 5.4 Red first where it matters: 5.1/5.2 against a store that does not prune / does not skip stored days, then green.

## 6. Verify

- [ ] 6.1 `openspec validate --all --strict` and `node scripts/gates/gate-check.mjs --run-tests`.
- [ ] 6.2 Instrumented run on the emulator: `FocoDatabaseTest` (expect the known `UnlockCountTest` flake to stay unrelated).
- [ ] 6.3 Confirm by hand that the database file exists after a start and that a second start adds no duplicate rows.

## Workflow follow-up

- Review the apply report, then `--pre-archive` (the living spec changes at **archive** time).
- `tech_brief: true`: write `walkthrough.md` (mechanism, physical traceability, failure modes) and record the comprehension sign-off before archiving.
- No Worker and no Week screen here; both are follow-up changes.
