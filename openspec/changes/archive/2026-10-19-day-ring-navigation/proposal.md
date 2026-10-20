# Proposal — The home walks the days as a ring

## Why

Reading a past day costs three taps: the calendar control, the week, the bar (`WeekScreen.kt:263`).
The day arrows exist only inside an app's detail, and the home has none. And the model behind those
screens is stacked: the week is a route you enter and leave, a day is another route opened from it
(`FocoNavHost.kt:34-57`), so "the day before this one" is not a step but a navigation decision.

The human's model is the opposite: the home is one place, and the day it shows is a position. Moving
one position either way — with a swipe anywhere on the content or with visible arrows — reads the
day before or the day after, and the week is one of those positions.

## What Changes

- The home keeps **one** day view and changes the day **in place**: no screen is stacked when the day
  changes, and the `day/{date}` and `week` routes are removed — the ring is the only way to a day and
  to the week.
- A bar tapped in the week is a **shortcut**, not a step down a hierarchy: it moves the home to that
  day's position, and nothing records the week as that day's parent.
- The back action **retraces the positions visited**, one per back, and from today with nothing behind
  it leaves the app. Settings returns to whatever position was showing when it was opened.
- The positions form a **ring**: `today → week → oldest → the day after it → … → yesterday → today`.
  Forward from today lands on the week; forward from the week lands on the oldest stored day; back
  from the oldest day lands on the week.
- Every position moves with a **horizontal swipe anywhere on the content** and with a visible
  **‹ ›** control, which is the same pair the detail already offers.
- The calendar control stays: it jumps straight to the week position.
- The day ring is a pure function in `domain/DayRing.kt`, so the order and the wraps are testable
  without a device.

## State and persistence mechanism

- **Where**: in memory, in the home's composition state (`remember`) — the selected position of the
  ring, a `LocalDate` plus the "week" stop. Nothing is written to disk and nothing new is stored.
- **Reads**: the existing ones. A day position reads like the dated home does today (stored sessions
  for a past day, the live read for today, `DashboardScreen.kt:103-127`); the week position reuses
  `WeekScreen`'s reads.
- **Lifecycle**: the state lives while the home is in composition; leaving the app and coming back
  re-enters at today, exactly as the dashboard does now.

## Expected outcome

Moving between days costs one gesture instead of three taps, and the week is somewhere you land
instead of a screen you visit. We will know it was worth it when a day change is a single swipe from
the home and when the calendar control is no longer the only way into the week.

## Alternatives considered

| Option | Why yes | Why not / status |
|---|---|---|
| **One home that changes the day in place, with the week as a stop (chosen)** | The day change is a position, not a navigation; the week shares the same state; no stack growth | The home grows a container state and the gesture needs care around the lists |
| Navigate to the existing `day/{date}` route on each step, with `popUpTo` to keep the stack flat | Reuses the dated screen as it is; the code stays route-shaped | Every swipe becomes a navigation event (back-stack churn, lost state, slower) — **rejected by the human** ("cambia de día en el lugar") |
| A `HorizontalPager` over the days | Native gesture, free animation | A pager composes neighbouring pages and re-reads the store per page; the ring wraps through the week, which is not a page of the same shape; heavier than one state field |
| Keep the calendar control only | No new interaction to design | The human wants the calendar to be optional, not the only door |

## Non-goals

- **No change to the app detail's day selector**: this change is the home. The detail's arrows keep
  working as they do.
- **No page animation**: the day swaps in place; a pager/animation is a later decision.
- **No persistence of the selected position**: the home always re-enters at today.
- **No new dependency**: the swipe uses `pointerInput` from Compose foundation, already present.

## Impact

- The `day/{date}` and `week` routes are deleted: with the ring, they would be a second way to the
  same content, and the human chose to throw them out.
- `WeekScreen.kt` is shared by two changes: this one and `header-alignment` (declared dep, sequenced).
- The living requirement "A day bar opens that day's view" contradicts this model; the reversal is
  recorded as a ⚔ conflict and needs the human's explicit approval plus an ADR before `verified`.
- No protected zone (`openspec/config.yaml` → `protected: []`).
