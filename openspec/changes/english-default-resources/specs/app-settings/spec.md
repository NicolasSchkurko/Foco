# Spec Delta

## ADDED Requirements

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
