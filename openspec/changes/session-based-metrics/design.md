# Design

## Context

Usage is read from `UsageStatsManager` events and eagerly aggregated inside `UsageRepository.loadToday()` (`UsageRepository.kt:45-131`) into fixed shapes (`UsageModels.kt:19-23`); the dashboard consumes those shapes directly (`DashboardScreen.kt:143-230`). There are no tests and no `test_command` yet, so the gate cannot produce green evidence. See proposal.md for motivation.

## Goals / Non-Goals

**Goals:**
- One data primitive for usage: per-app sessions (package, start, end).
- Day metrics derived from it in the UI layer with pure functions.
- Same dashboard information; zero visual change.
- First JVM unit tests plus `test_command`, so the gate can verify with real evidence.

**Non-Goals:**
- Room persistence (Phase 2) — this change only makes the data contract compatible with it.
- New metrics or views; the dashboard shows exactly what it shows today.
- Performance work: a day of sessions is at most a few hundred items; passing the list to the UI is cheap.

## Decisions

- **D1 — Flat session list, not nested per app.** Chosen over alternatives B/C (see proposal.md). A global timeline is what Phase 3 (limits/friction) needs, and every view derives from it with a single grouping pass.
- **D2 — The session fold is a pure function.** `buildSessions(events)` has no Android dependency; `UsageRepository` only maps `UsageEvents` to the plain event type, resolves the package policy sets (launcher/home/self via `PackageManager`) and applies the pure filter. Rationale: the trickiest logic (cross-app switches, open session) becomes JVM-testable.
- **D3 — Derived metrics live in the UI layer** (e.g. `ui/metrics/`) as pure functions over sessions: total, hourly distribution (port of `UsageRepository.kt:144-153`), ranking. `AppUsageEntry`/`HourBucket` change role: they become derivation outputs (UI-side) instead of repository outputs.
- **D4 — Labels and icons resolve in the UI.** Icon resolution already lives in the UI (`DashboardScreen.kt:233-262`); labels follow the same pattern, so `data/` returns framework-free session objects only.

## Risks / Trade-offs

- Porting the hour-distribution logic could change rounding behavior → mitigated with unit tests on boundary cases (10:50–11:10).
- Derivations run on each recomposition → memoize at the call site (`remember(usage)`); correctness is unaffected.
- The first test infrastructure touches Gradle config → task 1 gates everything else; if `gradlew.bat test` misbehaves on this machine, stop and report.

## Migration Plan

No persisted data exists and the app is not released, so there is nothing to migrate: Phase 2 persistence arrives after this contract.
