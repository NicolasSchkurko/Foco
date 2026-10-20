# app-limits Specification

## ADDED Requirements

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
