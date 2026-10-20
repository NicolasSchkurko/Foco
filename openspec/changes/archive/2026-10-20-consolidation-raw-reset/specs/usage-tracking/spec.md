# Spec Delta

## ADDED Requirements

### Requirement: A consolidation retry replaces the day's raw rows
When a day is consolidated again, the store SHALL replace that day's raw session rows with the new read instead of merging them with rows left by an earlier attempt. The new rows, totals and marker SHALL follow the existing order; a row whose start changed MUST NOT survive.
[@test] `app/src/test/java/com/foco/app/data/HistoryConsolidationTest.kt`

#### Scenario: A pending day is retried with a changed read
- **WHEN** a marker write failed after the raw rows and totals, and the next read returns a merged session with a different start
- **THEN** the day's old raw rows are gone and only the new read remains

#### Scenario: The same read is retried
- **WHEN** the next read is identical
- **THEN** the day still has exactly the same rows and totals

#### Scenario: A marker failure still leaves the day pending
- **WHEN** the marker write fails after the new raw rows and totals
- **THEN** the day remains pending for the next run
