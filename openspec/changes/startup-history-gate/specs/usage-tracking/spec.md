# Spec Delta

## ADDED Requirements

### Requirement: The startup backfill closes the first-open gap
The app SHALL consolidate the finished days of the seven-day window when usage access becomes available, before the home exposes the week and detail, so the first open sees the same stored history as a later open. The existing week and detail reads SHALL use that store.
[@test] `app/src/test/java/com/foco/app/data/HistoryConsolidationTest.kt`
[@test] `app/src/androidTest/java/com/foco/app/ui/WeekScreenTest.kt`

#### Scenario: The first open sees six stored days
- **WHEN** the permission is available and the startup run completes
- **THEN** the week's previous six days come from stored totals and the detail's stored-day window is populated

#### Scenario: No close and reopen
- **WHEN** the user opens the week or a stored day immediately after the loading state
- **THEN** the data is already there; no process restart is needed

#### Scenario: A missing permission
- **WHEN** usage access is not granted
- **THEN** the startup run does not start and onboarding remains the entry
