# Proposal — A session already open at the day's start counts for that day

## Why

Foco loses the usage of an app that was already in the foreground when the day's query window
opened. The chain, reproduced from the code:

1. `UsageRepository.loadDay` asks the system for `[startOfDay, min(dayEndExclusive, now)]`
   (`app/src/main/java/com/foco/app/data/UsageRepository.kt:68,70`) and folds only those events (`:98`).
2. An app open across midnight (resumed 23:50, still open at 00:05) has **no `RESUMED` inside the
   new day's window** — the one that opened its session belongs to the previous day.
3. When the user leaves that app at 00:06, its pause/stop *is* in the window, but the fold discards
   it because no session is open (`app/src/main/java/com/foco/app/domain/SessionBuilder.kt:38-44`).
4. The day therefore reports **nothing** for that app: its time is silently undercounted.

The trigger is ordinary — any app used across midnight, which is precisely what this app exists to
measure — and it is systematic: the day that starts loses the tail, while the previous day closes
correctly at its own end. The living requirement promises the opposite
(`openspec/specs/usage-tracking/spec.md:8-22`, scenario "Session open at query time", `:16-18`).

## What Changes

- `UsageRepository.loadDay` widens the **lookback** of its `queryEvents` call, so the event that
  opened a session before the day's start is seen.
- The day's sessions are **clamped** to `[startOfDay, effectiveEnd]`: a session opened before the day
  contributes from the day's start on, and the previous day keeps its own slice (nothing is counted twice).
- The pause/stop that closes such a session now meets an open session, so it is no longer discarded.
- Nothing else moves: the day selector, the unlock query, the metrics and the UI keep their behavior.

## Alternatives Considered

| Option | Why yes | Why not / status |
|---|---|---|
| **Widen the lookback and clamp the session to the day's start** — chosen (human, 2026-10-18: "the code is wrong") | Reuses the existing query and the existing fold; the clamp is arithmetic, so it can live in the pure layer and stay unit-testable | The lookback needs a bound; the natural one is the previous day's start |
| Query a fixed extra margin (e.g. `startOfDay − 1 h`) | One-line change | Still wrong when the app has been open longer than the margin — a guess, not a rule |
| Clamp only, without looking back | No query change | There is no session to clamp: the opening event is never seen |
| Persist "what was in the foreground at midnight" | Exact by construction | Adds persistence and a writer path for a value the system already holds — against reuse-first |

## Impact

- `app/src/main/java/com/foco/app/data/**` (the query) · `app/src/main/java/com/foco/app/domain/**`
  (the pure clamp) · `app/src/test/**` (the regression)
- Ordering: overlaps `themed-window` on the `data/**` and `test/**` globs — **no file collision**, and
  no dep is declared, so it reports as a plain overlap warning instead of blocking this fix behind that
  draft. Reversible with one line if you prefer the sequencing.
- No protected zones, no new dependency. Tech brief: **yes** — it changes how the platform's usage
  events are queried (`UsageStatsManager`), so `walkthrough.md` is written during the apply.
