# Proposal — notification-permission-toggle

## Why

Today Foco requests the `POST_NOTIFICATIONS` permission as a side effect of setting a daily limit
(`SettingsScreen.onLimitChange`), even though the limit itself does not need notifications. That
surprises the user mid-configuration and couples two unrelated decisions. Notifications are only
needed for the alert tiers, so the permission should be requested exactly when the user opts into
alerts via the toggle.

## What Changes

- Remove the `requestNotificationPermission()` call from the daily-limit change path.
- Keep requesting the permission when the alert toggle is turned on (already the case).
- No change to the alert engine, the monitor or the toggle's enabled/disabled rules.

## Capabilities

### Modified Capabilities

- `app-settings`: "Each app limit has an alert toggle" gains the rule that a daily limit never
  triggers a notification permission request; the request happens only on toggle activation.

## Impact

- `app/src/main/java/com/foco/app/ui/settings/SettingsScreen.kt` (limit change path)
- No new dependency, storage, service or permission declaration.

## Expected outcome

Setting a daily limit never shows a notification permission dialog; the dialog appears only when
the user turns on the alert toggle for an app with a positive limit. (Inferred from the chosen
scope; the user scoped the request to the toggle only.)

## State and persistence mechanism

No new persistence: the notification permission is system-managed state, read through
`ContextCompat.checkSelfPermission(POST_NOTIFICATIONS)` and requested via the activity result
launcher already present in `SettingsScreen`. No app-private storage, DataStore or database is
added or changed.

## Alternatives considered

| Option | Why yes | Why not | Recommendation |
| --- | --- | --- | --- |
| Request only on toggle activation (chosen) | Permission tied to the feature that needs it; no surprise dialogs while setting limits | Users who set a limit first still see the dialog later when enabling alerts | **Chosen** |
| Keep requesting on limit activation | One fewer step for users who want alerts | Couples permission to an unrelated action; the reported confusion | Rejected |
| Dedicated "Enable notifications" button | Explicit, discoverable | Extra UI and flow; larger change | Rejected (out of scope) |