# Spec Delta

## ADDED Requirements

### Requirement: The dashboard shows the day's unlock count
The dashboard SHALL show how many times the keyguard was dismissed today, derived from the same day's usage events that feed the sessions, displayed next to the screen-time total.
[@test] `app/src/androidTest/java/com/foco/app/ui/UnlockCountTest.kt`

#### Scenario: Unlocks today
- **WHEN** the day totals 12 keyguard dismissals
- **THEN** the dashboard subtitle shows 12 desbloqueos next to the screen-time total

#### Scenario: The count follows the session data source
- **WHEN** the dashboard reloads (first show, returning to the app, or the periodic refresh)
- **THEN** the unlock count is re-derived from the same query and the same day window
