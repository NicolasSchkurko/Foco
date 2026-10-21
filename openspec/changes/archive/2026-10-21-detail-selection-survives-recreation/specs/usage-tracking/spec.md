# Spec Delta

## ADDED Requirements

### Requirement: The detail's selected day survives recreation
The detail SHALL restore the day it was showing when the screen is recreated — rotation, a trip to Settings or a process recreation — and SHALL resolve the restored day against the stored window that exists at that moment, landing on the nearest valid day when it is gone, never on a fixed default.
[@test] `app/src/androidTest/java/com/foco/app/ui/AppDetailNavigationTest.kt`

#### Scenario: Rotation keeps the day
- **WHEN** the detail is showing a past day and the device rotates
- **THEN** the detail comes back on that same day, not on today

#### Scenario: A trip to settings
- **WHEN** the detail is showing a past day and settings is opened and closed
- **THEN** the detail shows the same day it was showing before

#### Scenario: The window shrank meanwhile
- **WHEN** the restored day is no longer inside the stored window
- **THEN** the detail lands on the nearest day that still exists, inside the window
