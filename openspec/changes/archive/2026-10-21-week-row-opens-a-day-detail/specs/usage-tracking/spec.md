# Spec Delta — week-row-opens-a-day-detail

## ADDED Requirements
### Requirement: A week app row opens today's detail
An app row in the week's ranking SHALL open that app's detail for **today**, the same day the week position
summarises. A dated day view is untouched: its app rows keep opening that app's detail for their own day.
[@test] `app/src/androidTest/java/com/foco/app/ui/WeekScreenTest.kt`

#### Scenario: A week row opens today
- **WHEN** an app row of the week's ranking is tapped
- **THEN** the app's detail opens for today, not for another day of the week

#### Scenario: A dated day keeps its own day
- **WHEN** an app row of a dated day view is tapped
- **THEN** the app's detail opens for that day
