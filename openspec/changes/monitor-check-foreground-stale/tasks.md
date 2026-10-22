# Tasks — monitor-check-foreground-stale

## Riskiest test first:

- [ ] 1. `UsageMonitorService.checkOnce()`: re-evaluate `currentPackage` in the ticker before `evaluateLimitCheck`:
  - Keep `packageName = ForegroundApp.currentPackage()` (that part is already correct).
  - If `packageName != current` (the `foreground.current` read at the start of the check), return early.
  - Fix: move the read of `packageName` to the beginning, next to `if (!store.hasAnyLimitOrWindow())`.
  - Add a test in `app/src/test/java/com/foco/app/data/UsageMonitorServiceTest.kt` that covers the case where the user switches apps between the first read and the evaluation.
  - The test must capture the right events (`UsageStats`, `ForegroundApp`, etc.) to simulate the app switch.
- [ ] 2. Add `monitor-check-foreground-stale` to `.openspec.yaml`:
  - `x-kind: fix`
  - `x-targets` to `app/src/main/java/com/foco/app/data/UsageMonitorService.kt`
  - `x-tests` to `app/src/test/java/com/foco/app/data/UsageMonitorServiceTest.kt`
  - `x-tech_brief: true` (if `walkthrough.md` is needed)
- [ ] 3. Unify `walkthrough.md` if needed (the change is very small).
- [ ] 4. Run `gradlew.bat test` and `gradlew.bat ktlintCheck`; everything green.
- [ ] 5. `--pre-archive`, `archive`, record the board, the human approval for `walkthrough.md`.

## Ideas

- The code that re-evaluates `currentPackage` before blocking is minimal, but important.
- Re-evaluate `usedSeconds` only when the package matches.
- Validation plan: CI on every change.

## Decision

- Add `x-deps: []` (not needed).
- If `tech_brief` is enabled, add `walkthrough.md`.
- Make sure the `gate-check --run-tests` adds the test as an `[@test]`.

## Dependencies

- None (independent report).

## Steps

1. Change `UsageMonitorService.kt` to re-evaluate the current package.
2. Add the test or check the changes.
3. Run CI.
4. Complete `walkthrough.md` if needed.
5. Process `/spec-triage` and `--pre-archive`.

## Pending list

- [ ] Change `UsageMonitorService.kt`.
- [ ] Add the unit test.
- [ ] Run the tests.
- [ ] Check CI.
- [ ] Add `walkthrough.md` (if needed).
- [ ] Update `DECISIONS.md`.

## Risks

- The above is well covered.

## Log

- 2026-10-21: Accepted by /spec-triage as a fix for the stale monitor.

## Checklist

- [ ] Change `UsageMonitorService.kt`.
- [ ] Add the test and the validations.
- [ ] Run CI and the tests.
- [ ] Add `walkthrough.md` if needed.
- [ ] Generate `BOARD.md` and `DECISIONS.md`.

## Riskiest First Vertical Slice

- [ ] 1. Re-evaluate the package in the ticker (the crucial change).
- [ ] 2. Make sure the tests hold.
- [ ] 3. Fix CI and the tests.
- [ ] 4. Validate `--pre-archive`.

## Tasks

- [ ] Prepare the change script.
- [ ] Prepare the test.
- [ ] Make sure the CI verification holds.
- [ ] Validate the approval.
- [ ] Make sure the final checklists hold.
