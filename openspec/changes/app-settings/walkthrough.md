# Walkthrough — app-settings (tech brief)

> For the human: this brief explains the mechanism of the change. Answer the questions in
> `## Cognitive sign-off` yourself; the agent never fills them.

## Mechanism

The app gains a **Settings screen** (gear in the dashboard header) with three sections:
language, appearance and the excluded-apps manager. **Language**: picking Automático /
Español / Inglés writes the choice to the app's local DataStore and immediately calls the
appcompat backport `AppCompatDelegate.setApplicationLocales(...)`; on Android 13+ Foco also
appears in the system's per-app language list (declared via `android:localeConfig`). At
startup `MainActivity.onCreate` reads the stored choice and applies it **before the first
frame**. **Appearance**: the choice (Automático / Claro / Oscuro) is stored in the same
DataStore; `MainActivity` collects it and resolves `FocoTheme(darkTheme = ...)` — Automático
follows the system, Claro/Oscuro override it, and dynamic colors keep working. **Excluded
apps**: the screen lists the persisted exclusions (icon + label) and restores any of them;
it is the same DataStore-backed set the dashboard's long-press writes. All user-facing
strings moved from Kotlin literals to resources (default Spanish + `values-en`).

## Physical traceability

- `app/src/main/java/com/foco/app/data/AppPreferences.kt` — the single DataStore declaration;
  physical file `excluded_apps` in app-private storage (name kept — data preservation).
- Keys: `excluded_packages` (string set; written by the dashboard long-press and the settings
  restore), `language` (`auto`/`es`/`en`), `appearance` (`auto`/`light`/`dark`).
- `data/SettingsStore.kt` reads/writes language + appearance; `data/ExcludedAppsStore.kt`
  owns the exclusions list; `data/AppLanguage.kt` applies the language to the system;
  `model/LanguageOption.kt` + `model/AppearanceOption.kt` are the enums (wire values, `AUTO`
  fallback).
- `MainActivity.kt` — startup: one read of the stored language (applied immediately) and
  collection of the appearance for the theme. `ui/settings/SettingsScreen.kt` is the screen;
  `ui/FocoNavHost.kt` adds the `settings` route; `res/values/strings.xml` +
  `res/values-en/strings.xml` hold the strings.
- Lifecycle: the preferences live until uninstall/clear-data. No backend, no network.

## Limits and failure modes

- **Startup language read**: one `runBlocking` read of a small file on the main thread. If the
  file is corrupt/cleared or the value unknown, DataStore emits defaults → `AUTO` (system
  language); nothing crashes.
- **Language switch**: appcompat recreates the running activity to apply the new
  configuration; the DataStore write happens **before** the switch, so the restart comes up
  already in the new language (no toggle-back). The screen may briefly restart — a fresh
  launch of the UI in the new language.
- **System integration**: the per-app language entry in the system settings is Android 13+;
  below that only the backport applies. `autoStoreLocales=false`: appcompat does not persist
  on its own — persistence is ours (DataStore).
- **Appearance** is Compose-level (`FocoTheme`); the activity window theme is AppCompat
  DayNight and follows the system, not the in-app override.
- Uninstalled apps in the excluded list fall back to the package name (same adapter as the
  ranking; `data/AppPackageInfo.kt`).

## Touched APIs

- `AppCompatDelegate.setApplicationLocales` + `LocaleListCompat` (androidx.appcompat; one API
  for 29–36, syncs with the system's per-app language on 33+).
- `android:localeConfig` + `res/xml/locales_config.xml` (Android 13+: registers ES/EN in the
  system's per-app language screen).
- Preferences DataStore (existing dependency): same store, two new keys.
- `MainActivity` now extends `AppCompatActivity` (backport requirement) with an AppCompat
  DayNight `Theme.Foco`.

## Implicit decisions

- Startup language applied with `runBlocking` (small file) instead of a post-frame apply, to
  avoid one activity recreation per cold start.
- The DataStore file keeps its historical name `excluded_apps` even though it now holds more
  than exclusions.
- "Automático" appears in both sections; the enumerated wire values make the stored form
  stable and the `fromWire` fallback is `AUTO`.
- `formatDayLabel` now receives the today/yesterday labels from the UI (it is a pure function
  and cannot read resources).
- Instrumented tests that asserted Spanish literals now resolve strings from resources, so
  they pass in any device language.

## Cognitive sign-off

Answer in your own words (the agent must not fill these):

1. Where do the language and appearance choices live (file and keys), who reads them — and at
   what moment is the language applied?
   > se guardan en data y despues setting store lo trae en el main activity y lo pone a disposicion
   >
   > — the human, 2026-10-16
2. If that file is cleared or holds an unknown value, what does Foco show at startup, and
   why?
   > perfecto cae en el callback de languaje option o aparreance option retorna default
   >
   > — the human, 2026-10-16
3. To add a new preference to Settings (say, a fourth option), which files and functions do
   you touch?
   > creo un option, settings store, settings screen
   >
   > — the human, 2026-10-16

When satisfied, the sign-off is recorded in `/spec-verify` (`understood_by` + `understood_at` in
`.openspec.yaml`).
