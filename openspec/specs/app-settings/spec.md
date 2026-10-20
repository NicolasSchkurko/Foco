# app-settings Specification

## Purpose
Give Foco a configuration surface: choose the app language and manage the excluded-apps list in one place.

## Requirements

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

### Requirement: The language is selectable, applied and persisted

The settings screen SHALL offer Automático (system), Español and Inglés through a single row that
shows the current choice; tapping that row SHALL open a dropdown menu anchored to it, listing the
three options with the current one marked; picking an option SHALL close the menu, switch the UI
language immediately and survive restarts, and Automático SHALL follow the device language.

[@test] `app/src/androidTest/java/com/foco/app/ui/SettingsScreenTest.kt`

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

### Requirement: The appearance is selectable and persisted

The settings screen SHALL offer Automático (system), Claro and Oscuro through a single row that
shows the current choice; tapping that row SHALL open a dropdown menu anchored to it, listing the
three options with the current one marked; picking one SHALL close the menu, apply it immediately
across the app — the activity window included — and survive restarts, and Automático SHALL follow
the system theme.

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

### Requirement: Excluded apps are managed in settings
The settings screen SHALL list the excluded packages and allow restoring any of them.
[@test] `app/src/androidTest/java/com/foco/app/ui/SettingsScreenTest.kt`

#### Scenario: The list shows exclusions
- **WHEN** an app was excluded from the ranking
- **THEN** it appears in the settings list

#### Scenario: Restoring an excluded app
- **WHEN** an excluded app is restored from settings
- **THEN** it no longer appears in the excluded list

### Requirement: The app's default (base) resources are English
The app's base resources SHALL be English, every supported language SHALL live in its own locale resources, and a device whose language has no dedicated resources SHALL see the English text — never Spanish.
[@test] `app/src/test/java/com/foco/app/structure/DefaultLocaleResourcesTest.kt`
[@test] `app/src/androidTest/java/com/foco/app/ui/SettingsScreenTest.kt`

#### Scenario: A language without its own resources falls back to English
- **WHEN** the device language has no dedicated resources (e.g. French)
- **THEN** the app shows its English text

#### Scenario: Spanish is a real locale, not the fallback
- **WHEN** the app language is Español
- **THEN** the app shows its Spanish text, resolved from the `es` resources

#### Scenario: Base and Spanish resources carry the same keys
- **WHEN** a string is added, renamed or removed in the base resources
- **THEN** the `es` resources declare the same key set, and the suite fails naming the key that is missing on either side

#### Scenario: The base does not regress to Spanish
- **WHEN** the base resources are edited again
- **THEN** the suite fails if a base string holds the Spanish value of a key that has a Spanish counterpart

### Requirement: Settings shows a centered app identity block

The settings screen SHALL show, above the Language section and horizontally centered, the app name
with the installed version and the credit `Open source project by Nicolas Schkurko`. The version
SHALL come from the installed package, never from a literal in the screen or in a string resource.

[@test] `app/src/androidTest/java/com/foco/app/ui/SettingsScreenTest.kt`
[@test] `app/src/test/java/com/foco/app/structure/DefaultLocaleResourcesTest.kt`

#### Scenario: The block sits above Language
- **WHEN** the settings screen opens
- **THEN** the identity block is the first content, above the `Language` section title, and its lines are horizontally centered

#### Scenario: The line shows the installed version
- **WHEN** the installed package reports `versionName` `0.1.0`
- **THEN** the first line reads `Foco 0.1.0`

#### Scenario: The version is unavailable
- **WHEN** the platform reports no `versionName`
- **THEN** the first line shows the app name alone, with no placeholder or stray separator

#### Scenario: The block follows the UI language
- **WHEN** the UI is in Spanish
- **THEN** the credit line resolves from `values-es` and the first line keeps the same app name and version number

### Requirement: The repository link opens in the system browser

The repository line SHALL be a link that hands `https://github.com/NicolasSchkurko/Foco` to an
`ACTION_VIEW` intent served by an external browser. It SHALL NOT open an in-app browser, download
anything, request a permission, or make a network request from Foco.

[@test] `app/src/androidTest/java/com/foco/app/ui/SettingsScreenTest.kt`

#### Scenario: The link leaves the app
- **WHEN** the `Git repository` link is tapped
- **THEN** an `ACTION_VIEW` intent for the repository URL is started and Foco does not navigate in-app

#### Scenario: The URL is the repository, not a copy
- **WHEN** the link intent is built
- **THEN** its data is exactly the `https` repository URL, with no tracking parameters, shortener or in-app scheme
