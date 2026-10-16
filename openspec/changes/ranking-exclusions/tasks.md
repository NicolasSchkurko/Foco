# Tasks

## 1. Data: the persisted list (the base)

- [x] 1.1 Add `androidx.datastore:datastore-preferences` to the version catalog and the module (stable **1.2.1**, compatible with `compileSdk 36`); verify `gradlew.bat assembleDebug`.
- [x] 1.2 Create the thin store in `data/` (`ExcludedAppsStore`): `Flow<Set<String>>` + `add`/`remove` (DataStore Preferences, string-set key); keep it dumb (no policy).

## 2. Pure derivation (red first)

- [x] 2.1 Extend the ranking derivation with an excluded-packages input (totals and hourly untouched); extend `UsageMetricsTest`: the excluded app is omitted from the ranking while `totalMillis`/`hourly` still include its time — red first, then green.

## 3. UI: gesture, undo and the excluded section

- [x] 3.1 `AppUsageRow`: `combinedClickable` with long-press → exclude; snackbar "Excluida de las estadísticas" with **Undo** (in `DashboardScreen`, which owns the snackbar host).
- [x] 3.2 Wire the store into the dashboard state (collect the flow; the ranking recomputes with the exclusions; totals unchanged). Management UI lives in the `app-settings` change.

## 4. Instrumented tests + device pass

- [x] 4.1 `ExcludedAppsTest`: long-press hides the row (total unchanged); Undo restores; restart keeps the exclusion. Verified green on the API 36 AVD.
- [x] 4.2 `gradlew.bat connectedDebugAndroidTest` (6/6 green) + `installDebug` + manual pass on device: long-press → row gone + "Deshacer" snackbar + total intact; relaunch → exclusion persists. (Manual Undo step skipped — it is covered deterministically by `ExcludedAppsTest`; the device was cleaned afterwards.)

## 5. Integration

- [x] 5.1 `gradlew.bat test` (JVM) + `assembleDebug` + the gate with green evidence (`node scripts/gates/gate-check.mjs --run-tests`); update `gitref` to the branch tip.

## Workflow follow-up

- Review the apply report, merge, and archive with `gate-check --pre-archive ranking-exclusions`.
- Ordering (human decision): `app-usage-detail` applies first (archived); `app-settings` lands after this change (declared dep).
- The excluded-apps manager lands in `app-settings` — it reads the same store; this change keeps only the gesture and undo.
