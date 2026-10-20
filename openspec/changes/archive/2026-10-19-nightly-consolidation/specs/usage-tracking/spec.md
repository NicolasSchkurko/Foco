# Spec Delta

## ADDED Requirements

### Requirement: History is consolidated without opening the app
The app SHALL enqueue one unique periodic background job (24 h period, first run deferred to 03:00 local, no charging constraint) that consolidates the finished days and prunes raw rows, so a day stays readable even when the app is not opened for more than the raw window.
[@test] `app/src/androidTest/java/com/foco/app/data/ConsolidationWorkerTest.kt`

#### Scenario: The nightly run
- **WHEN** the scheduled job runs
- **THEN** the finished days are consolidated and raw rows older than seven days are pruned

#### Scenario: Scheduled once
- **WHEN** the app starts on several different days
- **THEN** exactly one job is enqueued, and starting again does not replace or duplicate it

#### Scenario: A day the user never saw
- **WHEN** the app was not opened for eight days and the job ran each night
- **THEN** every finished day of that stretch has its stored totals

### Requirement: The two triggers share one serialized run
The app-start trigger and the nightly job SHALL run consolidation through one serialized runner, so a trigger that finds a run in progress waits for it instead of starting a second one, and no day is consolidated or pruned twice.
[@test] `app/src/test/java/com/foco/app/data/ConsolidationConcurrencyTest.kt`

#### Scenario: Both triggers at once
- **WHEN** the job fires while the app-start run is still working
- **THEN** only one run touches the store, and the second waits for it

#### Scenario: No duplicates
- **WHEN** two triggers complete one after the other
- **THEN** the stored rows are the same as after a single run

### Requirement: A failed run retries and prunes nothing
A run that cannot finish SHALL leave the stored data unchanged, SHALL NOT prune any raw row, and SHALL signal a retry with backoff, so a failure costs a delay and not a day.
[@test] `app/src/test/java/com/foco/app/data/ConsolidationRetryTest.kt`

#### Scenario: The run fails midway
- **WHEN** reading the day's sessions throws during consolidation
- **THEN** no raw row is pruned and the job asks to be retried

#### Scenario: Pruning follows consolidation
- **WHEN** the days older than the raw window were consolidated successfully
- **THEN** only then are their raw rows deleted
