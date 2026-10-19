# Proposal — Close the foundation's verification findings

## Why

`usage-history-foundation` was archived with five findings in its `findings.md`
(`openspec/changes/archive/2026-10-19-usage-history-foundation/findings.md`). None of them breaks a
requirement; all of them are gaps the tests did not cover or things a reader has to reverse-engineer:
`week(until)` shipped with no requirement and no test, a test claimed a retry it never performed, the
"today is not final" rule lived as a positional `drop(1)`, two calendars were implicit, and a failed
backfill vanished without a trace.

## What changes

- **Real requirement + test for the stored window**: `week(until)` becomes
  *Stored totals are read as a seven-day window*, with `HistoryWeekTest` proving both bounds.
- **The retry is actually proven**: the empty-day test now consolidates twice and asserts the day
  lands on the second run.
- **The finished-day rule gets a name**: `domain/AvailableDays.kt` exposes `finishedDays(today)`, so
  `consolidate` no longer depends on a positional `drop(1)`.
- **One calendar**: the store drops its `zone` parameter and prunes on `ZoneId.systemDefault()`, the
  same calendar `UsageRepository` uses to build the day window.
- **A failure leaves a trace**: the backfill logs before letting the next start retry.
- **Test doubles are shared**: `HistoryFakes.kt` holds the two fake DAOs instead of three copies.

## Non-goals

No schema change, no new dependency, no UI, no retention change. The behavior of `consolidate` is
unchanged; `MainActivity` gains one log line.

## Alternatives

- **Let `week-screen` own the window**: it consumes `week()`, but then the foundation's store keeps
  untested production API for the whole of another change; rejected because the read is cheap to test
  now and the requirement is real.
- **Keep the `zone` parameter and document the coupling**: a parameter that only moves the prune
  cutoff invites a caller to pass a second calendar; removing it makes the single calendar explicit.
