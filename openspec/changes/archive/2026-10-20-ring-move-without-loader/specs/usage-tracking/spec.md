# Spec Delta

## MODIFIED Requirements

### Requirement: Moving a ring position is animated

The home SHALL animate a ring-position change with a short horizontal slide and fade in the travel
direction, and SHALL change what it shows only once the requested position's content is ready: the
position on screen SHALL stay — without a spinner or loading text — until then, so no loading
placeholder is composed during a move. Swipe and arrow moves SHALL use a 320 ms tween (scaled by the
system animator duration), an eighth-width slide and ease-out; bar, calendar and back keep the
existing slide/fade. At scale 0 no transition SHALL run. A loading placeholder SHALL appear only
while the home has no position to show yet.

[@test] `app/src/androidTest/java/com/foco/app/ui/DashboardRingTest.kt`
[@test] `app/src/test/java/com/foco/app/ui/dashboard/RingMotionTest.kt`

#### Scenario: Forward and back

- **WHEN** the user moves one position forward and then back
- **THEN** the content slides in from the forward side and then from the back side, while the header
  and the arrows stay in place

#### Scenario: A bar shortcut

- **WHEN** a week bar moves the home to that day
- **THEN** the content animates in the bar's ring direction instead of hard-cutting

#### Scenario: Swipe and arrows are soft

- **WHEN** the user moves one position with a swipe or an arrow at scale 1
- **THEN** the content slides in by an eighth of the width over 320 ms with ease-out and fade, while
  the header and the arrows stay in place

#### Scenario: Reduced motion

- **WHEN** the system animator duration scale is 0
- **THEN** the position changes without a slide or fade

#### Scenario: Other entry paths

- **WHEN** a bar, the calendar or the back action changes the position
- **THEN** the existing directional slide/fade runs instead of the soft swipe spec

#### Scenario: A move waits for its content

- **WHEN** the requested position's content is not in memory yet
- **THEN** the position already on screen stays visible with no spinner and no loading text, and the
  new content enters with the move as soon as its data is ready

#### Scenario: The week is a position too

- **WHEN** the user moves to or from the week
- **THEN** the week is composed only with its stored totals and today's live day already read, so no
  loading placeholder appears during the move

#### Scenario: The first load

- **WHEN** the home has not shown any position yet
- **THEN** the loading placeholder is shown until that position's content is ready
