# Spec Delta

## ADDED Requirements

### Requirement: Input-method classification stays current
The service SHALL classify window events with the input methods the system currently reports: a window of an input method installed or enabled after the service started SHALL be treated as an input method, never as the app in use, so the app in use remains the tracked foreground and keeps its limit enforcement.
[@test] `app/src/test/java/com/foco/app/domain/ForegroundAppStateTest.kt`

#### Scenario: A keyboard installed later
- **WHEN** an input method installed or enabled after the service started emits a window event while the user types in a limited app
- **THEN** it does not become the tracked foreground and the app in use keeps being evaluated against its limit

#### Scenario: A package that stopped being an input method
- **WHEN** a package that is no longer an input method emits a window event
- **THEN** it is classified under the normal app rules, not exempted as an input method
