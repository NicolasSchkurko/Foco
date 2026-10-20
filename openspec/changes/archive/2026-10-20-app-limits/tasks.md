# Tasks — app-limits

## Vertical slice 1 — the riskiest part first

- [x] 1. Add `AppLimit`, `AppLimitDao`, `AppLimitDatabase` (version 1, exported schema) and
      `AppLimitsStore` over a dedicated private `limits.db`; cover set/clear/flow in
      `AppLimitsStoreTest`.
- [x] 2. Add the pure `AppLimitRules` (0, 15-minute grid, at/over the limit) and
      `AppLimitRulesTest`; no Android imports.
- [x] 3. Declare the foreground service (`specialUse` + `PROPERTY_SPECIAL_USE_FGS_SUBTYPE`), the
      accessibility service, `BIND_ACCESSIBILITY_SERVICE`, `FOREGROUND_SERVICE`,
      `FOREGROUND_SERVICE_SPECIAL_USE`, `RECEIVE_BOOT_COMPLETED` and `POST_NOTIFICATIONS`; add
      `res/xml/accessibility_service_config.xml` and `AccessibilityAccess`.
- [x] 4. Add `UsageMonitorAccessibilityService`: track the foreground package, host the full-screen
      overlay and perform `GLOBAL_ACTION_HOME` after the countdown; verify on the API 36 AVD.
- [x] 5. Add `AppLimitBlockScreen` with the app name, the daily-limit message and the 3, 2, 1
      countdown; cover it in `AppLimitBlockTest`.

## Vertical slice 2 — enforcement and settings

- [x] 6. Add `UsageMonitorEvaluator`, `UsageMonitorService` (2-minute tick, start on first limit,
      stop on the last, ongoing notification) and `UsageMonitorBootReceiver`; cover start/stop,
      notification and the accessibility-disabled stop in `UsageMonitorServiceTest`.
- [x] 7. Add the Settings section: every launchable app, the No limit/Block immediately/15-minute
      picker, persistence and start/stop; add both locale strings and `AppLimitsSettingsTest`.
- [x] 8. Extend the entry gate and onboarding for accessibility (usage first, then accessibility)
      and update `AppEntryGateTest`, `AppEntrySeamTest` and `OnboardingContrastTest`.
- [x] 9. Add `NoNetworkPermissionTest`; keep `BackupDisabledManifestTest` green; update the
      `ARCHITECTURE.dsl` `cap:app-limits` tags.

## Close

- [x] 10. Run `gradlew.bat test`, `gradlew.bat :app:assembleDebugAndroidTest`, the five class-level
      instrumented runs on the API 36 AVD, `openspec validate app-limits --strict` and the gate.
- [x] 11. Write the tech-brief `walkthrough.md` and collect the human's comprehension sign-off
      before archiving.
