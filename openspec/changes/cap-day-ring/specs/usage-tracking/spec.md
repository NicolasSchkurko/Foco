# Spec Delta

## MODIFIED Requirements

### Requirement: The day ring wraps through the week

The ring SHALL hold today, the week, the oldest stored day inside the seven-day window and every
finished day between them; the step forward from today SHALL land on the week, the step forward
from the week on the oldest stored day, and the step back from the oldest stored day on the week.

[@test] `app/src/test/java/com/foco/app/domain/DayRingTest.kt`

#### Scenario: Forward from today
- **WHEN** the home is on today and the user steps forward
- **THEN** the week is shown

#### Scenario: Forward from the week
- **WHEN** the home is on the week and the user steps forward
- **THEN** the oldest stored day inside the window is shown

#### Scenario: Back from the oldest day
- **WHEN** the home is on the oldest stored day and the user steps back
- **THEN** the week is shown

#### Scenario: A store with no finished days
- **WHEN** the store holds no finished day
- **THEN** the ring is today and the week, and forward from the week returns to today

#### Scenario: The guard day stays out of the ring
- **WHEN** the store holds a day older than today minus six (the guard day kept for a lagging run)
- **THEN** the ring offers today and the six days before it, and the guard day is not reachable
