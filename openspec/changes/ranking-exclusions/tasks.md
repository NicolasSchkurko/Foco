# Tasks

## 1. Data: the persisted list (the base)

- [ ] 1.1 Add `androidx.datastore:datastore-preferences` to the version catalog and the module; verify `gradlew.bat assembleDebug`.
- [ ] 1.2 Create the thin store in `data/` (e.g. `ExcludedAppsStore`): `Flow<Set<String>>` + `add`/`remove` (DataStore Preferences, string-set key); keep it dumb (no policy).

## 2. Pure derivation (red first)

- [ ] 2.1 Extend the ranking derivation with an excluded-packages input (totals and hourly untouched); extend `UsageMetricsTest`: the excluded app is omitted from the ranking while `totalMillis`/`hourly` still include its time — red first, then green.

## 3. UI: gesture, undo and the excluded section

- [ ] 3.1 `AppUsageRow`: `combinedClickable` with long-press → exclude; snackbar "Excluida de las estadísticas" with **Undo**.
- [ ] 3.2 Wire the store into the dashboard state (collect the flow; the ranking recomputes with the exclusions; totals unchanged). Management UI lives in the `app-settings` change.

## 4. Instrumented tests + device pass

- [ ] 4.1 `ExcludedAppsTest`: long-press hides the row (total unchanged); Undo restores; restart (activity recreate) keeps the exclusion.
- [ ] 4.2 `gradlew.bat connectedDebugAndroidTest` (API 36 AVD `Medium_Phone_API_36.0`) + `installDebug` + manual pass (exclude, undo, restart, section).

## 5. Integration

- [ ] 5.1 `gradlew.bat test` (JVM) + `assembleDebug` + the gate with green evidence (`node scripts/gates/gate-check.mjs --run-tests`); update `gitref` to the branch tip.

## Workflow follow-up

- Review the apply report, merge, and archive with `gate-check --pre-archive ranking-exclusions`.
- Ordering (human decision): `app-usage-detail` applies first (declared dep); overlapping `androidTest/**` with `app-entry-gate` stays informational — whichever applies second adapts.
- The excluded-apps manager lands in `app-settings` (draft) — it reads the same store; this change keeps only the gesture and undo.
