# Tasks — dates-follow-app-language

- [ ] 1. Delta `specs/app-settings/spec.md`: ADDED *Dates and times follow the app language* with
      the English-UI/Spanish-device, Spanish-UI and Automático scenarios.
- [ ] 2. `Formatting.kt:24-35`: format against the app's active locale instead of
      `Locale.getDefault()`; keep every pinned pattern (day/week/hour shapes) unchanged.
- [ ] 3. Thread the active locale through the call sites that render date labels (dashboard,
      week, detail, export headers).
- [ ] 4. Regression: new `app/src/test/java/com/foco/app/ui/FormattingTest.kt` — English app
      locale + Spanish device default → English labels (red today); Spanish app locale → Spanish
      labels; patterns byte-identical to the current output for a fixed input/locale pair.
- [ ] 5. `gradlew.bat test` green; `gate-check --run-tests` records evidence; `openspec validate
      dates-follow-app-language --strict` passes.

## Riskiest slice first

Task 3 (finding every call site) — the helper change alone would leave view labels on the device
locale; the test of task 4 pins both layers.

## Deps

None. `Formatting.kt` is claimed by no other active change.
