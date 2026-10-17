# Tasks

## 1. Seam (production, minimal)

- [x] 1.1 Add the injectable predicate to `App()` — `isUsageAccessGranted: (Context) -> Boolean = UsageAccess::isGranted` — used at first composition and on the resume re-check; verify `gradlew.bat assembleDebug` and a manual cold start (granted → dashboard).

## 2. Seam tests (deterministic)

- [x] 2.1 `AppEntrySeamTest` (ComponentActivity + `App(isUsageAccessGranted = fake)`): granted at start → dashboard; missing → onboarding with explanation + "Abrir Ajustes"; flip false→true on return → dashboard; true→false → onboarding.
- [x] 2.2 Run `gradlew.bat connectedDebugAndroidTest` (API 36 AVD, `Medium_Phone_API_36.0`) and verify the cases green.

## 3. E2E tests (real appops)

- [x] 3.1 `AppEntryGateTest` (MainActivity; appops via `uiAutomation.executeShellCommand`; `createEmptyComposeRule()` + manual `ActivityScenario.launch` so appops is set BEFORE the cold start): granted → dashboard; revoked → onboarding; **restore `allow` in cleanup** (a fresh install starts without the permission — the artifact that started this change).
- [x] 3.2 Live transitions e2e: revoke while open → return → onboarding; grant → return → dashboard.
- [x] 3.3 Verify `connectedDebugAndroidTest` green and the emulator left with `GET_USAGE_STATS: allow`.

## 4. Integration

- [x] 4.1 `gradlew.bat test` (JVM suite unchanged) + `assembleDebug` + `connectedDebugAndroidTest`; gate with green evidence (`node scripts/gates/gate-check.mjs --run-tests`); update `gitref` to the branch tip.

## Workflow follow-up

- Review the apply report, merge, and archive with `gate-check --pre-archive app-entry-gate`.
- If any test was red during the apply, the fix + regression landed inside this change.
- `app-usage-detail` (draft) stays independent (human decision): both touch `App.kt` — whichever applies second adapts; if either becomes `applying` while the other is live, the gate will ask for the ordering (as it did twice before).
