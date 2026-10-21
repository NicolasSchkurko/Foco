# Proposal — detail-selection-survives-recreation

## Why

The detail's selected day lives in plain `remember` state
(`AppDetailScreen.kt:85` `selectedIndex`, `:109`-area selection state) — not `rememberSaveable`.
Rotate the device or take the trip to Settings and the screen comes back on **Hoy**, losing the day
the user was reading. The specs are silent about the detail's selection across recreation (🟡):
they pin the window (`usage-tracking:286-314`) and the home's own guarantees, and
`DECISIONS.md:374` protects the **home's** ring position across Settings — the detail was never
given the same rule.

## What changes (proposed contract)

- New requirement in `usage-tracking`: the detail's selected day survives recreation — rotation and
  a Settings round-trip return the detail on the day it was showing, inside the same stored window
  (if that day still exists; otherwise the nearest valid day, consistent with
  `detail-days-index-reclamp`).
- Implementation: make the selection saveable (the existing `rememberSaveable` pattern the home
  already uses for its position).

## Alternatives

| Option | Effect | Verdict |
|---|---|---|
| `rememberSaveable` on the selection (index or date) | survives rotation, navigation and process recreation; same pattern as the home | **Proposed** |
| Re-derive from the route (`date` query param) only | loses selection on rotation, the reported defect | not proposed |
| Hoist selection above `NavHost` | heavier ownership change for one value | not proposed |

## Risks

- Restoring an index into a window that shrank meanwhile must go through the same clamp as
  `detail-days-index-reclamp` (declared dep, same file) — restoring a date key and resolving it
  against the current window avoids the class of bug entirely.

## Origin

Filed 2026-10-21 by the anchored bug-finder audit (finding 8, severity medium, 🟡 spec-silent);
`kind: feature` via /spec-add. Sequenced after `detail-days-index-reclamp` (same file). Awaiting
human acceptance — no code written.
