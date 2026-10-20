# Tasks — startup-history-gate

- [ ] 1. Remove the fire-and-forget `consolidateHistory()` from `MainActivity`; keep the nightly
      `ConsolidationScheduler`.
- [ ] 2. In `App`, after usage access is granted, run `ConsolidationRunner.forApp(...).run(today)` on
      IO and show a loading state until it ends; mount `FocoNavHost` only after it ends. On failure,
      clear the loading state and let the next start retry.
- [ ] 3. Add a `startupLoad` seam to `App` so tests can suspend the gate.
- [ ] 4. Update `AppEntrySeamTest` with the no-op startup seam; add `StartupLoadingTest` that asserts
      the loading state before and the dashboard after the deferred completes.
- [ ] 5. Run `gradlew.bat test`, the startup/entry tests, `WeekScreenTest` and the gate.
- [ ] 6. Verify on the Pixel: first open after permission shows loading then a complete week; granting
      permission from onboarding also works.
- [ ] 7. Run `openspec validate startup-history-gate --strict` and `build-board.mjs`.
