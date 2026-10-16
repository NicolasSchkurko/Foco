# Spec Delta

## Purpose

Give each app a drill-down: when its time was spent, day by day within what the system retains, derived from the same session data as the dashboard.

## ADDED Requirements

### Requirement: Tapping an app opens its usage detail
Tapping an app in the ranking SHALL open a detail view for that app, and the system back action SHALL return to the dashboard.
[@test] `app/src/androidTest/java/com/foco/app/ui/AppDetailNavigationTest.kt`

#### Scenario: Opening the detail
- **WHEN** an app row is tapped in the ranking
- **THEN** the detail view for that app opens

#### Scenario: Returning to the dashboard
- **WHEN** the back action is used from the detail
- **THEN** the dashboard is shown again

### Requirement: The detail shows the app's hourly breakdown
The detail SHALL show the selected app's usage distributed by hour (00–23) for the chosen day, splitting sessions that cross an hour boundary, using the same representation as the dashboard's day chart.
[@test] `app/src/test/java/com/foco/app/ui/metrics/AppDetailMetricsTest.kt`

#### Scenario: Only the selected app counts
- **WHEN** the day also has usage from other apps
- **THEN** only the selected app's sessions feed the hourly breakdown

#### Scenario: Session crossing an hour boundary
- **WHEN** a selected-app session runs from 10:50 to 11:10
- **THEN** 10 minutes count toward hour 10 and 10 minutes toward hour 11

### Requirement: The detail summarizes the app's day
The detail SHALL show, for the chosen app and day: total time, number of sessions, first open time (earliest session start), last use time (latest session end), average session length, the longest session and the chronological session list.
[@test] `app/src/test/java/com/foco/app/ui/metrics/AppDetailMetricsTest.kt`

#### Scenario: Typical day
- **WHEN** the app has sessions 10:00–10:30 and 11:00–11:50
- **THEN** total is 1 h 20 min, count is 2, first is 10:00, last is 11:50, average is 40 min, longest is 50 min, and the list shows both sessions in order

#### Scenario: Day without data
- **WHEN** the app has no sessions that day
- **THEN** the detail shows an explicit empty state instead of numbers

### Requirement: Day selection within system retention
The detail SHALL offer moving to previous days with ‹ › controls limited to the window the system retains (today plus the previous six days by default), the forward arrow SHALL be disabled at today and the back arrow at the oldest available day, and switching days SHALL reload the breakdown for that day.
[@test] `app/src/test/java/com/foco/app/data/DaySelectionTest.kt`

#### Scenario: Bounds
- **WHEN** the oldest available day is shown
- **THEN** the back arrow is disabled and selection stays within the retained window

#### Scenario: Switching days
- **WHEN** the user moves to a previous day
- **THEN** the detail reloads totals, chart and sessions for that day
