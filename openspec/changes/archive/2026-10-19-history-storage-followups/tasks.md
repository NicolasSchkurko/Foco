# Tasks

## 1. The reading contract

- [x] 1.1 `AvailableDays.kt`: `finishedDays(today, windowDays = 7)` = `availableDays(...).drop(1)`, so the "today is still being built" rule has a name.
- [x] 1.2 `SessionHistoryStore.consolidate` uses `finishedDays(today)`.
- [x] 1.3 `SessionHistoryStore` drops the `zone` parameter; `rawCutoff` uses `ZoneId.systemDefault()`, the calendar the reader uses too.

## 2. Tests

- [x] 2.1 `HistoryWeekTest`: the window is the seven days ending at the requested day (both bounds), and an empty store yields nothing.
- [x] 2.2 `HistoryRetentionTest`: the empty-day test now runs the second consolidation and asserts the day is stored (a retry that is actually exercised).
- [x] 2.3 `HistoryFakes.kt`: the two fake DAOs move here; the three test classes share them.

## 3. Trace and record

- [x] 3.1 `MainActivity.consolidateHistory`: log the failure with `Log.w` before the next start retries.
- [x] 3.2 Delta spec: ADDED *Stored totals are read as a seven-day window*; MODIFIED *The day's data outlives the platform's retention* with the retry scenario.
- [x] 3.3 ADR in `DECISIONS.md` recording the closure of the five findings.

## 4. Verify

- [x] 4.1 `gradlew.bat test` green.
- [x] 4.2 `node scripts/gates/gate-check.mjs --run-tests`.
