# Proposal — The day's completion is one row, and that row carries the count

## Why

"Day consolidated" is inferred today from `daily_aggregate`: `consolidate` subtracts the days that
have per-app totals from the finished days (`SessionHistoryStore.kt:33-36`). That works, but it
couples *completion* to a table that is about totals, and it leaves the unlock count as the one
write without a safety net: the order is sessions → totals → count (`SessionHistoryStore.kt:63-65`),
so a process death between the totals and the count marks the day done and loses its count forever
(`openspec/changes/archive/2026-10-19-stored-day-unlocks/walkthrough.md:138-146`). Sessions and
totals heal because the day stays pending; the count does not, because the day does not.

The count is the smallest datum in the schema and the only one that can be lost; that asymmetry is
the bug. The fix is not another ordering rule under the same coupling, but one row that means "this
day is done" **and** holds the count.

## What Changes

- A new table, one row per consolidated day: the day (ISO text key, the shape `daily_aggregate` and
  `daily_unlock` already use) and its unlock count, **nullable** — a row whose count is unknown is
  still a finished day.
- That row is the **completion marker**: the pending set is `finishedDays - markers`, and the row is
  written **last**. A day interrupted before it is retried; a day with a marker was written whole.
- The unlock count moves into the marker and `daily_unlock` disappears: one table, one writer, one
  meaning.
- The schema leaves version 2: `MIGRATION_2_3` creates the marker, carries every stored count over,
  records the days that only had totals (count unknown) and drops `daily_unlock`.
- `storedDays` / `storedDaysFlow` (the detail's window) read the marker instead of the totals table;
  the value they answer is the same set.

## State and persistence mechanism

- **Where**: the same `foco.db`, built once per process by `SessionHistoryStore.forApp`
  (`SessionHistoryStore.kt:82-100`). Three tables become three: `session_record` (raw),
  `daily_aggregate` (per-app totals) and `consolidated_day` (one row per finished day + its count).
- **Writes**: only the consolidation path. Per day: sessions, totals, marker — the marker last, so it
  is the only write whose failure leaves the day pending.
- **Reads**: `storedDays()` / `storedDaysFlow()` from the marker; `unlocksFor(date)` from the marker's
  count; `week(until)` and `sessionsFor(date)` unchanged.
- **Upgrade**: `version = 3` with a hand-written `Migration(2, 3)`; no destructive fallback (the
  policy the foundation recorded). Days stored before the upgrade get a marker with an unknown
  count, so "not stored" (`null`) and `0` keep meaning what they mean today.

## Expected outcome

A count can no longer be lost: a finished day is either fully stored — sessions, totals and count —
or it stays pending and the next start retries it. And the question "is this day done?" has a single
row to answer it, instead of being inferred from a table about per-app totals. We will know it was
worth it when the crash window disappears from the walkthrough's limits instead of being documented.

## Alternatives considered

| Option | Why yes | Why not / status |
|---|---|---|
| **A marker row holding the count, pending read from it, `daily_unlock` folded in (chosen)** | Completion and the count are the same write, so the count cannot be lost; the pending set stops hanging off `daily_aggregate`; one table fewer | A migration (2→3) that must carry the stored counts and keep the unknown-count days honest |
| Reuse `daily_unlock` as the completion marker | No new table, no rename | Its `count` is `NOT NULL`, so the days that only have totals would need a fabricated `0` — exactly the "unknown looks like zero" lie the schema avoids. Making it nullable is the chosen migration, with a misleading name |
| Reorder the writes: count before totals | One line, no schema change; the count stops being the last write | Leaves completion inferred from the totals table and keeps an asymmetry that only works by convention — **rejected by the human in favour of the explicit model** |
| Wrap the three writes in a Room transaction | Atomic, no intermediate state | The store would have to depend on the database (or on an injected transaction seam) with fakes modelling it, and it still leaves "done" inferred from `daily_aggregate` |
| A nullable count column on `daily_aggregate` | No new table | That table is per app and day, so the count would repeat per app or sit null on all but one row — rejected for the same reason in `stored-day-unlocks` |

## Non-goals

- **No retro-fill of counts**: days consolidated before version 3 keep an unknown count (`null`),
  as they do today; the upgrade only records that they are finished.
- **No change to what the screens show**: `unlocksFor` keeps its `Int?` contract and its callers.
- **No transaction wrapper**: the marker makes the order sufficient; a transaction can come later if
  a stronger guarantee is ever needed.
- **No per-app counts and no timestamps** (unchanged from `stored-day-unlocks`).

## Impact

- `tech_brief: true`: a schema bump, a table created and one removed, and a data migration worth a
  walkthrough.
- Supersedes the ordering rationale recorded by `stored-day-unlocks` (its walkthrough's "the count is
  last precisely because it has no self-healing"); the count keeps its place in the order and gains
  the healing.
- Two files disappear (`DailyUnlock.kt`, `DailyUnlockDao.kt`) and their targets are listed so the gate
  sees the deletion.
- No protected zone (`openspec/config.yaml` → `protected: []`).
