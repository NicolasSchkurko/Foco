# Tasks — consolidation-runner-deadlock

## Riskiest first vertical slice

- [ ] 1. `ConsolidationRunner`: change `gate` / `inFlight` to avoid the reentrancy:
  - Use `Mutex(locking = LockingMode.NON_REENTRANT)` explicitly, or restructure it so the `finally`
    does not need to lock (the outer `withLock` already does it).
  - Replace `gate.withLock { inFlight = null }` with `inFlight = null` (the outer `withLock` in `run()`
    protects it).
  - Test on the JVM that `run()` returns `ConsolidationOutcome.Failed` when `consolidate` throws.
- [ ] 2. Add `consolidation-runner-deadlock` to `.openspec.yaml` (`x-kind: fix`, `x-targets` to
  `app/src/main/java/com/foco/app/data/ConsolidationRunner.kt`).
- [ ] 3. Send `gate-check --run-tests` with `consolidation-runner-deadlock` included, in
  `app/src/test/...` if a new `[@test]` is needed.
- [ ] 4. Fix `gitignore` if the build changes; `gradlew.bat test`, `gradlew.bat ktlintCheck` → green.
- [ ] 5. [--pre-archive, archive, regenerate board, commit]

## Shortcuts

- The best solution is to restructure `ConsolidationRunner` so the lock is **external** (`run()` already
  does `gate.withLock`; only `inFlight` needs a field).
- The `finally` moves outside the outer `withLock`.

## Decisions

- Use `supervisorJob()`: the worker branch and the app-start branch share the **same**
  `ConsolidationRunner` (`shared`).
- A `ReentrantMutex` is only needed if `gate` is kept for `inFlight`.
- `inFlight` must always be under `gate.withLock` (the only protection).

## Variants

- Keep `gate` but make it reentrant with a `withLock` mutex that allows reentrancy (Kotlin/Multihost):
  `ReentrantMutex()`.
- Keep `gate` but let the `finally` do a `withLock` if the code externalizes the protection of
  `inFlight` with a fallback for the `await()` verification.

## Dependencies

- The change declares a test change in `app/src/test/java/com/foco/app/data/ConsolidationRunnerTest.kt`
  (if it exists) or adds a new one.
- `consolidation-runner-deadlock` has deps: [] (none), because no active change depends on it yet.
- The change depends on `usage-history-foundation` (`archived`), `nightly-consolidation` (draft) and
  `schema-drift-guard` (draft) through `.openspec.yaml`.

## Time task

- Make sure there is no lock leak: JMeter 100 sprints.
- Make sure an error in the consolidation clears all the nulls.
- Make sure the test runner keeps its cadence.

## Acceptance record

- [ ] Accept `consolidation-runner-deadlock` (draft) with `tech_brief: true`.
- [ ] Apply `walkthrough.md` and the human's comprehension approval before `--pre-archive`.
- [ ] After `verified`, register `ARCHIVE.md` -> `BOARD.md` updated.

## CHANGELOG edit

Added under `openspec/changes/consolidation-runner-deadlock/` in `DECISIONS.md`.

## Roadmap

1. Prepare the change's fix (the files are ready).
2. Open the PR for the human approval.
3. After the approval, apply it to dev, CI, green.
4. Fix the test runner.
5. Take it to `--pre-archive` + `walkthrough.md` + the human's approval.

## To-do list

- [ ] Modify `ConsolidationRunner.kt` to avoid the mutex reentrancy.
- [ ] Add a regression test for the error case with the lock.
- [ ] Run the whole test suite.
- [ ] Make sure the structured test safety holds.
- [ ] Generate `walkthrough.md` with the explanation and the traceability.
