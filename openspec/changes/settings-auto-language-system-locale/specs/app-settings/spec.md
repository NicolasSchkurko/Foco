# Spec Delta

## MODIFIED Requirements

### Requirement: The language is selectable, applied and persisted
The settings screen SHALL offer Automático (system), Español and Inglés through a single row that shows the current choice; tapping that row SHALL open a dropdown menu anchored to it, listing the three options with the current one marked; picking an option SHALL close the menu, switch the UI language immediately and survive restarts, and Automático SHALL follow the device language without clearing a per-app language chosen for Foco in the system settings.

[@test] `app/src/androidTest/java/com/foco/app/ui/SettingsScreenTest.kt`
[@test] `app/src/test/java/com/foco/app/MainActivityStartupTest.kt`

#### Scenario: The row states the current choice
- **WHEN** the settings screen is shown
- **THEN** the language row displays the label of the active option (`Automatic`, `Spanish` or `English`, localized) and no other language row is present

#### Scenario: Switching to English
- **WHEN** the dropdown is opened and English is picked
- **THEN** the dropdown closes, the UI renders in English (e.g. the dashboard title), and the row reads `English`

#### Scenario: Persisting the choice
- **WHEN** the app restarts after picking a language
- **THEN** the chosen language is still active and the row still names it

#### Scenario: Back to automático
- **WHEN** Automático is picked
- **THEN** the app follows the device language again and the row reads `Automatic`

#### Scenario: A per-app language set in the system settings
- **WHEN** the system settings name a language for Foco and the app runs on Automático
- **THEN** that language stays in effect across launches, and the row reads `Automatic`
