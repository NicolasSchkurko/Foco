# Spec Delta

## ADDED Requirements

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
