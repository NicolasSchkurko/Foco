# Proposal — settings-limits-quick-adjust

## Why

User feature request: simplify limit/time adjustments with quick buttons (`-15 min` / `+15 min`) and a visual, touch-friendly time-range picker (interval-based, 30/60 min steps, 48x48 dp targets).

## What changes

- `SettingsScreen.kt`: replace or complement text input with `-15 min` / `+15 min` buttons and a centered value indicator; keep lower limit (0 or 15 min); long-press accelerates.
- Time-range picker: replace plain text selectors with interval picker (30/60 min steps, 12h/24h format respecting system preference); touch targets ≥48x48 dp.
- `values`/`values-es` strings: add button labels (`-15 min`, `+15 min`) and picker format labels.
- No contract change to existing specs (`skip_specs: true` — new interaction, no behaviour contract changed); `SettingsScreenTest.kt` verifies button increments and picker visibility.

## Origin

Filed 2026-10-21 by user request; feature, no verified finding; draft.
