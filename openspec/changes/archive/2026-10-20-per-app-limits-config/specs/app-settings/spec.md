# app-settings Specification

## REMOVED Requirements

### Requirement: App limits are configured in Settings

**Reason**: The configuration surface for limits moved to each app's detail, behind the gear, so
the global Settings screen no longer lists every launchable app with its limit. Listing the same
values in two places is the duplication the human rejected.

**Migration**: The picker, its 15-minute grid and its persistence rules live in the `app-limits`
capability under "The dedicated screen edits the limit, window and alerts"; `SettingsScreenTest` no
longer asserts the limits section, and `AppAppSettingsTest` covers the moved controls.

### Requirement: Each app limit has an alert toggle

**Reason**: The alert toggle moved with the limit to the per-app configuration screen; the global
Settings screen no longer shows it.

**Migration**: The same toggle (off by default, enabled only with a positive limit, persisted per
package) is required by `app-limits` "The dedicated screen edits the limit, window and alerts";
`AppLimitsSettingsTest` continues to exercise the composable, now reached from the detail.

### Requirement: Each app has a use window picker

**Reason**: The window picker moved with the limit to the per-app configuration screen; the global
Settings screen no longer shows it.

**Migration**: The same picker (No window or a 15-minute start/end pair, overnight allowed, equal
start and end rejected) is required by `app-limits` "The dedicated screen edits the limit, window
and alerts".
