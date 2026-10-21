# Tasks — settings-about-strings-contract

- [x] 1. Flip `app/src/androidTest/java/com/foco/app/ui/SettingsScreenTest.kt` assertions at
      `:251-252` (and the repository-label assertion) to the spec's texts — red first: credit
      `Open source project by Nicolas Schkurko`, first line `Foco <version>`, label
      `Git repository`.
- [x] 2. `values/strings.xml:111,112,114` and `values-es/strings.xml:111,112,114`: align credit,
      line format and link label; keep `settings_about_version_unknown` (`:113`) exactly as is —
      it belongs to the ⚔ sub-item.
- [x] 3. `SettingsScreen.kt` (`:185-200` About block): compose the first line as `Foco <version>`
      (name + installed version, no `Version ` prefix), reading `AppIdentity.versionName()` as
      today; touch nothing about the null fallback until the conflict resolves.
- [x] 4. Confirm `DefaultLocaleResourcesTest` still passes (base/es key sets identical).
- [x] 5. `gradlew.bat test` green; `node scripts/gates/gate-check.mjs --run-tests` records evidence.
- [x] 6. ⚔ NOT in this change: the `Version unknown` placeholder (spec `:124-126` vs
      `DECISIONS.md:394`). It is declared in `x-conflicts` and waits for the human's explicit
      resolution (recorded as an ADR + entry removal, never written by the agent).

- [x] 7. Spec delta: `specs/app-settings/spec.md` — MODIFIED *Settings shows a centered app identity
      block* (+ the literal-credit scenario; the placeholder scenario untouched by the ⚔);
      `openspec validate settings-about-strings-contract --strict` passes.

## Riskiest slice first

Task 1 (red assertions) → tasks 2-3 make it green. The conflict stays parked.

## Deps

None. No other active change claims `strings.xml` or `SettingsScreen.kt`.
