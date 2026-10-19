# Spec Delta

## ADDED Requirements

### Requirement: The week view covers the last seven days
The week view SHALL show the last seven days as one bar per day (the day's total screen time) plus the ranked per-app totals of those days, derived in the UI by pure functions; today SHALL come live from the platform, and a day without stored data SHALL read as absent, not as zero.
[@test] `app/src/test/java/com/foco/app/ui/metrics/WeekMetricsTest.kt`
[@test] `app/src/androidTest/java/com/foco/app/ui/WeekScreenTest.kt`

#### Scenario: The seven bars
- **WHEN** the week view opens
- **THEN** it shows seven bars, today live and the previous six from the stored totals

#### Scenario: A day with no data
- **WHEN** a day in the window has no stored total
- **THEN** its bar reads as absent, not as zero screen time

#### Scenario: The ranking
- **WHEN** the week has usage
- **THEN** the apps are listed by their total over the seven days, descending

### Requirement: A day bar opens that day's view
The week view SHALL be reachable from the dashboard, the back action SHALL return to it, and tapping a day's bar SHALL open that day's view — the dashboard composition for that date, read from stored sessions — where an app row opens that app's detail for the day.
[@test] `app/src/androidTest/java/com/foco/app/ui/WeekScreenTest.kt`

#### Scenario: Entry and return
- **WHEN** the week control is used in the dashboard
- **THEN** the week view opens and the back action returns to the dashboard

#### Scenario: Opening a day
- **WHEN** a bar is tapped
- **THEN** that day's view opens with its total, hourly chart and ranking
