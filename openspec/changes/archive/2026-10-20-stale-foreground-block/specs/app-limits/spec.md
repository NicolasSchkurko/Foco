# app-limits Specification

## ADDED Requirements

### Requirement: The block acts only on the app in the foreground

The accessibility host SHALL track the current foreground package and SHALL clear it when Foco's
own interface comes to the front. A check that read the previous package SHALL NOT block after Foco
takes over. A block SHALL run only for the package still tracked as foreground, so it never shows
over Foco or sends Foco home. System UI and input-method windows SHALL NOT replace it.

[@test] `app/src/test/java/com/foco/app/domain/ForegroundAppStateTest.kt`

#### Scenario: Foco comes to the front

- **WHEN** YouTube is the tracked foreground and the user opens Foco from recents or the monitor notification
- **THEN** the tracked package is cleared and no block starts for YouTube

#### Scenario: A delayed check

- **WHEN** the 2-minute check read YouTube and Foco came to the front before the block request ran
- **THEN** the request is dropped and Foco stays open

#### Scenario: The app returns

- **WHEN** the user goes back to YouTube after Foco
- **THEN** YouTube is tracked again and is evaluated immediately

#### Scenario: System UI and the keyboard

- **WHEN** the notification shade, recents or an input method emits a window event
- **THEN** the app in use remains the tracked foreground
