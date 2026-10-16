# Tasks

## 1. The arrow

- [ ] 1.1 Add a back arrow (top-left) to the detail header in `AppDetailScreen.kt`, wired to the back dispatcher (e.g. `LocalOnBackPressedDispatcherOwner` — no signature changes to `FocoNavHost`); verify visually on the device.
- [ ] 1.2 Extend `AppDetailNavigationTest`: tapping the arrow returns to the dashboard (reuses the existing e2e setup); verify `gradlew.bat connectedDebugAndroidTest` green (API 36 AVD).
- [ ] 1.3 `gradlew.bat test` (JVM) + `assembleDebug` + the gate with green evidence (`node scripts/gates/gate-check.mjs --run-tests`); update `gitref`.

## Workflow follow-up

- Review the apply report, merge, and archive with `gate-check --pre-archive detail-back-arrow`.
- Ordering (pre-declared per the A+ queue policy): `app-settings` lands after this change (dep already declared). Chain: `ranking-exclusions → detail-back-arrow → app-settings → app-entry-gate`.
