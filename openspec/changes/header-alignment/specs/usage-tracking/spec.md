# Spec Delta

## ADDED Requirements

### Requirement: One header for every screen
The home, the week and a dated day SHALL share one header composable with the title and its actions and no back control; settings and an app's detail SHALL render through it with a back control at the leading edge, 4 dp inset, title centred.
[@test] `app/src/androidTest/java/com/foco/app/ui/WeekScreenTest.kt`
[@test] `app/src/androidTest/java/com/foco/app/ui/SettingsScreenTest.kt`

#### Scenario: A peer screen offers no back control
- **WHEN** the week or a dated day is shown
- **THEN** its header carries the title and its actions and no back control

#### Scenario: A submenu keeps its back control
- **WHEN** settings or an app's detail is shown
- **THEN** the back control sits at the leading edge with a 4 dp inset and the title is centred

#### Scenario: One geometry everywhere
- **WHEN** any screen header is measured
- **THEN** the arrow inset, the title style and the row height are the same values on every screen
