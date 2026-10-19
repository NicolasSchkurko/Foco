# Spec Delta

## MODIFIED Requirements

### Requirement: The appearance is selectable and persisted
The settings screen SHALL offer Automático (system), Claro and Oscuro; picking one SHALL apply it immediately across the app — the activity window included — and survive restarts, and Automático SHALL follow the system theme.
[@test] `app/src/androidTest/java/com/foco/app/ui/SettingsScreenTest.kt`
[@test] `app/src/test/java/com/foco/app/data/AppAppearanceTest.kt`

#### Scenario: Switching to dark
- **WHEN** Oscuro is picked
- **THEN** the app renders with the dark scheme, and the activity window behind it follows that scheme

#### Scenario: Persisting the appearance
- **WHEN** the app restarts after picking an appearance
- **THEN** the chosen appearance is still active (scheme, window and resources)

#### Scenario: Back to automático (appearance)
- **WHEN** Automático is picked again
- **THEN** the app follows the system theme

#### Scenario: A language change keeps the themed window
- **WHEN** the language is switched (the activity relaunches)
- **THEN** the background shown during the relaunch is the chosen scheme, never the opposite one
