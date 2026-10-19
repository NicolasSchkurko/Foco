# Spec Delta

## ADDED Requirements

### Requirement: A finished day keeps its unlock count
The store SHALL keep every consolidated day's unlock count in its own database, next to that day's totals, derived from the same platform read that stores the day, so the count of a finished day is readable after the platform stops serving it and is never pruned with the raw sessions.
[@test] `app/src/test/java/com/foco/app/data/StoredUnlocksTest.kt`
[@test] `app/src/androidTest/java/com/foco/app/data/db/UnlockMigrationTest.kt`

#### Scenario: A consolidated day keeps its count
- **WHEN** a finished day with keyguard unlocks is consolidated
- **THEN** reading that day back from the store answers its count, without asking the platform

#### Scenario: The count outlives the raw rows
- **WHEN** the day's raw sessions are pruned by a later consolidation
- **THEN** its unlock count is still readable

#### Scenario: The upgrade keeps what was stored
- **WHEN** a database written by version 1 opens after the version 2 upgrade
- **THEN** its sessions and totals are still there and the day's count reads as not stored
