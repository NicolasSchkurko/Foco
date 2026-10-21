# Proposal — app-detail-limits-preview

## Why

The detail screen (`AppDetailScreen`) shows usage, hourly chart and session history for an app, but it never tells whether that app has a daily limit, a recurring window or alerts turned on. The user must open global Settings to discover it. The request asks for a read-only preview in the detail and a dedicated per-app settings screen reachable from it.

## What Changes

- `AppDetailScreen` gains a **limit / window / alert preview** section with simple text (limit minutes, window times, alert on/off), or explicit “None” when empty.
- A **Configure** button opens a new per-app settings screen (`AppAppSettingsScreen`) where the same limit picker, window picker and alert toggle live for that package only.
- `FocoNavHost` adds `app_settings/{packageName}` route to `AppAppSettingsScreen`; back returns to detail.
- Strings (en/es) for the preview labels and button.
- No new database table, no new service, no new dependency: the preview reads existing `AppLimitsStore` flows (`limitsFlow`, `windowsFlow`, `alertsFlow`) and the settings screen writes through `AppLimitsStore.setLimit` / `clearLimit` / `setWindow` / `clearWindow` / `setAlertEnabled`.

## Expected outcome

Opening an app from the dashboard’s day ring opens the detail; the preview section states the configured limit/window/alerts (or “No limit / No window / Alerts off”). Tapping Configure opens the dedicated settings for that app. After change, at least 80 % of detail opens show a correct preview and the Configure path works; measured by instrumented Compose tests over `AppDetailScreen` and the new per-app screen, plus manual verification on the Pixel.

## State and persistence mechanism

No new persistence: `AppLimitsStore` (Room `limits.db`) already holds per-package limits, windows, alerts; the preview reads those flows; the per-app settings screen writes through the same store helpers. No DataStore, no network, no new SDK.

## Alternatives considered

| Option | Why yes | Why not | Recommendation |
| --- | --- | --- | --- |
| Preview + dedicated per-app settings screen (chosen) | Keeps settings localized; reuses existing picker rules; avoids scrolling long global list | New screen to maintain | **Chosen** |
| Preview + deep-link to global Settings (scroll to app) | No new screen | Requires scroll logic and filter in LazyColumn; user asked for dedicated settings | No |
| Preview + inline edit inside detail | One screen | Duplicates validation and picker logic; conflicts with existing `AppLimitsSection` | No |
| Preview only (no settings path) | Smallest change | User explicitly asked to configure from the detail | No |

## Risks and limits

- The preview reads from the same flows that Settings uses; if the store is temporarily unavailable the preview shows empty (same behavior as Settings on first load).
- No protected zones (`protected: []`) are touched; only `ui/**` and `res/`.
- The change is independent of `app-limits-window`; it depends on `app-limits` (already verified/archived).
