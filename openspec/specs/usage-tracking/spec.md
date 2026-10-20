# usage-tracking Specification

## Purpose
Track device usage per app as time sessions for the current day, and derive the dashboard's numbers (day total, hourly activity, per-app ranking) from those sessions.

## Requirements

### Requirement: Usage sessions are built from system events
The system SHALL build per-app usage sessions (package, start, end) for the day from resume/pause/stop events, over plain event data: the direct switch closes the previous session at the new start and a pause or stop closes the open session of its own package (both ties-inclusive); a session open at query time closes at "now"; same-app transitions do not split it; a zero-length session is not emitted.
[@test] `app/src/test/java/com/foco/app/domain/SessionBuilderTest.kt`

#### Scenario: Direct switch between apps
- **WHEN** app A resumes at 10:00 and app B resumes at 10:05
- **THEN** A yields the session 10:00â€“10:05 and B's session starts at 10:05

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
The dashboard SHALL derive its numbers â€” day total, hourly distribution (splitting sessions that cross an hour boundary) and per-app ranking (sorted by total duration, descending) â€” in the UI layer, from the session list, using pure computations verifiable by unit tests. Presentation formats SHALL remain those of `Formatting.kt`.
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
The detail SHALL show the selected app's usage distributed by hour (00â€“23) for the chosen day, splitting sessions that cross an hour boundary, using the same representation as the dashboard's day chart.
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
- **WHEN** the app has sessions 10:00â€“10:30 and 11:00â€“11:50
- **THEN** total is 1 h 20 min, count is 2, first is 10:00, last is 11:50, average is 40 min, longest is 50 min, and the list shows both sessions in order

#### Scenario: Day without data
- **WHEN** the app has no sessions that day
- **THEN** the detail shows an explicit empty state instead of numbers

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
Usage sessions SHALL include an app that was already in the foreground when the day began: the day's query SHALL look back to the start of the previous day â€” one day, not further â€” and the resulting session SHALL be clamped to the day's start so the previous day keeps its own slice.
[@test] `app/src/test/java/com/foco/app/domain/SessionBuilderTest.kt`

#### Scenario: An app open across midnight
- **WHEN** an app resumes at 23:50 and is still in the foreground when the next day is queried at 00:06
- **THEN** the new day yields a session for that app starting at 00:00 and ending at 00:06

#### Scenario: The previous day keeps its own slice
- **WHEN** the same app is queried for the previous day
- **THEN** its session there ends at that day's end, and the minutes are counted once and only once

#### Scenario: An app open for more than a day
- **WHEN** an app has been in the foreground since two days ago and is closed today
- **THEN** its opening event falls outside the one-day lookback, so today yields no session for it â€” the bound is deliberate, not an oversight

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

### Requirement: A failed stored read degrades instead of closing the app
The detail SHALL keep working when a stored read fails: it shows the empty state for that day, or today alone when the stored days could not be read, and the app never closes.
[@test] `app/src/androidTest/java/com/foco/app/ui/detail/AppDetailReadTest.kt`

#### Scenario: A stored read fails
- **WHEN** reading a day's sessions or the list of stored days throws
- **THEN** the screen answers its empty state (or today alone) and stays open

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

### Requirement: The week view covers the last seven days
The week view SHALL show the last seven days as one bar per day (the day's total screen time) plus the ranked per-app totals of those days, derived in the UI by pure functions; today SHALL come live from the platform, and a day without stored data SHALL read as absent, not as zero.
[@test] `app/src/test/java/com/foco/app/ui/metrics/WeekMetricsTest.kt`
[@test] `app/src/androidTest/java/com/foco/app/ui/WeekScreenTest.kt`

#### Scenario: The seven bars
- **WHEN** the week view opens
- **THEN** it shows seven bars, today live and the previous six from the stored totals

#### Scenario: A day with no data
- **WHEN** a day in the window has no stored total
- **THEN** its bar reads as absent, not as zero screen time

#### Scenario: The ranking
- **WHEN** the week has usage
- **THEN** the apps are listed by their total over the seven days, descending

### Requirement: A day bar opens that day's view
The week view SHALL be reachable from the dashboard and SHALL be one position of the home's ring; tapping a day's bar SHALL move the home to that day's position â€” the dashboard composition for that date, read from stored sessions â€” where an app row opens that app's detail for the day.
[@test] `app/src/androidTest/java/com/foco/app/ui/WeekScreenTest.kt`

#### Scenario: Entry and return
- **WHEN** the week control is used in the dashboard
- **THEN** the home shows the week position, with no back control to leave it and no screen stacked

#### Scenario: Opening a day
- **WHEN** a bar is tapped
- **THEN** the home shows that day with its total, hourly chart and ranking

### Requirement: One header for every screen
The home, the week and a dated day SHALL share one header composable with the title and its actions and no back control; settings and an app's detail SHALL render through it with a back control at the leading edge, 4 dp inset, title centred.
[@test] `app/src/androidTest/java/com/foco/app/ui/WeekScreenTest.kt`
[@test] `app/src/androidTest/java/com/foco/app/ui/SettingsScreenTest.kt`

#### Scenario: A peer screen offers no back control
- **WHEN** the week or a dated day is shown
- **THEN** its header carries the title and its actions and no back control

#### Scenario: A submenu keeps its back control
- **WHEN** settings or an app's detail is shown
- **THEN** the back control sits at the leading edge with a 4 dp inset and the title is centred

#### Scenario: One geometry everywhere
- **WHEN** any screen header is measured
- **THEN** the arrow inset, the title style and the row height are the same values on every screen

### Requirement: The home changes the day in place
The home SHALL read one day at a time and change it in place, without stacking a screen: a horizontal swipe anywhere on the content and the visible â€¹ â€º controls SHALL each move one position of the day ring, and the back action SHALL retrace the positions visited.
[@test] `app/src/androidTest/java/com/foco/app/ui/DashboardRingTest.kt`

#### Scenario: A swipe moves one day
- **WHEN** the user swipes horizontally over the home's content
- **THEN** the home shows the neighbouring day of the ring, with its own total, chart and ranking

#### Scenario: The arrows move the same way
- **WHEN** the user taps â€¹ or â€º
- **THEN** the home moves exactly one position in the same direction as the matching swipe

#### Scenario: Back walks the positions visited
- **WHEN** the home has moved through two positions and the system back action is used
- **THEN** the home returns to the position visited before, one step per back, and from today with nothing behind it, back leaves the app

#### Scenario: Settings returns to what was being viewed
- **WHEN** settings is opened from a position and closed with its back control
- **THEN** the home shows the position it was showing before settings was opened

### Requirement: The day ring wraps through the week
The ring SHALL hold today, the week, the oldest stored day inside the seven-day window and every finished day between them; the step forward from today SHALL land on the week, the step forward from the week on the oldest stored day, and the step back from the oldest stored day on the week.
[@test] `app/src/test/java/com/foco/app/domain/DayRingTest.kt`

#### Scenario: Forward from today
- **WHEN** the home is on today and the user steps forward
- **THEN** the week is shown

#### Scenario: Forward from the week
- **WHEN** the home is on the week and the user steps forward
- **THEN** the oldest stored day inside the window is shown

#### Scenario: Back from the oldest day
- **WHEN** the home is on the oldest stored day and the user steps back
- **THEN** the week is shown

#### Scenario: A store with no finished days
- **WHEN** the store holds no finished day
- **THEN** the ring is today and the week, and forward from the week returns to today

#### Scenario: The guard day stays out of the ring
- **WHEN** the store holds a day older than today minus six (the guard day kept for a lagging run)
- **THEN** the ring offers today and the six days before it, and the guard day is not reachable

### Requirement: Moving a ring position is animated

The home SHALL animate a ring-position change with a short horizontal slide and fade in the travel
direction, and SHALL change what it shows only once the requested position's content is ready: the
position on screen SHALL stay — without a spinner or loading text — until then, so no loading
placeholder is composed during a move. Swipe and arrow moves SHALL use a 320 ms tween (scaled by the
system animator duration), an eighth-width slide and ease-out; bar, calendar and back keep the
existing slide/fade. At scale 0 no transition SHALL run. A loading placeholder SHALL appear only
while the home has no position to show yet.

[@test] `app/src/androidTest/java/com/foco/app/ui/DashboardRingTest.kt`
[@test] `app/src/test/java/com/foco/app/ui/dashboard/RingMotionTest.kt`

#### Scenario: Forward and back

- **WHEN** the user moves one position forward and then back
- **THEN** the content slides in from the forward side and then from the back side, while the header
  and the arrows stay in place

#### Scenario: A bar shortcut

- **WHEN** a week bar moves the home to that day
- **THEN** the content animates in the bar's ring direction instead of hard-cutting

#### Scenario: Swipe and arrows are soft

- **WHEN** the user moves one position with a swipe or an arrow at scale 1
- **THEN** the content slides in by an eighth of the width over 320 ms with ease-out and fade, while
  the header and the arrows stay in place

#### Scenario: Reduced motion

- **WHEN** the system animator duration scale is 0
- **THEN** the position changes without a slide or fade

#### Scenario: Other entry paths

- **WHEN** a bar, the calendar or the back action changes the position
- **THEN** the existing directional slide/fade runs instead of the soft swipe spec

#### Scenario: A move waits for its content

- **WHEN** the requested position's content is not in memory yet
- **THEN** the position already on screen stays visible with no spinner and no loading text, and the
  new content enters with the move as soon as its data is ready

#### Scenario: The week is a position too

- **WHEN** the user moves to or from the week
- **THEN** the week is composed only with its stored totals and today's live day already read, so no
  loading placeholder appears during the move

#### Scenario: The first load

- **WHEN** the home has not shown any position yet
- **THEN** the loading placeholder is shown until that position's content is ready

### Requirement: The usage legend sits between the arrows
The screen-time and unlock line SHALL render between the `â€¹` and `â€º` controls, centered; the title
SHALL sit in the header without that subtitle, vertically centered with the calendar and settings
actions.
[@test] `app/src/androidTest/java/com/foco/app/ui/DashboardRingTest.kt`

#### Scenario: Legend geometry
- **WHEN** the home shows a day with a usage line
- **THEN** the line's bounds sit between the two arrow bounds and the title's vertical center matches
  the trailing actions

#### Scenario: The week has no usage line
- **WHEN** the home is on the week stop
- **THEN** the arrows remain visible with no day usage line and the week title is centered

### Requirement: Sub-second sessions are not visible in app views
The app SHALL treat a session shorter than 1000 ms as not visible: it SHALL NOT appear in the dashboard ranking or the app detail session list, and the detail summary SHALL be derived only from visible sessions.
[@test] `app/src/test/java/com/foco/app/ui/metrics/UsageMetricsTest.kt`
[@test] `app/src/test/java/com/foco/app/ui/metrics/AppDetailMetricsTest.kt`

#### Scenario: A sub-second session beside a visible one
- **WHEN** an app has a 2000 ms session and a 500 ms session
- **THEN** the dashboard lists the app with 2000 ms, and the detail lists only the 2000 ms session with a count of one

#### Scenario: An app with only sub-second time
- **WHEN** an app's only session lasts 500 ms
- **THEN** it does not appear in the dashboard ranking, and its detail shows the empty state

#### Scenario: The visibility boundary
- **WHEN** a session lasts exactly 1000 ms
- **THEN** it is visible and the app appears with 1 s

### Requirement: Apps whose total rounds to zero are absent from app rankings
The dashboard and week rankings SHALL omit an app whose visible total is below 1000 ms; for stored week data, an app whose stored total across the window is below 1000 ms SHALL be omitted.
[@test] `app/src/test/java/com/foco/app/ui/metrics/UsageMetricsTest.kt`
[@test] `app/src/test/java/com/foco/app/ui/metrics/WeekMetricsTest.kt`

#### Scenario: Only sub-second live time
- **WHEN** an app has only a 500 ms live session in the week
- **THEN** the week ranking omits it

#### Scenario: Only sub-second stored time
- **WHEN** an app's stored week total is 500 ms
- **THEN** the week ranking omits it

#### Scenario: Visible total across the window
- **WHEN** an app's visible total is at least 1000 ms
- **THEN** the week ranking includes it

### Requirement: Non-app aggregates keep the full time
The day total, hourly distribution and week day bars SHALL continue to include sub-second sessions; only app rankings and the app detail session list are filtered.
[@test] `app/src/test/java/com/foco/app/ui/metrics/UsageMetricsTest.kt`
[@test] `app/src/test/java/com/foco/app/ui/metrics/WeekMetricsTest.kt`

#### Scenario: A sub-second session still counts in the day
- **WHEN** the day contains a 500 ms session
- **THEN** the day total and hourly distribution still include its 500 ms, while no app row is produced for it

#### Scenario: A sub-second session still counts in the week bar
- **WHEN** a stored or live day contains a 500 ms session
- **THEN** that day's bar still includes the 500 ms, while the app ranking hides the app

### Requirement: The committed schema cannot drift silently
The app SHALL verify that the database's declared version has a committed exported schema and that its committed identity hash matches the one the open database reports, so an entity edit without a version bump, or a bump without an export, fails the build.
[@test] `app/src/test/java/com/foco/app/data/db/SchemaDriftTest.kt`
[@test] `app/src/androidTest/java/com/foco/app/data/db/SchemaBaselineTest.kt`

#### Scenario: An entity edited without a version bump
- **WHEN** the committed schema of the declared version carries a different identity hash than the open database reports
- **THEN** the verification reports drift and names the version

#### Scenario: A version bumped without an exported schema
- **WHEN** the declared version has no committed schema file
- **THEN** the verification reports the missing export

#### Scenario: The baseline is consistent
- **WHEN** the declared version has its committed schema and both identity hashes agree
- **THEN** the verification reports no drift

### Requirement: Schema changes never wipe the stored history
The app SHALL NOT use destructive migrations â€” no `fallbackToDestructiveMigration` call in the production sources â€” and every version bump SHALL ship a migration while the previous schema stays committed.
[@test] `app/src/test/java/com/foco/app/data/db/NoDestructiveMigrationTest.kt`

#### Scenario: No destructive fallback
- **WHEN** the production sources are scanned
- **THEN** no call to `fallbackToDestructiveMigration` exists

#### Scenario: The check bites
- **WHEN** the scan runs against a source tree that contains the call
- **THEN** it fails and names the file

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

### Requirement: Contiguous same-app sessions are normalized
After the fold, sessions of the same app that meet at the same local `HH:mm` on the same day SHALL be merged into one from the earliest start to the latest end, transitively; different apps or different local days SHALL NOT be merged. The normalization SHALL apply to the live day build and to the app detail read; stored rows may remain raw.
[@test] `app/src/test/java/com/foco/app/domain/SessionMergeTest.kt`
[@test] `app/src/test/java/com/foco/app/ui/metrics/AppDetailMetricsTest.kt`

#### Scenario: The same displayed minute
- **WHEN** app A ends at 03:44:10 and app A starts again at 03:44:55
- **THEN** the detail shows one A session from 03:44:10, not two rows

#### Scenario: A chain
- **WHEN** A runs 04:00â€“04:30 and then 04:30â€“04:45
- **THEN** A has one 04:00â€“04:45 session and a session count of one

#### Scenario: Different app or day
- **WHEN** A ends at 03:44 and B starts at 03:44, or A ends on one day and starts at 03:44 the next
- **THEN** the sessions are not merged

#### Scenario: Stored rows without a migration
- **WHEN** an already-stored day contains two contiguous A rows
- **THEN** the detail read merges them, while the raw rows and daily totals stay unchanged

#### Scenario: Visibility after the merge
- **WHEN** two contiguous A pieces are 500 ms each
- **THEN** the merged 1 s session is visible under the existing sub-second rule

### Requirement: The week can be exported as formatted JSON
When the home is on the week position, it SHALL offer an export action for a UTF-8, pretty-printed JSON document covering the same rolling seven-day window: today live and the previous six stored days. The document SHALL contain `weekStart`, `weekEnd`, and one `days` entry per day, oldest first.
[@test] `app/src/test/java/com/foco/app/domain/WeekExportTest.kt`
[@test] `app/src/androidTest/java/com/foco/app/ui/WeekScreenTest.kt`

#### Scenario: Seven days, including empty ones
- **WHEN** the user exports the week and one day has no activity
- **THEN** the JSON has seven `days` entries, oldest first, and that day has `totalMillis` 0 and `apps: []`

#### Scenario: Today comes live
- **WHEN** today has live sessions
- **THEN** the today entry contains them, while the previous six entries come from stored history

### Requirement: Each app and session is broken down
Each day entry SHALL contain `totalMillis` and `apps`; each app SHALL contain `packageName`, the resolved `appName`, its day `totalMillis`, `sessionsAvailable`, and its `sessions`; each session SHALL contain `start` and `end` as ISO-8601 strings with the local offset and `durationMillis`. An app's total SHALL be the sum of its sessions; a day's total SHALL be the sum of its app totals. Sessions SHALL be ordered oldest first.
[@test] `app/src/test/java/com/foco/app/domain/WeekExportTest.kt`

#### Scenario: App total and sessions
- **WHEN** an app has two sessions in a day
- **THEN** its `totalMillis` is their sum and `sessions` has both, oldest first, each with offset timestamps and its millisecond duration

#### Scenario: Stored total without raw rows
- **WHEN** a day has a stored total for an app but no raw session rows
- **THEN** the app is present with the stored total, `sessionsAvailable: false` and `sessions: []`

#### Scenario: Normalized records, not the ranking
- **WHEN** two same-app sessions meet at the same `HH:mm`, a session lasts less than 1 s, or an app is excluded from the ranking
- **THEN** contiguous ones are one, a standalone sub-second session is still present, excluded apps stay, and the export applies no visibility or exclusion filters

#### Scenario: A name that needs escaping
- **WHEN** an app label contains a quote, a backslash or a control character
- **THEN** the emitted JSON escapes it and remains valid JSON

### Requirement: The export is shared, not uploaded
The export SHALL be written as a transient file in the app's cache and offered through the Android share sheet as `application/json` using a read-only `FileProvider` URI. It SHALL NOT request storage permissions, use the network, or change `allowBackup`; a failed read SHALL share no partial file and SHALL show an export error.
[@test] `app/src/androidTest/java/com/foco/app/ui/dashboard/WeekExportShareTest.kt`
[@test] `app/src/androidTest/java/com/foco/app/ui/WeekScreenTest.kt`

#### Scenario: Share intent
- **WHEN** the export is ready
- **THEN** the share intent has `ACTION_SEND`, type `application/json`, a `content://` stream and the read-grant flag

#### Scenario: A read fails
- **WHEN** a history or live read fails during export
- **THEN** no file is shared and the week position shows the export error

### Requirement: Consolidating a day ends with its marker
The consolidation SHALL write a day's sessions, then its per-app totals, and finally one row marking the day consolidated with its unlock count; the pending set SHALL be read from those rows, so an interrupted day is retried on the next run and a day consolidated by this version always carries its count.
[@test] `app/src/test/java/com/foco/app/data/HistoryConsolidationTest.kt`

#### Scenario: Interrupted before the marker
- **WHEN** the process dies after a day's totals are stored and before its marker is written
- **THEN** the next run still sees that day as pending, consolidates it again and stores its count

#### Scenario: One marker per day
- **WHEN** the same day is consolidated twice
- **THEN** it keeps a single marker row holding the count of the latest run

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

### Requirement: Stored days are normalized in the export
The week export SHALL apply the same contiguous same-app `HH:mm` normalization to stored days as the app views. A stored pair that meets at the same `HH:mm` SHALL appear as one session with the earliest start and latest end; different apps or days SHALL NOT be merged.
[@test] `app/src/test/java/com/foco/app/domain/WeekExportTest.kt`

#### Scenario: A stored same-minute pair
- **WHEN** a stored day has app A from 03:44:10 to 03:44:50 and again from 03:44:55 to 03:46:00
- **THEN** the exported day has one A session from 03:44:10 to 03:46:00

#### Scenario: Today is unchanged
- **WHEN** today's live sessions are already normalized
- **THEN** the export applies the same rule idempotently and still shows one session

#### Scenario: Different apps or days
- **WHEN** A ends at 03:44 and B starts at 03:44, or A ends on one day and starts at 03:44 the next
- **THEN** the export does not merge them

### Requirement: Week metrics are derived in one grouped pass
The week metrics SHALL index the stored totals by day and package once and walk the seven-day window once, instead of re-filtering the stored rows for each day or allocating a list only to count distinct days. The ranking, totals, bars, absent states and `daysUsed` SHALL remain identical.
[@test] `app/src/test/java/com/foco/app/ui/metrics/WeekMetricsTest.kt`

#### Scenario: The same seven days
- **WHEN** stored totals and a live day are supplied
- **THEN** the bars, ranking totals and `daysUsed` are the same as before the refactor

#### Scenario: An empty store
- **WHEN** no stored totals exist
- **THEN** the seven bars are absent (except today live) and the ranking is unchanged

#### Scenario: The live day
- **WHEN** today has sessions
- **THEN** its total uses the shared `dayTotal` and the ranking includes it once

### Requirement: The current day follows the clock
The home, detail, week and export SHALL use the device's current date and SHALL roll it over when the date changes while the app is open, including on resume, so today's live data is never labeled with the previous date.
[@test] `app/src/androidTest/java/com/foco/app/ui/TodayRolloverTest.kt`
[@test] `app/src/androidTest/java/com/foco/app/ui/WeekScreenTest.kt`

#### Scenario: The app is resumed after midnight
- **WHEN** the app was showing today before midnight and is resumed after it
- **THEN** the home title/ring, detail selector, week window and export `weekEnd` use the new date

#### Scenario: The live read and the label agree
- **WHEN** the home shows today after the rollover
- **THEN** the live day read, the ring's today stop and the header date are the same day

#### Scenario: The export stays on one day
- **WHEN** the export is prepared after the rollover
- **THEN** its live sessions come from the same `today` used for `weekEnd`, not from a second clock read

### Requirement: A no-data week bar opens an empty day
A week bar whose day has no stored data SHALL remain tappable and SHALL open that day as a shortcut outside the ring's stored positions, with its day title and empty state. The shortcut SHALL NOT be added to the ring; back SHALL return to the previous position, and a store update SHALL not eject it while the date stays inside the seven-day window.
[@test] `app/src/androidTest/java/com/foco/app/ui/WeekScreenTest.kt`
[@test] `app/src/androidTest/java/com/foco/app/ui/DashboardRingTest.kt`

#### Scenario: A bar with data
- **WHEN** the user taps a bar for a stored or live day
- **THEN** the home opens that day's ring position

#### Scenario: A no-data bar
- **WHEN** the user taps a bar marked absent
- **THEN** the home shows that date with the empty state, without adding it to the ring

#### Scenario: Back from an empty day
- **WHEN** the user presses back while an empty shortcut day is showing
- **THEN** the home returns to the previous position, not to a synthetic ring step

#### Scenario: A store update while the empty day is open
- **WHEN** a stored day arrives or is pruned and the open date is still inside the seven-day window
- **THEN** the empty shortcut day stays open

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

### Requirement: Cancelling one trigger does not cancel the shared run
A cancellation of one caller SHALL stop only that caller's wait; it SHALL NOT cancel an in-flight consolidation started for another caller. A waiting nightly worker SHALL still receive `Done` or `Failed` and SHALL retry on failure.
[@test] `app/src/test/java/com/foco/app/data/ConsolidationConcurrencyTest.kt`
[@test] `app/src/androidTest/java/com/foco/app/data/ConsolidationWorkerTest.kt`

#### Scenario: The startup wait is cancelled
- **WHEN** the app startup effect is cancelled while the nightly worker waits for the same run
- **THEN** the run continues and the worker receives its outcome

#### Scenario: Two overlapping triggers
- **WHEN** two trigger callers overlap
- **THEN** the store is still executed once and both receive the same outcome

#### Scenario: The worker retries a failure
- **WHEN** the shared run ends in failure
- **THEN** the worker returns a retry result, not a cancellation
