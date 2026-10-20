# Tasks — app-limit-alerts

## Riskiest slice first

- [x] 0. Confirm `app-limits` is verified and its `limits.db` version 1 is on the branch.
- [x] 1. Add `alertEnabled` to `app_limit`, the `app_limit_alert_fired` table (package, day, tier)
      and `AppLimitDatabase` version 2 with a non-destructive 1→2 migration and exported schema;
      cover the store and migration in `AppLimitAlertStoreTest` and
      `AppLimitAlertMigrationTest`.
- [x] 2. Add the pure threshold rules to `AppLimitRules` (50/75/90, highest crossed, zero/no limit,
      100 %, foreground-only) and `AppLimitAlertRulesTest`.
- [x] 3. Add `AppLimitAlertNotifier` with the alert channel, content and the only **Entendido**
      action; add the dismiss receiver and cover it in `AppLimitAlertNotifierTest`.
- [x] 4. Wire `UsageMonitorService`/`UsageMonitorEvaluator` on the existing 2-minute tick: reuse the
      cumulative today's-usage read, add no separate timer and no alert from accessibility alone,
      evaluate only the foreground package, compute the highest tier, mark all crossed tiers fired,
      skip at/over 100 %, and never alert for no limit or 0 min; extend `UsageMonitorServiceTest`.
- [x] 5. Add the per-app toggle in `SettingsScreen`, the strings in both locales, and the permission
      state; extend `AppLimitsSettingsTest`.
- [x] 6. Clear the day's fired tiers when the limit or toggle changes; verify in
      `AppLimitAlertStoreTest`.
- [x] 7. Run `gradlew.bat test`, `assembleDebugAndroidTest`, the affected class-level instrumented
      runs on the API 36 AVD, `openspec validate app-limit-alerts --strict` and the gate.
- [x] 8a. Write the tech-brief `walkthrough.md`.
- [ ] 8b. Collect the human's comprehension sign-off before archiving.

## Decision

- [x] 9. Confirmed: alerts are evaluated and posted only while the app is open (foreground).
