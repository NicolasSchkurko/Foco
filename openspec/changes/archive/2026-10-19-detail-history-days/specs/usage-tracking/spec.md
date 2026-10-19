# Spec Delta

## REMOVED Requirements

### Requirement: Day selection within system retention
**Reason**: The detail no longer depends on what the platform retains. Keeping the bound would offer days the detail cannot read consistently and hide days it can.
**Migration**: Replaced by *Day selection within the stored history* below, with the same bounds and scenarios plus the day the platform no longer serves. `DaySelectionTest` moves its expectation from the platform window to the stored window; `DetailDaySourceTest` pins the source decision.

## ADDED Requirements

### Requirement: Day selection within the stored history
The detail SHALL offer every day from today back to the oldest stored day, seven at most (the raw retention), with ‹ › controls: today read live and the previous days from storage, the forward arrow disabled at today and the back arrow at the oldest day offered.
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

### Requirement: A failed stored read degrades instead of closing the app
The detail SHALL keep working when a stored read fails: it shows the empty state for that day, or today alone when the stored days could not be read, and the app never closes.
[@test] `app/src/androidTest/java/com/foco/app/ui/detail/AppDetailReadTest.kt`

#### Scenario: A stored read fails
- **WHEN** reading a day's sessions or the list of stored days throws
- **THEN** the screen answers its empty state (or today alone) and stays open
