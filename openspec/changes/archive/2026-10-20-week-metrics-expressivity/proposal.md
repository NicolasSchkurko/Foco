# Proposal — week-metrics-expressivity

## Why

`deriveWeekMetrics` re-filters the stored totals seven times (one per bar day) and builds a
per-package list just to count distinct days. It also re-sums session durations where the shared
`dayTotal` helper already exists. The behavior is correct; the derivation is noisier and allocates
more than it needs.

## What

Build a single index of stored totals by day and package, walk the seven days once, and derive the
ranking and `daysUsed` from that index. Reuse `dayTotal` for the live day. The ranking order, totals,
bars, absent states and `daysUsed` stay identical.

No behavior change, no UI change, no persistence change.

## Expected outcome

The same `WeekMetrics` for the same inputs with one grouped pass and fewer intermediate lists. The
existing `WeekMetricsTest` and the full suite stay green; no new dependency or abstraction.

## Alternatives considered

| Option | Why yes | Why not |
| --- | --- | --- |
| One grouped pass in `deriveWeekMetrics` (chosen) | Small, measured, no behavior change | The function grows slightly |
| Keep the seven filters | No code | Repeated scans and allocations on every recomposition |
| Extract a new metrics class/registry | More structure | Premature abstraction for a single pure function |
