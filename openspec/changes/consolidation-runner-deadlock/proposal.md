# Proposal — consolidation-runner-deadlock

## Why

`ConsolidationRunner` protects its shared run with a `Mutex` (`gate`) and tracks `inFlight` in a field.
If the user's call to `run()` (app start or nightly job) throws an exception (database error,
usage-stats reading failure, etc.), the `finally` block inside `run()` attempts to acquire the same mutex **again** while the thread already holds it. The `Mutex` used by Kotlin coroutines is **not reentrant**, so the `withLock` inside `finally` blocks indefinitely, causing a deadlock. The `run()` never returns, so:

- The app start on launch never shows the dashboard (stuck waiting for the consolidation).
- The nightly job's `Worker` never receives `Result.success()` and will retry every minute.

The spec requires that a failure leaves the system usable (`gate-check --run-tests` records green evidence only on success). This deadlock breaks that guarantee and breaks the offline guarantee the foundation promised.

## What changes

- `ConsolidationRunner.run()` uses a `SupervisorJob` scope that stays alive; we inject a **custom Mutex** that is reentrant (or we restructure to avoid re-entrancy). The easiest fix is to make the `gate` a `ReentrantMutex` or, better, split the `inFlight` access so the `finally` does not need to lock.
- `ConsolidationRunner.run()` tracks the `inFlight` with a plain `Deferred<ConsolidationOutcome>?` (no lock) protected by the caller (`run()`, the shared branch of the app-start or nightly job). The `finally` sets `inFlight = null` without acquiring a mutex.
- The `forApp` shared singleton ensures only one consolidation runner exists; its `run()` is the only path that writes `inFlight` or reads it (so it is safe to remove the mutex).
- We add a **JVM test** that seeds the store and kills the `consolidate` method (throwing `IOException`) to verify that the runner recovers and returns `ConsolidationOutcome.Failed`, not deadlock.

No persistence, permission, monitor-cadence, UI, or device change.

## Risks

- Multi-threaded error handling in a small code path; the `async { ... }.also { inFlight = it }` must not introduce race conditions when the later `finally` writes (if we keep a lock, we must ensure no two waiters race).
- `run()` returns `Deferred<ConsolidationOutcome>`; callers must `.await()`; existing callers are the **same** (the service and worker use `runnerFor(...).run()`), and they already await it.
- `ConsolidationRunner` is a singleton; its `gate` and `scope` are also singletons, so fixing deadlock only requires protecting the in-flight flag; no global side effects.

## Acceptance

Accepted by the human for the /spec-triage queue as a fix for the deadlock, with `tech_brief: true` (platform concurrency, persistence, deadlock).

Signed by: AI • 2026-10-21