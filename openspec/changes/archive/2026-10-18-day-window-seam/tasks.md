# Tasks

## 1. The extraction (no behavior change)

- [x] 1.1 New `app/src/main/java/com/foco/app/domain/DayWindow.kt`: a `DayWindow` value with `startOfDay`, `dayEndExclusive`, `lookbackStart` and `effectiveEnd`, plus `dayWindow(date: LocalDate, zone: ZoneId, now: Long)` — the arithmetic that lives inline in `UsageRepository.loadDay` today, moved verbatim (`java.time` is pure: `domain/` may use it).
- [x] 1.2 `UsageRepository.loadDay` calls `dayWindow(...)` and keeps only platform work: the `queryEvents` call, the event loop and the (already pure) clamp.

## 2. The test (the oracle the finding asked for)

- [x] 2.1 New `app/src/test/java/com/foco/app/domain/DayWindowTest.kt` (JVM): the day's boundaries for a fixed date and zone; the effective end is `now` while the day runs and the day's end when it is past; the lookback is exactly the previous day's start.
- [x] 2.2 Cover the boundary the finding cares about: `lookbackStart == startOfDay − (dayEndExclusive − startOfDay)`, i.e. one day, and the window used for the crossing case of `cross-midnight-sessions` reconstructs the same numbers the adapter used to compute inline.

## 3. Integration

- [x] 3.1 `gradlew.bat test` green: `DayWindowTest` plus the untouched suite (`SessionBuilderTest`, the metrics tests, the structure rules).
- [x] 3.2 `node scripts/gates/gate-check.mjs --run-tests` (green evidence); update `x-gitref` to the branch tip.
- [x] 3.3 Declare the sequencing the apply needs: `same-millisecond-transitions`, `english-source-prose` and `trim-long-comments` claim the globs this change lands in (`domain/**`, `app/src/**`) — each gets a dep on this change (they land after it), as with the two previous applies.

## Workflow follow-up

- Review the apply report, merge, then archive with `gate-check --pre-archive day-window-seam`.
- Closes finding 2 of the archived `cross-midnight-sessions` (`archive/2026-10-18-cross-midnight-sessions/findings.md`). Finding 1 (the requirement's wording) is the sibling change `lookback-bound-wording`.
- No tech brief: no walkthrough, no comprehension sign-off before archiving.

### Notes on the apply (2026-10-19)

- The window arithmetic moved verbatim: same `java.time` calls, same `minOf(dayEndExclusive, now)`. `UsageRepository.loadDay` keeps the query, the event loop and the clamp.
- `DayWindowTest` pins the four numbers (day boundaries, effective end for a running and a past day, the lookback exactly one day back, and the crossing sanity). `LayerRulesTest` and `OneTypePerFileTest` stay green with the new `domain/` file.
- Sequencing declared while applying (the gate escalated two overlaps to errors): `english-source-prose` and `trim-long-comments` now carry a dep on this change. `same-millisecond-transitions` needed none — its globs cover `domain/**` and `app/src/test/**`, and this change's files there are new (untracked at that moment), so no file matched.
- A refactor has no red-before to show: nothing was broken, the numbers were simply unreachable from a JVM test. The suite (10 session tests, 4 window tests, the metrics tests) is the proof of no behavior change.
