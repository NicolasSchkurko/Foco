# Tasks

## 1. The tripwire first (it is the regression proof)

- [ ] 1.1 `app/src/test/java/com/foco/app/structure/DefaultLocaleResourcesTest.kt` (JVM, file-scanning style like `LayerRulesTest`: reads `src/main/res/…` with the module dir as cwd): assert the base holds the English value for a set of keys that are Spanish today (`settings_option_dark`, `settings_option_light`, `settings_option_auto`, `settings_title`, `dashboard_title`, `loading`), assert `values-es/strings.xml` exists with the Spanish counterparts, and assert base and `es` declare the **same** `<string>`/`<plurals>` key set.
- [ ] 1.2 Run it on the **pre-change tree** and record the failure (expected: the base holds "Oscuro"/"Ajustes"/"Hoy" and `values-es/` does not exist) — that red is the proof this fixes a real defect.

## 2. The move

- [ ] 2.1 `values/strings.xml`: replace the Spanish values with the English ones from `values-en/strings.xml` — same key names, same order, section comments in English.
- [ ] 2.2 New `values-es/strings.xml`: the Spanish values the base has today, verbatim, with the same section comments (English, per the repo's prose convention).
- [ ] 2.3 Delete `values-en/strings.xml` (redundant once the base is English).
- [ ] 2.4 Confirm nothing references the deleted folder: `locales_config.xml` still lists `es` + `en` and needs no change (per-app language resolution for `en` falls back to the base, which is English).

## 3. Verify

- [ ] 3.1 `gradlew.bat test` green — including the tripwire now passing (1.1) and the structure tests.
- [ ] 3.2 `connectedDebugAndroidTest` on the API 36 AVD: `SettingsScreenTest` green, in particular `languageSwitchesImmediatelyFollowsTheSystemAndPersists` (it selects the Spanish UI by `"Idioma"`/`"Ajustes"`, now served by `values-es/`).
- [ ] 3.3 Visual pass on the AVD: set the device language to one with no resources (e.g. French) → the UI is English; then Español → Spanish; then English → English.
- [ ] 3.4 `node scripts/gates/gate-check.mjs --run-tests` (green evidence); update `gitref` to the branch tip.

## Workflow follow-up

- Review the apply report, merge, and archive with `gate-check --pre-archive english-default-resources`.
- Overlap to watch: three active drafts claim `app/src/test/**` as a glob. Today it is a warning (no shared file); if one of them starts applying, sequence it with a declared `deps` instead of letting the gate escalate it.
- The Spanish **prose** in the code (comments, KDoc, test messages) is not this change: see the draft `english-source-prose`.
