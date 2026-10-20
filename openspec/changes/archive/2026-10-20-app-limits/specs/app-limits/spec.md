# app-limits Specification

## Purpose

Keep a per-app daily minute budget in local storage and enforce it on the device: configure a limit,
read today's usage, and stop the app when the budget is reached.

## ADDED Requirements

### Requirement: A limit is per app, per day and optional
The app SHALL store at most one daily limit per package in a dedicated Room database in private
storage. The value `N` means N minutes are allowed today; `0` means block as soon as the app comes to
the foreground; no row means no limit. The limit SHALL survive process death and reboot, and SHALL be
read against the current local day.
[@test] `app/src/test/java/com/foco/app/domain/AppLimitRulesTest.kt`
[@test] `app/src/androidTest/java/com/foco/app/data/AppLimitsStoreTest.kt`

#### Scenario: No limit
- **WHEN** a package has no limit row
- **THEN** no rule ever blocks it

#### Scenario: Under and over a 30-minute limit
- **WHEN** Instagram has 30 minutes and today's usage is 29 min 59 s
- **THEN** it is not blocked; at 30 minutes or more it is blocked

#### Scenario: Zero minutes
- **WHEN** a package's limit is 0
- **THEN** the next foreground event blocks it immediately, even before any usage is recorded

#### Scenario: Local midnight
- **WHEN** the local date changes
- **THEN** the used time comes from the new day and the same limit is available again

#### Scenario: Surviving a restart
- **WHEN** the app process or the device restarts
- **THEN** the limit row is still there

### Requirement: The monitor checks today's usage at most every two minutes
While at least one limit exists, the app SHALL run `UsageMonitorService` and SHALL check the current
foreground package at most every 2 minutes and on every accessibility foreground event, using today's
UsageStats sessions. It SHALL NOT block a package without a limit.
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

### Requirement: The monitor follows the limit lifecycle
The service SHALL show an ongoing notification while running, start when the first limit is saved,
stop and remove the notification when no limit remains, and restart after boot when a limit and
accessibility are present.
[@test] `app/src/androidTest/java/com/foco/app/data/UsageMonitorServiceTest.kt`

#### Scenario: No limits configured
- **WHEN** no limit row exists
- **THEN** the service is not running

#### Scenario: Notification and stop
- **WHEN** the service is running and the last limit is cleared
- **THEN** the notification is removed and the service stops

#### Scenario: Reboot
- **WHEN** the device boots with at least one limit and accessibility enabled
- **THEN** the service is running again

### Requirement: The block is a three-second full-screen limit screen
When a limited app is at or over its daily limit, the app SHALL show a full-screen overlay that names
the app, states that the daily limit was reached and counts down 3, 2, 1 seconds. After the countdown
it SHALL remove the overlay and send the app to the home screen. It SHALL NOT show the overlay for an
app without a limit, and the periodic check SHALL NOT restart a countdown already in progress.
[@test] `app/src/androidTest/java/com/foco/app/ui/AppLimitBlockTest.kt`

#### Scenario: Countdown and close
- **WHEN** the block starts
- **THEN** the overlay shows the app, the daily-limit message and a 3, 2, 1 countdown, then the home
  screen is shown

#### Scenario: No limit
- **WHEN** an app without a limit is in the foreground
- **THEN** no overlay is shown

#### Scenario: The tick does not restart the countdown
- **WHEN** a block is showing and the 2-minute check runs again
- **THEN** the countdown continues from where it was

### Requirement: Enforcement requires the accessibility service
The monitor SHALL enforce limits only while the app's accessibility service is enabled. When it is
disabled, enforcement SHALL stop, no overlay SHALL be shown, and the entry gate SHALL show onboarding
that explains it and links to the accessibility settings; the gate SHALL re-check on every return to
the foreground.
[@test] `app/src/androidTest/java/com/foco/app/data/UsageMonitorServiceTest.kt`
[@test] `app/src/androidTest/java/com/foco/app/ui/AppEntrySeamTest.kt`

#### Scenario: Accessibility missing
- **WHEN** the app starts with usage access but without the accessibility service
- **THEN** onboarding is shown with the accessibility explanation and shortcut

#### Scenario: Revoked while away
- **WHEN** the accessibility service is disabled while the app is away
- **THEN** returning to Foco shows onboarding and the monitor stops enforcing

#### Scenario: Re-enabled
- **WHEN** the accessibility service is enabled again and a limit exists
- **THEN** the monitor starts enforcing again

### Requirement: Limits stay local
The limits and the monitor SHALL use only app-private storage and platform usage/accessibility APIs.
They SHALL NOT use the network, upload data, or change `allowBackup`.
[@test] `app/src/test/java/com/foco/app/NoNetworkPermissionTest.kt`
[@test] `app/src/test/java/com/foco/app/BackupDisabledManifestTest.kt`

#### Scenario: No network permission
- **WHEN** the manifest is inspected after this change
- **THEN** it declares no `android.permission.INTERNET`

#### Scenario: Private storage
- **WHEN** a limit is written
- **THEN** it lands in the app-private `limits.db`, and `allowBackup` remains `false`
