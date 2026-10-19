# Tasks

## 1. The derivations first (pure, JVM, no Compose)

- [x] 1.1 `ui/metrics/WeekDayBar.kt`: one bar's data — the date, the total, and whether it is `Live`, `Stored` or `Absent`. Absence is a case, not a zero.
- [x] 1.2 `ui/metrics/AppWeekTotal.kt`: an app's total over the window (package, millis, days used).
- [x] 1.3 `ui/metrics/WeekMetrics.kt`: `deriveWeekMetrics(days: List<DayTotals>, today: DayData, zone)` — bars in order, ranking descending, absent days preserved. No Android types.
- [x] 1.4 `WeekMetricsTest` (JVM): seven bars; today live and the six previous stored; an absent day stays absent; the ranking sums across days and sorts descending; a week with only today works.

## 2. The screen

- [x] 2.1 `ui/week/WeekScreen.kt`: the seven bars (labels: weekday + day), the ranking (reuse the row composition the dashboard uses), an empty state when the whole window has no data.
- [x] 2.2 Bars are tappable and carry the date; the ranking rows carry the package (the day view opens the detail).
- [x] 2.3 Read from the store (`week()` from the foundation) for the six previous days and from `loadDay` for today; on return to the screen, refresh only the live day.

## 3. Navigation and the day view

- [x] 3.1 `FocoNavHost.kt`: add `week` and `day/{date}`; the back action returns to the previous screen. The day view's app rows open `detail/{packageName}?date={date}` — `AppDetailScreen` starts on that day and its selector still walks the stored window; without a date it starts on today, unchanged.
- [x] 3.2 `DashboardScreen.kt`: accept an optional date — absent = today (current behavior, auto-refresh intact); present = read-only, data from `sessionsFor(date)` plus that day's stored unlock count (`unlockCount(date)` from `stored-day-unlocks`), no refresh effect. Reuse the composition; do not copy it.
- [x] 3.3 Dashboard entry: a control next to the settings icon opens the week view; strings in base English and `values-es`.
- [x] 3.4 Keep the dashboard's existing contract intact: `DashboardRefreshTest` must stay green for today.

## 4. Instrumented tests

- [x] 4.1 `WeekScreenTest`: entry from the dashboard and back; seven bars with a seeded store; tapping a bar opens that day's view with its total and ranking; a ranking row opens the app detail for that day.
- [x] 4.2 Seed through the dashboard's existing `loadDay` seam for the live day and through the app's **real** store for stored days (as `AppDetailNavigationTest` does), never through the real platform. The seam the detail change planned was dropped on apply (its task 3.3); the dashboard's own seam is the one that exists.

## 5. Verify

- [x] 5.1 `openspec validate --all --strict` and `node scripts/gates/gate-check.mjs --run-tests`.
- [x] 5.2 Instrumented run for 4.1 (the known `UnlockCountTest` flake stays unrelated).
- [x] 5.3 Both stems verified with the theme in light and dark (the bars are a new surface).

## Adjustments (apply, 2026-10-19 — the plan against the real tree)

- **1.3 — the input type**: `DayTotals` would need a file of its own, outside the change's targets, and
  the store already speaks the same shape; the derivation takes `storedTotals: List<DailyAggregate>`
  (the store's `week()` read) plus `today: DayData` and `todayDate: LocalDate`. The `zone` parameter
  drops: nothing in the week's derivation needs it (today's total comes from the sessions, the bars'
  dates from `today` and the stored day keys).
- **1.3 — `WeeklyMetrics.kt` declares one type**: `OneTypePerFileTest` allows one top-level type named
  after the file, so `WeekMetrics` lives in `WeekMetrics.kt`; the `State` of a bar is nested in
  `WeekDayBar` (nested declarations are exempt).
- **2.1 / 2.2 — the shared row**: `code-structure` forbids a `ui/<feature>/` file importing another
  feature, and the row now serves two screens, so `AppUsageRow` moves to `ui/common/` (the contract's
  "widgets used by more than one screen"), and its caption becomes a parameter: the week ranks totals
  whose last-used time it does not have.
- **2.3 / 3.2 — the degraded read**: `readOr` (today in `ui/detail`, used by three screens after this
  change) moves to `ui/common/`.
- **3.2 — the store's read name**: the day's stored unlock count is `SessionHistoryStore.unlocksFor(date)`
  (the task text says `unlockCount`; `stored-day-unlocks` shipped `unlocksFor`), and `null` counts as `0`
  in the line — a day whose count was not stored has no sessions, so no unlocks.
- **3.2 — the dated view of today**: a bar can be today's, and today is never stored; the dated view of
  today reads live, which is also what a tapped live bar showed.
- **4.1 / 4.2 — inject the live day through the nav root**: the instrumented test drives the whole chain
  (dashboard → week → day → detail) through `FocoNavHost`, so the dashboard's existing `loadDay` seam
  travels through it (a defaulted parameter, like `App(isUsageAccessGranted)`, documented as the tests'
  seam). The stored days are seeded in the app's real store; the platform is never read.
- **5.3 — light/dark**: the bars are new surfaces; verify both stems.

## Workflow follow-up

- `tech_brief: true`: `walkthrough.md` + comprehension sign-off before archiving.
- Deps: lands after `usage-history-foundation`, `detail-history-days` and `stored-day-unlocks` are `verified`; leave `status: draft` until the human accepts.
- Added after acceptance (2026-10-19, human): the day view's rows open the detail **for that day**, so `ui/detail/AppDetailScreen.kt` joins the targets; the dated day view shows the stored unlock count, which is why `stored-day-unlocks` is a dep.
