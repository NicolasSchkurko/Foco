# Tasks — code-quality-hardening

- [x] 1. Re-save affected `.kt`/`.md` as UTF-8 (no BOM) and replace mojibake glyphs in
      `DashboardScreen.kt` (`�?1`, `�?�`) with proper icons/resources. Add a scan that fails on
      replacement characters in source.
- [x] 2. Extract `DashboardState` (or equivalent) in `ui/dashboard/` owning `stopKey`, history as
      `List<String>`, `slideDirection`, `softMotion`, `moveTo`, `moveStep`, `goBack`; keep
      `rememberSaveable` keys stable.
- [x] 3. Refactor `DashboardScreen` to consume the state holder; remove repository/store creation from
      `remember` by accepting factories/params (preserve test seams `loadDay`, `clock`).
- [x] 4. Enable `warningsAsErrors = true` for main + unitTest; fix `UsageAccess` deprecated call and
      `WeekExportJson` experimental opt-in.
- [x] 5. Add Detekt (or ktlint) plugin + config; add a `lint`/`detekt` step to `.github/workflows/ci.yml`.
- [x] 6. Harden startup: bound the `runBlocking` in `MainActivity.onCreate` with timeout + logged
      fallback; keep first-frame appearance identical.
- [x] 7. Add `DashboardStateTest` (history push/pop, ring bounds, direction, soft motion) and keep
      `LayerRulesTest` green.
- [x] 8. Run `gradlew.bat test` + new lint task + `openspec validate code-quality-hardening --strict`.
