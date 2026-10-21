# Proposal — launchable-packages-refresh

## Why

`UsageRepository` freezes the set of launchable packages at construction:
`launchablePackages by lazy { … }` (`UsageMonitorAccessibilityService`'s data layer,
`UsageRepository.kt:30-36`; the session filter consumes it at `:111`). The monitor builds **one**
repository for the whole service life (`UsageMonitorService.kt:44` → `UsageMonitorEvaluator.kt:14`),
and `UsageMonitorService.sync()` (`:159-167`) on an already-running service only re-fires
`onStartCommand` — `onCreate` never re-runs, so the evaluator and its repository keep the set as it
was at service start.

Consequence: an app installed **after** Foco's monitor started is never in the set → its sessions
are filtered out → `usedSecondsToday` stays 0 → `isAtOrOverLimit` never holds → **its limit never
fires**, silently, until Foco is force-stopped — while the limits screen (which reads a fresh
repository) happily offers it. Dashboard and detail show the same stale set until the composition is
recreated.

Contract already in force:

- `openspec/specs/usage-tracking/spec.md:32-42` — "Only user-facing apps count … filtering over an
  allowed/excluded package set."
- `openspec/specs/app-limits/spec.md:37-50` — the monitor checks "today's UsageStats sessions" (a
  filtered read that currently drops the new app entirely).

## What changes

- The launchable set is re-resolved on demand (per day read / per filter pass, or invalidated when
  the package set changes) instead of living for the repository's lifetime, so the monitor's next
  2-minute check sees apps installed while it runs.
- Regression: `SessionFilterTest` pins that a package appearing in the launcher after the
  repository was created still passes the filter.

## Alternatives

| Option | Effect | Verdict |
|---|---|---|
| Re-query the launcher set per read (or per filter pass) | always current; one PackageManager walk per read | **Proposed** |
| Restart the monitor when `sync()` fires while running | whole service cycle, battery + notification churn, still stale for UI | not proposed |
| Broadcast `PACKAGE_ADDED` into the repository | event-driven but a second code path with its own filters (disabled apps, updated packages) | not proposed |

## Risks

- One extra launcher query per day read; the set is small (launcher-eligible packages) and the read
  is already off the main thread. Measured at apply time — if it shows, cache with an explicit
  invalidation instead of `by lazy`.

## Spec delta

`specs/usage-tracking/spec.md` MODIFIES *Only user-facing apps count* — restated verbatim plus the scenario *A package becomes launchable while Foco runs*, which turns the re-read from an implementation detail into contract.

## Origin

Filed 2026-10-21 by the anchored bug-finder audit (finding 3, severity high); `kind: fix` per
/spec-triage. Awaiting human acceptance — no code written.
