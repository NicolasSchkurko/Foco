# app-limits Specification

## MODIFIED Requirements

### Requirement: The block is a three-second full-screen limit screen

When a limited app is at or over its daily limit or outside its use window, the app SHALL show a
full-screen overlay that names the app, states the reason and counts down 3, 2, 1 seconds. After the
countdown it SHALL remove the overlay and send the app to the home screen. It SHALL NOT show the
overlay for an app without a limit, and the periodic check SHALL NOT restart a countdown already in
progress. Showing the overlay SHALL NOT crash the process or unbind the accessibility service;
after a block the service SHALL stay bound and keep evaluating the next foreground app.

[@test] `app/src/androidTest/java/com/foco/app/ui/AppLimitBlockTest.kt`

#### Scenario: Countdown and close

- **WHEN** the block starts
- **THEN** the overlay shows the app, the reason and a 3, 2, 1 countdown, then the home screen is shown

#### Scenario: No limit

- **WHEN** an app without a limit is in the foreground
- **THEN** no overlay is shown

#### Scenario: The tick does not restart the countdown

- **WHEN** a block is showing and the 2-minute check runs again
- **THEN** the countdown continues from where it was

#### Scenario: Showing the block does not kill the service

- **WHEN** the overlay is added to the window manager
- **THEN** the accessibility service stays bound, the process does not crash, and the next foreground app is still evaluated

#### Scenario: The overlay host restores its saved-state registry

- **WHEN** the block screen is composed into the overlay host
- **THEN** the host's saved-state registry has been attached and restored and its lifecycle reached CREATED before composition
