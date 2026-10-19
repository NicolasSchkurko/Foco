# Tasks

## 1. The reword (wording only — nothing may change meaning)

- [x] 1.1 `usage-tracking` → *A session already open at the day's start counts from the day's start*: replace "look back far enough to find the event that opened the session" with the bound — the query looks back to the start of the previous day, one day and not further. Keep the clamp clause and the `[@test]` line.
- [x] 1.2 Keep the two scenarios as they are (crossing; the previous day's slice) and add the third: an app in the foreground for more than a day yields no session for the new day.
- [x] 1.3 Confirm nothing else moved: no other requirement of `usage-tracking` is touched, and the wording stays under the validator's text budget.

## 2. Verify (the validator is the oracle)

- [x] 2.1 `openspec validate --all --strict` before and after: no width warning appears anywhere (the reworded text is shorter than the current one), and no new failure.
- [x] 2.2 `openspec validate lookback-bound-wording --strict`: the delta itself is valid.
- [x] 2.3 `gradlew.bat test` green — `SessionBuilderTest` unchanged, which is the regression proof for a text-only fix.
- [x] 2.4 `node scripts/gates/gate-check.mjs --run-tests` (green evidence); update `x-gitref` to the branch tip.

## Workflow follow-up

- Review the apply report, merge, then archive with `gate-check --pre-archive lookback-bound-wording` (the living spec changes at **archive** time, so the branch carries only this change's folder).
- Closes finding 1 of the archived `cross-midnight-sessions` (`archive/2026-10-18-cross-midnight-sessions/findings.md`). Finding 2 (the wiring has no test) is the sibling change `day-window-seam`.
- No tech brief: no walkthrough, no comprehension sign-off before archiving.

### Notes on the apply (2026-10-19)

- Delta-only change: there is no code to write — the delta *is* the change, so the apply is the delta plus the suite staying green, and the living spec is rewritten when this is archived.
- `openspec validate lookback-bound-wording --strict`: valid, and the reworded requirement is shorter than the one it replaces (no width warning).
- `gradlew.bat test` green with `SessionBuilderTest` untouched: the regression proof for a text-only fix.
