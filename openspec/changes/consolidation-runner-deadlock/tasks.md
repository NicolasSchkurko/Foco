# Tasks — consolidation-runner-deadlock

## Riskiest first vertical slice

- [ ] 1. `ConsolidationRunner`: change `gate` / `inFlight` to avoid reentrancy:
  - Use `Mutex(locking = LockingMode.NON_REENTRANT)` explicitly, or restructure so `finally` does not need to lock (the outer `withLock` does it).
  - Replace `gate.withLock { inFlight = null }` with `inFlight = null` (the outer `withLock` in `run()` protects it).
  - Test on the JVM that `run()` returns `ConsolidationOutcome.Failed` when `consolidate` throws.
- [ ] 2. Add `consolidation-runner-deadlock` to `.openspec.yaml` (`x-kind: fix`, `x-targets` to `app/src/main/java/com/foco/app/data/ConsolidationRunner.kt`).
- [ ] 3. Send `gate-check --run-tests` with `consolidation-runner-deadlock` included, in `app/src/test/...` if a new `[@test]` is needed.
- [ ] 4. Fix `gitignore` if the build changes; `gradlew.bat test`, `gradlew.bat ktlintCheck` → green.
- [ ] 5. [--pre-archive, archive, regenerate board, commit]

## Shortcuts

- The best solution is to restructure `ConsolidationRunner` so the lock is **external** (the `run()` already does `gate.withLock`, only `inFlight` needs a field.
- The `finally` moves out of the outer `withLock`.

## Decisions

- Use `supervisorJob()`: the worker branch and the app-start branch share the **same** `ConsolidationRunner` (`shared`).
- A `ReentrantMutex` is only needed if `gate` is kept for `inFlight`.
- `inFlight` must always be under `gate.withLock` (the only protection).

## Variants

- Keep `gate` but make it reentrant with a `withLock` mutex that allows reentrancy (Kotlin/Multihost): `ReentrantMutex()`.
- Keep `gate` but let `finally` do the `withLock` if the code externalizes the protection of `inFlight` with a fallback for the `await()` verification.

## Dependencies

- The change declares a test change in `app/src/test/java/com/foco/app/data/ConsolidationRunnerTest.kt` (if it exists) or adds a new one.
- `consolidation-runner-deadlock` has deps: [] (none) because no active change depends on it yet.
- The change depends on `usage-history-foundation` (`archived`), `nightly-consolidation` (draft), `schema-drift-guard` (draft) through `.openspec.yaml`.

## Time task

- Make sure there are no lock leaks: JMeter 100 sprints.
- Make sure an error in the consolidation clears every null.
- Make sure the test runner cadence holds.

## Acceptance act

- [ ] Accept `consolidation-runner-deadlock` (draft) with `tech_brief: true`.
- [ ] Apply `walkthrough.md` and understand the human approval before `--pre-archive`.
- [ ] After `verified`, record `ARCHIVE.md` -> `BOARD.md` updated.

## CHANGELOG edit

Added in `openspec/changes/consolidation-runner-deadlock/` under `DECISIONS.md`.

## Roadmap

1. Prepare the fix of the change (the files are ready).
2. Open the PR for the human approval.
3. After the approval, apply to dev, CI, green.
4. Fix the test runner.
5. Take it to `--pre-archive` + `walkthrough.md` + human approval.

## Pending list

- [ ] Change `ConsolidationRunner.kt` to avoid the reentrancy of the mutex.
- [ ] Add the regression test for the error case with the lock.
- [ ] Run the whole test suite.
- [ ] Make sure the structured tests are safe.
- [ ] Generate walkthrough.md with the explanation and the trace.
