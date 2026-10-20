# Tasks — midnight-today-rollover

- [x] 1. Add a `TodayRolloverTest` with an injectable clock: a screen composed before midnight and
      resumed after it must use the new date.
- [x] 2. Add one current-day state/source that refreshes on resume and when the date changes; pass it
      to home, detail and week.
- [x] 3. Make `WeekExportService` read the live sessions with `loadDay(today)`, so `weekEnd` and the
      live entry agree.
- [x] 4. Re-run `DashboardRingTest`, `WeekScreenTest`, `AppDetailNavigationTest` and the new test.
- [x] 5. Run `openspec validate midnight-today-rollover --strict` and the gate.
