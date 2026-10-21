# Tasks — notification-permission-toggle

## 1. Remove the permission request from the limit path

- [x] 1.1 In `SettingsScreen.kt`, remove the `requestNotificationPermission()` call from
      `onLimitChange` and verify setting a limit no longer triggers the permission launcher.
- [x] 1.2 Keep `requestNotificationPermission()` in `onAlertChange` when `enabled` is true and
      verify the toggle still requests permission when it is not granted.

## 2. Verification

- [x] 2.1 Run `gradlew.bat test` and `gradlew.bat ktlintCheck`; fix any failure.
- [x] 2.2 Run `openspec validate notification-permission-toggle --strict` and
      `node scripts/gates/build-board.mjs`; confirm the gate is green.

## Workflow follow-up

- Archive the change after review requirements are satisfied.
- Verify the archived result.