# Proposal — A transition settles by event order, not by strict inequality

## Why

`buildSessions` guards both of its transitions with a strict `>` against the open session's start:

- the resume guard — `app/src/main/java/com/foco/app/domain/SessionBuilder.kt:29`:
  `open != event.packageName && event.timestamp > openStart`
- the stop guard — `:40`: `event.packageName == open && event.timestamp > openStart`

A transition that lands on the **same millisecond** as the session's start therefore falls through,
with two outcomes:

- **A direct switch is not honoured.** `A RESUMED t`, then `B RESUMED t` (no stop for A in between):
  neither branch fires, so A stays "open" and B's time is attributed to **A** — and if no further
  event arrives (a video playing on B, no transitions for minutes), A's session stretches to the
  query time.
- **An instant stop is not honoured.** `A RESUMED t`, then `A STOPPED t`: the session is not closed,
  so it later ends at the query time instead of being the zero-length session it is.

The living requirement states the first case: `openspec/specs/usage-tracking/spec.md:9` ("switching
directly between apps closes the previous session at the new start"), with its scenario at `:12-14`.
The second case is only stated by the code's own comment (`SessionBuilder.kt:11`) — the spec is mute
about pause/stop.

Trigger: two events sharing a millisecond. Rare — but the damage is not: a whole session attributed
to the wrong package, or a day total that stays wrong for the rest of the day.

## What Changes

- Both guards drop the strict `>`: a resume for a different package closes the open session at that
  resume's timestamp, and a pause/stop for the open package closes it — whether or not the timestamps tie.
- The zero-length case is filtered **where sessions are emitted**, not where transitions are recognised:
  a session whose start equals its end produces no entry.
- The delta states the half the spec never wrote: a pause/stop always closes the open session of its package.
- Nothing else moves: the direct-switch, open-at-query and same-app-transition behaviors keep their
  current results, and a day without ties is bit-for-bit identical.

## Alternatives Considered

| Option | Why yes | Why not / status |
|---|---|---|
| **Drop the strict `>` from both guards, filter zero-length at emission** — chosen (human, 2026-10-18: "the code is wrong") | One rule for both guards; the degenerate case is handled once, where the output is produced; the fold stays pure and unit-testable | Treats a tie as ordered — which is what the events are |
| Keep `>` and special-case the tie only in the resume guard | Smaller diff | Leaves the identical bug in the stop guard, and scatters the rule |
| Tie-break by a secondary key (event type, sequence) | "More correct" intra-millisecond ordering | Nothing in the contract asks for an order the platform does not provide; inventing one would be silent behavior |
| Treat a tied transition as a no-op on purpose | No change | Contradicts `usage-tracking:9` and leaves A's whole session misattributed |

## Impact

- `app/src/main/java/com/foco/app/domain/**` (the fold) · `app/src/test/**` (the regression)
- Ordering: overlaps `cross-midnight-sessions` on `domain/**` and `test/**` (almost certainly the same
  file), so it declares a **one-way** dep on it — that change goes first, no deadlock — plus
  `datastore-test-on-windows` (already `verified`, so satisfied). It also overlaps `themed-window` on
  `test/**`, and **no dep is declared there**: that one is a draft, and declaring it would block this
  fix behind it. Reversible with one line if you prefer the queue discipline.
- No protected zones, no new dependency, no platform API: pure behavior inside the fold — **no tech brief**.
