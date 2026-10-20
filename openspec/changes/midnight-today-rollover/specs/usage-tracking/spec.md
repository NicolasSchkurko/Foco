# Spec Delta

## ADDED Requirements

### Requirement: The current day follows the clock
The home, detail, week and export SHALL use the device's current date and SHALL roll it over when the date changes while the app is open, including on resume, so today's live data is never labeled with the previous date.
[@test] `app/src/androidTest/java/com/foco/app/ui/TodayRolloverTest.kt`
[@test] `app/src/androidTest/java/com/foco/app/ui/WeekScreenTest.kt`

#### Scenario: The app is resumed after midnight
- **WHEN** the app was showing today before midnight and is resumed after it
- **THEN** the home title/ring, detail selector, week window and export `weekEnd` use the new date

#### Scenario: The live read and the label agree
- **WHEN** the home shows today after the rollover
- **THEN** the live day read, the ring's today stop and the header date are the same day

#### Scenario: The export stays on one day
- **WHEN** the export is prepared after the rollover
- **THEN** its live sessions come from the same `today` used for `weekEnd`, not from a second clock read
