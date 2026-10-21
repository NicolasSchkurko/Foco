# Spec Delta

## MODIFIED Requirements

### Requirement: A finished day keeps its unlock count
The store SHALL keep every consolidated day's unlock count in that day's marker, derived from the same platform read that stores the day, so the count of a finished day is readable after the platform stops serving it. The marker and its count SHALL be deleted together once the day leaves the retention window.
[@test] `app/src/test/java/com/foco/app/data/ConsolidatedDayTest.kt`
[@test] `app/src/test/java/com/foco/app/ui/dashboard/DashboardStateTest.kt`

#### Scenario: A consolidated day keeps its count
- **WHEN** a finished day with keyguard unlocks is consolidated
- **THEN** reading that day back from the store answers its count, without asking the platform

#### Scenario: The count leaves with its day
- **WHEN** a day falls outside the retention window and a consolidation runs
- **THEN** its marker is deleted with its totals and raw rows, and reading its count answers that none is stored

#### Scenario: The count outlives the raw rows
- **WHEN** a consolidation prunes raw sessions that left the window
- **THEN** the count of every day still inside the window keeps reading back

#### Scenario: The upgrade keeps what was stored
- **WHEN** a database written by version 1 opens after the version 2 upgrade
- **THEN** its sessions and totals are still there and the day's count reads as not stored

#### Scenario: A day without a count reads as absent, not zero
- **WHEN** the dashboard shows a stored day whose count was never stored
- **THEN** the unlock line reads absent — no zero claim — while a stored real `0` still renders as zero
