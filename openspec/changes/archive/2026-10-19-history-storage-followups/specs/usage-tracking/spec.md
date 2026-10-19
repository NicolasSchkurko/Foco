# Spec Delta

## ADDED Requirements

### Requirement: Stored totals are read as a seven-day window
The app SHALL read the stored per-app totals of the seven days ending at a given day, oldest first, so the stored history can be shown without re-deriving it from the platform.
[@test] `app/src/test/java/com/foco/app/data/HistoryWeekTest.kt`

#### Scenario: The window is bounded
- **WHEN** totals exist for days inside and outside the seven days ending at the requested day
- **THEN** only the totals inside that window are returned, oldest first

#### Scenario: Nothing is stored
- **WHEN** no day has stored totals
- **THEN** the read yields nothing

## MODIFIED Requirements

### Requirement: The day's data outlives the platform's retention
The app SHALL consolidate the day's sessions into a database in its private storage, so a day stays readable after the platform drops it from `queryEvents`; a record's identity SHALL be its package and start, so consolidating the same day twice leaves the same rows.
[@test] `app/src/test/java/com/foco/app/data/HistoryConsolidationTest.kt`
[@test] `app/src/test/java/com/foco/app/data/HistoryRetentionTest.kt`

#### Scenario: A day is consolidated
- **WHEN** the app consolidates a day the platform still serves
- **THEN** its sessions are stored and read back with the same total

#### Scenario: The platform no longer serves the day
- **WHEN** a consolidated day is no longer returned by the platform
- **THEN** the day still yields its stored sessions and total

#### Scenario: Consolidating twice
- **WHEN** the same day is consolidated twice
- **THEN** the stored sessions are identical, with no duplicates

#### Scenario: A day with no usage yet
- **WHEN** a consolidation finds nothing to store for a finished day and a later consolidation does
- **THEN** the later run stores that day, because a day without totals stays pending
