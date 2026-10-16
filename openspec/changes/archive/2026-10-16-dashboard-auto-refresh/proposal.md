# Proposal — Fresh dashboard data on every app open

## Why

The dashboard loads usage data once when it first appears and then only when the user taps "Actualizar" (`DashboardScreen.kt:61-65`, `:90-92`). Returning to the app from another app or the home screen with the permission already granted shows stale numbers: `MainActivity` re-checks only the permission on `ON_RESUME` (`MainActivity.kt:38-48`). The user wants to open the app and always see fresh data, without doing anything.

## What Changes

- The dashboard reloads its data automatically whenever the app returns to the foreground (and on first show).
- While the app is in the foreground, data refreshes periodically (~30 s); the timer pauses when the app is not visible.
- The manual "Actualizar" button is removed (no longer needed).
- Non-goals: background refresh (WorkManager / hidden work); persistence (Phase 2).

## Expected Outcome

"Open the app and it is already fresh": come back from anywhere and the numbers already reflect recent usage, with no user action. We will know it was worth it when the "Actualizar" button is never missed.

## Alternatives Considered

| Option | Summary | Why not chosen |
|---|---|---|
| A. Lifecycle effects inside the dashboard — **chosen** | ON_RESUME effect + RESUMED-scoped periodic loop in `DashboardScreen`; no new dependencies | — (human decision) |
| B. Signal from `MainActivity` | The existing ON_RESUME observer bumps a counter passed down | Couples permission logic with dashboard data; parameter plumbing; the periodic loop still needs the screen |
| C. WorkManager background job | System-scheduled periodic refresh | Solves a different problem (data in the background); battery/complexity with no benefit while data is only consumed on screen |

## Capabilities

### New Capabilities

(none)

### Modified Capabilities

- `usage-tracking`: adds dashboard-freshness requirements (automatic refresh on foreground return, periodic refresh while visible, no manual control). Note: `usage-tracking` is introduced by the in-flight `session-based-metrics` change; `x-deps` sequences this change after it.

## Impact

- `app/src/main/java/com/foco/app/ui/` — `DashboardScreen.kt` (refresh effects, button removal, injectable refresh interval/loader seam for tests)
- `app/src/androidTest/` — new instrumented test (the repo's first)
- `app/build.gradle.kts`, `gradle/libs.versions.toml` — Compose UI test dependencies
