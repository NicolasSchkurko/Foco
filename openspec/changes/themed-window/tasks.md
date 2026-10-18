# Tasks

## 1. The fix (riskiest first: the relaunch/theme interaction)

- [x] 1.1 `data/AppAppearance.kt`: map Apariencia → modo appcompat (AUTO→FOLLOW_SYSTEM, Claro→NO, Oscuro→YES) and apply it through a named wrapper; JVM `AppAppearanceTest` pins the mapping (the mode constants are inlined, so the assertion runs on the JVM).
- [x] 1.2 `MainActivity`: apply the stored appearance at startup (same pre-content read as the language) and react to later picks; `FocoTheme` keeps resolving the Compose scheme.
- [x] 1.3 Window background = Compose surface color (dynamic colors included) through a named effect in `MainActivity`, so relaunches and transitions show the chosen scheme.
- [x] 1.4 Instrumented: `SettingsScreenTest` asserts `AppCompatDelegate.getDefaultNightMode()` follows the pick (Oscuro→YES, Automático→FOLLOW_SYSTEM); the rest of the flow stays green.
- [ ] 1.5 Visual pass (AVD/device): language change with the chosen scheme's background; Oscuro over a light system without the white fade; cold start with a stored Oscuro.

## 2. Integration

- [x] 2.1 `gradlew.bat test` + `connectedDebugAndroidTest` (API 36 AVD) + `node scripts/gates/gate-check.mjs --run-tests` (green evidence); update `gitref` to the branch tip.

### Notes on 2.1 (apply of 2026-10-18)

- `gradlew.bat test`: green (JVM), including the new `AppAppearanceTest` (2 tests) and the structure checks over the new `data/` file.
- `connectedDebugAndroidTest` on the API 36 AVD: 19 of 20 green. `SettingsScreenTest` 4/4 (the appearance test now waits for the re-created hierarchy instead of asserting mid-recreation).
- `UnlockCountTest.subtitleShowsTheDaysUnlockCount` is red **for an environment reason, not this change**: on this headless AVD the SLEEP→WAKEUP→MENU dance produces no `KEYGUARD_HIDDEN` events — `adb shell dumpsys usagestats` lists none for the day — and the app counts exactly those (`UsageRepository.kt:80`). It fails alone and in the suite; it reads usage stats, a path this change does not touch. To be re-run on a device where the keyguard really engages.
- The window background (task 1.3) has no machine tripwire: `Window` exposes `setBackgroundDrawable` but no getter, so it is covered by 1.5 (visual) instead of a fragile assertion.

## Workflow follow-up

- Review the apply report, merge, and archive with `gate-check --pre-archive themed-window`.
- Ordering: waits for `app-entry-gate` and `structure-housekeeping` (declared deps; overlap on `data/**`, `test/**`, `androidTest/**`).
- Tech brief: `walkthrough.md` (the relaunch/theme interaction) — comprehension sign-off in `/spec-verify` before `--pre-archive`.
