# Spec Delta

## Purpose

Give Foco a configuration surface: choose the app language and manage the excluded-apps list in one place.

## ADDED Requirements

### Requirement: Settings is reachable from the dashboard
The dashboard SHALL offer an entry point to a Settings screen, and the back action SHALL return to the dashboard.
[@test] `app/src/androidTest/java/com/foco/app/ui/SettingsScreenTest.kt`

#### Scenario: Opening settings
- **WHEN** the settings entry point is tapped
- **THEN** the settings screen opens

#### Scenario: Going back
- **WHEN** the back action is used from settings
- **THEN** the dashboard is shown again

### Requirement: The language is selectable, applied and persisted
The settings screen SHALL offer Automático (system), Español and Inglés; picking an option SHALL switch the UI language immediately and survive restarts, and Automático SHALL follow the device language.
[@test] `app/src/androidTest/java/com/foco/app/ui/SettingsScreenTest.kt`

#### Scenario: Switching to English
- **WHEN** English is picked
- **THEN** the UI renders in English (e.g. the dashboard title)

#### Scenario: Persisting the choice
- **WHEN** the app restarts after picking a language
- **THEN** the chosen language is still active

#### Scenario: Back to automático
- **WHEN** Automático is picked
- **THEN** the app follows the device language again

### Requirement: The appearance is selectable and persisted
The settings screen SHALL offer Automático (system), Claro and Oscuro; picking one SHALL apply it immediately across the app and survive restarts, and Automático SHALL follow the system theme.
[@test] `app/src/androidTest/java/com/foco/app/ui/SettingsScreenTest.kt`

#### Scenario: Switching to dark
- **WHEN** Oscuro is picked
- **THEN** the app renders with the dark scheme

#### Scenario: Persisting the appearance
- **WHEN** the app restarts after picking an appearance
- **THEN** the chosen appearance is still active

#### Scenario: Back to automático (appearance)
- **WHEN** Automático is picked again
- **THEN** the app follows the system theme

### Requirement: Excluded apps are managed in settings
The settings screen SHALL list the excluded packages and allow restoring any of them.
[@test] `app/src/androidTest/java/com/foco/app/ui/SettingsScreenTest.kt`

#### Scenario: The list shows exclusions
- **WHEN** an app was excluded from the ranking
- **THEN** it appears in the settings list

#### Scenario: Restoring an excluded app
- **WHEN** an excluded app is restored from settings
- **THEN** it no longer appears in the excluded list
