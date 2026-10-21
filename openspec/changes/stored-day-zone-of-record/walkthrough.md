# Walkthrough — stored-day-zone-of-record

## Mechanism

Consolidation writes a day in two tables: raw sessions (epoch instants) and per-app totals keyed by
a **date** chosen with the zone in effect at that moment; the day's marker closes the triplet. The
read side slices raw sessions again: `SessionHistoryStore.sessionsFor(date)` computes the day's
start/end with **today's** zone (`SessionHistoryStore.kt:83`) and keeps the epochs that fall inside.
If the device changed zone between write and read, that slice is shifted relative to the key the
totals were stored under, so the week bar (totals) and the detail (sessions) disagree, and a session
can show up in the neighbouring day. The proposed contract makes the day's recorded offset the
calendar its sessions are read with; the day becomes immutable at write time, which is what the
totals side already assumes.

## Physical traceability

- `app/src/main/java/com/foco/app/data/SessionHistoryStore.kt:83` — the re-slice with today's zone;
  `:56-57` — the not-stored distinction this change must not blur; `:32-37` — prune (untouched).
- `app/src/main/java/com/foco/app/data/db/FocoDatabase.kt` — where the day's offset would be
  recorded (marker row) if the decision takes the proposal; migration + exported schema follow the
  no-destructive policy (`usage-tracking:575-586`).
- `app/src/test/java/com/foco/app/data/HistoryDayZoneTest.kt` — the JVM proof (fakes already exist
  in `HistoryFakes.kt`).
- `DECISIONS.md:338` — prune and read on `ZoneId.systemDefault()`; this change does not reverse it.

## Limits and failure modes

- Days consolidated before the offset record exists have no offset: the read falls back to the
  current zone for them (their window is short — retention keeps seven days), stated in the tasks.
- The schema bump (if the decision takes the proposal) must ship a `MigrationTestHelper` proof and
  a committed schema export; the parallel `consolidation-totals-reset` shares the store, hence the
  declared dep.

## Touched APIs

Room (schema + query), `java.time` zone math. No network, no new dependency, no platform API
beyond what the store already uses.

## Implicit decisions

- Pruning and the read window stay on the current zone (`DECISIONS.md:338` stands); only the
  day-slice gains the recorded calendar.
- The decision is the human's (open question in `.openspec.yaml`): the delta and this walkthrough
  describe the proposed side; nothing is resolved until an ADR says so.
