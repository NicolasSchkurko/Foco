# Tasks

## 1. The gap (riskiest first: the query)

- [x] 1.1 `UsageRepository.loadDay`: widen the lookback of the `queryEvents` call so the event that
  opened a session before the day's start is included; leave `effectiveEnd` as it is.
- [x] 1.2 The pure seam (`domain/`): clamp a session to the day's start, so one opened before the day
  yields `start = startOfDay`, the previous day keeps its own slice, and nothing is counted twice.
- [x] 1.3 Regression, extending `SessionBuilderTest`: an app open across midnight yields a session
  starting at the day's start and ending at the query time; a session opened *inside* the day is
  unchanged; a stop with no open session is still discarded.
- [x] 1.4 Confirm nothing else moved: the day selector, the unlock count and the metrics keep their
  current values for days without a crossing.

## 2. Integration

- [x] 2.1 `gradlew.bat test` + `node scripts/gates/gate-check.mjs --run-tests` (green evidence);
  update `x-gitref` to the branch tip.
- [ ] 2.2 Device pass on the API 36 AVD: leave an app in the foreground across midnight and check its
  minutes show up in the new day.

### Notes on 2.2 (apply of 2026-10-18)

- The crossing itself could not be reproduced in this session: it needs a real midnight with an app left in the foreground, which an apply session cannot wait for. What *was* run instead is the instrumented suite on the API 36 AVD (`connectedDebugAndroidTest`), which exercises the changed repository end to end: the dashboard loads, refreshes and returns to the foreground, the detail screen opens a past day, and the unlock counter reads from the same query. That covers the wiring (wider window + unlock filter + clamp) but **not** a real crossing.
- Known gap that the wider window does not close: an app in the foreground for more than a day has no opening event inside the lookback, so its stop is still discarded. Recorded in the walkthrough's *Limits and failure modes*.


## Workflow follow-up

- Review the apply report, merge, and archive with `gate-check --pre-archive cross-midnight-sessions`.
- Ordering: overlaps `themed-window` on the `data/**` / `test/**` globs (no file collision, no dep declared).
- Tech brief: complete `walkthrough.md` **and** the human's comprehension sign-off before archiving.
