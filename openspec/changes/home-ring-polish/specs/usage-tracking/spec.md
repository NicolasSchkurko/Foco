# Spec Delta

## ADDED Requirements

### Requirement: Moving a ring position is animated
The home SHALL animate a change of ring position with a short horizontal slide and fade, using the
travel direction: forward for `›` and a left swipe, back for `‹` and a right swipe, and the ring
direction for a bar or the calendar.
[@test] `app/src/androidTest/java/com/foco/app/ui/DashboardRingTest.kt`

#### Scenario: Forward and back
- **WHEN** the user moves one position forward and then back
- **THEN** the content slides in from the forward side and then from the back side, while the header
  and the arrows stay in place

#### Scenario: A bar shortcut
- **WHEN** a week bar moves the home to that day
- **THEN** the content animates in the bar's ring direction instead of hard-cutting

### Requirement: The usage legend sits between the arrows
The screen-time and unlock line SHALL render between the `‹` and `›` controls, centered; the title
SHALL sit in the header without that subtitle, vertically centered with the calendar and settings
actions.
[@test] `app/src/androidTest/java/com/foco/app/ui/DashboardRingTest.kt`

#### Scenario: Legend geometry
- **WHEN** the home shows a day with a usage line
- **THEN** the line's bounds sit between the two arrow bounds and the title's vertical center matches
  the trailing actions

#### Scenario: The week has no usage line
- **WHEN** the home is on the week stop
- **THEN** the arrows remain visible with no day usage line and the week title is centered
