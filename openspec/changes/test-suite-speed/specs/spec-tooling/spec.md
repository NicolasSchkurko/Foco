# Spec Delta

## ADDED Requirements

### Requirement: Test waits are event-driven
App test code SHALL wait for the expected UI or device state instead of using a fixed `Thread.sleep`; a structural check SHALL fail when a test under `app/src/androidTest` contains a fixed sleep.
[@test] `app/src/test/java/com/foco/app/structure/NoFixedSleepTest.kt`
[@test] `app/src/androidTest/java/com/foco/app/ui/AppDetailNavigationTest.kt`

#### Scenario: A test waits for a UI state
- **WHEN** a test needs to wait for a screen, text or idle state
- **THEN** it uses the Compose/test or `UiDevice` wait, not a fixed sleep

#### Scenario: A fixed sleep is added
- **WHEN** an androidTest file contains `Thread.sleep`
- **THEN** `NoFixedSleepTest` fails and names the file

#### Scenario: The suite keeps its coverage
- **WHEN** the waits are replaced
- **THEN** the same assertions and test cases remain and pass
