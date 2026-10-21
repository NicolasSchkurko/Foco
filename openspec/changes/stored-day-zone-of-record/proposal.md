# Proposal — stored-day-zone-of-record

## Why

A consolidated day is split two ways that can disagree after a zone change:

- `daily_aggregate` rows are keyed by the day as the zone saw it **at consolidation** (write time).
- `SessionHistoryStore.kt:83` re-slices stored raw sessions with **today's** zone when
  `sessionsFor(date)` answers; rows saved under an earlier offset can fall outside the day they
  were written for (the session "reappears" in the previous day).

So the week bar (reads `daily_aggregate`) and the detail/export (read `sessionsFor`) contradict each
other for those days. `DECISIONS.md:338` aligned pruning and reading on `ZoneId.systemDefault()` —
one calendar for prune and read — but **nobody decided which calendar owns a stored day written
under a different zone**. The specs are silent: no requirement says how a stored day keeps (or
loses) its zone. 🟡 spec-silent → /spec-add with an open decision.

## What changes (proposed contract)

- New requirement in `usage-tracking`: a stored day keeps the calendar it was consolidated with —
  stored sessions are sliced with the day's own recorded offset, so every reader answers the same
  day and a zone change moves only the days still being built, never the stored ones.
- Implementation follows the open decision (record the offset with the day — the proposal above —
  or accept current-zone semantics; both stated in `proposal.md`'s alternatives).

## Alternatives

| Option | Effect | Cost |
|---|---|---|
| Record the day's zone offset at consolidation; slice with it | `daily_aggregate` and `sessionsFor` always agree; stored days immutable | small schema bump (marker carries the offset), migration test |
| Slice everywhere with the current zone (status quo made explicit) | no schema change; week bar and detail still disagree for days written under another offset | the defect stays, only declared |
| Re-stamp stored rows on zone change | data rewrite on a settings event; runs against the retention policy | not proposed |

## Risks

- The proposal's option is a schema bump (v3 → next) — it must ship a non-destructive migration
  (`usage-tracking:575-586`) and a `MigrationTestHelper` proof.
- Zone changes are rare; without the record the mismatch is permanent for the affected days.

## Origin

Filed 2026-10-21 by the anchored bug-finder audit (finding 7, severity medium, 🟡 spec-silent);
`kind: feature` via /spec-add with one open decision — the human answers it (the delta text above
is a proposal, not a resolution). Awaiting human acceptance — no code written.
