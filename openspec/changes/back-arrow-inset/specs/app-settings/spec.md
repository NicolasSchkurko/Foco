# Spec Delta

## MODIFIED Requirements

### Requirement: Settings is reachable from the dashboard
The dashboard SHALL offer an entry point to a Settings screen, and the back action SHALL return to the dashboard; the settings header SHALL show its back control at the leading edge (the same 4 dp inset as the detail's), with the title centered.
[@test] `app/src/androidTest/java/com/foco/app/ui/SettingsScreenTest.kt`

#### Scenario: Opening settings
- **WHEN** the settings entry point is tapped
- **THEN** the settings screen opens

#### Scenario: Going back
- **WHEN** the back action is used from settings
- **THEN** the dashboard is shown again

#### Scenario: The arrow sits at the leading edge
- **WHEN** the settings screen is shown
- **THEN** the back control sits at the header's leading edge (4 dp inset) and the title is centered
