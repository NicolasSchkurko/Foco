# Spec Delta

## MODIFIED Requirements

### Requirement: The stored history is bounded

The app SHALL keep raw sessions for the last eight days — today and the seven days before it — and
SHALL delete, on every consolidation run, every stored row older than that day: raw sessions,
per-app daily totals and the day's marker with its unlock count.

[@test] `app/src/test/java/com/foco/app/data/HistoryRetentionTest.kt`
[@test] `app/src/test/java/com/foco/app/data/ConsolidationRetryTest.kt`

#### Scenario: Pruning outside the window
- **WHEN** a consolidation runs and rows exist for a day older than today minus seven days
- **THEN** that day's sessions, totals and marker are deleted

#### Scenario: The guard day stays
- **WHEN** rows exist exactly for the day today minus seven days
- **THEN** they are kept, so a lagging consolidation can still rebuild the seven-day window

#### Scenario: The read window is unchanged
- **WHEN** the week view reads the totals of the seven days ending at a day
- **THEN** it returns the same rows as before this rule existed

#### Scenario: Rows left by an older version
- **WHEN** the store holds totals or a marker outside the window (written before this rule)
- **THEN** the next run deletes them, so the database still shrinks

#### Scenario: Pruning raw rows
- **WHEN** a consolidation finds raw sessions older than the window
- **THEN** those rows are deleted

#### Scenario: Totals are kept
- **WHEN** a day still inside the window is consolidated
- **THEN** its per-app totals are stored and stay readable until the day leaves the window

### Requirement: A finished day keeps its unlock count

The store SHALL keep every consolidated day's unlock count in that day's marker, derived from the
same platform read that stores the day, so the count of a finished day is readable after the
platform stops serving it. The marker and its count SHALL be deleted together once the day leaves
the retention window.

[@test] `app/src/test/java/com/foco/app/data/ConsolidatedDayTest.kt`

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

### Requirement: Day selection within the stored history

The detail SHALL offer every day from today back to the oldest stored day, seven at most (today and
the six days before it), with ‹ › controls stopped at today and at the oldest day: today live,
earlier days from storage. The window SHALL follow the store while the detail is open.

[@test] `app/src/test/java/com/foco/app/domain/DaySelectionTest.kt`
[@test] `app/src/test/java/com/foco/app/domain/DetailDaySourceTest.kt`
[@test] `app/src/androidTest/java/com/foco/app/ui/AppDetailNavigationTest.kt`

#### Scenario: Bounds
- **WHEN** the oldest stored day is shown
- **THEN** the back arrow is disabled and selection stays within the stored window

#### Scenario: Switching days
- **WHEN** the user moves to a previous day
- **THEN** the detail reloads totals, chart and sessions from that day's stored sessions

#### Scenario: A day inside the window without stored sessions
- **WHEN** the offered day has no stored sessions (the store never got it)
- **THEN** the detail shows its empty state and the arrows keep moving

#### Scenario: A day the platform no longer serves
- **WHEN** the chosen day is in the stored history but no longer returned by `queryEvents`
- **THEN** the detail still shows that day's totals, chart and sessions

#### Scenario: A day stored while the detail is open
- **WHEN** a finished day is consolidated into the store while the detail is showing
- **THEN** the selector gains that day and the back arrow reaches it, without the screen being paused and resumed

### Requirement: History is consolidated without opening the app

The app SHALL enqueue one unique periodic background job (24 h period, first run deferred to 03:00
local, no charging constraint) that consolidates the finished days and prunes the rows that left
the retention window, so a day stays readable even when the app is not opened for longer than that
window.

[@test] `app/src/androidTest/java/com/foco/app/data/ConsolidationWorkerTest.kt`

#### Scenario: The nightly run
- **WHEN** the scheduled job runs
- **THEN** the finished days are consolidated and every row older than the window is pruned from the three tables

#### Scenario: Scheduled once
- **WHEN** the app starts on several different days
- **THEN** exactly one job is enqueued, and starting again does not replace or duplicate it

#### Scenario: A day the user never saw
- **WHEN** the app was not opened for eight days and the job ran each night
- **THEN** every finished day of that stretch has its stored totals
