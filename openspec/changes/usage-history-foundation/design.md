# Design — Persistence foundation

## Context

The tracker reads `UsageStatsManager` on every screen (`data/UsageRepository.loadDay`) and keeps nothing. `PLAN.md` Phase 2 wants its own history; the platform's retention (~7 days) is the clock. The database has to answer one question first — *"what did app X do on day D?"* — for days the platform no longer serves, without changing how today is read.

## Shape

```
MainActivity.onCreate ──(IO, once)──► SessionHistoryStore.consolidate(today)
                                          │
                     availableDays(today).dropLast(today)   (pure, existing)
                                          │
                     (LocalDate) -> DayData                 (UsageRepository.loadDay)
                                          │
                    ┌─────────────────────┴─────────────────────┐
              SessionRecordDao.upsert                   DailyAggregateDao.upsert
                    │                                           │
              SessionRecordDao.pruneBefore(7 days)             (totals stay)
```

The UI keeps reading the platform for today; the database is a **backfill**, never a live source. That ordering is deliberate: a consolidation that lags by a day would make today's dashboard wrong, and the dashboard is the app.

## Schema

| table | columns | key | why |
|---|---|---|---|
| `session_record` | `packageName TEXT`, `start INTEGER`, `end INTEGER` | `(packageName, start)` | A session is identified by when it began in that app; `REPLACE` on the same key makes re-consolidation idempotent |
| `daily_aggregate` | `packageName TEXT`, `day TEXT` (ISO-8601), `totalMillis INTEGER` | `(packageName, day)` | One row per app per day: what the Week screen reads, and what survives pruning |

`Index("start")` on `session_record` is for the half-open range read and for pruning by `end`; without it, both scan.

## Test strategy

Room's generated DAOs need Android, but the **policy** does not. So the store depends on the two DAO *interfaces* and a `(LocalDate) -> DayData` lambda:

- JVM (`HistoryConsolidationTest`, `HistoryRetentionTest`): in-memory fakes implement the DAO interfaces; the policy (skip stored days, exclude today, prune cutoff, idempotence) is exercised without an emulator.
- Instrumented (`FocoDatabaseTest`): only what fakes cannot prove — that SQLite enforces both composite keys and that `@Insert(REPLACE)` overwrites.

This keeps the repo's JVM suite portable (`spec-tooling`: *JVM tests are portable across development platforms*) and confines the Android dependency to one instrumented file.

## Failure modes

- **KSP/AGP mismatch**: the build fails at configuration, not at runtime; task 1 proves it before any logic exists.
- **A day consolidated while still in progress**: excluded by dropping today; a day is final the next morning.
- **App not opened for more than seven days**: the gap is real and stays a gap — the platform no longer has those days either. The store does not pretend otherwise.
- **Clock/timezone change**: days are named as `LocalDate` (the same notion `dayWindow` already uses); a user crossing timezones may consolidate a day twice under two names, which is why the composite key and `REPLACE` matter.
- **Schema drift**: `exportSchema = true` plus the committed `1.json`; a later change adds `Migration(1, 2)` rather than wiping the file.

## Migration policy

Version 1 today, `exportSchema = true`, schema committed. `fallbackToDestructiveMigration()` is forbidden by decision: it deletes the users' history on a schema change, which is the one thing this change exists to protect. Migration tests open the previous schema and assert the data survives (they arrive with the first migration, not before).

## Open risks

1. **Version pinning** (Room + KSP against AGP 9.4 / Kotlin 2.4) — mitigated by task 1 and by not writing logic until the build is green.
2. **The foundation has no reader**: the database accumulates and only tests read it until the Week screen lands. Accepted because the alternative is losing the days that are still inside the window.
3. **No UI lock**: the data is sandboxed and not backed up, but anyone holding the unlocked phone can open Foco. That is a UI decision (biometric gate), out of scope, and deliberately not solved with encryption (out of scope by the proposal's non-goals).
