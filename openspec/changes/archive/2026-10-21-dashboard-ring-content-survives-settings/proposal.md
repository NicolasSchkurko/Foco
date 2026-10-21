# Proposal — dashboard-ring-content-survives-settings

## Why

The ring's position is saveable state, but its content is not: `ringContent` lives in a plain
`remember { mutableStateMapOf(...) }` (`DashboardScreen.kt:161`) while the ring position/history is
restored through saved instance state. Every trip to Settings (or any recreation that restores the
position) therefore comes back to a position whose content was dropped → `RingLoading()` composes
(`DashboardScreen.kt:504-527`) until the read lands — **on every round-trip**, for content the
app already had in memory.

Contract in force:

- `openspec/specs/usage-tracking/spec.md:443-447` — "A loading placeholder SHALL appear only while
  the home has no position to show yet"; scenario "The first load" (`:491-494`) allows it only
  when "the home has not shown any position yet".
- Scenario "A move waits for its content" (`:479-483`) — the position on screen stays "without a
  spinner or loading text".
- `DECISIONS.md:415` (`ring-move-without-loader`) — "the placeholder survives only for a **cold
  start**". This is a partial regression of that decision: the placeholder now survives every
  Settings round-trip.

## What changes

- The ring content map is retained across recreation/navigation (hoisted to a saveable/remembered
  holder above the `NavHost`, or its state made saveable) so returning from Settings restores
  position **and** content — no loader for a position already shown.
- Regression: `DashboardRingTest` round-trips Settings and asserts the previous content is on
  screen without the placeholder.

## Alternatives

| Option | Effect | Verdict |
|---|---|---|
| Hoist the content holder above `NavHost` in a `rememberSaveable`-backed structure (or retained map keyed by position) | position and content survive together, one owner | **Proposed** |
| Re-read silently and keep the placeholder | still shows a loader for data already in memory — the defect itself | not proposed |
| Move only the position out of saved state | breaks the 2026-10-19 back-retrace decision (`DECISIONS.md:374`) | not proposed |

## Risks

- In-memory retention grows with the positions visited (bounded: today, week, seven days) — same
  bound the map already has.
- Content keys must stay aligned with position keys after restoration; the test pins the pair.

## Spec delta

`specs/usage-tracking/spec.md` adds *A restored position renders its content without a placeholder* rather than restating *Moving a ring position is animated* — that text already runs past the 500-character budget and a verbatim restatement would fail `--strict`; the first-load case stays intact in the new requirement.

## Origin

Filed 2026-10-21 by the anchored bug-finder audit (finding 12, severity low, 🔴 regression);
`kind: fix` per /spec-triage. Tech brief kept (in-memory state + coroutine lifecycle, the same
reason `ring-move-without-loader` kept one, `DECISIONS.md:415`). Awaiting human acceptance — no
code written.
