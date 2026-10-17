# Tasks

## 1. i18n baseline (the base)

- [x] 1.1 Externalize every user-facing string to `strings.xml` (default ES) and add `values-en` with the English translations (dashboard, onboarding, chart, rows, the exclusions snackbar); verify the build and that the UI renders identically in ES.

## 2. Language (riskiest first: system integration)

- [x] 2.1 Add `androidx.appcompat` to the catalog and the module; `MainActivity` becomes `AppCompatActivity`; add `android:localeConfig` + the locales config file (ES/EN); verify the build and that the app opens.
- [x] 2.2 Language preference in the DataStore (auto/es/en): picking one calls `AppCompatDelegate.setApplicationLocales(...)` (Automático = empty = system) and applies at startup; verify on device (33+: Foco appears in the system per-app language screen and changes sync).
- [x] 2.3 Instrumented checks: pick EN → a known label renders in English; restart keeps it; Automático restores the system language.

## 3. Settings screen: sections

- [x] 3.1 Settings route on the NavHost + gear entry point in the dashboard header; back returns to the dashboard.
- [x] 3.2 Language section (Auto/ES/EN) + Excluded section (list with icon + label, restore action, empty state) reading the store from `ranking-exclusions`.
- [x] 3.3 **Appearance**: preference in the DataStore (auto/light/dark) resolved in `FocoTheme` (`darkTheme` from the preference; dynamic colors on 12+ unchanged) and applied at `MainActivity`; Apariencia section (radio Auto/Claro/Oscuro) with immediate app-wide effect.

## 4. Instrumented tests + device pass

- [x] 4.1 `SettingsScreenTest`: entry/back; visible language switch (+ restart, + Automático); appearance applied (+ restart, + Automático); excluded list + restore.
- [x] 4.2 Device pass: language ES/EN/Automático (incl. system sync on 33+), appearance Claro/Oscuro/Automático, manager restore, restart persistence.

## 5. Integration

- [x] 5.1 `gradlew.bat test` + `connectedDebugAndroidTest` (API 36 AVD) + gate with green evidence (`node scripts/gates/gate-check.mjs --run-tests`); update `gitref` to the branch tip.

## Workflow follow-up

- Review the apply report, merge, and archive with `gate-check --pre-archive app-settings`.
- Ordering (human decision): after `app-usage-detail` and `ranking-exclusions` (declared deps).
- This change moved exclusions management out of `ranking-exclusions` (which keeps the gesture + undo) — recorded in its proposal.
- Scope (human decision): the light/dark selection was folded into this change (no separate `appearance-mode` change).
