# Tasks

## 1. Data (the base)

- [x] 1.1 `model/day/DayData.kt` (new): `sessions: List<UsageSession>` + `unlockTimes: List<Long>`; `UsageRepository.loadDay` maps `KEYGUARD_HIDDEN` events and returns `DayData`; adapt the callers (`DashboardScreen`, `AppDetailScreen` use `.sessions`); verify the build and the dashboard unchanged.
- [x] 1.2 Dashboard subtitle: "Uso en pantalla: X · N desbloqueos" (N = `unlockTimes.size`, same reloads).

## 2. Tests + device

- [x] 2.1 `UnlockCountTest` (instrumented): generate 2 unlocks via shell (`input keyevent KEYCODE_SLEEP`, `KEYCODE_WAKEUP`, `input keyevent 82`) → launch → the subtitle contains "desbloqueos" and the number is ≥ 2. If the AVD does not emit `KEYGUARD_HIDDEN`, enable the swipe lockscreen and retry (note the finding in the report).
- [x] 2.2 Manual pass on the API 36 AVD: generate unlocks, open the app, screenshot with the count.

## 3. Integration

- [ ] 3.1 `gradlew.bat test` (JVM) + `assembleDebug` + the gate with green evidence (`node scripts/gates/gate-check.mjs --run-tests`); update `gitref`.

## Workflow follow-up

- Review the apply report, merge, and archive with `gate-check --pre-archive unlock-counter`.
- Tech brief: complete `walkthrough.md` (done at creation) + **your** sign-off in `/spec-verify` before `--pre-archive`.
- Ordering (pre-declared per the A+ policy): after `detail-back-ux`; `app-settings` waits for it; `app-entry-gate` follows transitively.
