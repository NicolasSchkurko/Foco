# Tasks

## 1. The fix (riskiest first)

- [ ] 1.1 Declare `android:enableOnBackInvokedCallback="true"` on `<application>` in `AndroidManifest.xml`; verify the build and **manually on the API 36 AVD**: edge-swipe on a detail screen → dashboard.
- [x] 1.2 `BackInvokedCallbackManifestTest` (JVM; reads `src/main/AndroidManifest.xml` from the module dir): asserts the flag is present — the regression test for this bug.

## 2. The icon

- [x] 2.1 Swap the glyph for `Icons.AutoMirrored.Filled.ArrowBack` with `contentDescription = "Volver"`; add `androidx.compose.material:material-icons-core` (BOM-managed) to the catalog/module if the build demands it.

## 3. Tests + integration

- [x] 3.1 Update `AppDetailNavigationTest`: find the control by content description (the key/back cases stay green); verify `gradlew.bat connectedDebugAndroidTest` on the API 36 AVD.
- [x] 3.2 `gradlew.bat test` (JVM) + `assembleDebug` + the gate with green evidence (`node scripts/gates/gate-check.mjs --run-tests`); update `gitref`.

## Workflow follow-up

- Review the apply report, merge, and archive with `gate-check --pre-archive detail-back-ux`.
- Ordering (pre-declared per the A+ policy): `app-settings` waits for this change (dep); `app-entry-gate` follows transitively.
