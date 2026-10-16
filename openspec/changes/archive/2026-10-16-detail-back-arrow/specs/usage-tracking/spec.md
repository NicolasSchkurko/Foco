# Spec Delta

## ADDED Requirements

### Requirement: The detail offers a visible way back
The detail screen SHALL show a visible back control; tapping it SHALL return to the dashboard, with the same behavior as the system back action.
[@test] `app/src/androidTest/java/com/foco/app/ui/AppDetailNavigationTest.kt`

#### Scenario: Tapping the arrow
- **WHEN** the back control is tapped on the detail screen
- **THEN** the dashboard is shown again
