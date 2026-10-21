# Tasks

## 1. DAO

- [ ] 1.1 Add `deleteForDay(day: String)` to `DailyAggregateDao` and its fake.

## 2. Store

- [ ] 2.1 In `consolidateDay`, delete that day's aggregates before upserting.
- [ ] 2.2 In `consolidateDay`, when the read is empty, delete raw rows and aggregates for the day and return without writing the marker.

## 3. Tests

- [ ] 3.1 Extend `HistoryConsolidationTest` with the two new scenarios (vanished package, empty retry).