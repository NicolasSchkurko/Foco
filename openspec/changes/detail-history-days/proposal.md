# Proposal — The detail reads its own history

## Why

The Week screen is next, and its bars will open a day. But the detail today reads **only** the platform: `AppDetailScreen` builds a `UsageRepository` and calls `loadDay` for the chosen date (`ui/detail/AppDetailScreen.kt:62`), and its selector is bounded by what the system retains (`Day selection within system retention`, `domain/AvailableDays.kt`). So a day that `usage-history-foundation` stored, but the platform already dropped, would open **empty** — the bar would promise data the detail cannot show.

This change makes the detail read its own history for past days and keeps the platform only for today, where the data is not final yet.

## What Changes

- `SessionHistoryStore` gains `sessionsFor(date)`: the stored raw sessions of one day (half-open on `start`), the same shape the detail's computations already consume.
- A pure source decision (`DaySource`, `DetailDaySource`): today → live, a stored day → storage, neither → absent. Pure so it is JVM-testable and the screen keeps no branching.
- `availableDays` stops deriving its window from "today minus six" by assumption and takes the days the store actually has (plus today). The bounds stay at seven because that is the raw retention chosen in the foundation.
- The living requirement `Day selection within system retention` is **replaced** by `Day selection within the stored history` (REMOVED + ADDED, with reason and migration): the bound is the stored window, not the platform's.

## State and persistence mechanism

- **Reads**: `session_record` rows for one day (raw retention: seven days) through the store the foundation owns; today from `UsageRepository.loadDay`, because today is not consolidated by design.
- **Writes**: none. This change only reads; the consolidation stays where it is.
- **Nothing leaves the device**: unchanged — no network permission, no backup.

## Expected outcome

Opening a day from the Week screen shows the same total the bar showed, even when the platform no longer returns that day's events. We will know it was worth it when the week's numbers and the detail's numbers agree for every one of the seven days.

## Alternatives considered

| Option | Why yes | Why not / status |
|---|---|---|
| **Read the store for past days, live for today (chosen)** | One source per day, the numbers the Week shows are the numbers the detail shows, and the selector stops pretending the platform is the owner | Needs one read on the store and a source decision — small |
| Option 0 — reuse as-is (live only) | Zero work; the detail already works | Days the platform dropped open empty while the Week shows them — **rejected** |
| Raise the raw retention so the platform window never matters | Keeps one source | Contradicts the retention decision recorded for the foundation (seven days of raw), and does not fix the source: today's live data still differs — **rejected** |
| Read the aggregates instead of raw sessions | Aggregates survive pruning | The detail needs hourly buckets and the session list; aggregates only carry per-app totals — **rejected** |

## Non-goals

- **No Week screen here**: its bars, ranking and navigation are `week-screen`, which declares a dep on this change.
- **No more than seven days of raw detail**: aggregates live longer, but the breakdown (chart + session list) needs raw rows; extending it is a retention decision, not a UI one.
- **No change to today's behavior**: today keeps being read live and refreshing on return.

## Impact

- `tech_brief: true`: the read path and the "which source" rule are state semantics worth a walkthrough.
- Depends on `usage-history-foundation` (draft): its file `SessionHistoryStore.kt` is touched, so the gate treats the overlap as sequenced.
- No protected zone (`openspec/config.yaml` → `protected: []`).
