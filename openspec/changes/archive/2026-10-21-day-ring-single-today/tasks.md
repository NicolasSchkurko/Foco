# Tasks — day-ring-single-today

- [x] 1. Delta `specs/usage-tracking/spec.md`: MODIFIED *The day ring wraps through the week* —
      add "today appears exactly once" to the text and the marker-for-today scenario; restate the
      five existing scenarios unchanged.
- [x] 2. `DayStop.kt:20-24` / `AvailableDays.kt:18`: build the ring from unique days with today as
      the single live head — a stored marker for today never appends a second position.
- [x] 3. Regression: `app/src/test/java/com/foco/app/domain/DayRingTest.kt` — a store with a
      marker for today yields one today position, and step forward from today lands on the week
      (the no-op case fails today).
- [x] 4. Trigger sanity: date/zone change backwards → ring still today → week → … with no repeat.
- [x] 5. `gradlew.bat test` green; `gate-check --run-tests` records evidence; `openspec validate
      day-ring-single-today --strict` passes.

## Riskiest slice first

Task 3's red-first composition test, then task 2.

## Deps

None. No other active change claims `DayStop.kt` / `AvailableDays.kt`.
