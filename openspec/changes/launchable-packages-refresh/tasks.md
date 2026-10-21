# Tasks — launchable-packages-refresh

- [ ] 1. Replace `launchablePackages by lazy` (`UsageRepository.kt:30-36`) with a re-resolved read
      (function call per use, or a set cached with an explicit invalidation) and keep
      `launchablePackageNames()` (`:52`) returning the current set.
- [ ] 2. Confirm every consumer goes through the refreshed read: session filter (`:111`), the
      monitor's `usedSecondsToday` path, dashboard/detail queries, and the limits screen's picker.
- [ ] 3. Regression: `app/src/test/java/com/foco/app/data/SessionFilterTest.kt` — a package that
      becomes launchable after the repository exists is counted (fails today).
- [ ] 4. Verify the monitor case end to end on device: install an app, give it a limit while Foco
      is running, use it past the limit → block fires without restarting Foco.
- [ ] 5. `gradlew.bat test` green; `node scripts/gates/gate-check.mjs --run-tests` records evidence.
- [ ] 6. Tech brief: complete `walkthrough.md` before `--pre-archive`; comprehension sign-off is
      the human's.

- [ ] 7. Spec delta: `specs/usage-tracking/spec.md` — MODIFIED *Only user-facing apps count* (+ the
      late-install scenario); `openspec validate launchable-packages-refresh --strict` passes.

## Riskiest slice first

Task 1 (the re-resolve) with task 3's red-first test; task 4 is the acceptance evidence for the
user-visible defect.

## Deps

None. No other active change claims `UsageRepository.kt`.
