# Proposal — soften-ring-swipe-animation

## Why

The ring's swipe/arrow transition currently uses the Compose default spring and a quarter-width
slide. It reads as a hard, fast swap. The user wants a softer motion for swipes and arrows only,
without changing the header or the direction semantics.

## What

- Add a pure `RingMotion` spec: 320 ms base duration, an eighth-width slide, and the standard
  ease-out; the existing fade stays.
- Use that spec only for swipe and arrow moves. Calendar, bar and back keep the existing directional
  slide/fade.
- Read the system animator duration scale once per home composition through a `data/MotionScale`
  adapter: scale 0 means no transition; a positive scale multiplies the base duration.
- Keep the direction state and the `AnimatedContent` structure; no new dependency.

## Expected outcome

Swipe/arrow moves are visibly slower and shorter without a bounce, the content still enters from the
direction of travel, and the header/arrows stay put. With system animations disabled, the position
changes without a transition.

## State and persistence mechanism

No persistent state. `MotionScale` reads `Settings.Global.ANIMATOR_DURATION_SCALE` from the system
(no permission, no new store); the value lives only in the home composition. The 320 ms constant and
the 1/8 divisor live in `RingMotion`; no database or backup change.

## Alternatives considered

| Option | Why yes | Why not |
| --- | --- | --- |
| Soft tween + eighth-width slide for swipe/arrows (chosen) | Directly answers the request; direction preserved | Needs two transition branches |
| Softer spring | Natural deceleration | Harder to pin and to scale with the system setting |
| Crossfade only | Simplest | Loses the direction the ring already has |
| Apply the soft spec to every entry path | One branch | The user scoped it to swipe and arrows |

## Impact

- `DashboardScreen`, new `RingMotion`, new `MotionScale` data adapter, and their tests.
- No protected zone (`openspec/config.yaml` → `protected: []`); no new dependency.
