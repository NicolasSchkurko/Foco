# Spec Delta

## ADDED Requirements

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
