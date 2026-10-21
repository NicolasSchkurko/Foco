# Spec Delta

## ADDED Requirements

### Requirement: One notification per app
Every app's alert SHALL be posted under its own notification id, derived from its package, so alerts for different apps stand side by side instead of replacing one another; the Entendido action SHALL dismiss only the notification it belongs to, and a later alert for the same app SHALL update that app's own notification without touching any other app's.
[@test] `app/src/androidTest/java/com/foco/app/data/AppLimitAlertNotifierTest.kt`

#### Scenario: Two apps cross a tier
- **WHEN** two limited apps each reach a tier while the monitor runs
- **THEN** both notifications are present at the same time, each naming its own app

#### Scenario: A colliding pair
- **WHEN** two packages whose names hash to the same small value both alert
- **THEN** neither notification replaces the other

#### Scenario: Entendido
- **WHEN** the action is tapped on one of the two notifications
- **THEN** only that notification is removed and the other stays

#### Scenario: A higher tier for the same app
- **WHEN** the same app later crosses a higher tier
- **THEN** its own notification is updated in place, and other apps' notifications are untouched
