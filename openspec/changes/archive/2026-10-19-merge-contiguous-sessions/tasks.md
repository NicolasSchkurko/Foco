# Tasks — merge-contiguous-sessions

- [x] 1. Add `SessionMergeTest` for the pure rule: same `HH:mm` with different seconds, a transitive
      chain, different app, different local day, and a list already ordered by start.
- [x] 2. Add `mergeContiguousSessions(sessions, zone)` in `domain/`; keep it independent of Android
      and of storage.
- [x] 3. Apply it in `UsageRepository.loadDay` after `clampSessionsToDay`/`filterSessions`; the live
      `DayData` carries the normalized list.
- [x] 4. Apply it in `appDayDetail` before `visibleSessions`, so stored raw rows are normalized on
      read; keep the total, hourly chart and week aggregates unchanged.
- [x] 5. Extend `AppDetailMetricsTest` with the 03:44/03:44 and 04:00–04:30/04:30 chains and the
      500 ms + 500 ms visibility boundary.
- [x] 6. Decide the export boundary (merged or raw) and update `weekly-json-export` accordingly.
- [ ] 7. Run `gradlew.bat test`, `AppDetailMetricsTest` and the detail instrumentation tests; verify
      on the Pixel that the detail lists one row for a contiguous pair.
- [x] 8. Run `openspec validate merge-contiguous-sessions --strict` and
      `node scripts/gates/build-board.mjs`.
