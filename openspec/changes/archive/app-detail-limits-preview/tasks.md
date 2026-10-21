# Tasks — app-detail-limits-preview

## Riskiest first vertical slice

- [x] 0. Confirm `app-limits` spec is live; `AppLimitsStore` flows and `AppLimitWindowRules` are green.
- [x] 1. Add the read-only preview section to `AppDetailScreen` (limit/window/alerts text) with `AppLimitsStore` read.
- [x] 2. Add `app_settings/{packageName}` route to `FocoNavHost` and wire `AppAppSettingsScreen` (reuse `AppLimitsSection` for one package).
- [x] 3. Add the Configure action to `AppDetailScreen`; verify back returns to detail.
- [x] 4. Add instrumented tests (`AppDetailLimitsTest`, `AppAppSettingsTest`) and string resources; run `gradlew.bat test` and `gradlew.bat ktlintCheck`.
- [x] 5. Gate (`--run-tests`) green; then `pre-archive`; record comprehension and archive.
