# Proposal — consolidation-raw-reset

## Why

`SessionHistoryStore.consolidateDay` upserts the new raw rows but never deletes the day's previous
rows. A process death after raw/totals but before the marker leaves the day pending; the next run
reads the day again. If the read changes (a late platform event, or the new session-merge shape),
the old rows with other `start` values remain. The detail merges them and the export reads them raw,
so the day can contain duplicate/inflated sessions while its totals are correct.

## What

Add a DAO operation that deletes the raw rows of one day and call it before writing the day's new
rows in `consolidateDay`. The order stays raw → totals → marker; a failure before the marker still
leaves the day pending. Same schema, same retention.

## Expected outcome

A retry after a marker failure replaces the day's raw rows with the latest read instead of adding a
second set; the detail and export see the same sessions as the totals; a repeated identical read
remains idempotent.

## State and persistence mechanism

The existing `session_record` table in `foco.db`; the delete is scoped to the day being consolidated.
No schema migration, no new table, no retention or backup change.

## Alternatives considered

| Option | Why yes | Why not |
| --- | --- | --- |
| Delete the day's rows before the raw upsert (chosen) | Simple; makes the retry a replacement; keeps marker-last | One extra query per consolidated day |
| Rely on `packageName + start` upsert | No schema change | Does not remove rows whose starts changed |
| Wrap the writes in a transaction | Atomic | The marker already models completion; a transaction alone does not remove stale rows |
