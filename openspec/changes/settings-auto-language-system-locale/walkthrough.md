# Walkthrough — settings-auto-language-system-locale

## Mechanism

Foco declares Android 13's per-app language support (`localeConfig` in the manifest,
`autoStoreLocales=false` in `MainActivity`), so the **system** can own Foco's language. On top of
that, every start calls `AppLanguage.apply` (`AppLanguage.kt:14`), which ends in
`AppCompatDelegate.setApplicationLocales(getEmptyLocaleList())` — an empty list meaning "no app
override": the device default. When the system stored a per-app language for Foco, that call
overwrites it with the reset, and the settings row — which reports the in-app state — reads
`Automático` while the system's choice is gone. The contract proposed here makes Automático mean
"defer to whatever the system answers (device default **or** Foco's per-app setting)" and forbids
the wipe; the implementation stops applying the empty list over an existing system choice and
mirrors the system answer in the row.

## Physical traceability

- `app/src/main/java/com/foco/app/data/AppLanguage.kt:14` — the empty-locale application.
- `app/src/main/java/com/foco/app/MainActivity.kt:57` — applied on every start (and `:56`'s
  migration path feeding it).
- `app/src/main/AndroidManifest.xml:40` — `localeConfig` publication (why the system setting
  exists at all).
- `app/src/test/java/com/foco/app/MainActivityStartupTest.kt`,
  `app/src/androidTest/java/com/foco/app/ui/SettingsScreenTest.kt` — JVM startup proof and row
  proof.

## Limits and failure modes

- The wipe itself is a **partially confirmed hypothesis** (found by inspection): task 1 of
  `tasks.md` reproduces it on a device before any code moves; if it does not reproduce, the change
  reduces to the contract text plus the row mirror.
- AppCompat's precedence between stored system value and an explicit empty call varies by version;
  `MainActivityStartupTest` pins the behavior on the supported floor.
- Picking Español/Inglés inside Foco still overrides (explicit choice wins), as today.

## Touched APIs

`AppCompatDelegate.setApplicationLocales`, `LocaleList`, `autoStoreLocales`, `localeConfig` — the
platform/AppCompat locale stack at activity startup. No storage format change, no new dependency.

## Implicit decisions

- The system's per-app choice outranks the in-app Automático (proposal, open decision): Foco owns
  its language, the system is a first-class way to set it, and neither silently erases the other.
- The migration (`SettingsMigration.kt`) is out of scope here except that its stored language keeps
  behaving as decided in `settings-migration-no-overwrite` (declared dep chain).
