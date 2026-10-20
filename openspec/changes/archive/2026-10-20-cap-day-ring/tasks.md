# Tasks — cap-day-ring

## 1. The ring reads the same window as the detail

- [x] In `domain/DayStop.kt`, filter the stored days through `availableDays` so the ring only offers the seven-day window; the guard day stops being a ring position.
- [x] Extend `DayRingTest`: a stored guard day is excluded, and the step forward from the week still lands on the oldest offered day.

## 2. Verification

- [x] `gradlew.bat test` green (126 tests).
- [x] Emulator check (2026-10-20, today = 20/10): markers for 13/10 … 19/10; the ring stops at `Wed 14 Oct` (today − 6) and the seventh ‹ wraps to the week; 12/10 pruned, 13/10 kept in the database.
- [x] `openspec validate cap-day-ring --strict`, `node scripts/gates/gate-check.mjs`, `node scripts/gates/build-board.mjs`.
