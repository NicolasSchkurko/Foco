# Proposal — The week view

## Why

`PLAN.md` Phase 2 asks for a Week screen (bars per day + totals per app), and the app has no cross-day view at all: the dashboard is **today**, the detail is **one app**. Once `usage-history-foundation` stores the days and `detail-history-days` lets the detail read them, the missing piece is the view that makes the stored history visible.

## What Changes

- A `week` route reached from the dashboard, with the back action returning there.
- `WeekScreen`: seven bars (one per day, the day's total screen time) plus the ranked per-app totals over those seven days.
- Pure derivations in `ui/metrics` (`WeekMetrics`, `WeekDayBar`, `AppWeekTotal`), the same convention the dashboard already follows — the screen composes, it does not compute.
- **Today is live**; the previous six days come from the stored totals. A day with no stored total is shown as **absent**, not as a zero-height bar.
- Tapping a day's bar opens **that day's view**: the existing dashboard composition for a past date, read-only (no auto-refresh). Tapping an app row there opens that app's detail for the day, which now serves stored days.

## State and persistence mechanism

- **Reads**: `daily_aggregate` for the six previous days (the store's week read, from the foundation) and today live from the platform (`loadDay`), because today is not consolidated by design.
- **Day view**: reads the day's raw sessions (the detail change's `sessionsFor`) — the hourly chart and the ranking need sessions, not totals.
- **Writes**: none. This change only reads what the two earlier changes store.
- **Nothing leaves the device**: unchanged — no network permission, no backup.

## Expected outcome

One glance answers "how was my week?", and any bar opens the day with the same numbers the bar showed. We will know it was worth it when the week view replaces "open the detail seven times" as the way to look back.

## Alternatives considered

| Option | Why yes | Why not / status |
|---|---|---|
| **Stored totals for the six previous days + today live; bars open the dashboard in dated mode (chosen)** | Reuses the store the foundation owns and the dashboard’s composition; one source per day; no new screen | The dated dashboard needs a read-only mode, and its rows depend on `detail-history-days` |
| Read everything live from the platform | No dependency; ships now | Shows only what the system retains and duplicates the detail — no history at all — **rejected** |
| Build the week from raw sessions only | Raw exists for seven days anyway | Ignores the aggregates that survive pruning, and re-derives per-app totals the store already has — **rejected** |
| A dedicated day screen instead of the dashboard in dated mode | Cleaner separation of "today" from "a date" | Duplicates the whole composition (total, hourly chart, ranking, rows) for the same output — **rejected**: reuse first |
| Calendar week (Mon–Sun) instead of the last seven days | More natural to read | The first week shows fewer than seven days and the window stops matching the raw retention — **rejected by the human (rolling seven)** |

## Non-goals

- **No calendar weeks, comparisons, goals or alarms**: the window is the last seven days, as chosen.
- **No change to retention** (seven days of raw, totals beyond) or to the detail's contract — both belong to the earlier changes.
- **No tab bar**: the entry is a control in the dashboard, next to settings.

## Impact

- `tech_brief: true`: each number has a source (stored total vs live day) worth a walkthrough.
- Deps: `usage-history-foundation` and `detail-history-days` — it lands after both.
- No protected zone (`openspec/config.yaml` → `protected: []`).
