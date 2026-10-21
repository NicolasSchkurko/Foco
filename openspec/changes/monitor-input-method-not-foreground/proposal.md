# Proposal — monitor-input-method-not-foreground

## Why

The service classifies window events against a keyboard set frozen at service start:
`inputMethodPackages: Set<String> by lazy { … }` (`UsageMonitorAccessibilityService.kt:45-55`),
handed to `ForegroundAppState(packageName, IGNORED_PACKAGES, inputMethodPackages)` at `:54`. An
input method installed or enabled **later** is not in the set, so its
`TYPE_WINDOW_STATE_CHANGED` is taken for a normal app: `current` becomes the keyboard
(`ForegroundAppState.kt:40,46`). While the user types, the monitor reads `current = <IME>` → no
limit row → it skips, and the real app's `requestBlock` fails `foreground.accepts` → **enforcement
is silent** until the next real window event or the 2-minute tick.

Contract already in force:

- `openspec/specs/app-limits/spec.md:483` — "System UI and input-method windows SHALL NOT replace
  it [the tracked foreground]."
- Scenario "System UI and the keyboard" (`:510-513`) — "the app in use remains the tracked
  foreground."

The contract names the behavior; the code implements it against a stale set — 🔴. What the contract does not say is that the input-method set must reflect the **current** system: that freshness rule is the delta.

## What changes

- The input-method classification re-resolves the current IME set when it meets a package it does
  not know (or refreshes it with the same cadence as the event stream), so a late keyboard is
  recognized as an IME and never replaces the tracked app.
- Regression: `ForegroundAppStateTest` pins "unknown package that is an installed input method →
  the app in use stays tracked".

## Risks

- One extra `PackageManager` query only on the unknown-package path (per fresh IME install, not per
  event); cached afterwards.
- A package that was an IME and no longer is must not linger in the refreshed set — the refresh
  reads the current input-method list, it does not only grow.

## Spec delta

`specs/app-limits/spec.md` adds *Input-method classification stays current* — a freshness requirement of its own, so the existing requirement's *System UI and the keyboard* scenario (which assumes a known set) is not edited from two active changes at once.

## Origin

Filed 2026-10-21 by the anchored bug-finder audit (finding 5, severity medium); `kind: fix` per
/spec-triage. Sequenced after `monitor-revalidate-foreground-before-block` (same file). Awaiting
human acceptance — no code written.
