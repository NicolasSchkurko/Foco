# Walkthrough — monitor-revalidate-foreground-before-block

## Mechanism

`UsageMonitorService`'s ticker and accessibility events call `requestBlock(pkg)` on
`UsageMonitorAccessibilityService`. The gate `foreground.accepts(pkg)` runs once on the monitor
thread (`UsageMonitorAccessibilityService.kt:88`). `evaluate()` then runs on `Main.immediate` and
suspends twice on data reads before `showBlock` (`:118`). Between gate and show, the accessibility
callback may update `ForegroundAppState.current` (another app to front, or Foco clearing it). The
fix adds the same `foreground.accepts(packageName)` check after the suspensions, so the block only
starts for the package still tracked as foreground — the invariant `app-limits/spec.md:482-483`
already states.

## Physical traceability

- `app/src/main/java/com/foco/app/UsageMonitorAccessibilityService.kt:84-139` — gate, evaluate,
  the new re-check before `showBlock`.
- `app/src/main/java/com/foco/app/domain/ForegroundAppState.kt` — the pure state object both reads
  use (no second source of truth).
- `app/src/test/java/com/foco/app/domain/ForegroundAppStateTest.kt` — JVM proof of the drop.
- `app/src/androidTest/java/com/foco/app/data/UsageMonitorServiceTest.kt` — device proof that the
  foreground app is untouched.

## Limits and failure modes

- If the foreground changes in the microseconds after the re-check and before the overlay attach,
  the block still shows for the old app; the window is a few instructions with no suspension. The
  residual is bounded by the next event/tick re-evaluation and is strictly narrower than today's
  multi-second window.
- A dropped request costs at most one 2-minute tick of delayed enforcement for a legitimately
  over-limit app — availability over precision, matching the archived stale-foreground-block ADR.

## Touched APIs

Kotlin coroutines (`Main.immediate`, suspension points) and the accessibility service's existing
window-event callback. No new platform API, no permission, no storage or network.

## Implicit decisions

- The re-check mirrors the entry gate instead of moving the whole evaluation to the monitor thread:
  smallest diff, same seam, no new concurrency.
- The check runs once, immediately before `showBlock`, not after every line — one suspension point
  remains between check and show (see Limits), accepted.
