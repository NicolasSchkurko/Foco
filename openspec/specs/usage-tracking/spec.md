# usage-tracking Specification

## Purpose
Track device usage per app as time sessions for the current day, and derive the dashboard's numbers (day total, hourly activity, per-app ranking) from those sessions.

## Requirements

### Requirement: Usage sessions are built from system events
The system SHALL build per-app usage sessions (package, start, end) for the current day from activity resume/pause/stop events, as a computation over plain event data: switching directly between apps closes the previous session at the new start; a session open at query time closes at "now"; transitions inside the same app do not split the session.
[@test] `app/src/test/java/com/foco/app/data/SessionBuilderTest.kt`

#### Scenario: Direct switch between apps
- **WHEN** app A resumes at 10:00 and app B resumes at 10:05
- **THEN** A yields the session 10:00–10:05 and B's session starts at 10:05

#### Scenario: Session open at query time
- **WHEN** an app is in the foreground when the day is queried
- **THEN** its session ends at the query time

#### Scenario: No activity
- **WHEN** there are no relevant events for the day
- **THEN** the session list is empty

### Requirement: Only user-facing apps count
The system SHALL keep only sessions whose package has a launcher icon, excluding the device home app and Foco itself, by filtering over an allowed/excluded package set.
[@test] `app/src/test/java/com/foco/app/data/SessionFilterTest.kt`

#### Scenario: Internal packages are dropped
- **WHEN** sessions include a package with no launcher icon
- **THEN** it is not present in the returned sessions

#### Scenario: Home and self are dropped
- **WHEN** sessions include the home package or Foco's own package
- **THEN** neither is present in the returned sessions

### Requirement: Dashboard metrics are derived in the UI from sessions
The dashboard SHALL derive its numbers — day total, hourly distribution (splitting sessions that cross an hour boundary) and per-app ranking (sorted by total duration, descending) — in the UI layer, from the session list, using pure computations verifiable by unit tests. Presentation formats SHALL remain those of `Formatting.kt`.
[@test] `app/src/test/java/com/foco/app/ui/metrics/UsageMetricsTest.kt`

#### Scenario: Day total
- **WHEN** the day's sessions sum to 3 h 24 min
- **THEN** the derived total equals that duration in milliseconds

#### Scenario: Session crossing an hour boundary
- **WHEN** a session runs from 10:50 to 11:10
- **THEN** 10 minutes count toward hour 10 and 10 minutes toward hour 11

#### Scenario: Ranking order
- **WHEN** app A totals 40 min and app B totals 10 min
- **THEN** A ranks before B

### Requirement: The dashboard refreshes automatically on returning to the app
The dashboard SHALL reload usage data every time the app returns to the foreground, and on first show, without any user action.
[@test] `app/src/androidTest/java/com/foco/app/ui/DashboardRefreshTest.kt`

#### Scenario: Returning from another app
- **WHEN** the user leaves Foco and comes back with the usage permission already granted
- **THEN** the dashboard has reloaded its data on resume

#### Scenario: First show after opening
- **WHEN** the app is opened with the permission granted
- **THEN** the dashboard loads its data without user action

### Requirement: Data refreshes periodically while the app is in the foreground
While the dashboard is visible, the app SHALL refresh usage data periodically (about every 30 seconds), and SHALL NOT run refresh work while the app is not in the foreground.
[@test] `app/src/androidTest/java/com/foco/app/ui/DashboardRefreshTest.kt`

#### Scenario: Periodic refresh while visible
- **WHEN** the dashboard stays open for longer than the refresh interval
- **THEN** the data is reloaded again without user action

#### Scenario: No refresh work in the background
- **WHEN** the app goes to the background
- **THEN** the periodic refresh stops until the app is visible again

### Requirement: No manual refresh control is needed
The dashboard SHALL provide automatic freshness and SHALL NOT require a manual refresh control.
[@test] `app/src/androidTest/java/com/foco/app/ui/DashboardRefreshTest.kt`

#### Scenario: No refresh button
- **WHEN** the dashboard is shown
- **THEN** there is no manual refresh button, and data freshness is guaranteed automatically

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
