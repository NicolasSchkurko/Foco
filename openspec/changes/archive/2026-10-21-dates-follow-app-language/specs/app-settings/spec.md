# Spec Delta

## ADDED Requirements

### Requirement: Dates and times follow the app language
Every date and time label the app renders SHALL be formatted with the app's active language — the same language its strings resolve in — and not with the device default locale, while keeping the existing patterns.
[@test] `app/src/test/java/com/foco/app/ui/FormattingTest.kt`

#### Scenario: English UI on a Spanish device
- **WHEN** the app language is English and the device locale is Spanish
- **THEN** date labels read in English, matching the surrounding strings

#### Scenario: Spanish UI on an English device
- **WHEN** the app language is Spanish and the device locale is English
- **THEN** date labels read in Spanish, matching the surrounding strings

#### Scenario: Automático
- **WHEN** the app language is Automático
- **THEN** date labels use the same language the strings resolve to under Automático
