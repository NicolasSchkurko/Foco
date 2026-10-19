# Spec Delta

## MODIFIED Requirements

### Requirement: Day selection within the stored history
The detail SHALL offer every day from today back to the oldest stored day, seven at most (the raw retention), with ‹ › controls: today read live and the previous days from storage, the forward arrow disabled at today and the back arrow at the oldest day offered. The offered window SHALL follow the store while the detail is open: a day the store gains becomes reachable without the screen being paused and resumed.
[@test] `app/src/test/java/com/foco/app/domain/DaySelectionTest.kt`
[@test] `app/src/test/java/com/foco/app/domain/DetailDaySourceTest.kt`
[@test] `app/src/androidTest/java/com/foco/app/ui/AppDetailNavigationTest.kt`

#### Scenario: Bounds
- **WHEN** the oldest stored day is shown
- **THEN** the back arrow is disabled and selection stays within the stored window

#### Scenario: Switching days
- **WHEN** the user moves to a previous day
- **THEN** the detail reloads totals, chart and sessions from that day's stored sessions

#### Scenario: A day inside the window without stored sessions
- **WHEN** the offered day has no stored sessions (the store never got it)
- **THEN** the detail shows its empty state and the arrows keep moving

#### Scenario: A day the platform no longer serves
- **WHEN** the chosen day is in the stored history but no longer returned by `queryEvents`
- **THEN** the detail still shows that day's totals, chart and sessions

#### Scenario: A day stored while the detail is open
- **WHEN** a finished day is consolidated into the store while the detail is showing
- **THEN** the selector gains that day and the back arrow reaches it, without the screen being paused and resumed
