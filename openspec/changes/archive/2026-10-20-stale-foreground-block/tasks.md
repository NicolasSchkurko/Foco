# Tasks — stale-foreground-block

## Riskiest first vertical slice

- [x] 1. Add `domain/ForegroundAppState.kt`: a normal window event tracks that app, Foco's own
      package clears the track, System UI and input methods leave it alone, and `accepts()` answers
      only for the current package.
- [x] 2. JVM regression tests in `app/src/test/java/com/foco/app/domain/ForegroundAppStateTest.kt`,
      including the stale request after Foco comes to the front.
- [x] 3. `UsageMonitorAccessibilityService` delegates to the state: `onAccessibilityEvent` evaluates
      only the returned package, `currentPackage()` reads it, `requestBlock` drops a stale request,
      and the boundary job compares against the same value.
- [x] 4. `gradlew.bat test` and `gradlew.bat ktlintCheck` green.
- [x] 5. Device pass: with a limit crossed while an app is open, return to Foco from recents and
      from the monitor notification; no block over Foco and no Foco home; the app blocks on its
      next foreground. (The monitor notification is not rendered in the shade on this device; its
      PendingIntent is the same `MainActivity` exercised by bringing the task to the front.)
- [x] 6. Gate `--run-tests`; walkthrough for the tech brief; comprehension sign-off; archive.
