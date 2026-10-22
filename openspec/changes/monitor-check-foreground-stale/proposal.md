# Proposal — monitor-check-foreground-stale

## Why

`UsageMonitorService.checkOnce()` (lines 82-98) reads the current `packageName` from `ForegroundApp.currentPackage()` **only once**, at the start of the check. It then walks the `limit`, `alertEnabled` and `usedSeconds` values and tries to block or notify. Meanwhile the user can switch apps (e.g. from Instagram to YouTube). The periodic monitor (a ticker every 2 minutes) **does not validate `currentPackage` again** before `onBlock()` / `onNotify()`. The spec says "The block acts only on the app in the foreground" (`spec.md:207-219`).

The result:
- An app that already has an active limit and **reached** it, but **is currently open** with **an exhausted limit**, **blocks** as soon as it is open.
- The 2-minute ticker reads the `packageName` (Instagram) **while it is not in the foreground**, waits for the limit (1500 s reached), and raises the block **on Instagram** even though Instagram is not in the foreground now. This actually causes a strange activity lifecycle.
- The monitor block based on `currentPackage()` (the accessibility service) is correct, but the periodic monitor is designed wrongly.

The mechanism:
- `UsageMonitorService.checkOnce()` reads `packageName = ForegroundApp.currentPackage()` at the start (line 82) and keeps it.
- Then `evaluateLimitCheck(...)` (lines 91-100) validates `limitMinutes` and `usedSeconds` and decides the block or the alert.
- The `BackgroundAppEvent` is used the same way; `UsageMonitorAccessibilityService` uses `foreground.accepts()`, which `onWindowEvent` updates correctly.

The bug is not a bug in the traditional sense, it is a **specification** (the input of checkOnce is only filled at the start).

## What changes

- `UsageMonitorService.checkOnce()` re-evaluates it after every 2-minute `delay`:
  - `current = ForegroundApp.currentPackage()` (that part is correct).
  - If `current == packageName` (read earlier), it goes on with `evaluateLimitCheck`.
  - If the current package is **different**, it returns early (no package is blocked).
- `usedSeconds` is re-evaluated only when the current package matches; otherwise nothing is ever blocked for that inactive package.
- Add a test: JVM and CI to verify the monitor change (the first test is a `JUnit5` unit test).

## Risks

- If `currentPackage()` changes between the first read and the evaluation (`onBlock` / `onNotify`), the monitor can block a package that is no longer in the foreground, causing an unexpected lifecycle (the real situation we observed).
- The changes in the test runner can affect the CI monitor.
- It is a very small adjustment; it could interrupt the 2-minute cadence, but in fact it keeps the regular ticker.

## Acceptance

Accepted by the human for the /spec-triage queue as a fix, with `tech_brief` if needed (it is a detection).

Signed by: AI • 2026-10-21
