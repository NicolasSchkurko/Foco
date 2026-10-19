# Proposal — The day's query window is computed by a pure function

## Why

`/spec-verify` on `cross-midnight-sessions` (archived 2026-10-18) filed this as finding 2: that change widened the day's query (it now looks back a day) and filtered the unlock marks to the day, but **nothing pins those numbers**. Its `[@test]` is the pure fold (`SessionBuilderTest`), which proves the clamp arithmetic; the four timestamps the adapter asks the platform for live inline in `UsageRepository.loadDay` — a file that cannot run in a JVM test because it is Android platform access. So today a silent edit to `lookbackStart` or to the unlock filter would go unnoticed.

It is also where this repo's own layer rule points: *"Pure computation helpers SHALL live in `domain/`, never in `data/`"* (`openspec/specs/code-structure/spec.md:21`). The date arithmetic is pure — it reads a clock value passed in, not the platform.

## What Changes

- New `domain/DayWindow.kt`: `DayWindow(startOfDay, dayEndExclusive, lookbackStart, effectiveEnd)` plus `dayWindow(date, zone, now)` — the same arithmetic that lives inline today, moved to the pure layer.
- `UsageRepository.loadDay` calls it and keeps only what is genuinely platform access: the query, the event loop and the clamp (which is already pure).
- New `DayWindowTest` pins the four numbers: the day's boundaries for the requested date, the effective end (`now` while the day is running, the day's end once it is past), and the lookback exactly one day before the start.
- No behavior changes: same numbers, same query, same results.

## Proof (a refactor changes no behavior)

The existing suite is the proof — `SessionBuilderTest` (the folding and clamping), `UsageMetricsTest`/`AppDetailMetricsTest` (the numbers the UI derives) and the instrumented dashboard tests all keep passing — and the new `DayWindowTest` is the added oracle for the numbers that had none. Unlike `layer-check-scope` there is no red-before to show: nothing was broken, the numbers were simply unreadable from outside the adapter.

## Alternatives considered

| Option | Why yes | Why not / status |
|---|---|---|
| **A pure `dayWindow(...)` in `domain/` + a JVM test pinning its four numbers** | Makes the window testable and follows the repo's own "pure helpers live in `domain/`" rule; no new dependency; the adapter keeps only platform work | **Chosen (human, 2026-10-19)** |
| Test the adapter directly (Robolectric, or an instrumented test) | No production code moves | Robolectric is a new dependency for one assertion, and an instrumented test cannot assert the query window either (the platform decides what it returns) — **rejected** |
| Leave the numbers inline and add a comment | Zero risk | A comment is not a test; the finding stays open — **rejected** |
| Move the whole `loadDay` into `domain/` | Everything testable | `day/` would stop being platform access and would lose the query — the layer rule forbids it — **rejected** |

## Impact

- `app/src/main/java/com/foco/app/domain/DayWindow.kt` (new), `app/src/main/java/com/foco/app/data/UsageRepository.kt` (calls it), `app/src/test/java/com/foco/app/domain/DayWindowTest.kt` (new).
- Ordering: `domain/**` and a new file under `domain/` are inside the globs three drafts claim (`same-millisecond-transitions`, `english-source-prose`, `trim-long-comments`), so applying this change will require sequencing them (they land after it) — the same bookkeeping the last two applies needed.
- No protected zones, no new dependency, no platform API added: **no tech brief**.
