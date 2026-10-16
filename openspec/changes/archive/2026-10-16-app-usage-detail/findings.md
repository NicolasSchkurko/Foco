# Findings — semantic pass (2026-10-16)

- [x] Stale evidence reference in the proposal: it cites `DashboardScreen.kt:191-230` for the non-clickable ranking rows, but `dashboard-auto-refresh` rewrote the file — `AppUsageRow` now starts at `DashboardScreen.kt:215` (clickability re-verified: still no `clickable`/`onClick` in that file). Update the reference when applying.
- [x] Task 3.1 conditional resolved: the Compose UI test infrastructure already landed with `dashboard-auto-refresh` (`testInstrumentationRunner` at `app/build.gradle.kts:19`; `ui-test-junit4`/`ui-test-manifest` at `gradle/libs.versions.toml:26-27`), so this apply only needs to add `androidx.navigation:navigation-compose` (still absent). The Impact parenthetical about the test infra (`proposal.md:45`) is moot.
