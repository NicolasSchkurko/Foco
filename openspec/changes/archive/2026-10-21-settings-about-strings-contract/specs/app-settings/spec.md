# Spec Delta

## MODIFIED Requirements

### Requirement: Settings shows a centered app identity block
The settings screen SHALL show, above the Language section and horizontally centered, the app name with the installed version and the credit `Open source project by Nicolas Schkurko`. The version SHALL come from the installed package, never from a literal in the screen or in a string resource.
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

#### Scenario: The credit is not paraphrased
- **WHEN** the identity block renders in English
- **THEN** the credit reads exactly `Open source project by Nicolas Schkurko` and the first line is `Foco <version>` without a `Version ` prefix
