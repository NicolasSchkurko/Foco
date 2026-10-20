# Proposal — code-quality-hardening

## Why

The repo's domain and data layers are solid (pure functions, tested, non-destructive migrations), but an
internal quality review found the debt concentrated in three places: (1) mojibake in source literals and
documentation that reaches the user (broken arrow glyphs in `DashboardScreen`), (2) a ~300-line
`DashboardScreen` composable that owns navigation history as a `"\n"`-joined `String`, creates
`UsageRepository`/`SessionHistoryStore` inside `remember`, and carries motion/history logic that is not
unit-testable, and (3) build hygiene that does not fail on warnings (`unsafeCheckOpNoThrow`,
experimental serialization opt-in unmarked) and has no static analysis in CI. None of this changes user
behavior; it lowers the risk of regressions and makes the UI layer match the discipline of `domain/`.

## What

- Fix source and documentation encoding: replace mojibake glyphs with proper resources/drawables and
  re-save affected files as UTF-8 without BOM. Add a check that fails the build on non-ASCII mojibake
  in `.kt`/`.md`.
- Extract the dashboard's day-ring navigation and motion into a testable state holder
  (`DashboardState`/`DashboardViewModel`-free holder) that owns `stopKey`, the history stack as a
  `List<DayStop>` (not a joined string), `slideDirection`, `softMotion`, and exposes `moveTo`,
  `moveStep`, `goBack`. `DashboardScreen` becomes a thin view.
- Move `UsageRepository` and `SessionHistoryStore` creation out of the composable into a factory/params
  seam (already partially present via `loadDay`/`clock`), so the screen is fully drivable in tests.
- Turn on `warningsAsErrors` for the main + unit-test source sets and resolve the two current warnings
  (`UsageAccess` deprecated call, `WeekExportJson` experimental opt-in). Add Detekt (or ktlint) to the
  Gradle build and to CI.
- Make startup robust: wrap `migrateSettingsFromLegacyDataStore` + initial reads in `runBlocking` with
  a bounded timeout and a logged fallback instead of an unguarded blocking read; keep the first frame
  behavior identical.
- Add tests: `DashboardStateTest` covering history push/pop, ring bounds, direction, and soft motion;
  keep `LayerRulesTest` green.

No behavior change, no persistence change, no UI visual change (except fixing the broken glyphs).

## Expected outcome

Same app behavior, with the dashboard navigation unit-testable, zero compiler warnings, static analysis
in CI, and no mojibake in source. `gradlew.bat test` and the new lint task stay green.

## Alternatives considered

| Option | Why yes | Why not |
| --- | --- | --- |
| Rewrite DashboardScreen from scratch | Clean slate | High risk, loses verified motion tuning |
| Extract a ViewModel (Hilt) | Standard Android | New dependency + scope; the repo avoids DI frameworks |
| State holder in `ui/dashboard` (chosen) | Testable, no new dep, matches existing `remember` seams | Slightly more plumbing |
| Keep string-joined history | No change | Stays untestable and fragile |