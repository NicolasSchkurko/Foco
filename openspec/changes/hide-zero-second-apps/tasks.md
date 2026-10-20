# Tasks — hide-zero-second-apps

- [x] 1. Add failing unit coverage in `UsageMetricsTest`, `WeekMetricsTest` and `AppDetailMetricsTest`: 500 ms hidden, 1000 ms visible, an app with only 500 ms absent, day total/hourly still include 500 ms, week bar still includes 500 ms, and the detail summary recomputes over visible sessions.
- [x] 2. Add `MIN_VISIBLE_SESSION_MILLIS = 1_000L` and a `visibleSessions()` helper in `ui/metrics/UsageMetrics.kt` as the single owner of the rule.
- [x] 3. Apply the helper in `appRanking` and `appDayDetail`; in the detail, return `null` when no visible session remains and recompute every summary field from the visible list.
- [x] 4. Apply the rule in `deriveWeekMetrics`: filter today's live sessions and hide a ranking entry whose total is below 1000 ms; leave stored bars and stored totals untouched.
- [x] 5. Run `gradlew.bat test`, then `openspec validate hide-zero-second-apps --strict` and `node scripts/gates/build-board.mjs`.
