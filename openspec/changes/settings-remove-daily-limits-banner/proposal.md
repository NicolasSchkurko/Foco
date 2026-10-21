# Proposal — settings-remove-daily-limits-banner

## Why

User feature request: remove the fixed banner/card/text showing "Daily limits are active".

## What changes

- `DashboardScreen.kt` (or limits section): remove the banner/card/text that displays the message.
- If the active state needs to remain visible, make it subtle: a color change or small indicator on the limits toggle/icon instead of a banner.
- `values/strings.xml`: remove the banner string resource; no new resource needed for subtle state.
- No behaviour contract change (`skip_specs: true`); the limits mechanism stays the same, only the persistent banner is removed.

## Origin

Filed 2026-10-21 by user request; feature; draft.
