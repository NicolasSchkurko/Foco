# Spec Delta

## ADDED Requirements

### Requirement: Each app has a use window picker
The settings screen SHALL show a per-app use-window control beside the limit. It SHALL offer No
window and a start/end pair on a 15-minute grid in local time, allow overnight ranges, reject equal
start and end, persist the value per package, and allow a window without a minute limit.
[@test] `app/src/androidTest/java/com/foco/app/ui/AppLimitsSettingsTest.kt`

#### Scenario: No window by default
- **WHEN** an app is shown for the first time
- **THEN** its window control reads No window

#### Scenario: Limit without a window
- **WHEN** the user sets 30 minutes and leaves the window at No window
- **THEN** the limit persists and there is no time-of-day restriction

#### Scenario: Both active
- **WHEN** the user sets 30 minutes and 09:00–23:00
- **THEN** both persist and the app is blocked outside the window and limited inside

#### Scenario: Setting 09:00–23:00
- **WHEN** the user picks 09:00 and 23:00
- **THEN** the row reads 09:00–23:00 and the value survives a restart

#### Scenario: Overnight
- **WHEN** the user picks 22:00 and 06:00
- **THEN** the row accepts the overnight range and shows 22:00–06:00

#### Scenario: Equal start and end
- **WHEN** the user tries to set the same start and end
- **THEN** the value is rejected and the previous window stays

#### Scenario: Window without a limit
- **WHEN** the user sets a window and leaves the minute limit at No limit
- **THEN** the window is persisted and the app is restricted outside it
