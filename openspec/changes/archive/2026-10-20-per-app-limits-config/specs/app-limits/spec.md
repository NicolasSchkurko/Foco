# app-limits Specification

## ADDED Requirements

### Requirement: The detail previews the configured limit, window and alerts

The app detail SHALL show a read-only preview of the daily limit, the recurring use window and the
alert state, read from the same per-app configuration the configuration screen writes. The preview
block SHALL NOT carry an editing action.

[@test] `app/src/androidTest/java/com/foco/app/ui/AppDetailLimitsTest.kt`

#### Scenario: An app with a limit, a window and alerts

- **WHEN** Instagram has a 30-minute limit, a 09:00-23:00 window and alerts on
- **THEN** the detail preview shows 30 minutes, 09:00-23:00 and that alerts are on

#### Scenario: An app with nothing configured

- **WHEN** the viewed app has no limit, no window and alerts off
- **THEN** the preview shows "No limit", "No window" and "Alerts off" rather than omitting the row

#### Scenario: The preview does not change the configuration

- **WHEN** the preview is shown
- **THEN** no control in it changes the stored limit, window or alert state

### Requirement: The detail header opens the app's configuration with a gear

The app detail header SHALL show a gear action, the same icon the dashboard uses to open Settings,
that opens the configuration screen for the app being viewed. The dedicated screen SHALL show only
that package, and its back action SHALL return to the detail of the same app.

[@test] `app/src/androidTest/java/com/foco/app/ui/AppDetailLimitsTest.kt`
[@test] `app/src/androidTest/java/com/foco/app/ui/AppAppSettingsTest.kt`

#### Scenario: Opening the dedicated settings

- **WHEN** the gear in the detail of Instagram is tapped
- **THEN** the configuration screen for Instagram opens, showing its limit, window and alert controls

#### Scenario: Only the viewed app

- **WHEN** the dedicated screen for Instagram is open
- **THEN** no other app is listed

#### Scenario: Going back

- **WHEN** the back action is used from the dedicated screen
- **THEN** the detail of the same app is shown again

### Requirement: The dedicated screen edits the daily limit

The dedicated screen SHALL offer No limit, Block immediately and the 15-minute steps through 24 h.
Each pick SHALL persist per package through `AppLimitsStore` and SHALL sync the monitor.

[@test] `app/src/androidTest/java/com/foco/app/ui/AppAppSettingsTest.kt`

#### Scenario: Picking a 30-minute limit

- **WHEN** 30 min is picked for the app on the dedicated screen
- **THEN** the limit is persisted for that package and the monitor is synced

#### Scenario: Clearing the limit

- **WHEN** No limit is picked on the dedicated screen
- **THEN** the limit row is cleared for that package and the monitor is synced

### Requirement: The dedicated screen edits the use window

The dedicated screen SHALL offer No window or a 15-minute start/end pair, SHALL allow overnight
ranges, SHALL reject an equal start and end, and SHALL persist the value per package.

[@test] `app/src/androidTest/java/com/foco/app/ui/AppAppSettingsTest.kt`

#### Scenario: Picking a window

- **WHEN** 09:00 and 23:00 are picked on the dedicated screen
- **THEN** the window is persisted for that package and the monitor is synced

#### Scenario: Equal start and end

- **WHEN** the same start and end are picked
- **THEN** the value is rejected and the previous window stays

### Requirement: The dedicated screen edits the alert toggle

The dedicated screen SHALL offer the alert toggle, off by default and enableable only with a
positive limit. Turning it on SHALL request the notification permission when missing and SHALL
persist the flag per package.

[@test] `app/src/androidTest/java/com/foco/app/ui/AppAppSettingsTest.kt`

#### Scenario: Enabling alerts

- **WHEN** the alert toggle is turned on for an app with a positive limit
- **THEN** the notification permission is requested if missing and the flag is persisted per package

#### Scenario: No limit or zero

- **WHEN** the app has No limit or Block immediately
- **THEN** its alert toggle is off and disabled
