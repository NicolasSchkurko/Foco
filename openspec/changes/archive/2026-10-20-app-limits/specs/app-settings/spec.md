# Spec Delta

## ADDED Requirements

### Requirement: App limits are configured in Settings
The settings screen SHALL list every launchable app with its current daily limit and a picker that
offers No limit, Block immediately and 15-minute steps up to 24 hours. Picking a value SHALL persist
it, update the row and start or stop the monitor; a package with no row SHALL read No limit.
[@test] `app/src/androidTest/java/com/foco/app/ui/AppLimitsSettingsTest.kt`

#### Scenario: Setting a 30-minute limit
- **WHEN** the user picks 30 min for Instagram
- **THEN** the row reads 30 min and the value is still there after a restart

#### Scenario: Blocking immediately
- **WHEN** the user picks Block immediately for an app
- **THEN** the row reads Block immediately and the app is blocked on its next foreground entry

#### Scenario: Clearing the limit
- **WHEN** the user picks No limit
- **THEN** the row reads No limit and the app is never blocked

#### Scenario: The 15-minute grid
- **WHEN** the picker is opened
- **THEN** it offers No limit, Block immediately, 15 min, 30 min, 45 min and the rest of the
  15-minute steps through 24 h
