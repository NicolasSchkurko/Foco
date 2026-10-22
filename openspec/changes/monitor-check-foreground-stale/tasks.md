# Tasks — monitor-check-foreground-stale

## Riskiest test first:

- [ ] 1. `UsageMonitorService.checkOnce()`: re-evaluate `currentPackage` in the ticker before
  `evaluateLimitCheck`:
  - Keep `packageName = ForegroundApp.currentPackage()` (it is already correct).
  - If `packageName != current` (the `foreground.current` recorded at the start of the check), return
    early.
  - Fix: move the `packageName` read to the beginning, next to `if (!store.hasAnyLimitOrWindow())`.
  - Add a test in `app/src/test/java/com/foco/app/data/UsageMonitorServiceTest.kt` that covers the case
    where the user switches apps between the first read and the evaluation.
  - The test must capture the proper events (`UsageStats`, `ForegroundApp`, etc.) to simulate the app
    switch.
- [ ] 2. Add `monitor-check-foreground-stale` to `.openspec.yaml`:
  - `x-kind: fix`
  - `x-targets` to `app/src/main/java/com/foco/app/data/UsageMonitorService.kt`
  - `x-tests` to `app/src/test/java/com/foco/app/data/UsageMonitorServiceTest.kt`
  - `x-tech_brief: true` (if `walkthrough.md` is needed)
- [ ] 3. Unify `walkthrough.md` if needed (the change is very small).
- [ ] 4. Run `gradlew.bat test` and `gradlew.bat ktlintCheck`; everything green.
- [ ] 5. `--pre-archive`, `archive`, register the board, the human approval for `walkthrough.md`.

## Ideas

- The code that re-evaluates `currentPackage` before blocking is minimal, but important.
- `usedSeconds` is only re-evaluated when the package matches.
- Validation plan: CI on every change.

## Decision

- Add `x-deps: []` (it is not needed).
- If `tech_brief` is enabled, add `walkthrough.md`.
- Make sure `gate-check --run-tests` adds the test as an `[@test]`.

## Dependencies

- None (independent report).

## Steps

1. Modify `UsageMonitorService.kt` to re-evaluate the current package.
2. Add the test or try the changes.
3. Run CI.
4. Complete `walkthrough.md` if needed.
5. Process `/spec-triage` and `--pre-archive`.

## To-do list

- [ ] Modify `UsageMonitorService.kt`.
- [ ] Add a unit test.
- [ ] Run the tests.
- [ ] Verify CI.
- [ ] Add `walkthrough.md` (if needed).
- [ ] Update `DECISIONS.md`.

## Risks

- The above is well covered.

## Log

- 2026-10-21: Accepted by /spec-triage as a fix for the stale monitor.

## Checklist

- [ ] Modify `UsageMonitorService.kt`.
- [ ] Add the test and the validations.
- [ ] Run CI and the tests.
- [ ] Add `walkthrough.md` if needed.
- [ ] Generate `BOARD.md` and `DECISIONS.md`.

## Riskiest first vertical slice

- [ ] 1. Re-evaluate the package in the ticker (the crucial change).
- [ ] 2. Make sure the tests hold.
- [ ] 3. Fix CI and the tests.
- [ ] 4. Validate `--pre-archive`.

## Tasks

- [ ] Prepare the modification script.
- [ ] Prepare the test.
- [ ] Make sure the CI verification holds.
- [ ] Validate the approval.
- [ ] Make sure the final checklists hold.
