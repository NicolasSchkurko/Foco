# Spec Delta

## ADDED Requirements

### Requirement: The home changes the day in place
The home SHALL read one day at a time and change it in place, without stacking a screen: a horizontal swipe anywhere on the content and the visible ‹ › controls SHALL each move one position of the day ring, and the back action SHALL retrace the positions visited.
[@test] `app/src/androidTest/java/com/foco/app/ui/DashboardRingTest.kt`

#### Scenario: A swipe moves one day
- **WHEN** the user swipes horizontally over the home's content
- **THEN** the home shows the neighbouring day of the ring, with its own total, chart and ranking

#### Scenario: The arrows move the same way
- **WHEN** the user taps ‹ or ›
- **THEN** the home moves exactly one position in the same direction as the matching swipe

#### Scenario: Back walks the positions visited
- **WHEN** the home has moved through two positions and the system back action is used
- **THEN** the home returns to the position visited before, one step per back, and from today with nothing behind it, back leaves the app

#### Scenario: Settings returns to what was being viewed
- **WHEN** settings is opened from a position and closed with its back control
- **THEN** the home shows the position it was showing before settings was opened

### Requirement: The day ring wraps through the week
The ring SHALL hold today, the week, the oldest stored day and every finished day between them; the step forward from today SHALL land on the week, the step forward from the week on the oldest stored day, and the step back from the oldest stored day on the week.
[@test] `app/src/test/java/com/foco/app/domain/DayRingTest.kt`

#### Scenario: Forward from today
- **WHEN** the home is on today and the user steps forward
- **THEN** the week is shown

#### Scenario: Forward from the week
- **WHEN** the home is on the week and the user steps forward
- **THEN** the oldest stored day is shown

#### Scenario: Back from the oldest day
- **WHEN** the home is on the oldest stored day and the user steps back
- **THEN** the week is shown

#### Scenario: A store with no finished days
- **WHEN** the store holds no finished day
- **THEN** the ring is today and the week, and forward from the week returns to today

## MODIFIED Requirements

### Requirement: A day bar opens that day's view
The week view SHALL be reachable from the dashboard and SHALL be one position of the home's ring; tapping a day's bar SHALL move the home to that day's position — the dashboard composition for that date, read from stored sessions — where an app row opens that app's detail for the day.
[@test] `app/src/androidTest/java/com/foco/app/ui/WeekScreenTest.kt`

#### Scenario: Entry and return
- **WHEN** the week control is used in the dashboard
- **THEN** the home shows the week position, with no back control to leave it and no screen stacked

#### Scenario: Opening a day
- **WHEN** a bar is tapped
- **THEN** the home shows that day with its total, hourly chart and ranking
