# Proposal — Unlock count on the dashboard

## Why

The user wants the number of phone unlocks visible on the dashboard: the screen-time total says *how long*, not *how often* the phone was picked up. The day's keyguard dismissals already travel in the same `UsageEvents` stream the sessions come from — nothing new to fetch.

## What Changes

- The repository's day load grows a primitive: `DayData(sessions, unlockTimes)` — `KEYGUARD_HIDDEN` timestamps from the same query and day window; the sessions contract is unchanged.
- The dashboard subtitle becomes **"Uso en pantalla: X · N desbloqueos"** (N derived in the UI from the timestamps, like every other metric).
- Standard definition (fixed in the spec): an unlock = the keyguard was dismissed (`KEYGUARD_HIDDEN`). On devices without a secure lock, each screen wake counts — the platform's standard behavior.
- Non-goals: unlocks per app, history beyond the current retention window (the count follows the same day window as the sessions).

## Expected Outcome

"Dato confiable y bajo contrato": open the app and read today's unlock count next to the screen time — same query, same day, same reloads as everything else, and a regression breaks a test.

## Alternatives Considered

| Source | Summary | Why not chosen |
|---|---|---|
| **`KEYGUARD_HIDDEN` from the same day query — chosen** (human decision) | Full-day history, zero new dependencies, works offline, testable on the emulator (sleep → wake → dismiss) | On devices without a secure lock it also counts screen wakes — the standard definition |
| `ACTION_USER_PRESENT` broadcast | The classic "user present" signal | Live-only: no history — cannot report *today* when the app opens |
| `KeyguardManager` / screen wakeups | Easy to query | No history / different semantics (wakeups ≠ unlocks) |

## Capabilities

### New Capabilities

(none)

### Modified Capabilities

- `usage-tracking`: the dashboard shows the day's unlock count.

## Impact

- `app/src/main/java/com/foco/app/model/day/DayData.kt` (new) — the day primitive grows
- `app/src/main/java/com/foco/app/data/**` — map `KEYGUARD_HIDDEN`, return `DayData`
- `app/src/main/java/com/foco/app/ui/**` — dashboard subtitle
- `app/src/test/**`, `app/src/androidTest/**` — unlock count test (generated via shell)
- Ordering (pre-declared under the A+ policy): next after `detail-back-ux`; `app-settings` waits on it
- Tech brief (`x-tech_brief: true`): platform SDK (`UsageEvents`) — `walkthrough.md` included; sign-off in `/spec-verify`
