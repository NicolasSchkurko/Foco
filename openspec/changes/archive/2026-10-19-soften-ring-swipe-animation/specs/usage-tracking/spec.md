# Spec Delta

## MODIFIED Requirements

### Requirement: Moving a ring position is animated
The home SHALL animate a change of ring position with a short horizontal slide and fade, using the travel direction: forward for `›` and a left swipe, back for `‹` and a right swipe, and the ring direction for a bar or the calendar. Swipe and arrow moves SHALL use a 320 ms base tween (multiplied by the system animator duration scale), an eighth-width slide and the standard ease-out; bar, calendar and back keep the existing directional slide/fade. When the system animator duration scale is 0, no transition SHALL run.
[@test] `app/src/androidTest/java/com/foco/app/ui/DashboardRingTest.kt`
[@test] `app/src/test/java/com/foco/app/ui/dashboard/RingMotionTest.kt`

#### Scenario: Forward and back
- **WHEN** the user moves one position forward and then back
- **THEN** the content slides in from the forward side and then from the back side, while the header and the arrows stay in place

#### Scenario: A bar shortcut
- **WHEN** a week bar moves the home to that day
- **THEN** the content animates in the bar's ring direction instead of hard-cutting

#### Scenario: Swipe and arrows are soft
- **WHEN** the user moves one position with a swipe or an arrow at scale 1
- **THEN** the content slides in by an eighth of the width over 320 ms with ease-out and fade, while the header and the arrows stay in place

#### Scenario: Reduced motion
- **WHEN** the system animator duration scale is 0
- **THEN** the position changes without a slide or fade

#### Scenario: Other entry paths
- **WHEN** a bar, the calendar or the back action changes the position
- **THEN** the existing directional slide/fade runs instead of the soft swipe spec
