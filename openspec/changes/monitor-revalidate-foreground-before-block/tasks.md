# Tasks — monitor-revalidate-foreground-before-block

- [ ] 1. In `UsageMonitorAccessibilityService.evaluate()`, re-check `foreground.accepts(packageName)`
      after the suspending reads (`store.windowFor`/`limitFor`, `evaluator.usedSecondsToday`) and
      return early — before `showBlock` at `:118` — when the tracked foreground changed.
- [ ] 2. Keep the decision on the existing pure seam (`domain/ForegroundAppState`); no new locks or
      threads. Any debug logging present in the file belongs to other in-flight work and is not
      part of this change.
- [ ] 3. Regression (JVM): extend `app/src/test/java/com/foco/app/domain/ForegroundAppStateTest.kt`
      with the case "request accepted at entry, foreground changed during evaluation → drop".
- [ ] 4. Regression (device): in `app/src/androidTest/java/com/foco/app/data/UsageMonitorServiceTest.kt`,
      a foreground switch between request and block leaves the current app untouched (no overlay,
      no HOME action).
- [ ] 5. `gradlew.bat test` green; `node scripts/gates/gate-check.mjs --run-tests` records evidence.
- [ ] 6. Tech brief: complete `walkthrough.md` before `--pre-archive`; the comprehension sign-off is
      the human's, never written by the agent.

- [ ] 7. Spec delta: `specs/app-limits/spec.md` — ADDED *The block is re-validated right before it
      shows* with both drop scenarios; `openspec validate monitor-revalidate-foreground-before-block
      --strict` passes.

## Riskiest slice first

The re-check inside `evaluate` (task 1) is the whole fix; tests 3-4 are its proof.

## Deps

None. Independent from `monitor-check-foreground-stale` (different file, alert path).
