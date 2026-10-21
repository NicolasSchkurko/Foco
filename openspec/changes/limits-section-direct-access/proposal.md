# Proposal — limits-section-direct-access

## Why

User feature request: the limits section/card in the main screen should be fully interactive (pressed/ripple state) and tapping it navigates directly to settings focused on limits.

## What changes

- Make the limits card/cell interactive in `DashboardScreen.kt` (pressed/ripple state); add `ACTION_VIEW` or direct navigation to settings focused on limits (`SettingsScreen.kt`).
- No behaviour contract change; interaction-only feature; `skip_specs: true`; `SettingsScreenTest.kt` verifies navigation and ripple.

## Origin

Filed 2026-10-21 by user request; feature; draft.
