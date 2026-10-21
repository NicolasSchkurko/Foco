# Tasks — duplicate-block-after-home

## Riskiest first vertical slice

- [x] 1. `ForegroundAppState`: remember the package a block sent home (`blockFinished`) and ignore
      that package's late **view-group** events for a 1-second settling window; a real
      activity/window event is still tracked. Inject the clock for the JVM tests.
- [x] 2. `UsageMonitorAccessibilityService`: record the blocked package when the block is shown,
      pass the event's view-group classification to the state, and call `blockFinished` when the
      countdown ends, before HOME. No event-class filter on tracking.
- [x] 3. JVM regression tests: the late view-group event after a block does not re-track; a real
      reopen inside the settling window does; a real reopen after it does; the existing rules still
      pass.
- [x] 4. `gradlew.bat test` and `gradlew.bat ktlintCheck` green.
- [x] 5. Device pass, clean foreground: opening Instagram (outside its window, so it blocks) shows
      one overlay, sends it home, and no second overlay appears over the launcher. **Reopen
      immediately** (187 ms after the block finished) and it blocks once more over the app (overlay
      828 ms after the block finished); then reopen after the settling window and it blocks again.
      No third overlay. YouTube was inside its allowed window at test time, so Instagram exercised
      the same rule.
- [x] 6. Latency check: the overlay window and its content appear within ~0.5 s of the foreground
      event (the first event may be a view class; it must still block).
- [x] 7. Gate `--run-tests`; walkthrough for the tech brief; comprehension sign-off.
- [x] 8. `--pre-archive`; archive; regenerate the board; commit.
