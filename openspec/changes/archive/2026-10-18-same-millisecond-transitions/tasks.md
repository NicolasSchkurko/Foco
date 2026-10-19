# Tasks

## 1. The guards (both, together — one rule)

- [x] 1.1 Resume guard (`SessionBuilder.kt:29`): drop the strict `>` so a resume for a different
  package closes the open session at that resume's timestamp, even on a tie.
- [x] 1.2 Stop guard (`:40`): drop the strict `>` so a pause/stop closes the open session of its own
  package, even on a tie.
- [x] 1.3 Filter the zero-length session **where sessions are emitted** (start equals end produces no
  entry), so a tie can no longer leak a 0 ms session into the total or the ranking.
- [x] 1.4 Regression in `SessionBuilderTest`: a same-millisecond switch closes the previous session at
  that instant; a same-millisecond stop yields **no** session and leaves nothing open; and the four
  existing scenarios (direct switch, open at query time, stop + later resume, same-app transition)
  keep their results unchanged.

## 2. Integration

- [x] 2.1 `gradlew.bat test` + `node scripts/gates/gate-check.mjs --run-tests` (green evidence);
  update `x-gitref` to the branch tip. Confirm that a normal day (no ties) yields the same total and
  ranking as before.

## Workflow follow-up

- Review the apply report, merge, and archive with `gate-check --pre-archive same-millisecond-transitions`.
- Ordering: `cross-midnight-sessions` first (declared dep); no dep on `themed-window` (overlap kept as
  a warning by choice).
- No tech brief: the comprehension pass is not required before archiving.

### Notes on the apply (2026-10-19)

- Both guards now use `>=`: a resume for another package closes the open session at that resume's timestamp, and a pause/stop closes the open session of its own package — ties included (`app/src/main/java/com/foco/app/domain/SessionBuilder.kt:29,47`).
- The zero-length rule lives in one place: a local `emit(packageName, start, end)` drops any session whose start equals its end, and all three emission sites (the direct switch, the stop and the open-at-query tail) go through it.
- **Red before / green after, both recorded.** With the guards temporarily restored to the strict `>`, `testDebugUnitTest --tests "*SessionBuilderTest*"` reported `13 tests completed, 3 failed` — exactly the three new cases (same-millisecond switch, same-millisecond stop, and resume+stop+resume in the same millisecond). With the `>=` guards: 13/13. The file was byte-identical after restoring (hash checked).
- The four pre-existing scenarios kept their results (`SessionBuilderTest`'s earlier ten cases are untouched and green), and the metrics tests are unchanged: `DaySelectionTest` 3/3, `UsageMetricsTest` 4/4, `AppDetailMetricsTest` 4/4 — a day without ties yields the same totals and ranking.
- `/spec-verify`'s finding on this change stays **open** and is not addressed here: the requirement is **ADDED**, so the direct-switch fact ends up stated twice in `usage-tracking` (the living requirement at `:9` already says it). See `findings.md`; the fix is a small follow-up (fold it into that requirement as a MODIFIED).
