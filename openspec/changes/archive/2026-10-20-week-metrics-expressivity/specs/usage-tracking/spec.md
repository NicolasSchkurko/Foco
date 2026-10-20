# Spec Delta

## ADDED Requirements

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
