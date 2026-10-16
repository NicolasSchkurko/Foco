# Proposal — Layer compliance: `availableDays` to domain, platform reads behind data adapters

## Why

The `/diagram-sync` review surfaced two silent discrepancies against `code-structure`. The human triaged both (2026-10-16, DECISIONS.md):

1. `data/AvailableDays.kt` is a pure helper (`java.time` only) living in the layer defined as "Android platform access"; pure behavior belongs to `domain/`.
2. `ui/dashboard/AppIcon.kt` / `AppLabel.kt` call `PackageManager` raw from the UI; platform reads must be reached through `data/` adapters.

## What Changes

- Move `availableDays` to `domain/` (package `com.foco.app.domain`); update its caller (`AppDetailScreen`) and move `DaySelectionTest` with it. **No behavior change.**
- New `data/AppPackageInfo.kt`: a context-scoped adapter (same shape as the other data sources) owning the PackageManager icon/label reads with the exact same threading and fallbacks; `AppIcon`/`AppLabel` keep their `produceState` + IO usage but call the adapter.
- Extend the structure checks (`LayerRulesTest`) to pin both boundaries: a `data/` file without `android`/`androidx` imports fails; a raw platform marker (`packageManager.`, `getSystemService(`, `queryEvents(`, `unsafeCheckOpNoThrow(`) in `ui/` fails.
- Clarify the requirement in the `code-structure` delta (the two boundary SHALLs).

## Alternatives Considered

Both decided by the human in triage (DECISIONS.md, 2026-10-16):
- `availableDays`: amend the spec to allow pure helpers in `data/` → rejected (the layer definition stands); no-op → rejected.
- Icon/label reads: leave as accepted reality (already drawn in the DSL) → rejected; note it in the spec → rejected. The code moves behind `data/` instead.

## Capabilities

### New Capabilities

(none)

### Modified Capabilities

- `code-structure`: the layer requirement gains the two boundary SHALLs (pure helpers in `domain/`; platform reads via `data/` adapters).

## Impact

- `app/src/main/java/com/foco/app/domain/AvailableDays.kt` (moved from `data/`), `data/AppPackageInfo.kt` (new), `ui/dashboard/{AppIcon,AppLabel,AppDetailScreen}.kt`
- `app/src/test/`: `DaySelectionTest` (moved), `LayerRulesTest` (extended with the tripwires)
- Ordering (pre-declared, A+): goes before `app-settings` (added as its dep); `app-entry-gate` follows transitively.
- Refactor proof: no observable behavior change — the suite stays green (regression: `DaySelectionTest`).
