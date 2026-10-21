# Spec Delta

## ADDED Requirements

### Requirement: The limits editor lives in the app's settings tab
The controls that configure one app's daily limit and recurring use window SHALL be shown in that
app's settings tab, and nowhere else; the global settings screen SHALL NOT offer a limits editor.
[@test] `app/src/androidTest/java/com/foco/app/ui/LimitsQuickAdjustTest.kt`

#### Scenario: The app's tab owns the editor
- **WHEN** the user opens the settings of an app
- **THEN** the quick-adjust buttons and the time-range picker are there, editing that app

#### Scenario: The global tab has no limits editor
- **WHEN** the user opens the global settings
- **THEN** no limit or window editor is shown

### Requirement: The app detail's limits preview opens that app's settings
The app detail's limits preview block SHALL be interactive: tapping anywhere on it SHALL open the
settings of the app being shown, and the header action of that screen SHALL be the limits control.
[@test] `app/src/androidTest/java/com/foco/app/ui/AppDetailLimitsTest.kt`

#### Scenario: The preview is a shortcut
- **WHEN** the user taps the limits preview of the app being shown
- **THEN** the settings of that app open

#### Scenario: The header action is the limits control
- **WHEN** the user taps the header's action on the app detail
- **THEN** the settings of that app open

### Requirement: The detail's swipe walks the days
A horizontal swipe on the app detail SHALL move the selected day inside the stored window, the way
the home's ring walks its positions, instead of leaving the screen: swiping left moves to the next
(older) day and swiping right to the previous one. The back control and the system back keep
leaving the detail.
[@test] `app/src/androidTest/java/com/foco/app/ui/AppDetailLimitsTest.kt`

#### Scenario: A swipe moves the day
- **WHEN** the user swipes horizontally on the app detail
- **THEN** the detail shows another day of its window, and the screen stays open

#### Scenario: The swipe stops at the ends
- **WHEN** the selected day is the first or the last of the window
- **THEN** a swipe beyond it changes nothing

### Requirement: The daily limit can be turned on and off
The app's limit SHALL be a switch plus its size: with the switch off the app has no limit, and with
it on the limit is a number of minutes on the 15-minute grid, where zero means "block as soon as
the app is opened" and is shown as such, never as the absence of a limit. The alerts toggle SHALL
follow the limit: it is available whenever the app has one.
[@test] `app/src/androidTest/java/com/foco/app/ui/LimitsQuickAdjustTest.kt`
[@test] `app/src/androidTest/java/com/foco/app/ui/AppAppSettingsTest.kt`

#### Scenario: Turning the limit off
- **WHEN** the user turns the limit off
- **THEN** the app has no limit and the stepper stays disabled

#### Scenario: Turning the limit on
- **WHEN** the user turns the limit on
- **THEN** the app gets a limit of fifteen minutes and the stepper becomes usable

#### Scenario: Block as soon as it opens
- **WHEN** the limit is at zero minutes
- **THEN** the screen says the app is blocked immediately, and the alerts toggle is available
