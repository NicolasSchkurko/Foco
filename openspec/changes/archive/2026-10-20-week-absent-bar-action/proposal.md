# Proposal — week-absent-bar-action

## Why

`WeekContent` makes every bar clickable, including `WeekDayBar.State.Absent` (“no data”). The home’s
`moveTo` only accepts ring stops, and the ring only contains stored days, so tapping an absent bar
silently does nothing. The user chose the other behavior: an absent bar opens an empty day.

## What

Keep the absent bar tappable and let the home open a **shortcut day** outside the ring’s stored
positions: the day gets its title, empty state and back history, but is not added to the ring. The
ring’s step order is unchanged; a store update does not eject an open shortcut day while it is inside
the seven-day window.

No new storage, no schema change.

## Expected outcome

A bar with data opens its day as before. A bar with no data opens that date with the empty state,
and back returns to the previous position. The absent bar keeps its “no data” marker and description.

## Alternatives considered

| Option | Why yes | Why not |
| --- | --- | --- |
| Open an empty day (chosen by the human) | Honest “every bar opens a day”; consistent with the detail’s empty state | The day is a shortcut outside the ring; the ring order cannot include it |
| Disable the absent bar | Simple; no synthetic stop | The user cannot inspect an empty day |
| Leave the dead tap | No code | Silent affordance; violates accessibility expectations |
