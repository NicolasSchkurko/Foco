# Spec Delta

## MODIFIED Requirements

### Requirement: A finished day keeps its unlock count
The store SHALL keep every consolidated day's unlock count in the row that marks that day consolidated, derived from the same platform read that stores the day, so the count of a finished day is readable after the platform stops serving it and is never pruned with the raw sessions.
[@test] `app/src/test/java/com/foco/app/data/ConsolidatedDayTest.kt`
[@test] `app/src/androidTest/java/com/foco/app/data/db/ConsolidationMigrationTest.kt`

#### Scenario: A consolidated day keeps its count
- **WHEN** a finished day with keyguard unlocks is consolidated
- **THEN** reading that day back from the store answers its count, without asking the platform

#### Scenario: The count outlives the raw rows
- **WHEN** the day's raw sessions are pruned by a later consolidation
- **THEN** its unlock count is still readable

#### Scenario: The upgrade keeps what was stored
- **WHEN** a database written by version 2 opens after the version 3 upgrade
- **THEN** its sessions and totals are still there, every day that stored a count still answers it, and a day that stored none reads as not stored

## ADDED Requirements

### Requirement: Consolidating a day ends with its marker
The consolidation SHALL write a day's sessions, then its per-app totals, and finally one row marking the day consolidated with its unlock count; the pending set SHALL be read from those rows, so an interrupted day is retried on the next run and a day consolidated by this version always carries its count.
[@test] `app/src/test/java/com/foco/app/data/HistoryConsolidationTest.kt`

#### Scenario: Interrupted before the marker
- **WHEN** the process dies after a day's totals are stored and before its marker is written
- **THEN** the next run still sees that day as pending, consolidates it again and stores its count

#### Scenario: One marker per day
- **WHEN** the same day is consolidated twice
- **THEN** it keeps a single marker row holding the count of the latest run
