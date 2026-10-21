# Proposal — day-ring-single-today

## Why

The ring can hold **today twice**, and then forward from today is a silent no-op:

- `DayStop.kt:20-24` appends the "today" position from the store's day marker when one exists for
  today, while `AvailableDays.kt:18` already includes today as the live head → the ring reads
  `[today, week, …, today]`.
- `DayRingHistory.kt:11` resolves positions by first `indexOf` occurrence → step forward from
  today lands on today again → `next == current`, nothing moves.

Triggerable with a date/zone change backwards (today gets a marker while still being "today").
The living requirement (`usage-tracking:415-437`) describes the ring's composition — "hold today,
the week, the oldest stored day … and every finished day between them" — but never says today
appears **once**; the step rules it does state ("step forward from today SHALL land on the week")
are exactly what the duplicate breaks. 🟡 spec-silent on the uniqueness, violated in the step
consequence → /spec-add pinning both.

## What changes (proposed contract)

- MODIFIED requirement in `usage-tracking`: today appears exactly once (with or without a stored
  marker), keeping all existing text and scenarios, plus the marker-for-today scenario that pins
  the step.
- Implementation: deduplicate at ring construction (`DayStop`/`AvailableDays` seam), one source of
  truth for the live head.

## Alternatives

| Option | Effect | Verdict |
|---|---|---|
| Build the ring from a set of days (today first, marker never re-adds it) | duplicate impossible by construction | **Proposed** |
| Drop the marker in the store when it equals today | writes on a read path; loses today's stored count | not proposed |
| Special-case `next == current` to skip | hides the duplicate, ring still offers it twice via back | not proposed |

## Risks

- Pure domain change; `DayRingTest` already covers composition and steps — the new scenario is the
  regression.

## Origin

Filed 2026-10-21 by the anchored bug-finder audit (finding 13, severity low, 🟡 spec-silent);
`kind: feature` via /spec-add. Awaiting human acceptance — no code written.
