# Tasks

## 1. Data: day windows (the base)

- [x] 1.1 Generalize `UsageRepository.loadToday()` to `loadDay(date)` (window `[day start, min(day end, now)]`; for past days an open session closes at the day's end); `loadToday()` delegates for today; verify the existing suite and the dashboard stay green/unchanged.
- [x] 1.2 Add the pure available-days helper (today + previous days within the retention window — 7 by default) and `DaySelectionTest` (window contents, no future days, clamp at the oldest); verify `gradlew.bat test`.

## 2. Detail metrics (pure first)

- [x] 2.1 `AppDetailMetricsTest` (red first): per-app filter, hourly distribution (reuses `hourlyDistribution`), total, session count, first open, last use, average and longest session, chronological list; then implement the derivations (`ui/metrics/AppDetail.kt` + `AppDayDetail.kt`).
- [x] 2.2 Empty-day case: explicit "no data" result (`null`), covered in the same test.
- [x] 2.3 Verify `gradlew.bat test` green.

## 3. UI: navigation and detail screen

- [x] 3.1 Add `androidx.navigation:navigation-compose` to the version catalog and the module (the Compose UI test infra already exists). **Note:** `navigation-compose 2.10.x` requires `compileSdk 37`; the project pins the stable 36 → **2.9.8** is used; revisit when the project moves to 37.
- [x] 3.2 Wire Navigation Compose (`ui/FocoNavHost.kt`: `dashboard` + `detail/{packageName}`); ranking rows navigate with the package as argument; system back returns; the permission-based onboarding↔app switch stays as the outer gate in `App`.
- [x] 3.3 Build the detail screen: header (icon, label, total), ‹ day › selector (bounds from `availableDays`), hourly bars reusing `HourChart`, summary (total, sessions, first/last, average, longest), chronological session list, per-day empty state; verified on device (today, Ayer + empty state, back).
- [x] 3.4 `AppDetailNavigationTest` (instrumented): tap a row → detail opens; back → dashboard. Verified green on the API 36 AVD.

## 4. Integration

- [x] 4.1 Device pass with real activity: the numbers, chart and session list match the raw sessions of the day; the selector moves to previous days and the empty state shows.
- [x] 4.2 Run the full gate with green evidence (`node scripts/gates/gate-check.mjs --run-tests`) and `gradlew.bat assembleDebug`; update `gitref` to the branch tip.

## Workflow follow-up

- Review the apply report, merge, and archive with `gate-check --pre-archive app-usage-detail`.
- Deps (`session-based-metrics`, `dashboard-auto-refresh`, `layered-structure`) are archived; `ranking-exclusions` and `app-settings` land after this change (declared deps).
- `findings.md`: both semantic-pass items resolved in this apply.
