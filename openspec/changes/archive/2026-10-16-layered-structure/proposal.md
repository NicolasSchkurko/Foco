# Proposal — Layered code structure, one declaration per file

## Why

The codebase mixes layers and packs multiple declarations per file: `data/Sessions.kt` holds the session type, the event type and the event-kind enum (3 types); `ui/metrics/UsageMetrics.kt` holds three types plus four functions; `ui/DashboardScreen.kt` holds the screen plus four private composables; `MainActivity.kt` holds the activity plus the routing composable; and the root package mixes the entry point with `UsageAccess`. Nothing tells you where a new piece should live, and Phases 2–4 (Room persistence, limits, Reels blocking) will pile more code on top.

The user wants: every named declaration in its own file, and folders organized by layers.

## What Changes

- New layout — four layers: `model/` (pure data types) · `domain/` (pure behavior) · `data/` (Android platform access) · `ui/` (Compose presentation, with `dashboard/`, `onboarding/`, `metrics/`, `theme/`). `MainActivity.kt` stays at the root as the Android entry point; the routing composable moves to `ui/App.kt`.
- Every class/interface/object/enum gets its own file named after it; the private composables (`HourChart`, `AppUsageRow`, `AppLabel`, `AppIcon`) get their own files too. Groups of top-level functions stay cohesive (`Formatting.kt`, `UsageMetrics.kt`).
- Two JVM tests enforce the rules going forward: one type per file, and layer rules (`model/` `domain/` free of Android/Compose imports; dependencies point downward only: `ui → data → domain → model`).
- Zero behavior change: same UI, same numbers, the existing suite re-homed and green.
- Non-goals: new features, symbol renames, signature changes of the computation functions. Sequencing: independent from `dashboard-auto-refresh` (human decision) — target overlaps stay visible as warnings and whichever change applies second adapts.

## Expected Outcome

"Find everything by folder + name": with the project open, `model/ · domain/ · data/ · ui/` says what each thing is and `X.kt` is declaration `X`. We will know it was worth it when the first Phase 2 class (e.g. a Room entity) has an obvious home.

## Alternatives Considered

| Option | Summary | Why not chosen |
|---|---|---|
| B. `model/` + `domain/` + `data/` + `ui/` — **chosen** | Types separate from pure behavior; four explicit layers | — (human decision) |
| A. `domain/` + `data/` + `ui/` | Three layers; session types and behavior share `domain/` | Keeping pure types in their own layer makes the data-flow boundary explicit |
| C. Split files without moving packages | Minimal risk, no package churn | Does not deliver the requested layering |
| Enforcement: structure tests — **chosen** | Dependency-free JVM tests: one type per file + layer rules | — (human decision) |
| Enforcement: none | Less work now | The rule would rot; structure requirements would need `deferred-test` waivers |
| Enforcement: ktlint/detekt | Industry tooling | Stock rules do not enforce one declaration per file; plugin + config weight without the needed rule |

## Capabilities

### New Capabilities

- `code-structure`: enforceable layout rules for the source tree (one type per file; explicit layers; pure layers free of framework imports; downward dependencies).

### Modified Capabilities

(none)

## Impact

- `app/src/main/java/com/foco/app/**` — every file moves or splits; packages become `com.foco.app.model`, `.domain`, `.data`, `ui.dashboard`, `ui.onboarding`, `ui.metrics`
- `app/src/test/java/com/foco/app/**` — tests move to mirror the layers; two new structure tests
- No build-file changes (JUnit already present from `session-based-metrics`)
