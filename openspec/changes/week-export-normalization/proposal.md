# Proposal — week-export-normalization

## Why

The living export contract says stored days are normalized (`usage-tracking/spec.md` — “Normalized
records”), but the export path reads `WeekExportService.sessionsFor` raw and `WeekExportBuilder`
sums those rows directly. `mergeContiguousSessions` runs in the live repository and in the detail,
not in the export. A 03:44–03:44 pair on a stored day is exported as two sessions while the same
pair today is one.

## What

Apply `mergeContiguousSessions` to each day's sessions inside `buildWeekExport`, before grouping and
summing. The helper is idempotent, so today's already-normalized sessions are unchanged; stored
sessions and the stored-total fallback keep their existing behavior.

No storage change, no migration, no UI change.

## Expected outcome

A stored day with two same-app sessions meeting at the same `HH:mm` exports one session with the
earliest start and latest end; different apps/days stay separate; the export matches the detail and
the living contract.

## Alternatives considered

| Option | Why yes | Why not |
| --- | --- | --- |
| Normalize inside `buildWeekExport` (chosen) | One place; covers every caller; tests are pure domain | Slightly more work in the builder loop |
| Normalize in `WeekExportService` only | Smaller diff | A future caller of the builder can bypass the rule |
| Change the spec to raw records | No code | Contradicts the approved merge/export decision |
