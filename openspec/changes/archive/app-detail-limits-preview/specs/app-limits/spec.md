# app-limits Specification

## ADDED Requirements

### Requirement: The app detail previews the configured limit, window and alerts (read-only)
The app detail screen SHALL show a read-only preview of the daily limit, the recurring use
window and the alert state, read from the same per-app configuration that settings uses.
[@test] `app/src/androidTest/java/com/foco/app/ui/AppDetailLimitsTest.kt`

#### Scenario: The preview does not change the configuration
- **WHEN** the preview is shown
- **THEN** no control in it changes the stored limit, window or alert state

### Requirement: The preview states each value or "not configured" explicitly
The preview SHALL show the limit in minutes, the window as start/end times, and whether
alerts are on; when a value is absent it SHALL say so explicitly rather than hiding the row.
[@test] `app/src/androidTest/java/com/foco/app/ui/AppDetailLimitsTest.kt`

#### Scenario: An app with a limit, a window and alerts
- **WHEN** Instagram has a 30-minute limit, a 09:00-23:00 window and alerts on
- **THEN** the detail preview shows 30 minutes, 09:00-23:00 and that alerts are on

#### Scenario: An app with nothing configured
- **WHEN** the viewed app has no limit, no window and alerts off
- **THEN** the preview shows "No limit", "No window" and "Alerts off" rather than omitting the row

#### Scenario: Only a window
- **WHEN** the viewed app has a 22:00-06:00 window and no minute limit
- **THEN** the preview shows "No limit" and 22:00-06:00

### Requirement: The detail opens the app's dedicated settings
The app detail SHALL offer a Configure action that opens a settings screen dedicated to the app
being viewed, carrying the same limit picker, window picker and alert toggle for that package, and
the back action from that screen SHALL return to the detail of the same app. The dedicated screen
SHALL NOT show other apps.
[@test] `app/src/androidTest/java/com/foco/app/ui/AppDetailLimitsTest.kt`
[@test] `app/src/androidTest/java/com/foco/app/ui/AppAppSettingsTest.kt`

#### Scenario: Opening the dedicated settings
- **WHEN** Configure is tapped in the detail of Instagram
- **THEN** the settings screen for Instagram opens, showing its limit, window and alert controls

#### Scenario: Only the viewed app
- **WHEN** the dedicated settings screen for Instagram is open
- **THEN** no other app is listed

#### Scenario: Going back
- **WHEN** the back action is used from the dedicated settings screen
- **THEN** the detail of the same app is shown again

#### Scenario: The configured values reach the store
- **WHEN** a limit, a window or the alert toggle is changed in the dedicated settings screen
- **THEN** the new value is persisted per package, as it is from the global settings screen
