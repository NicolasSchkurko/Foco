# Tasks — detail-refresh-on-resume

- [x] 1. Delta `specs/usage-tracking/spec.md`: ADDED *The detail refreshes on return to the
      foreground* — reload the shown day on every resume and on first show, no user action, same
      spirit as the dashboard's requirement.
- [x] 2. `AppDetailScreen.kt`: add the resume trigger to the load effect (`:121-135`) so coming
      back to Foco re-reads the visible day; keep the selection (chain dep) and the rollover.
- [x] 3. Regression: new `app/src/androidTest/java/com/foco/app/ui/detail/AppDetailRefreshTest.kt`
      — open the detail, background the app, accumulate usage in another app, resume → the detail
      shows the new total without touching the day selector (red today).
- [x] 4. Confirm `AppDetailReadTest` (failed-read degradation) still passes with the extra trigger.
- [x] 5. `gradlew.bat test` green; `gate-check --run-tests` records evidence; `openspec validate
      detail-refresh-on-resume --strict` passes.

## Riskiest slice first

Task 2 (the trigger) with task 3's red-first device test.

## Deps

`detail-selection-survives-recreation` (same file; the refresh must not reset the restored day).
