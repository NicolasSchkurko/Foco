# Spec Delta

## ADDED Requirements

### Requirement: The day's query window is computed by a pure function
The timestamps a day's query uses — the day's start and end, the lookback start and the effective end — SHALL be computed by a pure function in `domain/`, so the numbers the platform adapter asks for are pinned by a JVM test instead of being readable only inside it.
[@test] `app/src/test/java/com/foco/app/domain/DayWindowTest.kt`

#### Scenario: The day is still running
- **WHEN** the window is computed for today with a clock inside the day
- **THEN** the effective end is that clock value, the day's end is the next midnight, and the lookback is one day before the day's start

#### Scenario: The day is past
- **WHEN** the window is computed for a past day
- **THEN** the effective end is the day's own end, not the clock

#### Scenario: The lookback is exactly one day
- **WHEN** the day's start moves by a day
- **THEN** the lookback moves with it, one day before it — never further
