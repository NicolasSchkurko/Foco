# Walkthrough — soften-ring-swipe-animation

## Mechanism

`MotionScale.scale()` reads `Settings.Global.ANIMATOR_DURATION_SCALE` once when the home enters
composition. `DashboardScreen` keeps that value in `motionScale` and sets `softMotion` only from
`moveStep`, which is the path used by the arrows and the horizontal swipe. The bar, calendar and back
leave `softMotion` false.

`AnimatedContent` chooses one of three transitions:

- `motionScale <= 0`: no transition (reduced motion).
- `softMotion`: a 1/8-width slide plus fade, with a `tween` of `RingMotion.durationMillis(scale)` and
  `FastOutSlowInEasing`.
- Otherwise: the existing directional slide/fade.

`RingMotion` owns the 320 ms base, the 1/8 divisor and the easing; `durationMillis(0)` is clamped to
1 so only the explicit zero-scale branch disables the transition. The header and arrows are outside
the animated content, so they stay in place.

## Physical traceability

`Settings.Global.ANIMATOR_DURATION_SCALE` (Android system setting) → `MotionScale.scale()` →
`motionScale` in `DashboardScreen` → `transitionSpec` in `AnimatedContent` → the slide/fade of the
day/week content. The 320/8/ease-out values live in `RingMotion`; the direction comes from the
existing `slideDirection`.

## Limits and failure modes

- The scale is read once per home composition. Changing it while the home is already open may need a
  trip away and back (or an app restart); this is deliberate to avoid a settings observer for a
  cosmetic value.
- `Settings.Global.getFloat` falls back to `1f` if the setting is absent; a scale of 0 disables the
  transition, and a positive scale multiplies the 320 ms base. The duration never drops below 1 ms.
- Only swipe and arrow moves get the soft spec; bar, calendar and back keep the previous transition,
  as scoped by the human.
- No new dependency, no permission, no persistence. The setting is read, not stored; `allowBackup`
  and the no-network posture are unchanged.
- The animation is decorative: if it is skipped or reduced, the position still changes and the data
  loads normally.

## Touched APIs

- `android.provider.Settings.Global.ANIMATOR_DURATION_SCALE` (system animator scale).
- Compose `AnimatedContent`, `slideInHorizontally`, `slideOutHorizontally`, `fadeIn`, `fadeOut`,
  `tween`, `EnterTransition.None`, `ExitTransition.None`.

## Tests

- `RingMotionTest` pins 320 ms, the 1/8 divisor, the easing and the scale multiplication.
- `DashboardRingTest` walks arrows, swipes, the calendar and Settings; it passes with the system
  animation scale at 1 and at 0 on the emulator.

## Implicit decisions

- The soft spec is only for swipe and arrow moves; bar, calendar and back keep the previous
  transition, as the human scoped it.
- 320 ms, 1/8 width and ease-out are the chosen values; they live in `RingMotion`, not in the screen.
- A system scale of 0 means no transition; a positive scale multiplies the base duration.
- The scale is read once per home composition; there is no settings observer and no stored copy.
- The header and arrows stay outside the animated content.

## Cognitive sign-off

Signed off by Nschkurko on 2026-10-19.

1. Before it was a spring with a 1/4-width slide, hard and fast. The change uses a 320 ms tween with
   ease-out (decelerates, no bounce) and 1/8 width (half the travel): softer and shorter. The values
   live in `RingMotion.kt:9-12`. The rest was not changed because the request covered only swipes and
   arrows.
2. The day changes the same because what changes is the `stopKey` state, not the animation;
   `AnimatedContent` only chooses how to show it, and `None` shows it immediately.
3. It is not persisted because it is a global system setting, not an app setting; it is read in
   `MotionScale.scale()` from `Settings.Global.ANIMATOR_DURATION_SCALE`.
