# Tasks

## 1. The source decision (pure, testable, first)

- [ ] 1.1 `data/DaySource.kt`: `sealed` or `enum` with the three cases — `Live` (today), `Stored` (a day the store has), `Absent` (neither).
- [ ] 1.2 `data/DetailDaySource.kt`: `daySource(date, today, storedDays)` — pure, no Android types. Today is always `Live`; a date in `storedDays` is `Stored`; otherwise `Absent`.
- [ ] 1.3 `app/src/test/java/com/foco/app/data/DetailDaySourceTest.kt`: the three branches, including a stored day the platform no longer serves (the point of the change). Write it **before** the implementation and watch it fail.

## 2. The store read

- [ ] 2.1 `SessionHistoryStore.sessionsFor(date)`: `SessionRecordDao.sessionsBetween(dayStart, dayEndExclusive)` mapped to `UsageSession`, the shape the detail already derives from.
- [ ] 2.2 Empty result is a real answer (the day has no stored rows), not an error: the screen's empty state already exists and stays.

## 3. Bounds and wiring

- [ ] 3.1 `domain/AvailableDays.kt`: derive the window from the stored days plus today (still at most seven, the raw retention), instead of "today minus six" by assumption. Keep the `require` guard and the ordering.
- [ ] 3.2 `ui/detail/AppDetailScreen.kt`: load today's sessions with `loadDay` and a past day's with `sessionsFor`; keep the existing derivations untouched (`deriveDayMetrics`, `AppDayDetail`).
- [ ] 3.3 Give the screen a seam for tests (a store/repository parameter with a real default, like `App(isUsageAccessGranted)`), so an instrumented test can feed a fixed day.

## 4. Tests

- [ ] 4.1 `DaySelectionTest`: red with the old bounds (a store missing the sixth previous day), green with the stored window.
- [ ] 4.2 `AppDetailNavigationTest`: a stored day outside the platform window still renders its total, chart and sessions.

## 5. Verify

- [ ] 5.1 `openspec validate --all --strict` and `node scripts/gates/gate-check.mjs --run-tests`.
- [ ] 5.2 Instrumented run for 4.2 (the known `UnlockCountTest` flake stays unrelated).

## Workflow follow-up

- `tech_brief: true`: `walkthrough.md` + comprehension sign-off before archiving.
- Leave `status: draft` until the human accepts; `week-screen` declares a dep on this change, so it lands after it is `verified`.
