# Proposal — per-app-limits-config

## Why

The per-app configuration is split in the wrong place. Settings still lists every launchable app
with its limit, window and alert toggle, while the detail screen offers a half-built "Configure"
screen that shows a single line of text, has a dead back arrow and no controls. The human asked to
finish the move: Settings stops configuring limits, and each app's configuration lives behind a
gear on that app's detail.

## What Changes

- `SettingsScreen` loses the app-limits section (rows, limit/window/alert state and the
  notification permission request that only served it).
- `AppDetailScreen` replaces the "Configure" text button with a gear action in the header, the same
  icon the dashboard uses to open Settings.
- `AppAppSettingsScreen` becomes the real per-app configuration: app identity, daily-limit picker,
  use-window picker and alert toggle for exactly one package, reusing the existing
  `appLimitsSection` composable and the existing `AppLimitsStore` writes.
- The screen's back control returns to the detail of the same app.
- The limits editor moves from `ui/settings/` to `ui/detail/` so the detail feature does not import
  the settings feature (`LayerRulesTest`).
- Strings for the new header and labels (en/es).

## Expected outcome

Opening an app from the dashboard, the detail header shows a gear; tapping it opens that app's
configuration with working pickers and a back arrow that returns to the detail. Settings shows
language, appearance, excluded apps and about — no limits. Verified by the instrumented Compose
tests over the gear, the per-app screen and Settings.

## State and persistence mechanism

No new persistence. The limit, window and alert values continue to live in the existing Room
`limits.db` through `AppLimitsStore` (`setLimit`/`clearLimit`, `setWindow`/`clearWindow`,
`setAlertEnabled`); `UsageMonitorService.sync` keeps running after each write. The notification
permission is system-managed and requested from the alert toggle, as today. No DataStore, network,
service or dependency is added.

## Alternatives considered

| Option | Why yes | Why not | Recommendation |
| --- | --- | --- | --- |
| Gear in the detail header + full per-app screen (chosen) | Matches the dashboard's affordance; keeps one configuration surface per app; reuses the existing pickers | A second place to maintain for the pickers | **Chosen** |
| Keep the limits list in Settings and only fix the per-app screen | Smaller diff | The human explicitly rejected the duplicated configuration surface | No |
| Inline editing in the detail | No new screen | Duplicates validation and fights the existing preview | No |
| Move the editor to `ui/common` | Shared folder by convention | Only the detail uses it after the move; `ui/common` is for cross-feature reuse | No |

## Risks and limits

- `AppLimitsSettingsTest` exercises the composable directly; it must follow the new package.
- The layer rule forbids `ui/detail` importing `ui/settings`; moving the file is the fix, not an
  exception.
- No protected zones (`protected: []`) are touched; only `ui/**` and `res/`.
