# Spec Delta

## ADDED Requirements

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
