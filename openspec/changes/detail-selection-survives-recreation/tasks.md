# Tasks — detail-selection-survives-recreation

- [ ] 1. Delta `specs/usage-tracking/spec.md`: ADDED *The detail's selected day survives
      recreation* — selection restored on rotation, on return from Settings and after process
      recreation, always resolved inside the current stored window.
- [ ] 2. `AppDetailScreen.kt`: make the selection saveable — restore a **date key** (not a raw
      index) and resolve it against the current `days`, so a shrunken window lands on the nearest
      valid day through the `detail-days-index-reclamp` helper.
- [ ] 3. Regression: `app/src/androidTest/java/com/foco/app/ui/AppDetailNavigationTest.kt` —
      open the detail on a past day, recreate the activity (rotation) → the same day is shown; a
      Settings round-trip from the detail returns to the same day.
- [ ] 4. `gradlew.bat test` green; `gate-check --run-tests` records evidence; `openspec validate
      detail-selection-survives-recreation --strict` passes.

## Riskiest slice first

Task 2's restore key choice (date vs index) — it decides whether the clamp bug can re-enter.

## Deps

`detail-days-index-reclamp` (same file; lands first). This change owns the file for
`detail-refresh-on-resume`, which chains after it.
