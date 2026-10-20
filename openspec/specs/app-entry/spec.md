# app-entry Specification

## Purpose
Gate the app's entry behind the usage-access permission: onboarding while it is missing, dashboard once granted; re-evaluated every time the app comes to the foreground.

## Requirements

### Requirement: Entry is gated by the usage-access permission
Launching the app SHALL open the dashboard when the permission is granted and the onboarding screen when it is missing.
[@test] `app/src/androidTest/java/com/foco/app/ui/AppEntryGateTest.kt`

#### Scenario: Granted at launch
- **WHEN** the app launches with the usage-access permission granted
- **THEN** the dashboard is shown (no onboarding)

#### Scenario: Missing at launch
- **WHEN** the app launches without the permission
- **THEN** the onboarding screen is shown

### Requirement: Permission changes are re-evaluated on return
The entry decision SHALL be re-evaluated every time the app returns to the foreground, switching the shown screen when the permission changed outside the app.
[@test] `app/src/androidTest/java/com/foco/app/ui/AppEntrySeamTest.kt`

#### Scenario: Granted while away
- **WHEN** the permission is granted in Settings and the user returns to the app
- **THEN** the dashboard replaces the onboarding

#### Scenario: Revoked while away
- **WHEN** the permission is revoked and the user returns to the app
- **THEN** the onboarding replaces the dashboard

### Requirement: Onboarding explains the permission and links to Settings
Without the permission, the onboarding SHALL explain why usage access is needed and offer a shortcut to the Android usage-access settings.
[@test] `app/src/androidTest/java/com/foco/app/ui/AppEntrySeamTest.kt`

#### Scenario: Shortcut present
- **WHEN** the onboarding screen is shown
- **THEN** it explains the permission and offers the settings shortcut button

### Requirement: Onboarding text stays legible over the themed window
The onboarding screen SHALL render its title and explanation with the theme's content colour so the display title reaches at least 3:1 contrast and the body text at least 4.5:1 against the themed background, in both light and dark appearances.
[@test] `app/src/androidTest/java/com/foco/app/ui/OnboardingContrastTest.kt`

#### Scenario: Dark appearance
- **WHEN** the onboarding screen is shown with the dark appearance active
- **THEN** the title and explanation meet their contrast thresholds against the near-black surface, never black on black

#### Scenario: Light appearance
- **WHEN** the onboarding screen is shown with the light appearance active
- **THEN** the title and explanation meet their contrast thresholds against the light surface

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
