# Tasks

## 1. The tripwire first (it is the regression proof)

- [x] 1.1 `app/src/test/java/com/foco/app/structure/DefaultLocaleResourcesTest.kt` (JVM, file-scanning style like `LayerRulesTest`: reads `src/main/res/…` with the module dir as cwd): assert the base holds the English value for a set of keys that are Spanish today (`settings_option_dark`, `settings_option_light`, `settings_option_auto`, `settings_title`, `dashboard_title`, `loading`), assert `values-es/strings.xml` exists with the Spanish counterparts, and assert base and `es` declare the **same** `<string>`/`<plurals>` key set.
- [x] 1.2 Run it on the **pre-change tree** and record the failure (expected: the base holds "Oscuro"/"Ajustes"/"Hoy" and `values-es/` does not exist) — that red is the proof this fixes a real defect.

## 2. The move

- [x] 2.1 `values/strings.xml`: replace the Spanish values with the English ones from `values-en/strings.xml` — same key names, same order, section comments in English.
- [x] 2.2 New `values-es/strings.xml`: the Spanish values the base has today, verbatim, with the same section comments (English, per the repo's prose convention).
- [x] 2.3 Delete `values-en/strings.xml` (redundant once the base is English).
- [x] 2.4 Confirm nothing references the deleted folder: `locales_config.xml` still lists `es` + `en` and needs no change (per-app language resolution for `en` falls back to the base, which is English).

## 3. Verify

- [x] 3.1 `gradlew.bat test` green — including the tripwire now passing (1.1) and the structure tests.
- [x] 3.2 `connectedDebugAndroidTest` on the API 36 AVD: `SettingsScreenTest` green, in particular `languageSwitchesImmediatelyFollowsTheSystemAndPersists` (it selects the Spanish UI by `"Idioma"`/`"Ajustes"`, now served by `values-es/`).
- [x] 3.3 Visual pass on the AVD: set the device language to one with no resources (e.g. French) → the UI is English; then Español → Spanish; then English → English.
- [x] 3.4 `node scripts/gates/gate-check.mjs --run-tests` (green evidence); update `gitref` to the branch tip.

## Workflow follow-up

- Review the apply report, merge, and archive with `gate-check --pre-archive english-default-resources`.
- Overlap to watch: three active drafts claim `app/src/test/**` as a glob. Today it is a warning (no shared file); if one of them starts applying, sequence it with a declared `deps` instead of letting the gate escalate it.
- The Spanish **prose** in the code (comments, KDoc, test messages) is not this change: see the draft `english-source-prose`.

### Notes on the apply (2026-10-19)

- **Red before / green after, both recorded.** The tripwire was written first and run against the tree as it was: `testDebugUnitTest --tests "*DefaultLocaleResourcesTest*"` reported both tests failing (`theBaseIsEnglishAndSpanishLivesInItsOwnLocale`, `baseAndSpanishCarryTheSameKeys` — the base held "Oscuro"/"Ajustes"/"Hoy" and `values-es/` did not exist). After the move it is 2/2 green.
- The move itself: `values/strings.xml` now carries the English text verbatim as `values-en/strings.xml` had it (same 37 keys, same order, English section comments); `values-es/strings.xml` is new and carries the Spanish values the base had, with the section comments in English; `values-en/` was deleted (folder included — it held only that file).
- `locales_config.xml` is untouched: it still lists `es` + `en`, and "en" resolves to the base, which is English now.
- Instrumented pass on the API 36 AVD: `SettingsScreenTest` green — including `languageSwitchesImmediatelyFollowsTheSystemAndPersists`, which selects the Spanish UI by `"Idioma"`/`"Ajustes"`: those now come from `values-es/`, so the check is that Español still resolves as a real locale.
