# Tasks — per-app-limits-config

## Riskiest first vertical slice

- [x] 1. Move `AppLimitsSection.kt` from `ui/settings/` to `ui/detail/` (package change) and fix
      `AppLimitsSettingsTest`'s import so `LayerRulesTest` stays green.
- [x] 2. Remove the limits section from `SettingsScreen.kt`: the `appLimitsSection` call, its
      `limits`/`windows`/`alerts`/`limitApps` state, the notification-permission scaffolding that
      only served it, and the now-unused imports.
- [x] 3. Rebuild `AppAppSettingsScreen`: header with a working back arrow (back dispatcher), app
      identity, and the full `appLimitsSection` for the single package with monitor sync on every
      write.
- [x] 4. Replace the "Configure" text button in `AppDetailScreen` with a gear `IconButton` in
      `ScreenHeader(actions = …)`, using `Icons.Filled.Settings` like the dashboard; drop the
      button from the preview block.
- [x] 5. Wire `FocoNavHost`: the gear navigates to `app_settings/{packageName}`; back pops to the
      detail.
- [x] 6. Strings (en/es) for the configuration header and any new labels; remove strings that only
      the old Settings section used if they become unused.
- [x] 7. Update `AppDetailLimitsTest` (gear opens the screen), `AppAppSettingsTest` (pickers +
      back), and `SettingsScreenTest` (no limits section); run `gradlew.bat test` and
      `gradlew.bat ktlintCheck`.
- [x] 8. Gate (`--run-tests`) green; then `pre-archive`; record comprehension if the brief applies
      and archive.

