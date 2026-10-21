# Spec Delta

## ADDED Requirements

### Requirement: Window boundaries follow the local wall clock
The app SHALL evaluate a use window's boundaries on the local wall clock: a boundary stored as `06:00` SHALL be 06:00 local time on every day, including a day the zone's offset changes. The budget counting, the inside check and the boundary check SHALL use that same definition, so neither counts time the other treats as outside the window.
[@test] `app/src/test/java/com/foco/app/domain/AppLimitWindowRulesTest.kt`

#### Scenario: A window ends on a spring-forward day
- **WHEN** the zone moves the clock forward on a day whose window ends at 06:00 local time
- **THEN** the budget stops counting at 06:00 wall time and the boundary check fires for that same instant, not for the epoch instant that reads 07:00

#### Scenario: A boundary inside the skipped range
- **WHEN** the stored boundary minute falls inside the local range the zone skips that day
- **THEN** the boundary resolves to the first existing local time after the gap, and no budget is counted for the gap

#### Scenario: An ordinary day is unchanged
- **WHEN** the local offset does not change during the day
- **THEN** budget, inside check and boundary check answer exactly what they answered before this requirement existed
