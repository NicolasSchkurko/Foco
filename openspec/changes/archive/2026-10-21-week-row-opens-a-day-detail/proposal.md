# Proposal — week-row-opens-a-day-detail

## Why

The week's ranking row opens **today's** detail: `DashboardScreen.kt:517` passes `date = null` (the
route's "absent = today" default, decided in `DECISIONS.md:352`), so tapping *"Instagram — 5 h
12 min (7 days)"* lands on a detail showing today's 10 minutes (or the empty state). The row
summarizes seven days; the screen shows one that has nothing to do with it.

The specs are silent on this: `usage-tracking:366-376` pins that **from a dated day view** an app row
opens that app's detail *for the day* — but the week position is not a day, and no requirement says
which day its rows open. 🟡 spec-silent → /spec-add documenting the chosen behaviour.

## What changes (proposed contract)

- New requirement in `usage-tracking`: the week's app rows open **today's** detail (documenting what
  the route's `date = null` already does), so the behaviour is contracted instead of implicit; dated
  day views are untouched and keep opening their own day (`usage-tracking:366-376`).
- No implementation change: this only pins the current, accepted behaviour (`DashboardScreen.kt:517`)
  under the decision so a future change to "a day of that week" can't sneak in by accident.

## Alternatives

| Option | Effect | Verdict |
|---|---|---|
| Document today's behaviour (what the row already does) | contract matches reality; mismatch with the row's total stays visible | **Chosen (human, 2026-10-21, option c)** |
| Open the most recent day with usage | matches the row's seven-day total; a real behaviour change | not chosen |
| Open the most-used day of the week | same behaviour-change surface, different day | not chosen |

## Risks

- Pure documentation delta: no runtime change, no new read — the row carries on passing `date = null`.
- The visible mismatch (a 5 h row → today's 10 min) stays by explicit decision; a future proposal may revisit it under a new change.

## Spec delta

`specs/usage-tracking/spec.md` adds *A week app row opens today's detail*.

## Origin

Filed 2026-10-21 by the anchored bug-finder audit (finding 16, severity low, 🟡 spec-silent);
`kind: feature` via /spec-add. Decision (c) chosen by the human on 2026-10-21. Accepted — no code
written.
