# Tasks — week-metrics-expressivity

- [x] 1. Keep `WeekMetricsTest` green as the regression; add any case needed for an empty store and
      `daysUsed`.
- [x] 2. Index `storedTotals` by day and package once; walk the window once.
- [x] 3. Reuse `dayTotal` for the live day; preserve the ranking order, bars, absent states and
      `daysUsed`.
- [x] 4. Run `gradlew.bat test` and `openspec validate week-metrics-expressivity --strict`.
