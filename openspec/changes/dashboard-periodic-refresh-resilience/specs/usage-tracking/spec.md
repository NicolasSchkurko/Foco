# Spec Delta

## MODIFIED Requirements

### Requirement: Data refreshes periodically while the app is in the foreground
While the dashboard is visible, the app SHALL refresh usage data periodically (about every 30 seconds), and SHALL NOT run refresh work while the app is not in the foreground. A failed read SHALL NOT stop the periodic refresh.
[@test] `app/src/androidTest/java/com/foco/app/ui/dashboard/DashboardRefreshTest.kt`

#### Scenario: Periodic refresh while visible
- **WHEN** the dashboard stays open for longer than the refresh interval
- **THEN** the data is reloaded again without user action

#### Scenario: No refresh work in the background
- **WHEN** the app goes to the background
- **THEN** the periodic refresh stops until the app is visible again

#### Scenario: A failed read does not stop refresh
- **WHEN** a periodic read throws
- **THEN** the refresh loop keeps running and retries on the next interval