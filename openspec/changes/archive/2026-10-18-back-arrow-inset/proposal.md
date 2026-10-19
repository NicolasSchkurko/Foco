# Proposal — Back arrow at the header's leading edge (detail + settings), the rest centered

## Why

The back arrow in the app detail sits at the content's 20 dp padding (≈32 dp from the edge to the glyph), away from the Material convention for header navigation icons; reported by the user (2026-10-16). The Settings header repeats the same pattern, so both move together (consistency).

## What Changes

- **Detail** (`AppDetailScreen`) and **Settings** (`SettingsScreen`) headers: the back control sits at the header's leading edge (a 4 dp icon-button inset — ≈16 dp to the glyph), and the rest of the header (detail: app icon + name; settings: title) is **centered**, balancing space against the arrow.
- `AppDetailNavigationTest` and `SettingsScreenTest` gain a positional regression assertion (the back control's left bound), so the inset cannot silently drift back.
- Nothing else changes: the rest of the content keeps its 20 dp padding, and every way back (tap, key, gesture) behaves as today.

## Alternatives Considered

| Option | Why yes | Why not / status |
|---|---|---|
| Move the whole header row to 4 dp | Simplest row change | The app identity would also hug the edge — **not chosen** |
| Arrow only, rest stays at 20 dp | Minimal | Leaves an odd gap between the arrow and the identity — **not chosen** |
| **Arrow at the edge + the rest centered** — chosen (human, 2026-10-16) | Standard header pattern; identity reads centered | Needs a balancing space against the arrow in the row |

## Impact

- `app/src/main/java/com/foco/app/ui/detail/**` + `ui/settings/**` (both headers) · `app/src/androidTest/**` (the two regression assertions)
- Ordering: waits for `app-entry-gate` → `structure-housekeeping` → `themed-window` (declared deps)
- No protected zones, no new dependencies; visual-only — no tech brief
