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

### Requirement: Use windows are per app, local and recurring
The app SHALL store at most one recurring use window per package in `limits.db`. No row means no
window; a row has a start and end on a 15-minute grid in the local zone. Overnight windows are
allowed, equal start and end is rejected, and a window may exist without a minute limit. The window
SHALL survive reboot and recur every local day.
[@test] `app/src/androidTest/java/com/foco/app/data/AppLimitWindowStoreTest.kt`
[@test] `app/src/androidTest/java/com/foco/app/ui/AppLimitsSettingsTest.kt`

#### Scenario: No window and no limit
- **WHEN** a package has neither a window row nor a limit row
- **THEN** it has no time-of-day restriction and no minute budget

#### Scenario: Limit only
- **WHEN** a package has a limit and no window row
- **THEN** the limit applies and there is no time-of-day restriction

#### Scenario: Both active
- **WHEN** a package has a 30-minute limit and a 09:00–23:00 window
- **THEN** the window blocks outside and the limit applies inside

#### Scenario: A 09:00–23:00 window
- **WHEN** the local time is 09:00 or later and before 23:00
- **THEN** the app is inside the window; at 23:00 and later it is outside

#### Scenario: An overnight window
- **WHEN** the window is 22:00–06:00
- **THEN** 23:00 and 05:00 are inside, and 12:00 is outside

#### Scenario: Equal start and end
- **WHEN** the picker would set the same start and end
- **THEN** the value is rejected; the user clears the window to allow all day

#### Scenario: A window without a minute limit
- **WHEN** the package has a window and no limit row
- **THEN** it is blocked outside the window and unrestricted inside it

#### Scenario: Surviving a restart
- **WHEN** the app process or the device restarts
- **THEN** the window row is still there

### Requirement: Outside the window the app is blocked
Outside the window the monitor SHALL block the app with the existing 3-second full-screen overlay and
an “Outside allowed hours” message, with no override.
[@test] `app/src/androidTest/java/com/foco/app/data/UsageMonitorServiceTest.kt`
[@test] `app/src/androidTest/java/com/foco/app/ui/AppLimitBlockTest.kt`

#### Scenario: Opening outside
- **WHEN** the app is opened outside its window
- **THEN** the outside-hours overlay is shown immediately and cannot be dismissed into the app

#### Scenario: No window
- **WHEN** the package has no window row
- **THEN** no outside-hours block is shown

### Requirement: The window boundary is enforced promptly
At app open the accessibility event SHALL block immediately. If the app is already open when the
boundary arrives, a one-shot boundary check SHALL block then, with the 2-minute check as fallback.
[@test] `app/src/test/java/com/foco/app/domain/AppLimitWindowRulesTest.kt`
[@test] `app/src/androidTest/java/com/foco/app/data/UsageMonitorServiceTest.kt`

#### Scenario: The window closes while using
- **WHEN** the app is open at 23:00 and the window ends then
- **THEN** the boundary check starts the outside-hours block

#### Scenario: The limit is already complete
- **WHEN** the app is opened with its daily limit already reached
- **THEN** the same accessibility event blocks immediately; the window is not the only immediate reason

#### Scenario: The boundary check is delayed
- **WHEN** the one-shot boundary check cannot run on time
- **THEN** the next 2-minute check blocks the app outside the window

### Requirement: The minute budget counts only inside the window
The daily minute budget SHALL count only the portions of today's sessions that fall inside the
window. Time outside the window SHALL NOT be added to the used minutes, and the budget SHALL still
reset at local midnight.
[@test] `app/src/test/java/com/foco/app/domain/AppLimitWindowRulesTest.kt`
[@test] `app/src/androidTest/java/com/foco/app/data/UsageMonitorServiceTest.kt`

#### Scenario: Mixed time
- **WHEN** a 30-minute limit has 10 minutes outside the window and 25 minutes inside
- **THEN** the used budget is 25 minutes, not 35

#### Scenario: Reaching the limit inside
- **WHEN** the in-window usage reaches 30 minutes
- **THEN** the existing limit block applies, not the outside-hours message

#### Scenario: Overnight split
- **WHEN** a session crosses midnight inside a 22:00–06:00 window
- **THEN** each local day counts only its own in-window portion

### Requirement: Alerts are evaluated only inside the window
The 50 %, 75 % and 90 % alerts SHALL be evaluated only while the current local time is inside the
window. Outside the window, no alert SHALL be posted and the outside-hours block SHALL be the only
feedback.
[@test] `app/src/test/java/com/foco/app/domain/AppLimitWindowRulesTest.kt`
[@test] `app/src/androidTest/java/com/foco/app/data/UsageMonitorServiceTest.kt`

#### Scenario: A threshold outside
- **WHEN** the usage is at 75 % but the app is outside its window
- **THEN** no threshold alert is posted; the app is blocked instead

#### Scenario: A threshold inside
- **WHEN** the usage crosses 75 % while inside the window
- **THEN** the existing 75 % alert can fire once, as specified by the alerts change

### Requirement: The detail previews the configured limit, window and alerts

The app detail SHALL show a read-only preview of the daily limit, the recurring use window and the
alert state, read from the same per-app configuration the configuration screen writes. The preview
block SHALL NOT carry an editing action.

[@test] `app/src/androidTest/java/com/foco/app/ui/AppDetailLimitsTest.kt`

#### Scenario: An app with a limit, a window and alerts

- **WHEN** Instagram has a 30-minute limit, a 09:00-23:00 window and alerts on
- **THEN** the detail preview shows 30 minutes, 09:00-23:00 and that alerts are on

#### Scenario: An app with nothing configured

- **WHEN** the viewed app has no limit, no window and alerts off
- **THEN** the preview shows "No limit", "No window" and "Alerts off" rather than omitting the row

#### Scenario: The preview does not change the configuration

- **WHEN** the preview is shown
- **THEN** no control in it changes the stored limit, window or alert state

### Requirement: The detail header opens the app's configuration with a gear

The app detail header SHALL show a gear action, the same icon the dashboard uses to open Settings,
that opens the configuration screen for the app being viewed. The dedicated screen SHALL show only
that package, and its back action SHALL return to the detail of the same app.

[@test] `app/src/androidTest/java/com/foco/app/ui/AppDetailLimitsTest.kt`
[@test] `app/src/androidTest/java/com/foco/app/ui/AppAppSettingsTest.kt`

#### Scenario: Opening the dedicated settings

- **WHEN** the gear in the detail of Instagram is tapped
- **THEN** the configuration screen for Instagram opens, showing its limit, window and alert controls

#### Scenario: Only the viewed app

- **WHEN** the dedicated screen for Instagram is open
- **THEN** no other app is listed

#### Scenario: Going back

- **WHEN** the back action is used from the dedicated screen
- **THEN** the detail of the same app is shown again

### Requirement: The dedicated screen edits the daily limit

The dedicated screen SHALL offer No limit, Block immediately and the 15-minute steps through 24 h.
Each pick SHALL persist per package through `AppLimitsStore` and SHALL sync the monitor.

[@test] `app/src/androidTest/java/com/foco/app/ui/AppAppSettingsTest.kt`

#### Scenario: Picking a 30-minute limit

- **WHEN** 30 min is picked for the app on the dedicated screen
- **THEN** the limit is persisted for that package and the monitor is synced

#### Scenario: Clearing the limit

- **WHEN** No limit is picked on the dedicated screen
- **THEN** the limit row is cleared for that package and the monitor is synced

### Requirement: The dedicated screen edits the use window

The dedicated screen SHALL offer No window or a 15-minute start/end pair, SHALL allow overnight
ranges, SHALL reject an equal start and end, and SHALL persist the value per package.

[@test] `app/src/androidTest/java/com/foco/app/ui/AppAppSettingsTest.kt`

#### Scenario: Picking a window

- **WHEN** 09:00 and 23:00 are picked on the dedicated screen
- **THEN** the window is persisted for that package and the monitor is synced

#### Scenario: Equal start and end

- **WHEN** the same start and end are picked
- **THEN** the value is rejected and the previous window stays

### Requirement: The dedicated screen edits the alert toggle

The dedicated screen SHALL offer the alert toggle, off by default and enableable only with a
positive limit. Turning it on SHALL request the notification permission when missing and SHALL
persist the flag per package.

[@test] `app/src/androidTest/java/com/foco/app/ui/AppAppSettingsTest.kt`

#### Scenario: Enabling alerts

- **WHEN** the alert toggle is turned on for an app with a positive limit
- **THEN** the notification permission is requested if missing and the flag is persisted per package

#### Scenario: No limit or zero

- **WHEN** the app has No limit or Block immediately
- **THEN** its alert toggle is off and disabled

### Requirement: The block acts only on the app in the foreground

The accessibility host SHALL track the current foreground package and SHALL clear it when Foco's
own interface comes to the front. A check that read the previous package SHALL NOT block after Foco
takes over. A block SHALL run only for the package still tracked as foreground, so it never shows
over Foco or sends Foco home. System UI and input-method windows SHALL NOT replace it. After a
block sends an app home, late view-group window events from that same app during a short settling
window SHALL NOT replace the tracked app or start a second block. Any other window event from
that app SHALL still be evaluated, including a relaunch inside the window.

[@test] `app/src/test/java/com/foco/app/domain/ForegroundAppStateTest.kt`

#### Scenario: Foco comes to the front

- **WHEN** YouTube is the tracked foreground and the user opens Foco from recents or the monitor notification
- **THEN** the tracked package is cleared and no block starts for YouTube

#### Scenario: A delayed check

- **WHEN** the 2-minute check read YouTube and Foco came to the front before the block request ran
- **THEN** the request is dropped and Foco stays open

#### Scenario: The app returns

- **WHEN** the user goes back to YouTube after Foco, once the settling window has passed
- **THEN** YouTube is tracked again and is evaluated immediately

#### Scenario: The app is reopened right away

- **WHEN** the user reopens the blocked app inside the settling window
- **THEN** the new launch event is evaluated and the app blocks once more

#### Scenario: System UI and the keyboard

- **WHEN** the notification shade, recents or an input method emits a window event
- **THEN** the app in use remains the tracked foreground

#### Scenario: The app leaves after a block

- **WHEN** a block sends the app home and that app emits a late window-state event while the launcher comes to the front
- **THEN** the tracked app becomes the launcher and no second block starts
