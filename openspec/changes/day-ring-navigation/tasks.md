# Tasks — day-ring-navigation

Riskiest first: the ring is a pure function, so it is built and proven first, then the home is made to
render by position, and only then the gestures are attached.

## 1. The ring (hardest, no UI)

- [x] 1.1 `domain/DayRing.kt`: `DayStop` (`Day(date)` / `Week`), `dayRing(today, storedDays)` in
      forward order, and `step(ring, forward)` with the wraps.
- [x] 1.2 `DayRingTest` (JVM): `today → week`, `week → oldest`, `oldest → week` going back, forward
      from yesterday lands on today, and a store with no finished day yields `[today, week]`.
- [x] 1.3 The history: `DayRingTest` proves pushing a step and popping it returns the previous
      position, that a bar tap pushes one step like a swipe, and that a vanished position drops out of
      both the ring and the history.

## 2. The home renders by position

- [x] 2.1 The home keeps the current `DayStop` in composition state and builds the ring from the store
      stream (reusing `storedDaysFlow`), falling back to today when the position vanishes.
- [x] 2.2 A `Day` position renders the composition the dated home renders today (live for today, one
      stored read otherwise, no refresh loop for a past day).
- [x] 2.3 The `Week` position renders the week's content; tapping a bar moves the home to that day's
      position instead of navigating.
- [x] 2.4 The calendar control jumps to the `Week` position; the settings gear stays next to it; the
      `day/{date}` and `week` routes are deleted from `FocoNavHost`.

## 3. Gesture and arrows

- [x] 3.1 `‹ ›` visible on every position, disabled nowhere (the ring wraps), each moving one step.
- [x] 3.2 A horizontal swipe anywhere on the content moves one step, with a committed threshold and
      the dominant axis deciding against the vertical list.
- [x] 3.3 Nothing is pushed onto the navigation stack when the position changes; the position (and its
      history) survive a trip to Settings.
- [x] 3.4 The back action pops the history one position at a time, and from today with an empty
      history it leaves the app.

## 4. Tests

- [x] 4.1 `DashboardRingTest` (instrumented): swipe each way, the arrows, the calendar control, that
      back retraces the positions visited one per press, and that Settings returns to the position it
      was opened from.
- [x] 4.2 `WeekScreenTest` updated for the week as a position.

## 5. Verification

- [x] 5.1 `gate-check --run-tests` green (JVM + instrumented).
- [x] 5.2 `openspec validate --all --strict` clean.
- [x] 5.3 The reversal recorded in `DECISIONS.md` (2026-10-19, the home is a ring) stays the reference
      for the delta, before `verified`.
- [ ] 5.4 On the emulator: the ring walked end to end with swipes and with arrows, including both
      wraps, and the week still opens from the calendar control.
