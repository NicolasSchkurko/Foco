# Proposal — detail-days-index-reclamp

## Why

The detail's day selector keeps an unbounded index into a recomputed list:

- `AppDetailScreen.kt:85` — `selectedIndex by remember { mutableIntStateOf(0) }` (plain `remember`,
  no clamp on either axis).
- `:91-104` — `days` is rebuilt when `today` rolls over (`rememberCurrentDate` rolls on
  `ON_RESUME`/midnight) and `availableDays(today, storedDays)` moves its floor.
- `:120` — `val selectedDate = days[selectedIndex]`.

When the user has walked to the oldest day (`selectedIndex == days.lastIndex`) and the window then
shrinks — the day that justified the depth leaves the window (easy: days without usage receive no
marker, `SessionHistoryStore.kt:62`) — `days[selectedIndex]` reads past the end:
`IndexOutOfBoundsException` → **crash**. The `LaunchedEffect` at `:110-118` only applies a
`requested` value and never reclamps `selectedIndex`.

Contract already in force:

- `openspec/specs/usage-tracking/spec.md:290` — "The window SHALL follow the store while the detail
  is open."
- Scenario "Bounds" (`:296-298`) — "selection stays within the stored window."

## What changes

- Reclamp `selectedIndex` to the current `days` bounds whenever `days` changes (selection stays at
  the same date when it still exists, otherwise lands on the nearest valid day, defaulting today).
- The clamp lives as a pure helper in `domain` (next to `availableDays`), so the regression is a JVM
  test: `DaySelectionTest` proves an index beyond the shrunken window resolves inside it.

## Risks

- A visible jump of one position when the oldest day leaves the window — unavoidable, the date no
  longer exists; today or the nearest remaining day is the least surprising landing.
- Trigger is rare (rollover with a live composition over an old day), so no behavior change is
  observable in the common path: with `days` unchanged the clamp is a no-op.

## Spec delta

`specs/usage-tracking/spec.md` MODIFIES *Day selection within the stored history* — restated verbatim plus the scenario *The window shrinks under the selection*, which pins the reclamp (`Bounds` only covers standing at the oldest day, not the window moving under a deep selection).

## Origin

Filed 2026-10-21 by the anchored bug-finder audit (finding 2, severity high); `kind: fix` per
/spec-triage. Awaiting human acceptance — no code written.
