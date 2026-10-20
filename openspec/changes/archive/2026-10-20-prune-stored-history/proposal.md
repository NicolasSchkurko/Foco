# Proposal — prune-stored-history

## Why

The store only ever deletes one table: raw sessions older than seven days
(`data/SessionHistoryStore.kt:36`, cutoff at `:74-75` from `SIX_DAYS = 6`). `daily_aggregate` and
`consolidated_day` are never deleted — `data/db/DailyAggregateDao.kt:9` and
`data/db/ConsolidatedDayDao.kt:9` ("No delete: retention keeps it") declare no delete at all — so the
database grows one row per app per day and one row per day, forever. That data is unreachable the
day it is written: the week reads a seven-day window (`SessionHistoryStore.kt:39-41`) and the day
selector offers seven days at most (`domain/AvailableDays.kt:10`). Whatever leaves that window is
never read again, and keeping it only costs space.

## What

- One rule for everything stored: **rows older than the window are deleted on every consolidation
  run** — raw sessions, per-app daily totals, and the day's marker with its unlock count.
- The window keeps **today and the seven days before it (eight days)**: the seven the UI shows, plus
  one day of guard so a lagging consolidation can still rebuild the window.
- The **read** window does not change: the week still reads seven days, the day selector still offers
  seven at most, the ring is untouched.
- The guard gets its own constant; `SIX_DAYS` keeps meaning the read window only, so the two no
  longer share one number.

## Expected outcome

After eight days of use the database stops growing: it holds at most eight days of sessions, totals
and markers, and nothing older. Nothing the UI shows changes. A day that leaves the window leaves
all three tables in the same run, so no orphan totals or counts stay behind.

Worth it when: `foco.db` has a flat size over time, the ring and the detail still show the same
seven days, and a day still inside the window still answers its totals and its unlock count.

## State and persistence mechanism

No new store and no schema change: the same `foco.db` (Room v3), the same three tables. The change
adds two `DELETE` queries (totals and markers, keyed by the ISO day text both tables already use)
and moves the raw cutoff from six to seven days behind today. Pruning stays inside
`SessionHistoryStore.consolidate` (`:32-37`), which both triggers already share through
`ConsolidationRunner` (app start and the nightly job), so a run that fails still prunes nothing
(`openspec/specs/usage-tracking/spec.md:546-556`). No migration, no new dependency, no file cache.

## Alternatives considered

### A. What gets pruned

| Option | Why yes | Why not |
| --- | --- | --- |
| Everything outside the window: raw rows, totals, markers (requested) | The database stops growing; nothing kept is readable anyway | Reverses "pruning SHALL never remove a total" (`spec.md:234`) and an ADR, so it needs an explicit approval; a day outside the window can no longer be re-derived |
| Only raw rows, widened to eight days | Keeps the totals-forever invariant untouched | The tables that actually grow (one row per app per day) keep growing forever — that is the space the request is about |
| Keep today's rule (raw at seven days, totals forever) | No change, no conflict | Exactly what the request rejects |

### B. Where the prune happens

| Option | Why yes | Why not |
| --- | --- | --- |
| Inside `SessionHistoryStore.consolidate`, so both triggers prune (chosen) | One policy in one place; the app-start run also cleans up after a week of Doze, a force-stop or a battery saver | Not literally "each worker" |
| Only inside `ConsolidationWorker.doWork` | The literal reading of "en cada worker" | If the job never runs, nothing is pruned and the space keeps growing: the exact complaint. The app-start trigger would silently stop pruning |
| A separate periodic vacuum job | Pruning decoupled from consolidation | A second job to schedule, serialize and test for no gain |

## Conflicts (⚔) — resolved

This change reverses what was written, and the reversal was approved on 2026-10-19
("resolvamos el conflicto"). The ADR is `DECISIONS.md` → *Reversal approved: every stored row leaves
with the window*.

What it reversed:

1. `openspec/specs/usage-tracking/spec.md:234` — "keep the per-app, per-day totals of every
   consolidated day; pruning SHALL never remove a total", with its scenario *Totals are kept*
   (`:241-243`); and `:300` — the unlock count "is never pruned with the raw sessions".
2. `DECISIONS.md:317` — the retention choice, "raw sessions for seven days with the per-app totals
   kept for every consolidated day" — and `DECISIONS.md:353` — the count "is never pruned, because
   outliving the platform's window is its whole point".

What it did **not** reverse: the store still guarantees the seven-day window when the platform
serves fewer days, a failed run still prunes nothing, consolidation still runs through one
serialized runner, and no migration is destructive.

## Decisions taken

1. **Where the prune happens: on every consolidation run** — app start and the nightly job — as
   `consolidate` already does for raw rows. Decided on 2026-10-19; recorded in `DECISIONS.md`.

Still open: nothing structural — the reversal is approved (see *Conflicts* below); the change only
awaits the human's acceptance before any code.

## Prior decisions

Nothing else is reversed. `DECISIONS.md:323-325` stays true: the nightly job is "a trigger, not a
second policy", and a failed run prunes nothing. `DECISIONS.md:318` (no
`fallbackToDestructiveMigration`) is untouched — this is a data policy, not a schema change.

## Overlaps in flight

`consolidated-day-marker` (accepted, apply in flight; its schema is already on `main` —
`data/db/FocoDatabase.kt:14` is version 3 with `ConsolidatedDay`) claims the same files:
`data/SessionHistoryStore.kt`, `data/db/ConsolidatedDayDao.kt` and four of the tests. Declared as a
dep, so the gate reads the overlap as **sequenced**: the marker lands first and this prune is
written once against the final tables (`daily_unlock` is gone, the count lives in
`consolidated_day`). Both changes touch the unlock-count requirement in the living spec; sequencing
is what keeps that a merge in order instead of a race.
