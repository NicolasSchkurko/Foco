# app-limits Specification

## Purpose
Keep a per-app daily minute budget in local storage and enforce it on the device: configure a limit,
read today's usage, and stop the app when the budget is reached.

## Requirements

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

### Requirement: Alerts are opt-in per app
The app SHALL store an alert flag per package next to the daily limit. The flag SHALL be off by
default and SHALL be enableable only while the package has a positive limit. A package without a
limit, or with a 0-minute limit, SHALL never produce an alert.
[@test] `app/src/androidTest/java/com/foco/app/data/AppLimitAlertStoreTest.kt`
[@test] `app/src/androidTest/java/com/foco/app/ui/AppLimitsSettingsTest.kt`

#### Scenario: Off by default
- **WHEN** a limit is set for the first time
- **THEN** its alert flag is off and no alert is scheduled

#### Scenario: Enabling alerts
- **WHEN** the user enables the toggle for an app with a 30-minute limit
- **THEN** the flag is persisted and the monitor may alert at 50/75/90 %

#### Scenario: No limit or zero
- **WHEN** the app has no limit or a 0-minute limit
- **THEN** the toggle cannot enable alerts and no alert is produced

### Requirement: Alerts use the same two-minute check
The alert decision SHALL run on the existing 2-minute monitor check, using the cumulative today's
usage read at that check. It SHALL NOT run a separate timer, SHALL NOT sum tick deltas, and SHALL NOT
post a threshold alert from an accessibility event alone. A threshold can therefore be reported up to
2 minutes after it is crossed.
[@test] `app/src/test/java/com/foco/app/domain/AppLimitAlertRulesTest.kt`
[@test] `app/src/androidTest/java/com/foco/app/data/UsageMonitorServiceTest.kt`

#### Scenario: The next check after crossing
- **WHEN** a 30-minute limit crosses 75 % at 22.5 minutes and the next check is at 23.5 minutes
- **THEN** the 75 % alert is evaluated at the 23.5-minute check, not earlier

#### Scenario: Accessibility alone
- **WHEN** an accessibility foreground event occurs between two checks
- **THEN** it may evaluate the 100 % block but does not post a threshold alert

#### Scenario: Cumulative, not tick deltas
- **WHEN** two consecutive checks read today's usage
- **THEN** each reading is the cumulative day total, not the time since the previous check

### Requirement: The monitor alerts on the highest threshold crossed
On the 2-minute check, while the alert flag is on, the monitor SHALL compute the 50 %, 75 % and 90 %
tiers from today's usage. If several tiers are crossed at once it SHALL notify only the highest and
mark the lower tiers as fired.
[@test] `app/src/test/java/com/foco/app/domain/AppLimitAlertRulesTest.kt`
[@test] `app/src/androidTest/java/com/foco/app/data/UsageMonitorServiceTest.kt`

#### Scenario: The three tiers
- **WHEN** a 30-minute limit's 2-minute check first reads usage at or above 15, 22.5 and 27 minutes
- **THEN** one alert is produced at each tier, in order, at those checks

#### Scenario: A jump from 60 % to 90 %
- **WHEN** the next check sees 90 % with 50 % and 75 % already crossed
- **THEN** only the 90 % alert is shown, and all three tiers are marked fired

### Requirement: The monitor never alerts at or above the limit
The monitor SHALL NOT post a threshold alert at or above 100 % or once the block has started.
[@test] `app/src/test/java/com/foco/app/domain/AppLimitAlertRulesTest.kt`
[@test] `app/src/androidTest/java/com/foco/app/data/UsageMonitorServiceTest.kt`

#### Scenario: At or over 100 %
- **WHEN** the check reads the usage at or above the limit
- **THEN** no threshold alert is shown; the existing block handles 100 %

### Requirement: Alerts fire only while the app is open
At the 2-minute check, the monitor SHALL evaluate and post an alert only while the limited package is
the current foreground app. It SHALL NOT post an alert while another app or the home screen is in
front.
[@test] `app/src/test/java/com/foco/app/domain/AppLimitAlertRulesTest.kt`
[@test] `app/src/androidTest/java/com/foco/app/data/UsageMonitorServiceTest.kt`

#### Scenario: The app is closed before the check
- **WHEN** the app leaves the foreground before the threshold is checked
- **THEN** no alert is posted at that moment; the next check while the app is open evaluates the tier

#### Scenario: The app returns
- **WHEN** the app comes back and the tier has not fired yet
- **THEN** it can alert once while the app is open

### Requirement: The alert is a notification with only Entendido
The alert SHALL be a system notification naming the app, the reached tier and the limit, with exactly
one action: **Entendido**, which dismisses it. The notification SHALL NOT block, close or change the
app, and SHALL NOT replace the 100 % block.
[@test] `app/src/androidTest/java/com/foco/app/data/AppLimitAlertNotifierTest.kt`

#### Scenario: The notification content
- **WHEN** Instagram reaches 75 % of a 30-minute limit
- **THEN** the notification names Instagram, 75 % and 30 minutes

#### Scenario: The only action
- **WHEN** the notification is shown
- **THEN** it offers only the Entendido action, and that action removes the notification

#### Scenario: No blocking
- **WHEN** the alert is shown
- **THEN** the app stays in the foreground; only the existing 100 % block closes it

### Requirement: Fired tiers reset per day and on configuration change
Fired tiers SHALL be scoped to the package, the local day and the tier, so each threshold alerts at
most once per package and local day and resets at local midnight. Changing the limit or the alert
toggle for a package SHALL clear that package's fired tiers for the current day before the next check.
[@test] `app/src/androidTest/java/com/foco/app/data/AppLimitAlertStoreTest.kt`
[@test] `app/src/test/java/com/foco/app/domain/AppLimitAlertRulesTest.kt`

#### Scenario: The next day
- **WHEN** the local date changes
- **THEN** all tiers are available again

#### Scenario: The limit is raised
- **WHEN** the user raises a 30-minute limit to 60 minutes
- **THEN** the day's fired tiers are cleared and the new percentages are evaluated from the start

#### Scenario: Alerts are re-enabled
- **WHEN** the toggle goes from off to on
- **THEN** the current day's fired tiers are cleared and a crossed tier can alert once

### Requirement: Notification permission is respected
When notifications are disabled for Foco, the app SHALL NOT post an alert and the Settings toggle
SHALL show that the system permission is missing. The limit and the block SHALL keep working.
[@test] `app/src/androidTest/java/com/foco/app/data/AppLimitAlertNotifierTest.kt`
[@test] `app/src/androidTest/java/com/foco/app/ui/AppLimitsSettingsTest.kt`

#### Scenario: Permission denied
- **WHEN** notifications are disabled
- **THEN** no alert is posted and the toggle states the permission is missing

#### Scenario: Permission granted later
- **WHEN** the user enables notifications and returns to Foco
- **THEN** the next crossed tier can alert, still once per threshold and day
