# Tasks

## 1. The fold (wording only — nothing may change meaning)

- [x] 1.1 `usage-tracking` → *Usage sessions are built from system events*: add the tie clause to the direct switch, the pause/stop half, and the zero-length rule; keep the open-at-query and same-app-transition clauses. A MODIFIED block replaces the whole requirement, so **all five scenarios** must be in it (three to keep, two folded in from the removed one) — the validator refuses a block that drops a scenario.
- [x] 1.2 Fix the requirement's `[@test]`: it points at `app/src/test/java/com/foco/app/data/SessionBuilderTest.kt`, which does not exist; the test lives in `app/src/test/java/com/foco/app/domain/SessionBuilderTest.kt`. (The gate's existence check reads deltas only, so a living spec carries this silently.)
- [x] 1.3 REMOVE *A transition settles by event order, not by strict inequality*, with its reason and its migration note (same `[@test]`, scenarios folded above).
- [x] 1.4 Sanity: no fact of the removed requirement is missing from the surviving one; no other requirement of `usage-tracking` is touched; the text stays inside the validator's budget.

## 2. Verify

- [x] 2.1 `openspec validate --all --strict` before and after: still `5 passed, 0 failed`, with no new warning (the reworded text is shorter than the two blocks it replaces, counted together).
- [x] 2.2 `openspec show usage-tracking --type spec` lists the sessions requirement once and no *"transition settles by event order"* requirement.
- [x] 2.3 `gradlew.bat test` green — `SessionBuilderTest` unchanged, the regression proof for a text-only refactor.
- [x] 2.4 `node scripts/gates/gate-check.mjs --run-tests` (green evidence); update `x-gitref` to the branch tip.

## Workflow follow-up

- Review the apply report, merge, then archive with `gate-check --pre-archive fold-tie-rule-into-sessions` (the living spec changes at **archive** time).
- Closes the medium finding of the archived `same-millisecond-transitions`; that folder is history and is not edited.
- No tech brief: no walkthrough, no comprehension sign-off before archiving.

### Notes on the apply (2026-10-19)

- Delta-only change: the delta *is* the change, so the apply is the delta plus the suite staying green; the living spec is rewritten when this is archived.
- **Recorded before**: `openspec validate --all --strict` → `6 passed, 0 failed`; the gate → 0 errors, 0 warnings with evidence recorded.
- The surviving requirement is well inside the budget (one sentence plus its `[@test]`), and it replaces two blocks that together said the same thing twice.
- Task 2.2 (`openspec show usage-tracking --type spec` lists the sessions requirement once and no *transition settles by event order*) is a **post-archive** check: it was run right after `openspec archive`, and it is recorded in the apply report rather than re-ticked after the folder had moved. Both drifts it looked for are gone: the duplicate requirement and the broken `[@test]` path.
