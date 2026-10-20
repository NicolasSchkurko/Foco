# Tasks — week-export-normalization

- [x] 1. Add a failing `WeekExportTest` scenario: a stored day with two same-app sessions meeting at
      the same `HH:mm` must export one session.
- [x] 2. Apply `mergeContiguousSessions` to each day's sessions inside `buildWeekExport`, before
      grouping and summing.
- [x] 3. Keep today idempotent and keep the stored-total fallback; do not change visibility or
      exclusion rules.
- [x] 4. Run `gradlew.bat test` and `openspec validate week-export-normalization --strict`.
