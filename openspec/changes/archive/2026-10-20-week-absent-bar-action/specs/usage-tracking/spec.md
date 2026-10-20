# Spec Delta

## ADDED Requirements

### Requirement: A no-data week bar opens an empty day
A week bar whose day has no stored data SHALL remain tappable and SHALL open that day as a shortcut outside the ring's stored positions, with its day title and empty state. The shortcut SHALL NOT be added to the ring; back SHALL return to the previous position, and a store update SHALL not eject it while the date stays inside the seven-day window.
[@test] `app/src/androidTest/java/com/foco/app/ui/WeekScreenTest.kt`
[@test] `app/src/androidTest/java/com/foco/app/ui/DashboardRingTest.kt`

#### Scenario: A bar with data
- **WHEN** the user taps a bar for a stored or live day
- **THEN** the home opens that day's ring position

#### Scenario: A no-data bar
- **WHEN** the user taps a bar marked absent
- **THEN** the home shows that date with the empty state, without adding it to the ring

#### Scenario: Back from an empty day
- **WHEN** the user presses back while an empty shortcut day is showing
- **THEN** the home returns to the previous position, not to a synthetic ring step

#### Scenario: A store update while the empty day is open
- **WHEN** a stored day arrives or is pruned and the open date is still inside the seven-day window
- **THEN** the empty shortcut day stays open
