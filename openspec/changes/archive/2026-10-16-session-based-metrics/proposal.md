# Proposal — Session-based usage metrics

## Why

Today the repository eagerly aggregates usage while walking system events: `UsageRepository.loadToday()` (`UsageRepository.kt:45-131`) returns fixed shapes (`DayUsage` with `entries` + `hourly`, `UsageModels.kt:19-23`) and the dashboard consumes them directly. Every new metric or view forces changes in three layers (model + repository + UI) and nothing is unit-testable in the JVM. Phase 2 already anticipates a session-based record (`SessionRecord (pkg, start, end)`, `PLAN.md:66`), so reshaping the data contract now avoids redesigning it later.

## What Changes

- The data layer returns **per-app usage sessions** (`packageName`, `start`, `end`) for the day — the single data primitive ("the metric per app with its log of hours").
- Everything derived — **day total, hourly series, per-app ranking** — is computed in the **UI layer** from those session objects, with pure functions.
- The dashboard keeps showing exactly the same information; only the way it obtains it changes.
- First unit tests in the repo (JVM, `app/src/test/`) plus `test_command` declared, so the gate can produce green evidence.
- Non-goal: any visual change; Room persistence itself (Phase 2).

## Expected Outcome

Room (Phase 2) plugs in without redesigning models or touching the UI: sessions are already the primitive and derived views are pure functions over them. We will know it was worth it when adding a new view or metric over existing data requires **no change in `data/`**.

## Alternatives Considered

| Option | Summary | Why not chosen |
|---|---|---|
| A. Flat sessions as the primitive — **chosen** | `data/` returns the day's sessions; the UI derives total/hours/ranking | — (human decision; aligns with the documented `SessionRecord`) |
| B. Sessions nested per app (`AppUsage(pkg, sessions[])`) | Grouping done in `data/` | The hourly series must re-flatten; Phase 3 limits want a global timeline; more structure for the same data |
| C. Declarative metric registry (descriptors) | Dashboard fully data-driven | Premature abstraction: 3 known metrics, no concrete fourth; migratable later if needed |

## Capabilities

### New Capabilities

- `usage-tracking`: per-app usage for the day is tracked as sessions (the data primitive), and the dashboard's numbers are derived from them in the UI.

### Modified Capabilities

(none)

## Impact

- `app/src/main/java/com/foco/app/data/` — `UsageModels.kt`, `UsageRepository.kt`
- `app/src/main/java/com/foco/app/ui/` — derived-metrics functions + `DashboardScreen.kt` rewiring
- `app/src/test/` — new JVM unit tests (the first ones)
- `gradle/libs.versions.toml`, `app/build.gradle.kts` — test dependencies (JUnit)
- `openspec/config.yaml` — `test_command` declared
