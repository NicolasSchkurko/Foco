# app-limits

## MODIFIED Requirements

### Requirement: The monitor checks today's usage at most every two minutes

While at least one limit exists, the app SHALL run `UsageMonitorService` and SHALL check the current
foreground package at most every 2 minutes and on every accessibility foreground event, using today's
UsageStats sessions. It SHALL NOT block a package without a limit. Shortly after Foco's own block
finishes, the app SHALL re-check from today's UsageStats sessions whether the blocked package is
again the foreground package, so a return the platform reported without any accessibility window
event is still blocked. That re-check SHALL be a one-shot per block (run twice, a couple of seconds
and a few seconds after the countdown), SHALL do nothing while the limit screen is showing, and
SHALL NOT change the 2-minute periodic cadence.

[@test] `app/src/test/java/com/foco/app/domain/AppLimitRulesTest.kt`
[@test] `app/src/androidTest/java/com/foco/app/data/UsageMonitorServiceTest.kt`

#### Scenario: Crossing the limit mid-use

- **WHEN** the current app crosses its limit while in the foreground
- **THEN** the next check (at most 2 minutes later) starts the block

#### Scenario: Already over the limit at open

- **WHEN** the current app is at or over its limit when it comes to the foreground
- **THEN** the accessibility event starts the block immediately

#### Scenario: A package without a limit

- **WHEN** the current package has no limit row
- **THEN** the check does not block it

#### Scenario: A return without any window event

- **WHEN** a block finishes and the blocked app becomes the foreground package again, and the
  platform delivers no accessibility window event for that return
- **THEN** the session-based re-check finds the app in the foreground and starts the limit screen

#### Scenario: The app stays home

- **WHEN** a block finishes and the blocked app does not return to the foreground
- **THEN** the session-based re-check finds no session of that app covering the re-check instant
  and no second limit screen starts
