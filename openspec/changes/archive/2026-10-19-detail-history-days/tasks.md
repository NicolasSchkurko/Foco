# Tasks

## 1. The source decision (pure, testable, first)

- [x] 1.1 `domain/DaySource.kt`: `sealed` or `enum` with the three cases — `Live` (today), `Stored` (a day the store has), `Absent` (neither). Landed as an `enum` in **`domain/`**, not `data/`: `LayerRulesTest` and the living `code-structure` require a file with no Android imports to live in `domain/`, so the two planned paths moved on apply.
- [x] 1.2 `domain/DetailDaySource.kt`: `daySource(date, today, storedDays)` — pure, no Android types. Today is always `Live`; a date in `storedDays` is `Stored`; otherwise `Absent`.
- [x] 1.3 `app/src/test/java/com/foco/app/domain/DetailDaySourceTest.kt`: the three branches, including a stored day the platform no longer serves (the point of the change). Write it **before** the implementation and watch it fail. (Moved from `data/` to `domain/` with its subject on apply; the delta's `[@test]` follows.)

## 2. The store read

- [x] 2.1 `SessionHistoryStore.sessionsFor(date)`: `SessionRecordDao.sessionsBetween(dayStart, dayEndExclusive)` mapped to `UsageSession`, the shape the detail already derives from; plus `storedDays()` over the existing `DailyAggregateDao` query, which is what bounds the selector.
- [x] 2.2 Empty result is a real answer (the day has no stored rows), not an error: the screen's empty state already exists and stays.

## 3. Bounds and wiring

- [x] 3.1 `domain/AvailableDays.kt`: derive the window from the stored days plus today (still at most seven, the raw retention), instead of "today minus six" by assumption. Keep the `require` guard and the ordering. The result is a **contiguous run** — today and every day back to the oldest stored day inside the window — so a day the store never got is walked over and answered with the empty state (the first cut was a union that skipped those days; changed after the human's review, see §6); `finishedDays` keeps the assumption window untouched, because consolidation backfills off it.
- [x] 3.2 `ui/detail/AppDetailScreen.kt`: load today's sessions with `loadDay` and a past day's with `sessionsFor`; keep the existing derivations untouched (`appDayDetail`).
- [x] 3.3 **Dropped on apply, by decision of the apply's owner:** no seam parameter on `AppDetailScreen`. The instrumented test seeds the app's own store instead (`SessionHistoryStore.forApp` with a fixed reader), which proves the real wiring end to end and leaves no unused production parameter. Recorded in `walkthrough.md` and in the report.

## 4. Tests

- [x] 4.1 `DaySelectionTest`: red with the old bounds (a store missing the sixth previous day), green with the stored window — 7 tests, including the day with no totals inside the window and the `finishedDays` regression for consolidation.
- [x] 4.2 `AppDetailNavigationTest`: a day seeded only in the app's own store (the platform reader contributes nothing for it) still renders its total, chart and sessions — 4 tests green on `medium_phone(AVD) - 16` (API 36), `aDayOnlyTheStoreHasStillShowsItsTotal` among them.

## 5. Verify

- [x] 5.1 `openspec validate --all --strict` (9 passed, 0 failed) and `node scripts/gates/gate-check.mjs --run-tests` (GATE GREEN, 0 errors; `gradlew.bat test` exit 0 — 66 tests, 0 failures, 2 pre-existing skips — recorded as real green evidence).
- [x] 5.2 Instrumented run for 4.2 (the known `UnlockCountTest` flake stays unrelated): `connectedDebugAndroidTest` filtered to `AppDetailNavigationTest` → 4 tests, 0 failures, 0 errors, 33.079 s.

## 6. Hardening after the human's review

- [x] 6.1 The selector walks the window day by day instead of offering only the days with totals: `domain/AvailableDays.kt` returns today and every day back to the oldest stored day. The delta's requirement, its scenarios and `DaySelectionTest` follow, and the instrumented test now proves that one step back is yesterday rather than the stored day three back (which is exactly what the skipped-days cut would do).
- [x] 6.2 A failing store read no longer kills the screen: `ui/detail/AppDetailScreen.kt` reads through `readOr`, which logs and answers a fallback — the stored days degrade to today alone, a day's read degrades to the empty state — and lets `CancellationException` through. It was glue without a test until the audit made it `internal` and gave it one (§7.1).

## 7. The audit's five corrections (H1-H5)

- [x] 7.1 **H1** (behavior no spec described) The degraded read is declared and tested: `readOr` is `internal`, `AppDetailReadTest` (device) proves the fallback is answered and that cancellation still propagates, and the delta carries its own requirement + scenario. No waiver needed.
- [x] 7.2 **H2** (a partial window could freeze) The stored days are now read inside `repeatOnLifecycle(RESUMED)` in `AppDetailScreen.kt` (no new dependency: `lifecycle-runtime-compose` was already there), so a day the startup backfill stores while the detail is open becomes reachable on resume; `AppDetailNavigationTest` proves the window grows by exactly the three days seeded in between.
- [x] 7.3 **H3** (the test asserted its premise) `AppDetailNavigationTest`'s KDoc and task 4.2 no longer claim the platform serves nothing for the day: the test seeds it past the platform reader, which is the part it exercises.
- [x] 7.4 **H4** (the delta under-declared its tests) The requirement now cites the instrumented test: `[@test] app/src/androidTest/java/com/foco/app/ui/AppDetailNavigationTest.kt`.
- [x] 7.5 **H5** (opaque name) `AvailableDays.kt:12` renames `oldestOffered` to `windowFloor`: the value is the floor of the window, not the oldest day offered.

## 8. Re-verify after the corrections

- [x] 8.1 JVM suite green on the corrected tree: `gradlew.bat test` → **67 tests, 0 failures, 2 pre-existing skips** (the 67th is the gap-day test §6.1 added; 5.1 recorded the 66 before it). `openspec validate --all --strict` → 9 passed, 0 failed.
- [x] 8.2 Device, both classes on `medium_phone(AVD) - 16` (API 36): `AppDetailNavigationTest` → **4 tests, 0 failures** (74.456 s, `connectedDebugAndroidTest`), `AppDetailReadTest` → **OK (2 tests)** through the same runner (`am instrument`, 0.164 s). The comma-separated `-Pandroid.testInstrumentationRunnerArguments.class` only ran the first class, so the second is reported from the direct runner invocation.
- [x] 8.3 `node scripts/gates/gate-check.mjs --run-tests` → **GREEN, 0 errors**, new fingerprint recorded; the change returns to `verified` under a new `x-gitref`.

## Workflow follow-up

- `tech_brief: true`: `walkthrough.md` + comprehension sign-off before archiving (the walkthrough is written; the sign-off is the human's, in `/spec-verify`).
- `week-screen` declares a dep on this change, so it lands after it is `verified`.
