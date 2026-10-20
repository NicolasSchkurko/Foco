# Proposal — hide-zero-second-apps

## Why

On the Pixel 8, apps with sub-second sessions appear as "0 s": `Formatting.kt:10` truncates 1–999 ms to `"0 s"`, and `SessionBuilder` already drops exactly zero-length sessions (`SessionBuilder.kt:15-17`, `:65-69`). The living contract says nothing about sub-second sessions or about hiding an app whose total rounds to zero (`usage-tracking/spec.md:140-146`, `:316-331`). The result is noise in the app lists and in the detail's session breakdown.

This is a presentation rule, not a reversal of `DECISIONS.md:261`: exact-zero sessions remain filtered where sessions are emitted.

## What

Add one visibility rule in `ui/metrics`: a session shorter than `1000 ms` is not visible. Reuse it in the three derivation points:

- `appRanking` ignores sub-second sessions; an app appears only if it has visible time, and its total is the sum of visible sessions.
- `appDayDetail` filters the app's sessions and recomputes the summary (total, count, first/last, average, longest, hourly) from the visible list; it returns `null` when none remain.
- `deriveWeekMetrics` filters today's live sessions and hides an app whose week total is below 1000 ms; stored `DailyAggregate` rows are read-only and are not rewritten.

The day total, hourly distribution and week day bars keep the full time; only app rankings and the detail's session list are filtered. A session of exactly 1000 ms is visible.

## Expected outcome

No app row or detail session displays "0 s". An app whose visible total is 0 s disappears from the dashboard, the week ranking and its detail; an app with visible time remains, showing the visible total. Existing stored days are covered by the read/derivation filter, so no migration is needed.

## Alternatives considered

- **Drop sub-second sessions at `SessionBuilder`:** changes stored history and totals, and still leaves already-consolidated rows to filter on read; rejected for a presentation-only request.
- **Change `formatDuration` to print "<1 s":** makes the rows visible instead of hiding them; rejected.
- **Filter only in `AppUsageRow`:** hides the dashboard/week rows but leaves the detail list and ranking totals intact; does not cover all app views; rejected.
