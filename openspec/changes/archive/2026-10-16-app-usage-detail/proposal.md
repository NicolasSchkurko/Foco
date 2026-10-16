# Proposal — Per-app usage detail with a day selector

## Why

The ranking rows do nothing when tapped (`app/src/main/java/com/foco/app/ui/dashboard/AppUsageRow.kt`). The user wants to open one app and understand *when* its time was spent — an hourly breakdown plus the day's details — and to move across recent days.

## What Changes

- Tapping an app in the ranking opens its detail (Navigation Compose; the dashboard stays the start destination and system back returns to it).
- The detail shows, for the chosen app and day: total time, hourly bars (same representation as the day chart), session count, first open and last use times, average and longest session, and the chronological list of the day's sessions.
- Day selector: ‹ day › arrows covering today and the previous days the system retains (7-day window); arrows disable at the bounds; a day without data shows an explicit empty state.
- Data: `UsageRepository.loadToday()` generalizes to `loadDay(date)` — same session pipeline, window `[day start, min(day end, now)]`, an open session closes at the day's end for past days. No persistence: previous days come from `UsageStatsManager` event retention — about the last week ("events are only kept by the system for a few days"); older history arrives with Phase 2 (Room).
- Non-goals: persistence or history beyond system retention; cross-day comparisons; per-app notifications.

## Expected Outcome

Understand the *when* of one app: the detail shows which hours the day's usage falls in and every number adds up from the raw sessions. It was worth it when you can tell the story of an app's day without guessing.

## Alternatives Considered

| Option | Summary | Why not chosen |
|---|---|---|
| Previous days: `UsageStatsManager` per day (~7-day window) — **chosen** | Small: `loadDay(date)` reuses the session pipeline; no persistence; reversible | Hard limit: system event retention (~a week); older days need Phase 2 |
| Previous days: Room now | Unlimited own history going forward | Doubles the change (entities, DAO, capture, migrations) and cannot recover days the system already evicted |
| Day selector: ‹ day › arrows — **chosen** | Compact; sequential navigation fits a ~7-day window | More taps to jump several days |
| Day selector: horizontal chips | Whole week at a glance, one tap | Extra row of chrome |
| Day selector: Material DatePicker | Standard for large ranges | Heavy for ~7 useful days |
| Navigation: `Navigation Compose` — **chosen** | Formal back stack and typed routes; scales as screens grow | New dependency |
| Navigation: local state + `BackHandler` | Zero dependencies; matches the current permission switch | Manual state to manage as screens multiply |

## Capabilities

### New Capabilities

(none)

### Modified Capabilities

- `usage-tracking`: adds the per-app detail view, day selection within system retention, and the repository's day-window generalization.

## Impact

- `app/src/main/java/com/foco/app/ui/**` — clickable rows, detail screen (header, selector, chart reuse, summary, session list), NavHost wiring
- `app/src/main/java/com/foco/app/data/**` — `loadDay(date)` + available-days window
- `app/build.gradle.kts`, `gradle/libs.versions.toml` — `androidx.navigation:navigation-compose` (the Compose UI test infra already landed with `dashboard-auto-refresh`)
- `app/src/test/**`, `app/src/androidTest/**` — detail metrics, day selection, navigation tests
- Ordering: independent of `dashboard-auto-refresh` and `layered-structure` (human decision); `x-deps` only on `session-based-metrics`, the session primitive this builds on (already verified)
