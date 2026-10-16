# Spec Delta

## ADDED Requirements

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
