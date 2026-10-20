# Proposal — home-ring-polish

## Why

The ring now works, but the day change is a hard cut: a swipe or an arrow swaps the content with no
sense of direction. The header also carries the screen-time/unlocks line under the title, so the
title sits high in the header instead of aligning with the calendar and settings actions, and the
line is not visually tied to the arrows that move the day.

## What

- Wrap the home's content in `AnimatedContent` keyed by the current ring stop: a short horizontal
  slide plus fade, with the direction taken from the step (forward/back) or from the ring position
  when a bar or the calendar is used.
- Keep the title in `ScreenHeader` with no subtitle, so it is vertically centered with the trailing
  actions.
- Move the `dashboard_usage_line` (screen time + unlock count) into the arrows row, between `‹` and
  `›`, centered. On the week stop there is no legend, so the arrows stay at the content edges with a
  spacer between them.

## Expected outcome

Moving a day feels directional instead of instant, and the title reads centered with the calendar
and gear. The usage line is visually the label between the two arrows that change it.

## Alternatives considered

| Option | Why yes | Why not |
| --- | --- | --- |
| Directional slide + fade (chosen) | Cheap, reads as movement, no new dependency | Needs a direction state and a small transition spec |
| Crossfade only | Simplest | Loses the forward/back direction the ring already knows |
| Animate the whole screen including the header | One continuous surface | The header actions and title would slide too; the controls should stay put |
| Leave the subtitle in the header | No layout work | Keeps the title high and does not tie the legend to the arrows |

## Impact

- `DashboardScreen` only. No new dependency; Compose animation is already available.
- `DashboardRingTest` gains a geometry check for the legend between the arrows and the title aligned
  with the actions.
