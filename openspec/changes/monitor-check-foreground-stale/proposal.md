# Proposal — monitor-check-foreground-stale

## Why

`UsageMonitorService.checkOnce()` (lines 82-98) reads the current `packageName` from
`ForegroundApp.currentPackage()` **only once**, at the start of the check. It then iterates over the
`limit`, `alertEnabled` and `usedSeconds` values and tries to block or notify. Meanwhile, the user can
switch apps (e.g. from Instagram to YouTube). The periodic monitor (a ticker every 2 minutes) **does
not re-validate** `currentPackage` before `onBlock()` / `onNotify()`. The spec says that "The block
acts only on the app in the foreground" (`spec.md:207-219`).

The result:

- An app that already has an active limit and **reached** it, and that **is currently open** with an
  **exhausted limit**, **blocks** as soon as it is open.
- The 2-minute ticker reads the `packageName` (Instagram) **while it is not in front**, waits for the
  limit (1500 s reached), and raises the block **on Instagram** even though Instagram is not the
  current app. This actually causes a strange activity lifecycle.
- The block driven by `currentPackage()` (the accessibility service) is correct, but the periodic
  monitor is wrongly designed.

The mechanism:

- `UsageMonitorService.checkOnce()` reads `packageName = ForegroundApp.currentPackage()` at the start
  (line 82) and keeps it.
- Then `evaluateLimitCheck(...)` (lines 91-100) validates `limitMinutes` and `usedSeconds` and decides
  the block or the alert.
- `BackgroundAppEvent` is used in a similar way; `UsageMonitorAccessibilityService` uses
  `foreground.accepts()`, which `onWindowEvent` keeps up to date correctly.

The bug is not a bug in the traditional sense: it is a **specification** (the check's input is only
completed at the start).

## What changes

- `UsageMonitorService.checkOnce()` re-evaluates it after every 2-minute `delay`:
  - `current = ForegroundApp.currentPackage()` (that is correct).
  - If `current == packageName` (read earlier), it continues with `evaluateLimitCheck`.
  - If the current package is **different**, it returns early (it blocks no package).
- `usedSeconds` is only re-evaluated when the current package matches; otherwise nothing is blocked
  for a package that is not in front.
- Add a test: a JVM one and a CI one to verify the monitor change (the first test is a `JUnit5` unit
  test).

## Risks

- If `currentPackage()` changes between the first read and the evaluation (`onBlock` / `onNotify`), we
  can block a package that is no longer in front, causing an unexpected lifecycle (the real situation
  we observed).
- The changes in the test runner may affect the CI monitor.
- It is a very small adjustment; it could interrupt the 2-minute cadence, but it actually keeps the
  regular ticker.

## Acceptance

Accepted by the human for /spec-triage as a `fix`, with `tech_brief` if needed (it is detection).

Signed by: AI • 2026-10-21
