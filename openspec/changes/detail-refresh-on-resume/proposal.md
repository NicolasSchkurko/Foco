# Proposal — detail-refresh-on-resume

## Why

The detail loads once and never again while it stays open: its data effect
(`AppDetailScreen.kt:121-135`) is keyed by `(packageName, selectedDate, storedDays, today)` — no
lifecycle trigger. Leave Foco, use another app for half an hour, come back: the total, chart and
session list still show the snapshot from when the screen opened, and there is no way to refresh
except switching days and back.

The dashboard already owns this rule (`usage-tracking:60-70`: reload every time the app returns to
the foreground, without user action; `:72-82` periodic while visible) — the detail is the one
screen the contract forgot. 🟡 spec-silent → /spec-add.

## What changes (proposed contract)

- New requirement in `usage-tracking`: the detail refreshes on every return to the foreground
  (same cadence contract as the dashboard: on resume and on first show, no manual control), for
  the day it is showing.
- Implementation: a lifecycle/resume trigger re-runs the existing load effect; today's rollover
  (`rememberCurrentDate` rolls on `ON_RESUME`) already moves the window, the load must follow.

## Alternatives

| Option | Effect | Verdict |
|---|---|---|
| Re-run the load effect on resume (Lifecycle STARTED/ON_RESUME trigger) | same pattern as the dashboard's refresh; no new UI | **Proposed** |
| Periodic refresh like the dashboard (30 s) | more churn than a day-slice needs; detail is read, not monitored | not proposed |
| Manual pull-to-refresh | contradicts `usage-tracking:84-90` (no manual refresh control) | not proposed |

## Risks

- A reload on resume re-reads storage; it is the same read the open already does — cost equal to
  switching days, off the main thread through the existing `ReadOr` path.
- Selection must be preserved through the refresh (chain with
  `detail-selection-survives-recreation`, same file).

## Origin

Filed 2026-10-21 by the anchored bug-finder audit (finding 9, severity medium, 🟡 spec-silent);
`kind: feature` via /spec-add. Sequenced after `detail-selection-survives-recreation` (same file).
Awaiting human acceptance — no code written.
