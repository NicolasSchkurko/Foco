# Proposal

## Why

`SessionHistoryStore.consolidateDay` replaces a day's raw rows on retry but only upserts `daily_aggregate` rows by primary key, so a package that disappears from a later read keeps a stale total forever (the marker is still written, so the day is never retried). An empty read after a partial write returns early and freezes the earlier rows. This makes the week totals disagree with the stored sessions.

## What Changes

- `SessionHistoryStore.consolidateDay` clears that day's aggregates before upserting the new ones, and clears raw rows + aggregates (without marker) when the new read is empty.
- `DailyAggregateDao` gains a per-day delete used by the store.
- The living requirement *A consolidation retry replaces the day's raw rows* is extended to cover totals and the empty-read case.

## Capabilities

### Modified Capabilities
- `usage-tracking`: the retry requirement now covers per-app totals and the empty-read-after-partial-write case.

## Impact

- `app/src/main/java/com/foco/app/data/SessionHistoryStore.kt`
- `app/src/main/java/com/foco/app/data/db/DailyAggregateDao.kt`
- `app/src/test/java/com/foco/app/data/HistoryConsolidationTest.kt`
- `app/src/test/java/com/foco/app/data/HistoryFakes.kt`

## Expected outcome

A retried day's totals match exactly the new read; an empty retry leaves no rows, totals or marker; the week view and the stored sessions agree.

## State and persistence mechanism

Reuses the existing `foco.db` Room database and the existing `daily_aggregate` table. Adds a per-day `DELETE FROM daily_aggregate WHERE day = :day` executed inside `consolidateDay` before the upsert, mirroring the existing `deleteBetween` on `session_record`. No new table, no migration, no new dependency.