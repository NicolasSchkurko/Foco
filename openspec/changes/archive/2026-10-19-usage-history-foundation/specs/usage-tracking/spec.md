# Spec Delta

## ADDED Requirements

### Requirement: The day's data outlives the platform's retention
The app SHALL consolidate the day's sessions into a database in its private storage, so a day stays readable after the platform drops it from `queryEvents`; a record's identity SHALL be its package and start, so consolidating the same day twice leaves the same rows.
[@test] `app/src/test/java/com/foco/app/data/HistoryConsolidationTest.kt`

#### Scenario: A day is consolidated
- **WHEN** the app consolidates a day the platform still serves
- **THEN** its sessions are stored and read back with the same total

#### Scenario: The platform no longer serves the day
- **WHEN** a consolidated day is no longer returned by the platform
- **THEN** the day still yields its stored sessions and total

#### Scenario: Consolidating twice
- **WHEN** the same day is consolidated twice
- **THEN** the stored sessions are identical, with no duplicates

### Requirement: The stored history is bounded
The app SHALL keep raw sessions for the last seven days, prune the raw rows older than that, and keep the per-app, per-day totals of every consolidated day; pruning SHALL never remove a total.
[@test] `app/src/test/java/com/foco/app/data/HistoryRetentionTest.kt`

#### Scenario: Pruning raw rows
- **WHEN** a consolidation finds raw sessions older than seven days
- **THEN** those rows are deleted and their per-app totals remain

#### Scenario: Totals are kept
- **WHEN** a day is consolidated
- **THEN** the per-app totals for that day are stored

### Requirement: Stored usage data does not leave the device
The app SHALL declare `android:allowBackup="false"`, so no copy of the stored history is uploaded to a cloud backup or restored onto another device.
[@test] `app/src/test/java/com/foco/app/BackupDisabledManifestTest.kt`

#### Scenario: Backup is off
- **WHEN** the app's manifest is read
- **THEN** `<application>` declares `android:allowBackup="false"`
