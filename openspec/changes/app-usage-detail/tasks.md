# Tasks

## 1. Data: day windows (the base)

- [ ] 1.1 Generalize `UsageRepository.loadToday()` to `loadDay(date)` (window `[day start, min(day end, now)]`; for past days an open session closes at the day's end); `loadToday()` delegates for today; verify the existing suite and the dashboard stay green/unchanged.
- [ ] 1.2 Add the pure available-days helper (today + previous days within the retention window — 7 by default) and `DaySelectionTest` (window contents, no future days, clamp at the oldest); verify `gradlew.bat test`.

## 2. Detail metrics (pure first)

- [ ] 2.1 `AppDetailMetricsTest` (red first): per-app filter, hourly distribution (reuses `hourlyDistribution`), total, session count, first open, last use, average and longest session, chronological list; then implement the derivations (e.g. `ui/metrics/AppDetail.kt`).
- [ ] 2.2 Empty-day case: explicit "no data" result, covered in the same test.
- [ ] 2.3 Verify `gradlew.bat test` green.

## 3. UI: navigation and detail screen

- [ ] 3.1 Add `androidx.navigation:navigation-compose` to the version catalog and the module; if the androidTest infrastructure does not exist yet (because `dashboard-auto-refresh` has not been applied), add the Compose UI test dependencies and runner config in the same step; verify `gradlew.bat assembleDebug`.
- [ ] 3.2 Wire Navigation Compose: ranking rows become clickable and navigate to the detail route with the package as argument; system back returns to the dashboard; the permission-based onboarding↔app switch stays as the outer gate.
- [ ] 3.3 Build the detail screen: header (icon, label, total), ‹ day › selector (bounds from 1.2), hourly bars reusing the existing chart, summary (total, sessions, first/last, average, longest), chronological session list, per-day empty state; verify on device (today, yesterday, bounds, empty day).
- [ ] 3.4 `AppDetailNavigationTest` (instrumented): tap a row → detail opens; back → dashboard. Verify with `gradlew.bat connectedDebugAndroidTest` on the emulator.

## 4. Integration

- [ ] 4.1 Device pass with real activity: the numbers, chart and session list match the raw sessions of the day; arrows behave at the bounds.
- [ ] 4.2 Run the full gate with green evidence (`node scripts/gates/gate-check.mjs --run-tests`) and `gradlew.bat assembleDebug`; update `gitref` to the branch tip.

## Workflow follow-up

- Review the apply report, merge, and archive with `gate-check --pre-archive app-usage-detail`.
- If this lands before `layered-structure`, files go to the current layout and the refactor moves them; if after, place them directly in the layered layout (`ui/dashboard/`, `data/`).
- `dashboard-auto-refresh` stays independent (human decision): whichever change applies second adapts to the other's tree.
