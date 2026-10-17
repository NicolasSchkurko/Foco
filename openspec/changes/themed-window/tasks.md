# Tasks

## 1. The fix (riskiest first: the relaunch/theme interaction)

- [ ] 1.1 `data/AppAppearance.kt`: map Apariencia → modo appcompat (AUTO→FOLLOW_SYSTEM, Claro→NO, Oscuro→YES) and apply it through a named wrapper; JVM `AppAppearanceTest` pins the mapping (the mode constants are inlined, so the assertion runs on the JVM).
- [ ] 1.2 `MainActivity`: apply the stored appearance at startup (same pre-content read as the language) and react to later picks; `FocoTheme` keeps resolving the Compose scheme.
- [ ] 1.3 Window background = Compose surface color (dynamic colors included) through a named effect in `MainActivity`, so relaunches and transitions show the chosen scheme.
- [ ] 1.4 Instrumented: `SettingsScreenTest` asserts `AppCompatDelegate.getDefaultNightMode()` follows the pick (Oscuro→YES, Automático→FOLLOW_SYSTEM); the rest of the flow stays green.
- [ ] 1.5 Visual pass (AVD/device): language change with the chosen scheme's background; Oscuro over a light system without the white fade; cold start with a stored Oscuro.

## 2. Integration

- [ ] 2.1 `gradlew.bat test` + `connectedDebugAndroidTest` (API 36 AVD) + `node scripts/gates/gate-check.mjs --run-tests` (green evidence); update `gitref` to the branch tip.

## Workflow follow-up

- Review the apply report, merge, and archive with `gate-check --pre-archive themed-window`.
- Ordering: waits for `app-entry-gate` and `structure-housekeeping` (declared deps; overlap on `data/**`, `test/**`, `androidTest/**`).
- Tech brief: `walkthrough.md` (the relaunch/theme interaction) — comprehension sign-off in `/spec-verify` before `--pre-archive`.
