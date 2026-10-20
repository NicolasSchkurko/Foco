# Tasks — ring-move-without-loader

## 1. The home keeps its position until the next one is ready

- [x] 1.1 Add `ui/dashboard/RingContent.kt`: a sealed `RingContent` with `Day(data, unlocks)` and
      `Week(totals)`, the data a ring position renders once it is ready.
- [x] 1.2 In `DashboardScreen`, split the position in two: `requestedKey` (what the arrows, the bars,
      the history and the back action move) and `shownKey` (what the header and `AnimatedContent`
      render). Both start at the restored position.
- [x] 1.3 Load the requested position into a `mutableStateMapOf<String, RingContent>`; advance
      `shownKey` only when its entry exists. Keep the previous entry for the exit side of the
      transition. A day reads through `UsageRepository.loadDay` (today) or
      `SessionHistoryStore.sessionsFor`/`unlocksFor` (a finished day); the week reads
      `SessionHistoryStore.week`.
- [x] 1.4 Keep today's live read under `repeatOnLifecycle(RESUMED)` while today or the week is the
      requested position (first read on entry, then every 30 s). It fills the day cache and the
      week's today bar; a request waits for it instead of issuing a second read.
- [x] 1.5 Build the header from `shownKey` and its cached content: title, usage line and the
      week-only export action. Drop the "Loading…" subtitle fallback and tag the remaining
      placeholder (`ring_loading`) so a test can assert its absence.

## 2. The week renders what the home hands it

- [x] 2.1 Change `WeekContent` to take `totals`, `todayData` and `excludedPackages`; remove its own
      repository, store, load seam, excluded-apps read and spinner. Keep `deriveWeekMetrics` and the
      bars/ranking rendering as they are.

## 3. Tests

- [x] 3.1 Add the regression to `DashboardRingTest`: seed yesterday in the store, hold `loadDay` on a
      gate, move back to today, and assert that no `ring_loading` node exists and yesterday's title is
      still on screen; release the gate and assert today appears.
- [x] 3.2 Keep `DashboardRingTest`, `WeekScreenTest`, `DashboardRefreshTest` and `RingMotionTest`
      green (the refresh counting and the ring's titles must not change).
- [x] 3.3 Run `gradlew.bat test`, the instrumented ring/week tests on the device, and the gate.

## 4. Close

- [x] 4.1 `walkthrough.md` (tech brief: readiness state, the cache's lifetime, the read cadence) and
      the human's comprehension sign-off.
- [x] 4.2 `openspec validate ring-move-without-loader --strict` and `node scripts/gates/build-board.mjs`.
