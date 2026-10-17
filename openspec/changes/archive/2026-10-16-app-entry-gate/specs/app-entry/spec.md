# Spec Delta

## Purpose

Gate the app's entry behind the usage-access permission: onboarding while it is missing, dashboard once granted; re-evaluated every time the app comes to the foreground.

## ADDED Requirements

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
