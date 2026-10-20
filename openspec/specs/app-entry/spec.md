# app-entry Specification

## Purpose
Gate the app's entry behind the usage-access permission: onboarding while it is missing, dashboard once granted; re-evaluated every time the app comes to the foreground.

## Requirements

### Requirement: Entry is gated by the usage-access permission
Launching the app SHALL open the dashboard when usage access and the accessibility service are granted, and the onboarding when either is missing: the usage-access step first, then the accessibility step.
[@test] `app/src/androidTest/java/com/foco/app/ui/AppEntryGateTest.kt`
[@test] `app/src/androidTest/java/com/foco/app/ui/AppEntrySeamTest.kt`

#### Scenario: Granted at launch
- **WHEN** the app launches with usage access and the accessibility service granted
- **THEN** the dashboard is shown (no onboarding)

#### Scenario: Missing at launch
- **WHEN** the app launches without usage access
- **THEN** the usage-access onboarding is shown

#### Scenario: Accessibility missing at launch
- **WHEN** the app launches with usage access but without the accessibility service
- **THEN** the accessibility onboarding is shown

### Requirement: Permission changes are re-evaluated on return
The entry decision SHALL be re-evaluated every time the app returns to the foreground, switching the shown screen when usage access or the accessibility service changed outside the app.
[@test] `app/src/androidTest/java/com/foco/app/ui/AppEntryGateTest.kt`
[@test] `app/src/androidTest/java/com/foco/app/ui/AppEntrySeamTest.kt`

#### Scenario: Granted while away
- **WHEN** usage access is granted in Settings and the user returns to the app
- **THEN** the next gate step (accessibility onboarding or dashboard) replaces the onboarding

#### Scenario: Revoked while away
- **WHEN** usage access is revoked and the user returns to the app
- **THEN** the usage-access onboarding replaces the dashboard

#### Scenario: Accessibility granted while away
- **WHEN** the accessibility service is enabled in Settings and the user returns to the app
- **THEN** the dashboard replaces the accessibility onboarding

#### Scenario: Accessibility revoked while away
- **WHEN** the accessibility service is disabled and the user returns to the app
- **THEN** the accessibility onboarding replaces the dashboard

### Requirement: Onboarding explains the permission and links to Settings
Without a permission, the onboarding SHALL explain why it is needed and offer a shortcut to the matching Android settings: usage access first, the accessibility service second.
[@test] `app/src/androidTest/java/com/foco/app/ui/AppEntrySeamTest.kt`

#### Scenario: Shortcut present
- **WHEN** the usage-access onboarding is shown
- **THEN** it explains usage access and offers the usage-access settings shortcut button

#### Scenario: Accessibility shortcut present
- **WHEN** the accessibility onboarding is shown
- **THEN** it explains the accessibility service and offers the accessibility settings shortcut button

### Requirement: Onboarding text stays legible over the themed window
Both onboarding steps SHALL render their title and explanation with the theme's content colour so the display title reaches at least 3:1 contrast and the body text at least 4.5:1 against the themed background, in both light and dark appearances.
[@test] `app/src/androidTest/java/com/foco/app/ui/OnboardingContrastTest.kt`

#### Scenario: Dark appearance
- **WHEN** either onboarding step is shown with the dark appearance active
- **THEN** the title and explanation meet their contrast thresholds against the near-black surface, never black on black

#### Scenario: Light appearance
- **WHEN** either onboarding step is shown with the light appearance active
- **THEN** the title and explanation meet their contrast thresholds against the light surface

### Requirement: The first open waits for the startup history
The app SHALL run the startup consolidation only after usage access and the accessibility service are granted and SHALL show a loading state until that run ends before mounting the home. A failed run SHALL not trap the app: the loading state SHALL end and the next start SHALL retry.
[@test] `app/src/androidTest/java/com/foco/app/ui/StartupLoadingTest.kt`
[@test] `app/src/androidTest/java/com/foco/app/ui/AppEntrySeamTest.kt`

#### Scenario: Permission is already granted
- **WHEN** the app starts with usage access and accessibility and the startup run is still in progress
- **THEN** the home is not mounted yet; the loading state is visible until the run ends

#### Scenario: Permission is granted from onboarding
- **WHEN** the user enables the missing permission and returns to the app
- **THEN** the startup run starts then, the loading state is shown, and the home follows without a process restart

#### Scenario: The startup run fails
- **WHEN** the startup consolidation ends in failure
- **THEN** the loading state ends, the app remains usable, and the next start retries
