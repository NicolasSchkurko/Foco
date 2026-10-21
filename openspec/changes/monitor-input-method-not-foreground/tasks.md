# Tasks — monitor-input-method-not-foreground

- [ ] 1. Replace the frozen `inputMethodPackages by lazy` (`UsageMonitorAccessibilityService.kt:45-55`)
      with a re-resolved set: refresh when an event's package is not yet classified (unknown path),
      so late-installed/enabled keyboards land in it — and drop ones that stopped being IMEs.
- [ ] 2. Keep the classification decision on the pure `domain/ForegroundAppState` seam; the service
      only supplies the current set (same shape as `IGNORED_PACKAGES` handling).
- [ ] 3. Regression: `app/src/test/java/com/foco/app/domain/ForegroundAppStateTest.kt` — an unknown
      package that answers as an installed input method does not replace the tracked foreground;
      the app in use stays tracked and `accepts` still holds for it (red before the refresh).
- [ ] 4. Device sanity: install a new keyboard, enable it, type in a limited app past its limit →
      the block still fires (today: silent until the next tick).
- [ ] 5. `gradlew.bat test` green; `node scripts/gates/gate-check.mjs --run-tests` records evidence.
- [ ] 6. Tech brief: complete `walkthrough.md` before `--pre-archive`; comprehension sign-off is
      the human's.

- [ ] 7. Spec delta: `specs/app-limits/spec.md` — ADDED *Input-method classification stays current*;
      `openspec validate monitor-input-method-not-foreground --strict` passes.

## Riskiest slice first

Task 1 + task 3's red-first test; task 4 is the user-visible proof.

## Deps

`monitor-revalidate-foreground-before-block` — both edit `UsageMonitorAccessibilityService.kt`;
that one lands first (the dep also sequences the shared `ForegroundAppStateTest.kt`).
