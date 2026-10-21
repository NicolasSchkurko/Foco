# Proposal — monitor-revalidate-foreground-before-block

## Why

The accessibility request gate checks the foreground **once**, before the block coroutine runs:
`requestBlock` validates `foreground.accepts(packageName)` (`UsageMonitorAccessibilityService.kt:84-90`)
and returns; `evaluate()` (`:92-139`) then suspends on the Room reads (`windowFor`/`limitFor`) and on
`evaluator.usedSecondsToday` before calling `showBlock(packageName, reason)` at `:118`. In any of those
suspension windows a `TYPE_WINDOW_STATE_CHANGED` updates the tracked foreground (another app comes to
front, or Foco itself clears it — `ForegroundAppState.kt:36-38`), and **nothing re-checks it**. The
result is the overlay of app A shown over app B, and `finishBlock()` three seconds later sending B to
the home screen.

Contract already in force:

- `openspec/specs/app-limits/spec.md:482-483` — "A block SHALL run only for the package still tracked
  as foreground, so it never shows over Foco or sends Foco home."
- Scenario "A delayed check" (`:495-498`) — the request is dropped when Foco took over.
- `DECISIONS.md:436` (stale-foreground-block) — "a stale request is dropped instead of starting a
  block"; the drop exists only before the `launch`, never inside `evaluate`.

The in-flight change `monitor-check-foreground-stale` targets `UsageMonitorService.kt` (the alert
path of the periodic ticker) and does **not** cover this file. This is the residual of the archived
`stale-foreground-block`, now at the coroutine level.

## What changes

- `evaluate()` re-validates `foreground.accepts(packageName)` after its suspending reads, immediately
  before `showBlock`, and drops the request when the tracked foreground changed.
- Regression tests: a JVM test that pins the drop decision when the tracked package changes between
  request and block, and an instrumented case in `UsageMonitorServiceTest` that an app switch during
  evaluation leaves the foreground app untouched.

## Risks

- A legitimate block can be dropped if the foreground changes in the microseconds between the
  re-check and `showBlock`; the next accessibility event or the 2-minute tick re-evaluates it —
  availability cost of at most one check, never a wrong-app block.
- The check reads shared state from the monitor thread; it must use the existing pure
  `domain/ForegroundAppState` seam, not new synchronization.

## Spec delta

`specs/app-limits/spec.md` adds *The block is re-validated right before it shows* — the timing rule the existing foreground requirement leaves implicit (it states which package a block may target, not the re-check after its suspending reads), carrying the regression citation.

## Origin

Filed 2026-10-21 by the anchored bug-finder audit (finding 1, severity high); `kind: fix` per
/spec-triage. Awaiting human acceptance — no code written.
