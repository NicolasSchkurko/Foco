# Proposal — ring-move-without-loader

## Why

Moving between ring positions flashes the loading placeholder. `DashboardScreen` drops the day's
data the moment the position changes (`day = null`, before the read starts), so the entering content
composes the spinner and the header composes "Loading…" while the new day is read; `WeekContent`
does the same with its own nulls. At ~320 ms of slide and fade, a read of a few milliseconds is
enough to show the placeholder for a frame or two. It reads as a glitch inside an otherwise smooth
move.

## What Changes

- The home keeps what it is showing until the requested position's content is ready; only then does
  the ring target change, so the existing slide and fade cover a swap of two loaded positions.
- A per-position content cache, bounded by the ring, gives the outgoing content its own data during
  the transition (today both sides render the same day) and makes a revisit instant.
- `WeekContent` becomes a renderer: the home reads the week's stored totals and today's live day and
  passes them in, so a position is shown only when both are ready.
- Today's live read keeps its cadence (on resume and every 30 s) while today or the week is the
  requested position; the week reuses that read instead of issuing a second one.
- The header title, the usage line and the week-only export action follow the shown position, so a
  title never pairs with another day's numbers. The "Loading…" subtitle goes away; the placeholder
  stays only when the home has nothing to show yet (cold start).

## Expected outcome

Swiping or arrowing between days, and to or from the week, keeps the current content in place and
then slides in the new one already filled: no spinner and no loading text inside a move. Revisits
are instantaneous.

## State and persistence mechanism

In-memory only: a `mutableStateMapOf<String, RingContent>` held by the home composition, bounded by
the ring (at most the seven offered days plus the week). Nothing is persisted; no schema, store or
backup change. Reads keep their current doors: `UsageRepository.loadDay` for today and
`SessionHistoryStore` for a finished day and the week's totals.

## Alternatives considered

| Option | Why yes | Why not |
| --- | --- | --- |
| Keep the shown position until the target is ready, then run the existing transition (chosen) | Removes the placeholder from every move and keeps the direction | Needs the requested/shown split and a content cache |
| Crossfade to a blank background while loading | Simple | The blank is still a flash, and it loses the direction |
| Skeleton placeholder during the move | No layout jump | Restyles the load instead of hiding it |
| Prefetch every ring position at startup | Moves never wait | Seven platform/Room reads on open, against reading one day at a time |

## Impact

- `DashboardScreen.kt` (position state and `AnimatedContent`), new `RingContent.kt`,
  `WeekContent.kt`, and the ring's instrumented test.
- No protected zone (`openspec/config.yaml` → `protected: []`), no new dependency.
