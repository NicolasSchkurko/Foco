# Tasks — test-suite-speed

- [x] 1. Add `NoFixedSleepTest` (JVM) that scans `app/src/androidTest` for `Thread.sleep` and reports
      the file.
- [x] 2. Replace each fixed wait with the matching event-driven wait: `waitUntil` for Compose/text
      state and the platform power/keyguard/usage state for device setup.
- [x] 3. Keep the current assertions and test names; do not move a test to another suite.
- [x] 4. Run the five modified instrumented classes on the emulator (all green). No controlled
      before/after wall-clock baseline was recorded in this apply.
- [x] 5. Run `gradlew.bat test` and `openspec validate test-suite-speed --strict`.
