# Spec Delta

## ADDED Requirements

### Requirement: The limits editor lives in the app's settings tab
The controls that configure one app's daily limit and recurring use window SHALL be shown in that
app's settings tab, and nowhere else; the global settings screen SHALL NOT offer a limits editor.
[@test] `app/src/androidTest/java/com/foco/app/ui/LimitsQuickAdjustTest.kt`

#### Scenario: The app's tab owns the editor
- **WHEN** the user opens the settings of an app
- **THEN** the quick-adjust buttons and the time-range picker are there, editing that app

#### Scenario: The global tab has no limits editor
- **WHEN** the user opens the global settings
- **THEN** no limit or window editor is shown

### Requirement: The app detail's limits preview opens that app's settings
The app detail's limits preview block SHALL be interactive: tapping anywhere on it SHALL open the
settings of the app being shown, and the header action of that screen SHALL be the limits control.
[@test] `app/src/androidTest/java/com/foco/app/ui/AppDetailLimitsTest.kt`

#### Scenario: The preview is a shortcut
- **WHEN** the user taps the limits preview of the app being shown
- **THEN** the settings of that app open

#### Scenario: The header action is the limits control
- **WHEN** the user taps the header's action on the app detail
- **THEN** the settings of that app open
