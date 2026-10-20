# Spec Delta

## ADDED Requirements

### Requirement: The first open waits for the startup history
The app SHALL run the startup consolidation only after usage access is granted and SHALL show a loading state until that run ends before mounting the home. A failed run SHALL not trap the app: the loading state SHALL end and the next start SHALL retry.
[@test] `app/src/androidTest/java/com/foco/app/ui/StartupLoadingTest.kt`
[@test] `app/src/androidTest/java/com/foco/app/ui/AppEntrySeamTest.kt`

#### Scenario: Permission is already granted
- **WHEN** the app starts with usage access and the startup run is still in progress
- **THEN** the home is not mounted yet; the loading state is visible until the run ends

#### Scenario: Permission is granted from onboarding
- **WHEN** the user enables usage access and returns to the app
- **THEN** the startup run starts then, the loading state is shown, and the home follows without a process restart

#### Scenario: The startup run fails
- **WHEN** the startup consolidation ends in failure
- **THEN** the loading state ends, the app remains usable, and the next start retries
