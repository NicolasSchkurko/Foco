# Tasks — stored-day-zone-of-record

- [ ] 1. **Decision first** (blocks everything): the human answers the open question in
      `.openspec.yaml` (write-time zone vs current zone); the answer is recorded as an ADR in
      `DECISIONS.md` and the delta is adjusted if the answer differs from the proposal.
- [ ] 2. Delta `specs/usage-tracking/spec.md`: ADDED *A stored day keeps one calendar* with the
      zone-change, agreement and ordinary-day scenarios (adjust wording to the decision).
- [ ] 3. If the decision is the proposal: store the day's zone offset with the consolidated day
      (non-destructive migration + schema export, `usage-tracking:575-586`), and slice
      `sessionsFor` (`SessionHistoryStore.kt:83`) with it; prune/read keep
      `ZoneId.systemDefault()` (`DECISIONS.md:338` untouched).
- [ ] 4. Regression: new `app/src/test/java/com/foco/app/data/HistoryDayZoneTest.kt` — a day
      consolidated under zone A and read under zone B yields the same sessions the week bar shows
      (fails today); plus the migration proof if the schema bumps.
- [ ] 5. Walkthrough (tech brief: persistence/state): complete `walkthrough.md`; comprehension
      sign-off is the human's.
- [ ] 6. `gradlew.bat test` green; `gate-check --run-tests` records evidence; `openspec validate
      stored-day-zone-of-record --strict` passes.

## Riskiest slice first

The decision (task 1): it picks the schema cost before any code exists.

## Deps

`consolidation-totals-reset` (parallel change owns `SessionHistoryStore.kt` first).
