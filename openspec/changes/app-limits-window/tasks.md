# Tasks — app-limits-window

## Riskiest slice first

- [x] 0. Confirm `app-limits` and `app-limit-alerts` are verified; the database is at version 2.
- [x] 1. Add `AppLimitWindow`/`AppLimitWindowDao` and `AppLimitDatabase` version 3 with a
      non-destructive 2→3 migration that only creates `app_limit_window`; export the schema and cover
      the store/migration in `AppLimitWindowStoreTest` and `AppLimitWindowMigrationTest`.
- [x] 2. Add the pure `AppLimitWindowRules`: inside/outside, overnight, equal-range rejection,
      in-window session clipping, next boundary; cover `AppLimitWindowRulesTest`.
- [x] 3. Wire `UsageMonitorEvaluator`/`UsageMonitorService`: block outside with the outside-hours
      reason, block immediately at open when the limit is already reached, arm the one-shot boundary
      check, keep the 2-minute fallback, count only in-window usage, and evaluate alerts only inside;
      extend `UsageMonitorServiceTest`.
- [x] 4. Extend `AppLimitBlockScreen`/`UsageMonitorAccessibilityService` with the block reason and the
      “Outside allowed hours” message; cover it in `AppLimitBlockTest`.
- [x] 5. Add the Settings window picker (No window, 15-minute start/end, overnight, no equal pair,
      window without a limit), both locale strings, and extend `AppLimitsSettingsTest`.
- [x] 6. Run `gradlew.bat test`, `assembleDebugAndroidTest`, the affected class-level instrumented
      runs on the API 36 AVD, `openspec validate app-limits-window --strict` and the gate.
- [x] 7. Write the tech-brief `walkthrough.md` and collect the human's comprehension sign-off
      before archiving.

## Notes

- The 2-minute limit/alert read does not change; the window boundary adds a one-shot check, not a
  faster poll.
- Outside-window time is excluded from the minute budget by clipping today's sessions to the window.
