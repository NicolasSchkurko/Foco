# Mechanisms & Architecture

## UI Flow & Navigation

1. **Entry Point:** `MainActivity` enables edge-to-edge drawing and sets Compose content with `FocoTheme { App() }`.
2. **Permission Gate:** `App()` checks `PACKAGE_USAGE_STATS` permission via `UsageAccess.isGranted()` on every `ON_RESUME`. If granted, opens `FocoNavHost`; otherwise displays `OnboardingScreen`.
3. **Navigation:** `FocoNavHost` uses `navigation-compose` with routes for `dashboard` and `detail/{packageName}`.
4. **Data Loop:** `DashboardScreen` runs a `repeatOnLifecycle(RESUMED)` loop (load → render → wait 30s) to fetch local usage stats without background work.
5. **Persistence:** Room database (`session_record`, `daily_aggregate`, `consolidated_day`) stores historical sessions and daily counts locally.

## Testing Mechanisms

- **Activity Re-creation:** Language and theme changes trigger Activity re-creation. UI assertions tolerate temporary gaps during Compose hierarchy re-creation.
