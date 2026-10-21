# Proposal — limit-window-dst-wall-boundaries

## Why

`AppLimitWindowRules.kt` defines "inside the window" **twice**, and on a day the zone's offset
changes the two definitions disagree:

- `:84-90` — `inWindowMillis` builds the in-window intervals as `dayStart + minute * 60_000L`
  (epoch arithmetic over the day's start instant), used by the budget count.
- `:28-46` — `isInside` compares `toSecondOfDay()` (wall clock), used by the enforcement check; and
  `:57-58` — `nextBoundaryMillis` builds the one-shot boundary instant with the same epoch math.

Reproduced in `jshell` with `America/New_York`, 2026-03-08 (spring forward): a window ending at
"06:00" yields an epoch end that falls at **07:00 wall time**, and `dayStart.plusMinutes(360)` also
answers 07:00 instead of 06:00. On offset-change days the budget counts an hour the block treats as
outside (or skips a permitted one), and the one-shot boundary check fires an hour late, leaving only
the 2-minute fallback.

Contract in force — the spec states "in the local zone" but never names offset changes:

- `openspec/specs/app-limits/spec.md:273-277` — window on a 15-minute grid "in the local zone",
  recurring every local day.
- `:345-348` — the budget "SHALL count only the portions … inside the window".
- `:327-343` — the boundary check is prompt.

## What changes

- All three computations resolve each boundary as a **local date + stored minute** in the zone
  (wall clock), instead of adding fixed minutes to the day's start epoch, so budget, `isInside` and
  the one-shot boundary share one definition on every day, DST included.
- Spec: the fix carries a delta that pins the wall-clock definition (the report's 🔴/🟡 frontier:
  the code contradicted itself; the spec was silent about DST).
- Regression: `AppLimitWindowRulesTest` grows spring-forward/fall-back cases (fixed test zones).

## Alternatives

| Option | Effect | Verdict |
|---|---|---|
| Wall-clock boundaries everywhere (local date + minute → instant) | one definition; budget, check and one-shot agree on DST days | **Proposed** |
| Epoch math everywhere (count by elapsed offsets) | blocks at a "06:00" that reads 07:00 on the wall — user-visible wrong hour | not proposed |
| Freeze the zone per window row | breaks "recur every local day" for users who travel | not proposed |

## Risks

- Spring-forward gap: a stored minute that does not exist that day (02:00–02:59 in a 02:00 jump)
  resolves to the first existing instant after the gap — chosen at apply, pinned by a test.
- Fall-back repeated hour: a session in the repeated wall hour is real elapsed time inside the
  window and counts once per elapsed portion; the window stays open for the wall range it names.
  Pinned by test.

## Origin

Filed 2026-10-21 by the anchored bug-finder audit (finding 4, severity medium, proven in `jshell`);
`kind: fix` per /spec-triage, delta included because the contract must name DST. Awaiting human
acceptance — no code written.
