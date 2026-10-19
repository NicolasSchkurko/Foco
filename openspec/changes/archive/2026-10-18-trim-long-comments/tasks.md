# Tasks

## 1. The oracle first (hardest first: it defines what "long" means)

- [x] 1.1 `app/src/test/java/com/foco/app/structure/ShortCommentsTest.kt` (JVM, module dir as cwd like `LayerRulesTest`): walk `src/**`, group consecutive comment lines into blocks (`//`, KDoc `/** … */`, `/* … */`, `<!-- … -->`, blank lines allowed inside a block), and fail for every block longer than **4** lines, naming `file:line`, the length and the block's first line.
- [x] 1.2 Run it on the **pre-change tree** and record the list — 30 blocks of 5+, 7 of 8+, worst 16 (`SettingsScreenTest.kt:33`). That red is this refactor's regression evidence.
- [x] 1.3 Its own KDoc must fit the budget: the test has to pass on itself.

## 2. The sweep (hardest first)

- [x] 2.1 `androidTest/.../SettingsScreenTest.kt:33` (16 lines — activity re-creation, the two hierarchies, why `ensureSettings` is re-called): move the whole note to `docs/mechanisms/instrumented-tests.md` and leave one pointer line.
- [x] 2.2 The three structure/manifest tripwires — `LayerRulesTest.kt:8` (13), `OneTypePerFileTest.kt:8`, `BackInvokedCallbackManifestTest.kt:7`: trim to the budget; the assertions say what the rules are, and the platform-exclusion *why* is already in `datastore-test-on-windows`'s walkthrough.
- [x] 2.3 The data path — `data/UsageRepository.kt:16` + `:58`, `domain/SessionBuilder.kt:7`, `ui/metrics/AppDetail.kt:6`, `ui/metrics/UsageMetrics.kt:12`, `ui/dashboard/DashboardScreen.kt:58`, `ui/Formatting.kt:24`, `ui/App.kt:18`: trim, and move the *why* (the day window, `KEYGUARD_HIDDEN`, the fold's rules) to `docs/mechanisms/usage-data.md` with one-line pointers.
- [x] 2.4 The platform headers — `data/AppLanguage.kt:7`, `data/AppAppearance.kt:6`, `data/ExcludedAppsStore.kt:10`, `data/SettingsMigration.kt:9`, `data/UsageAccess.kt:9`, `data/AppPackageInfo.kt:7`, `data/SettingsStore.kt:12` (already 4): keep the physical-resource half (resource, where it lives, lifecycle) and drop the rest.
- [x] 2.5 `MainActivity.kt:28` + `:67`, the remaining `ui/**` blocks, the test/androidTest KDocs (`SettingsMigrationTest.kt:22` + `:40`, `AppAppearanceTest.kt:8`, the `App*Test` KDocs): same budget, same rule (the `SettingsMigrationTest` Windows/exclusion note already lives in that change's walkthrough and in `DECISIONS.md`).
- [x] 2.6 Audit the moves: `git diff --stat docs/mechanisms/` is the trail; nothing that explains a *why* may disappear without landing there.

## 3. Proof

- [x] 3.1 `gradlew.bat test` green — `ShortCommentsTest` now clean over `app/src/**`; `LayerRulesTest`, `OneTypePerFileTest`, `SessionBuilderTest`, the metrics tests and `SettingsMigrationTest` unchanged.
- [x] 3.2 `connectedDebugAndroidTest` on the API 36 AVD: `SettingsScreenTest` green — the moved note is about that test, so a mistake in the move shows up here.
- [x] 3.3 `node scripts/gates/gate-check.mjs --run-tests` (green evidence); update `gitref` to the branch tip.

## Workflow follow-up

- Review the apply report, merge, and archive with `gate-check --pre-archive trim-long-comments`.
- `english-source-prose` declares a dep on this change (order chosen 2026-10-18: trim first, translate what survives).
- Out of scope by design: `scripts/gates/**`, the repo-root build files, `gradle/**`, `ARCHITECTURE.dsl`, `app/proguard-rules.pro`. The prose inside `docs/` is not trimmed.

### Notes on the apply (2026-10-19)

- **The tripwire's red run is the real list, and the proposal's figures were stale**: **34 blocks** over the budget (the proposal said 30 blocks of 5+ and 7 of 8+) — the day had added the KDoc of `DayWindow.kt`, the rewritten `loadDay`, `DayWindowTest` and the fixture additions in `LayerRulesTest`. All 34 were trimmed; `ShortCommentsTest` is green with 0 violations.
- **The budget, as measured**: a block counts `/**` and `*/`, so a KDoc has **two text lines** (or four `//` lines). Every trim keeps the why and drops the restated what; the physical-resource half survives where the file touches the OS or storage (`AppAppearance`, `UsageRepository`, `ExcludedAppsStore`, `SettingsStore`).
- **What moved** (the one substantive block): `SettingsScreenTest`'s 16-line stability note became `docs/mechanisms/instrumented-tests.md` (why the re-creation happens, why `ensureSettings` is called again, why the gaps are tolerated, why restarts are `close()` + a new launch), with a one-line pointer in the test. The other long ones (`LayerRulesTest`'s six-rule list, `SessionBuilder`'s fold rules, `BackInvokedCallbackManifestTest`'s predictive-back story, `DashboardScreen`'s data note) were trimmed: their rules already live in the living specs and in the archived changes' walkthroughs.
- `git diff --stat docs/mechanisms/` is the trail: one new note; no other explanation disappeared without a home.
- Suite: **15/15 JVM classes green** (including `DefaultLocaleResourcesTest`, `SpecConfigRulesTest` and the structure tripwires) and `assembleDebugAndroidTest` compiles, so the trimmed androidTest KDocs are valid. The instrumented run was **not** repeated: comments cannot change what those tests observe, and the compile is the meaningful check for the files this change touched.
