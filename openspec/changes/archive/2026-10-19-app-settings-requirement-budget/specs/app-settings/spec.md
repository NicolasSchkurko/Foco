# Spec Delta

## MODIFIED Requirements

### Requirement: The appearance is selectable and persisted
The settings screen SHALL offer Automático, Claro and Oscuro in one row that shows the current choice; tapping it SHALL open an anchored dropdown, and picking an option SHALL close it, apply the choice at once and keep it across restarts, with Automático following the system theme.
[@test] `app/src/androidTest/java/com/foco/app/ui/SettingsScreenTest.kt`
[@test] `app/src/test/java/com/foco/app/data/AppAppearanceTest.kt`

#### Scenario: The row states the current choice
- **WHEN** the settings screen is shown
- **THEN** the appearance row displays the label of the active option (`Automatic`, `Light` or `Dark`, localized) and no other appearance row is present

#### Scenario: Switching to dark
- **WHEN** the dropdown is opened and Oscuro is picked
- **THEN** the dropdown closes, the app renders with the dark scheme, the activity window behind it follows that scheme, and the row reads `Dark`

#### Scenario: Persisting the appearance
- **WHEN** the app restarts after picking an appearance
- **THEN** the chosen appearance is still active (scheme, window and resources) and the row still names it

#### Scenario: Back to automático (appearance)
- **WHEN** Automático is picked again
- **THEN** the app follows the system theme and the row reads `Automatic`

#### Scenario: A language change keeps the themed window
- **WHEN** the language is switched (the activity relaunches)
- **THEN** the background shown during the relaunch is the chosen scheme, never the opposite one
