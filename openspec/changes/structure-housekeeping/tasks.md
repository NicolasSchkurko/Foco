# Tasks

## 1. Settings store split + migration (riskiest first: persistence)

- [x] 1.1 `SettingsStore` declares its own DataStore (`foco_settings`) and reads/writes `language`/`appearance` there.
- [x] 1.2 One-shot migration in `data/`: copy `language`/`appearance` from `excluded_apps` into `foco_settings` and clear them from the legacy store; idempotent; wired in `MainActivity.onCreate` before the first language read.
- [x] 1.3 `SettingsMigrationTest` (JVM, two temp-file stores): copies the keys, clears them from the legacy store, a second run is a no-op, fresh install is untouched.
- [ ] 1.4 Device pass: install over a build with a stored language/appearance and verify both survive the upgrade (same choices in the UI after the update).

## 2. Expressivity (storage + names)

- [x] 2.1 Short physical-resource headers on `ExcludedAppsStore` and `SettingsStore` (which file, where it lives, lifecycle).
- [x] 2.2 `ExcludedAppsStore.add/remove` → `exclude/restore` (+ call sites: dashboard, settings, tests); `data/AppPreferences.kt` disappears (each store declares its own DataStore).

## 3. Shared widgets re-home

- [x] 3.1 `AppIcon` + `appLabel` move to `ui/common/`; imports updated (dashboard rows, detail, settings).
- [x] 3.2 `AppDetailScreen` moves to `ui/detail/`; `FocoNavHost` import updated.
- [x] 3.3 `LayerRulesTest`: new boundary check — a feature package must not import another feature package (`ui/common/`, `ui/metrics/`, `ui/theme/` and the `ui` root stay shared); the repo passes with the new homes.

## 4. Integration

- [x] 4.1 `gradlew.bat test` + `connectedDebugAndroidTest` (API 36 AVD) + `node scripts/gates/gate-check.mjs --run-tests` (green evidence); update `gitref` to the branch tip.

## Workflow follow-up

- Review the apply report, merge, and archive with `gate-check --pre-archive structure-housekeeping`.
- Ordering: waits for `app-entry-gate` (declared dep; overlap on `androidTest/**`).
- Tech brief: `walkthrough.md` (persistence split + migration) — comprehension sign-off in `/spec-verify` before `--pre-archive`.
