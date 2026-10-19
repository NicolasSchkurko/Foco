# Tasks

## 1. The guards (both, together — one rule)

- [ ] 1.1 Resume guard (`SessionBuilder.kt:29`): drop the strict `>` so a resume for a different
  package closes the open session at that resume's timestamp, even on a tie.
- [ ] 1.2 Stop guard (`:40`): drop the strict `>` so a pause/stop closes the open session of its own
  package, even on a tie.
- [ ] 1.3 Filter the zero-length session **where sessions are emitted** (start equals end produces no
  entry), so a tie can no longer leak a 0 ms session into the total or the ranking.
- [ ] 1.4 Regression in `SessionBuilderTest`: a same-millisecond switch closes the previous session at
  that instant; a same-millisecond stop yields **no** session and leaves nothing open; and the four
  existing scenarios (direct switch, open at query time, stop + later resume, same-app transition)
  keep their results unchanged.

## 2. Integration

- [ ] 2.1 `gradlew.bat test` + `node scripts/gates/gate-check.mjs --run-tests` (green evidence);
  update `x-gitref` to the branch tip. Confirm that a normal day (no ties) yields the same total and
  ranking as before.

## Workflow follow-up

- Review the apply report, merge, and archive with `gate-check --pre-archive same-millisecond-transitions`.
- Ordering: `cross-midnight-sessions` first (declared dep); no dep on `themed-window` (overlap kept as
  a warning by choice).
- No tech brief: the comprehension pass is not required before archiving.
