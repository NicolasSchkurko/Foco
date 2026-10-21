# Spec Delta

## ADDED Requirements

### Requirement: The detail refreshes on return to the foreground
The detail SHALL reload the day it is showing every time the app returns to the foreground, and on first show, without any user action and without changing the selected day.
[@test] `app/src/androidTest/java/com/foco/app/ui/detail/AppDetailRefreshTest.kt`

#### Scenario: Coming back after using another app
- **WHEN** the detail is open, the user spends time in another app and returns to Foco
- **THEN** the total, chart and session list reflect the time accumulated meanwhile, on the same day

#### Scenario: The selected day is kept
- **WHEN** the detail refreshes on return while a past day is selected
- **THEN** that same day stays selected and is the one reloaded

#### Scenario: First show
- **WHEN** the detail is opened for the first time
- **THEN** it loads its data without user action, as it does today
