# usage-tracking Specification

## Purpose
Track device usage per app as time sessions for the current day, and derive the dashboard's numbers (day total, hourly activity, per-app ranking) from those sessions.

## Requirements

### Requirement: Usage sessions are built from system events
The system SHALL build per-app usage sessions (package, start, end) for the day from resume/pause/stop events, over plain event data: the direct switch closes the previous session at the new start and a pause or stop closes the open session of its own package (both ties-inclusive); a session open at query time closes at "now"; same-app transitions do not split it; a zero-length session is not emitted.
[@test] `app/src/test/java/com/foco/app/domain/SessionBuilderTest.kt`

#### Scenario: Direct switch between apps
- **WHEN** app A resumes at 10:00 and app B resumes at 10:05
- **THEN** A yields the session 10:00–10:05 and B's session starts at 10:05

#### Scenario: A direct switch inside the same millisecond
- **WHEN** app A resumes and app B resumes at the same millisecond
- **THEN** A's session closes at that instant and B's session opens there, with A contributing no time

#### Scenario: Session open at query time
- **WHEN** an app is in the foreground when the day is queried
- **THEN** its session ends at the query time

#### Scenario: A stop in the same millisecond as the resume
- **WHEN** an app resumes and stops at the same millisecond
- **THEN** no session is emitted for it, and no package is left open afterwards

#### Scenario: No activity
- **WHEN** there are no relevant events for the day
- **THEN** the session list is empty

### Requirement: Only user-facing apps count
The system SHALL keep only sessions whose package has a launcher icon, excluding the device home app and Foco itself, by filtering over an allowed/excluded package set.
[@test] `app/src/test/java/com/foco/app/data/SessionFilterTest.kt`

#### Scenario: Internal packages are dropped
- **WHEN** sessions include a package with no launcher icon
- **THEN** it is not present in the returned sessions

#### Scenario: Home and self are dropped
- **WHEN** sessions include the home package or Foco's own package
- **THEN** neither is present in the returned sessions

### Requirement: Dashboard metrics are derived in the UI from sessions
The dashboard SHALL derive its numbers — day total, hourly distribution (splitting sessions that cross an hour boundary) and per-app ranking (sorted by total duration, descending) — in the UI layer, from the session list, using pure computations verifiable by unit tests. Presentation formats SHALL remain those of `Formatting.kt`.
[@test] `app/src/test/java/com/foco/app/ui/metrics/UsageMetricsTest.kt`

#### Scenario: Day total
- **WHEN** the day's sessions sum to 3 h 24 min
- **THEN** the derived total equals that duration in milliseconds

#### Scenario: Session crossing an hour boundary
- **WHEN** a session runs from 10:50 to 11:10
- **THEN** 10 minutes count toward hour 10 and 10 minutes toward hour 11

#### Scenario: Ranking order
- **WHEN** app A totals 40 min and app B totals 10 min
- **THEN** A ranks before B

### Requirement: The dashboard refreshes automatically on returning to the app
The dashboard SHALL reload usage data every time the app returns to the foreground, and on first show, without any user action.
[@test] `app/src/androidTest/java/com/foco/app/ui/DashboardRefreshTest.kt`

#### Scenario: Returning from another app
- **WHEN** the user leaves Foco and comes back with the usage permission already granted
- **THEN** the dashboard has reloaded its data on resume

#### Scenario: First show after opening
- **WHEN** the app is opened with the permission granted
- **THEN** the dashboard loads its data without user action

### Requirement: Data refreshes periodically while the app is in the foreground
While the dashboard is visible, the app SHALL refresh usage data periodically (about every 30 seconds), and SHALL NOT run refresh work while the app is not in the foreground.
[@test] `app/src/androidTest/java/com/foco/app/ui/DashboardRefreshTest.kt`

#### Scenario: Periodic refresh while visible
- **WHEN** the dashboard stays open for longer than the refresh interval
- **THEN** the data is reloaded again without user action

#### Scenario: No refresh work in the background
- **WHEN** the app goes to the background
- **THEN** the periodic refresh stops until the app is visible again

### Requirement: No manual refresh control is needed
The dashboard SHALL provide automatic freshness and SHALL NOT require a manual refresh control.
[@test] `app/src/androidTest/java/com/foco/app/ui/DashboardRefreshTest.kt`

#### Scenario: No refresh button
- **WHEN** the dashboard is shown
- **THEN** there is no manual refresh button, and data freshness is guaranteed automatically

### Requirement: Tapping an app opens its usage detail
Tapping an app in the ranking SHALL open a detail view for that app, and the system back action SHALL return to the dashboard.
[@test] `app/src/androidTest/java/com/foco/app/ui/AppDetailNavigationTest.kt`

#### Scenario: Opening the detail
- **WHEN** an app row is tapped in the ranking
- **THEN** the detail view for that app opens

#### Scenario: Returning to the dashboard
- **WHEN** the back action is used from the detail
- **THEN** the dashboard is shown again

### Requirement: The detail shows the app's hourly breakdown
The detail SHALL show the selected app's usage distributed by hour (00–23) for the chosen day, splitting sessions that cross an hour boundary, using the same representation as the dashboard's day chart.
[@test] `app/src/test/java/com/foco/app/ui/metrics/AppDetailMetricsTest.kt`

#### Scenario: Only the selected app counts
- **WHEN** the day also has usage from other apps
- **THEN** only the selected app's sessions feed the hourly breakdown

#### Scenario: Session crossing an hour boundary
- **WHEN** a selected-app session runs from 10:50 to 11:10
- **THEN** 10 minutes count toward hour 10 and 10 minutes toward hour 11

### Requirement: The detail summarizes the app's day
The detail SHALL show, for the chosen app and day: total time, number of sessions, first open time (earliest session start), last use time (latest session end), average session length, the longest session and the chronological session list.
[@test] `app/src/test/java/com/foco/app/ui/metrics/AppDetailMetricsTest.kt`

#### Scenario: Typical day
- **WHEN** the app has sessions 10:00–10:30 and 11:00–11:50
- **THEN** total is 1 h 20 min, count is 2, first is 10:00, last is 11:50, average is 40 min, longest is 50 min, and the list shows both sessions in order

#### Scenario: Day without data
- **WHEN** the app has no sessions that day
- **THEN** the detail shows an explicit empty state instead of numbers

### Requirement: Day selection within system retention
The detail SHALL offer moving to previous days with ‹ › controls limited to the window the system retains (today plus the previous six days by default), the forward arrow SHALL be disabled at today and the back arrow at the oldest available day, and switching days SHALL reload the breakdown for that day.
[@test] `app/src/test/java/com/foco/app/data/DaySelectionTest.kt`

#### Scenario: Bounds
- **WHEN** the oldest available day is shown
- **THEN** the back arrow is disabled and selection stays within the retained window

#### Scenario: Switching days
- **WHEN** the user moves to a previous day
- **THEN** the detail reloads totals, chart and sessions for that day

### Requirement: Long-press excludes an app from the ranking
Long-pressing an app in the ranking SHALL exclude it from the ranking while its time keeps counting in the day total and the hourly distribution, and SHALL offer an immediate undo.
[@test] `app/src/androidTest/java/com/foco/app/ui/ExcludedAppsTest.kt`

#### Scenario: Excluding removes the row, not the time
- **WHEN** an app is long-pressed in the ranking
- **THEN** it no longer appears in the ranking, and the day total stays the same

#### Scenario: Undo restores the app
- **WHEN** undo is used right after excluding
- **THEN** the app appears in the ranking again

### Requirement: The ranking derivation omits excluded packages
The ranking derivation SHALL omit packages the user excluded, while the day total and the hourly distribution SHALL keep including their sessions.
[@test] `app/src/test/java/com/foco/app/ui/metrics/UsageMetricsTest.kt`

#### Scenario: Totals unaffected by an exclusion
- **WHEN** an app with sessions is excluded
- **THEN** the ranking omits it while the day total and the hourly buckets still include its time

### Requirement: Exclusions persist
The excluded list SHALL persist across app restarts.
[@test] `app/src/androidTest/java/com/foco/app/ui/ExcludedAppsTest.kt`

#### Scenario: Persisting across restarts
- **WHEN** an app is excluded and the app is restarted
- **THEN** the app is still out of the ranking

### Requirement: The detail offers a visible way back
The detail screen SHALL show a visible back control at the leading edge of its header (a 4 dp icon-button inset, following the Material header convention), with the app's icon and name centered in the header; tapping it SHALL return to the dashboard, with the same behavior as the system back action.
[@test] `app/src/androidTest/java/com/foco/app/ui/AppDetailNavigationTest.kt`

#### Scenario: Tapping the arrow
- **WHEN** the back control is tapped on the detail screen
- **THEN** the dashboard is shown again

#### Scenario: The arrow sits at the leading edge
- **WHEN** the detail screen is shown
- **THEN** the back control sits at the header's leading edge (4 dp inset) while the app icon and name are centered

### Requirement: The system back gesture returns from the detail
The system back gesture on the detail screen SHALL return to the dashboard, behaving like the back key and the visible back control.
[@test] `app/src/test/java/com/foco/app/BackInvokedCallbackManifestTest.kt`

#### Scenario: Gesture on device
- **WHEN** the user swipes from the screen edge to go back while on a detail screen (Android 16)
- **THEN** the dashboard is shown again

### Requirement: The back control is a standard accessible icon
The detail's back control SHALL be the Material back-arrow icon with an accessibility content description.
[@test] `app/src/androidTest/java/com/foco/app/ui/AppDetailNavigationTest.kt`

#### Scenario: The icon is announced and works
- **WHEN** the back control is found by its content description and tapped
- **THEN** the dashboard is shown again

### Requirement: The dashboard shows the day's unlock count
The dashboard SHALL show how many times the keyguard was dismissed today, derived from the same day's usage events that feed the sessions, displayed next to the screen-time total.
[@test] `app/src/androidTest/java/com/foco/app/ui/UnlockCountTest.kt`

#### Scenario: Unlocks today
- **WHEN** the day totals 12 keyguard dismissals
- **THEN** the dashboard subtitle shows 12 desbloqueos next to the screen-time total

#### Scenario: The count follows the session data source
- **WHEN** the dashboard reloads (first show, returning to the app, or the periodic refresh)
- **THEN** the unlock count is re-derived from the same query and the same day window

### Requirement: A session already open at the day's start counts from the day's start
Usage sessions SHALL include an app that was already in the foreground when the day began: the day's query SHALL look back to the start of the previous day — one day, not further — and the resulting session SHALL be clamped to the day's start so the previous day keeps its own slice.
[@test] `app/src/test/java/com/foco/app/domain/SessionBuilderTest.kt`

#### Scenario: An app open across midnight
- **WHEN** an app resumes at 23:50 and is still in the foreground when the next day is queried at 00:06
- **THEN** the new day yields a session for that app starting at 00:00 and ending at 00:06

#### Scenario: The previous day keeps its own slice
- **WHEN** the same app is queried for the previous day
- **THEN** its session there ends at that day's end, and the minutes are counted once and only once

#### Scenario: An app open for more than a day
- **WHEN** an app has been in the foreground since two days ago and is closed today
- **THEN** its opening event falls outside the one-day lookback, so today yields no session for it — the bound is deliberate, not an oversight

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

### Requirement: Stored totals are read as a seven-day window
The app SHALL read the stored per-app totals of the seven days ending at a given day, oldest first, so the stored history can be shown without re-deriving it from the platform.
[@test] `app/src/test/java/com/foco/app/data/HistoryWeekTest.kt`

#### Scenario: The window is bounded
- **WHEN** totals exist for days inside and outside the seven days ending at the requested day
- **THEN** only the totals inside that window are returned, oldest first

#### Scenario: Nothing is stored
- **WHEN** no day has stored totals
- **THEN** the read yields nothing
