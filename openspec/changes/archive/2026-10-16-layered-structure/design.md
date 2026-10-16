# Design

## Context

See proposal.md for the current layout and its problems. The pure/computation core already exists as functions (`buildSessions`, `filterSessions`, the metrics) but lives physically inside `data/` and `ui/metrics/`, with multiple declarations per file. `session-based-metrics` established the session contract and the first JVM tests; nothing is persisted yet, so a reorganization is cheap now and expensive later.

## Goals / Non-Goals

**Goals:**
- Predictable location: file `X.kt` = type `X`; cohesive files only for function groups.
- Four explicit layers with a one-way dependency stack: `ui → data → domain → model`.
- `model/` and `domain/` with zero Android/Compose imports — the "pure core" is a real, testable boundary.
- Zero behavior change; existing suite (re-homed) proves it.

**Non-Goals:**
- Renaming symbols or changing function signatures (moves only).
- Splitting function groups further (`Formatting.kt`, `UsageMetrics.kt` stay as cohesive units).
- Enforcing composable-per-file with a test (the split is a task; the machine rule covers types).

## Decisions

- **D1 — Four layers, downward dependencies.** `model/` (types), `domain/` (pure behavior), `data/` (Android access: `UsageRepository`, `UsageAccess`), `ui/` (Compose + derived metrics). Direction: `ui → data → domain → model`; `LayerRulesTest` fails on upward imports and on framework imports in `model/`/`domain/`.
- **D2 — One type per file, named after it** (enforced by `OneTypePerFileTest`). Private composables also get their own files; the machine rule covers types only because function-only files like `Formatting.kt` are legitimate cohesive units.
- **D3 — `MainActivity` stays in the root package** (Android entry point); the routing composable (`App`) moves to `ui/App.kt` because it is presentation logic (permission check + screen switch).
- **D4 — Moves, not redesigns.** `UsageMetrics` stays in `ui/metrics/` (presentation math, per `session-based-metrics` D3); session math moves from `data/` to `domain/` unchanged. Package moves touch every import but no signature.
- **D5 — Enforcement via dependency-free JVM tests scanning the source tree.** ktlint/detekt rejected: stock rules do not express one-type-per-file; the scan is small, runs with the normal suite, and fails loudly on parse problems.

## Risks / Trade-offs

- Package moves touch every file → import churn; mitigated by the suite (`gradlew.bat test`) and the build (`assembleDebug`).
- Source-scanning tests are heuristic (regex on top-level declarations and imports); acceptable for this repo — a mis-parse fails the test rather than silently passing.
- Git history continuity across moves is lost to plain `log`; acceptable in an early project (use `--follow` when needed).
- Layer rules cover `app/src/main/java` only; unit tests mirror the layout by convention, not by enforcement.

## Migration Plan

No persisted data and no release: a single pass over `app/src/**`. The rules apply from then on; Phase 2 code lands directly in the new layout. `ARCHITECTURE.dsl` (created by `/diagram-sync`) mirrors these layers as the structural source of truth.
