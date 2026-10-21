# app-settings Specification

## ADDED Requirements

### Requirement: Per-app settings screen reaches from the detail
A dedicated settings screen for one package SHALL be reachable from the app detail and SHALL show
only that package's limit picker, window picker and alert toggle, using the same rules and
persisting to the same store as the global settings screen.
[@test] `app/src/androidTest/java/com/foco/app/ui/AppAppSettingsTest.kt`

#### Scenario: The per-app settings shows the package
- **WHEN** the Configure action in the detail of Instagram opens the screen
- **THEN** the screen shows only Instagram with its current limit, window and alert state

#### Scenario: Editing saves to the same store
- **WHEN** the user picks a new limit or window in the per-app screen
- **THEN** the value is persisted per package and the monitor updates accordingly
