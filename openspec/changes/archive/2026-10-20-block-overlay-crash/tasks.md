# Tasks — block-overlay-crash

## Riskiest first vertical slice

- [x] 1. Reproduce on the device with `adb logcat -b crash`: `IllegalStateException: Composed into
      the View which doesn't propagate ViewTreeSavedStateRegistryOwner!` /
      `consumeRestoredStateForKey ... CREATED`, and `dumpsys accessibility` listing Foco under
      `Crashed services`.
- [x] 2. Extract `OverlayLifecycleOwner` + `BlockOverlayHost` in `ui/limits/`: the owner attaches and
      restores its saved-state registry and reaches CREATED before content; the host adds the view
      through a `data/` `OverlayWindow` adapter (layer rule) and cleans up on failure.
- [x] 3. `UsageMonitorAccessibilityService.showBlock()` uses the host; the service no longer builds
      an owner or touches `WindowManager` itself.
- [x] 4. Regression tests in `AppLimitBlockTest`: the owner is restored, reaches CREATED, consumes a
      key without throwing, reaches RESUMED/DESTROYED, and a DESTROYED owner refuses RESUMED.
- [x] 5. `gradlew.bat test` and `gradlew.bat ktlintCheck` green; APK installed on the Pixel and the
      service is bound with `Crashed services:{}`.
- [x] 6. Device end-to-end with a 0-minute limit on Instagram, twice in a row: the overlay renders
      (screenshot), counts 3-2-1, sends the app home, and no `evaluate failed` is logged.
- [x] 7. Second defect found live: the host kept one owner across blocks, so the second block moved
      a DESTROYED owner to RESUMED and killed the service. The host now builds a fresh owner per
      block.
- [x] 8. Third defect found live: the block text rendered black on the dark surface (no
      `LocalContentColor` outside a `Surface`), so the overlay looked blank in dark mode. The screen
      now wraps its content in a `Surface`.
- [x] 9. Comprehension sign-off recorded (Nschkurko, 2026-10-20), `--pre-archive` OK, archived.
