# Spec Delta

## MODIFIED Requirements

### Requirement: The detail offers a visible way back
The detail screen SHALL show a visible back control at the leading edge of its header (a 4 dp icon-button inset, following the Material header convention), with the app's icon and name centered in the header; tapping it SHALL return to the dashboard, with the same behavior as the system back action.
[@test] `app/src/androidTest/java/com/foco/app/ui/AppDetailNavigationTest.kt`

#### Scenario: Tapping the arrow
- **WHEN** the back control is tapped on the detail screen
- **THEN** the dashboard is shown again

#### Scenario: The arrow sits at the leading edge
- **WHEN** the detail screen is shown
- **THEN** the back control sits at the header's leading edge (4 dp inset) while the app icon and name are centered
