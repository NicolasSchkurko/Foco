# usage-tracking Specification

## Purpose
Track device usage per app as time sessions for the current day, and derive the dashboard's numbers (day total, hourly activity, per-app ranking) from those sessions.

## Requirements

### Requirement: Usage sessions are built from system events
The system SHALL build per-app usage sessions (package, start, end) for the current day from activity resume/pause/stop events, as a computation over plain event data: switching directly between apps closes the previous session at the new start; a session open at query time closes at "now"; transitions inside the same app do not split the session.
[@test] `app/src/test/java/com/foco/app/data/SessionBuilderTest.kt`

#### Scenario: Direct switch between apps
- **WHEN** app A resumes at 10:00 and app B resumes at 10:05
- **THEN** A yields the session 10:00–10:05 and B's session starts at 10:05

#### Scenario: Session open at query time
- **WHEN** an app is in the foreground when the day is queried
- **THEN** its session ends at the query time

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
