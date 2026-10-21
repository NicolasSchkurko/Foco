# app-settings Specification

## MODIFIED Requirements

### Requirement: Each app limit has an alert toggle
The settings screen SHALL show a per-app alert toggle beside the daily limit. The toggle SHALL be
off by default, SHALL be enabled only while the app has a positive limit, SHALL persist per package,
and SHALL leave alerts off for No limit and 0 minutes. Activating a daily limit SHALL NOT request
the notification permission; the permission SHALL be requested only when the alert toggle is turned
on (and only if it is not already granted).
[@test] `app/src/androidTest/java/com/foco/app/ui/AppLimitsSettingsTest.kt`

#### Scenario: Off by default
- **WHEN** the app limits section is first shown
- **THEN** every alert toggle is off

#### Scenario: Enabling one app
- **WHEN** the user turns on the alert toggle for Instagram with a 30-minute limit
- **THEN** the choice is persisted and the other apps keep their own value

#### Scenario: No limit or zero
- **WHEN** the user picks No limit or Block immediately for an app
- **THEN** its alert toggle is off and disabled, and no alert is produced

#### Scenario: Surviving a restart
- **WHEN** the app restarts
- **THEN** the per-app alert choices are still shown

#### Scenario: Setting a limit does not request permission
- **WHEN** the user picks a daily limit for an app
- **THEN** no notification permission dialog is shown

#### Scenario: Toggle requests permission
- **WHEN** the user turns on the alert toggle and the notification permission is not granted
- **THEN** the notification permission is requested