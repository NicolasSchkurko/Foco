# Proposal — cap-day-ring

## Why

`prune-stored-history` keeps eight days in the store: seven visible plus one guard day, so a
lagging consolidation can still rebuild the window. The detail already caps what it offers
(`availableDays`, seven at most), but the home's ring lists **every** stored day
(`domain/DayStop.kt:12-17`), so the guard day shows up as an eighth position. The device check on
2026-10-20 reproduced it: with markers for `2026-10-13 … 2026-10-19`, the home stepped back to
`Mon 12 Oct` (the guard day) after `Tue 13 Oct`.

## What

- `dayRing` keeps only the stored days inside the detail's seven-day window: the guard day is
  stored, never offered. It reuses `availableDays` so the window has one owner.
- No data change: the guard day stays in all three tables, pruned at eight days as before.
- `DayRingTest` pins both sides: the guard day is excluded and the ring still wraps.

## Expected outcome

The home offers today plus six finished days, exactly like the detail and the week; the seventh
step back from the oldest day lands on the week. Worth it when the emulator check shows `Wed 14 Oct`
as the oldest position (today = 2026-10-20) and `Mon 12 Oct` / `Tue 13 Oct` unreachable.

## Prior decisions

Chosen by the human on 2026-10-20 ("Capar el anillo a 7"): seven visible, eight stored. It is a
`fix`, not a feature: the living ring requirement promised "the oldest stored day", and the guard
day made that literal reading visible.
