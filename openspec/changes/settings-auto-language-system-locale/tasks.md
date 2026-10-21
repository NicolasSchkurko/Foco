# Tasks — settings-auto-language-system-locale

- [ ] 1. **Reproduce first** (device, API 33+): set a language for Foco in the system settings,
      launch Foco on Automático → observe whether `AppLanguage.kt:14` /
      `MainActivity.kt:57` resets it. Record the result here; it decides how deep task 3 goes.
- [ ] 2. **Decision**: the human answers the open question in `.openspec.yaml`; the answer lands as
      an ADR in `DECISIONS.md` and the delta wording follows it.
- [ ] 3. Delta `specs/app-settings/spec.md`: MODIFIED *The language is selectable, applied and
      persisted* — add the system-per-app clause + scenario (restate all existing scenarios
      unchanged).
- [ ] 4. Apply the chosen side: on Automático do not overwrite an existing system per-app choice
      (and mirror it in the row); explicit picks inside Foco keep working and persist as today.
- [ ] 5. Regression: `MainActivityStartupTest.kt` (JVM — startup applies no wipe over a system
      choice) and `SettingsScreenTest.kt` (row reads `Automatic` while the system choice stands).
- [ ] 6. Walkthrough (tech brief: platform SDKs): complete `walkthrough.md`; comprehension
      sign-off is the human's.
- [ ] 7. `gradlew.bat test` green; `gate-check --run-tests` records evidence; `openspec validate
      settings-auto-language-system-locale --strict` passes.

## Riskiest slice first

Task 1 (does the wipe reproduce?) — the finding's mechanism is a partial hypothesis; the contract
text (task 3) is worth adding either way.

## Deps

`code-hygiene-tripwires` (shares `AppLanguage.kt`), `settings-about-strings-contract` (shares
`SettingsScreenTest.kt`).
