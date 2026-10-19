# Spec Delta

## REMOVED Requirements

### Requirement: Day selection within system retention
**Reason**: The detail no longer depends on what the platform retains. Keeping the bound would offer days the detail cannot read consistently and hide days it can.
**Migration**: Replaced by *Day selection within the stored history* below, with the same bounds and scenarios plus the day the platform no longer serves. `DaySelectionTest` moves its expectation from the platform window to the stored window; `DetailDaySourceTest` pins the source decision.

## ADDED Requirements

### Requirement: Day selection within the stored history
The detail SHALL offer the days its stored history covers with ‹ › controls: today read live, the previous six from storage (the raw retention), the forward arrow disabled at today and the back arrow at the oldest stored day; switching days SHALL reload the breakdown from that day's stored sessions.
[@test] `app/src/test/java/com/foco/app/domain/DaySelectionTest.kt`
[@test] `app/src/test/java/com/foco/app/data/DetailDaySourceTest.kt`

#### Scenario: Bounds
- **WHEN** the oldest stored day is shown
- **THEN** the back arrow is disabled and selection stays within the stored window

#### Scenario: Switching days
- **WHEN** the user moves to a previous day
- **THEN** the detail reloads totals, chart and sessions from that day's stored sessions

#### Scenario: A day the platform no longer serves
- **WHEN** the chosen day is in the stored history but no longer returned by `queryEvents`
- **THEN** the detail still shows that day's totals, chart and sessions
