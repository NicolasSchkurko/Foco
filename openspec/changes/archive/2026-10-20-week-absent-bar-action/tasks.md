# Tasks — week-absent-bar-action

- [x] 1. Allow `DashboardScreen.moveTo` to accept a `DayStop.Day` inside the seven-day window even when
      it is not a ring position, pushing it to the history.
- [x] 2. Keep an open off-ring day in the ring-change fallback while its date is inside the window;
      only fall back to today when the position is truly invalid.
- [x] 3. Do not add the empty day to `DayRing`; keep the ring step order and the “no data” marker.
- [x] 4. Extend `WeekScreenTest` (absent bar opens the empty day and back returns) and
      `DashboardRingTest` (a stored bar still moves the home; an absent bar does not mutate the ring).
- [x] 5. Run the UI tests and `openspec validate week-absent-bar-action --strict`.
