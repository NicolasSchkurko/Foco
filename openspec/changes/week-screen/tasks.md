# Tasks

## 1. The derivations first (pure, JVM, no Compose)

- [ ] 1.1 `ui/metrics/WeekDayBar.kt`: one bar's data — the date, the total, and whether it is `Live`, `Stored` or `Absent`. Absence is a case, not a zero.
- [ ] 1.2 `ui/metrics/AppWeekTotal.kt`: an app's total over the window (package, millis, days used).
- [ ] 1.3 `ui/metrics/WeekMetrics.kt`: `deriveWeekMetrics(days: List<DayTotals>, today: DayData, zone)` — bars in order, ranking descending, absent days preserved. No Android types.
- [ ] 1.4 `WeekMetricsTest` (JVM): seven bars; today live and the six previous stored; an absent day stays absent; the ranking sums across days and sorts descending; a week with only today works.

## 2. The screen

- [ ] 2.1 `ui/week/WeekScreen.kt`: the seven bars (labels: weekday + day), the ranking (reuse the row composition the dashboard uses), an empty state when the whole window has no data.
- [ ] 2.2 Bars are tappable and carry the date; the ranking rows carry the package (the day view opens the detail).
- [ ] 2.3 Read from the store (`week()` from the foundation) for the six previous days and from `loadDay` for today; on return to the screen, refresh only the live day.

## 3. Navigation and the day view

- [ ] 3.1 `FocoNavHost.kt`: add `week` and `day/{date}`; the back action returns to the previous screen.
- [ ] 3.2 `DashboardScreen.kt`: accept an optional date — absent = today (current behavior, auto-refresh intact); present = read-only, data from `sessionsFor(date)`, no refresh effect. Reuse the composition; do not copy it.
- [ ] 3.3 Dashboard entry: a control next to the settings icon opens the week view; strings in base English and `values-es`.
- [ ] 3.4 Keep the dashboard's existing contract intact: `DashboardRefreshTest` must stay green for today.

## 4. Instrumented tests

- [ ] 4.1 `WeekScreenTest`: entry from the dashboard and back; seven bars with a seeded store; tapping a bar opens that day's view with its total and ranking; a ranking row opens the app detail for that day.
- [ ] 4.2 Seed through the screen's data parameter (the seam the detail change adds), not through the real platform.

## 5. Verify

- [ ] 5.1 `openspec validate --all --strict` and `node scripts/gates/gate-check.mjs --run-tests`.
- [ ] 5.2 Instrumented run for 4.1 (the known `UnlockCountTest` flake stays unrelated).
- [ ] 5.3 Both stems verified with the theme in light and dark (the bars are a new surface).

## Workflow follow-up

- `tech_brief: true`: `walkthrough.md` + comprehension sign-off before archiving.
- Deps: lands after `usage-history-foundation` and `detail-history-days` are `verified`; leave `status: draft` until the human accepts.
