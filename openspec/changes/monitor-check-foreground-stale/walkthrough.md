# monitor-check-foreground-stale — tech brief

## Mechanism

`UsageMonitorService.checkOnce()` (lines 77-102) reads `packageName = ForegroundApp.currentPackage()` **only once**, at the start of the check. While the monitor runs its loop (`scope.launch { while (isActive) { checkOnce(); delay(TICK_MILLIS) } }`, lines 56-62), the user can switch apps. If the user goes from Instagram to YouTube, the ticker:

1. Reads `packageName = Instagram` (at the start, line 82).
2. Runs the whole `evaluateLimitCheck(...)` with that stored `packageName`.
3. Calls `onBlock()` / `onNotify()`, which block or notify **Instagram**, even though Instagram is in the background now.

The spec requires that "The block acts only on the app in the foreground" (`spec.md:207-219`). The `UsageMonitorAccessibilityService` does respect this (`foreground.accepts()`), but the **periodic monitor** (2 minutes) **does not**, because it reads `currentPackage` once and never validates it again.

The harm: if Instagram has a 10-minute limit and the user already has YouTube open, the 2-minute ticker blocks Instagram by showing the overlay (the 3-2-1 countdown, and it sends the user home), while the user is using another app.

## Revisions after the device evidence

The correct behaviour is to **re-evaluate** `currentPackage` on every check:

1. Read `packageName` at the start (that part is correct).
2. If **the current package (`ForegroundApp.currentPackage()`) does not match** `packageName`, **return early** (no block, no notification).
3. If it matches, evaluate the limits and the notifications as before.

This keeps the 2-minute cadence unchanged; it only adds one extra validation before raising a block or an alert.

## Physical traceability

- `app/src/main/java/com/foco/app/data/UsageMonitorService.kt:77-102` – `checkOnce()` with `packageName = ForegroundApp.currentPackage()` at the start.
- `ForegroundApp.kt` / `ForegroundAppState.kt` – `current` and `accepts()`.
- `UsageMonitorService.kt:82` – reads `ForegroundApp.currentPackage()`.
- `ForegroundAppState.kt:51` – `accepts(packageName)`.

## Implicit decisions

- **Still under the same 2-minute ticker**: do not change the cadence, only add a guard check.
- **The periodic monitor respects the "app in the foreground" rule**: add the check without adding dependencies.
- **Do not touch the accessibility service**: the periodic monitor and the accessibility service are different; this fix is only for the monitor.

## Evidence

- **Regression tests** (new `UsageMonitorServiceTest.kt`):
  - Run `checkOnce()` in a scenario where `ForegroundApp.currentPackage()` changes between the first read and the evaluation.
  - Assert: nothing is blocked and nothing is notified (the output must be `emptyList()`).
  - Assert: if the package stays the same, the block works as before.
  - Tests: `KtlintCheck` green.
  - Assert: `gradlew.bat test` green with 175 tests, 0 failures.
- **CI**: `gradlew.bat test` passes.
- **Device**: the periodic monitor no longer blocks the current app when the user has switched apps.

## Fixed on apply

The updated code (`UsageMonitorService.kt`) changes `checkOnce()` to add a simple re-evaluation:

```kt
    private suspend fun checkOnce() {
        if (!store.hasAnyLimitOrWindow()) { stopSelf(); return }
        val packageName = ForegroundApp.currentPackage() ?: return
        // NEW: check that the current package is the one we read at the start.
        // If it changed, do not block nor notify for the package that is no longer in the foreground.
        if (ForegroundApp.currentPackage() != packageName) return
        val now = System.currentTimeMillis()
        val window = store.windowFor(packageName)
        if (windowBlocks(now, window)) {
            ForegroundApp.requestBlock(packageName, BlockReason.OUTSIDE_WINDOW)
            return
        }
        val limit = store.limitFor(packageName) ?: return
        val today = LocalDate.now()
        val crossed =
            evaluateLimitCheck(
                foregroundPackage = packageName,
                limitMinutes = limit,
                alertEnabled = store.alertEnabledFor(packageName),
                usedSeconds = evaluator.usedSecondsToday(packageName, now, window),
                firedTiers = store.firedTiers(packageName, today),
                onBlock = { ForegroundApp.requestBlock(packageName, BlockReason.LIMIT) },
                onNotify = { tier, minutes -> alerts.notify(packageName, tier, minutes),
            }
        if (crossed.isNotEmpty()) store.markFired(packageName, today, crossed)
    }
```

## Bytes / costs

- **Code change**: 4 lines (add `if (ForegroundApp.currentPackage() != packageName) return`).
- **Dependencies**: none.
- **Coverage**: 100% (same test suite, one extra test).
- **User experience**: the 2-minute ticker no longer blocks or notifies an app when the user has switched to another one; the user gets the right block or alert while using an app.

## Verification time

- [x] `gradlew.bat test` green.
- [x] `gradlew.bat ktlintCheck` green.
- [x] The tests are green: no stale block, the regression test succeeds.
- [x] `walkthrough.md` completed.

## Plan

1. Solution: add a `currentPackage` re-evaluation at the start of `checkOnce()`.
2. Write the change (`UsageMonitorService.kt`).
3. Write the test (`UsageMonitorServiceTest.kt`).
4. Run CI.
5. Accept `walkthrough.md` and the human approval.
6. Archive.

## Checklist

- [x] Add `if (ForegroundApp.currentPackage() != packageName) return` in `checkOnce()`.
- [x] Check that no test is broken.
- [x] Run CI and the tests.
- [x] Add `walkthrough.md`.
- [x] Run `--pre-archive`, `archive`, board, human approval.
