# Tasks — consolidated-day-marker

## 1. The marker

- [x] 1.1 `ConsolidatedDay.kt`: Room entity `consolidated_day`, `day` (TEXT, primary key) and
      `count` (INTEGER, **nullable**); the day key is `LocalDate.toString()`, the same shape the other
      two tables use.
- [x] 1.2 `ConsolidatedDayDao.kt`: `@Insert(onConflict = REPLACE)` upsert, `@Query` for the stored
      days (`SELECT day FROM consolidated_day`), its `Flow` twin, and `countFor(day): Int?`. No delete
      and no prune: the marker joins the totals side of retention.
- [x] 1.3 Delete `DailyUnlock.kt` and `DailyUnlockDao.kt`; nothing reads the old table afterwards.

## 2. The schema

- [x] 2.1 `FocoDatabase`: `version = 3`, `ConsolidatedDay` in `entities`, `dailyUnlocks()` replaced by
      `consolidatedDays()`, and `MIGRATION_2_3`:
      `CREATE TABLE IF NOT EXISTS consolidated_day (day TEXT NOT NULL, count INTEGER, PRIMARY KEY(day))`,
      then `INSERT OR IGNORE ... SELECT DISTINCT day FROM daily_aggregate LEFT JOIN daily_unlock` to
      carry each stored count (and record the days that only had totals), then `DROP TABLE daily_unlock`.
- [x] 2.2 Register `MIGRATION_2_3` on the builder and commit the generated `3.json` baseline. No
      destructive fallback.

## 3. The store

- [x] 3.1 `SessionHistoryStore` takes `ConsolidatedDayDao` instead of `DailyUnlockDao`; its
      constructor sites (the singleton and every test) follow.
- [x] 3.2 `consolidate`: pending = `finishedDays - consolidatedDays.storedDays()`; a day with no
      sessions still writes nothing and stays pending.
- [x] 3.3 `consolidateDay`: sessions → totals → marker with `data.unlockTimes.size`, the marker last;
      the comment says why (it is the write that ends the day).
- [x] 3.4 `storedDays()` and `storedDaysFlow()` read the marker; `unlocksFor(date)` keeps its `Int?`
      contract, now from the marker's count.

## 4. Tests

- [x] 4.1 `ConsolidatedDayTest` (renames and extends `StoredUnlocksTest`): the count is written with
      the marker, zero is a real answer, a missing marker reads `null`, re-consolidating keeps one
      row.
- [x] 4.2 `HistoryConsolidationTest`: the regression — a marker write that fails leaves the day
      pending, and the next run stores the day **and** its count.
- [x] 4.3 `HistoryRetentionTest`, `HistoryWeekTest` and `HistoryFakes` follow the new DAO; the fake
      marker keeps the `MutableStateFlow` stand-in for Room's invalidation.
- [x] 4.4 `ConsolidationMigrationTest` (device): a real version-2 file with sessions, totals and
      counts migrates to 3 — sessions and totals intact, counts carried over, the never-counted day
      reading `null`, `daily_unlock` gone, and `runMigrationsAndValidate` accepting the schema.

## 5. Verification

- [x] 5.1 `gate-check --run-tests` green (JVM suite + the instrumented suite on the emulator).
- [x] 5.2 `openspec validate --all --strict` clean.
- [ ] 5.3 Walkthrough written and the comprehension sign-off recorded by the human.
