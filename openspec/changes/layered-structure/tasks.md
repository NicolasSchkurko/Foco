# Tasks

## 1. Structure tests first (the rules, red today)

- [x] 1.1 Write `structure/OneTypePerFileTest.kt` (scan `app/src/main/java`; fail on >1 top-level type per file, or a single type not matching the file name); verify it runs RED today listing exactly `data/Sessions.kt` and `ui/metrics/UsageMetrics.kt`.
- [x] 1.2 Write `structure/LayerRulesTest.kt` (package↔folder match; `model/`+`domain/` free of `android.`/`androidx.`; downward-only dependencies between `com.foco.app` layers); it passes trivially today and must stay green through the moves.

## 2. Pure core: model/ + domain/

- [x] 2.1 Create `model/` with one type per file: `UsageSession.kt`, `UsageEvent.kt`, `UsageEventType.kt` (package `com.foco.app.model`).
- [x] 2.2 Create `domain/`: `SessionBuilder.kt` (`buildSessions`), `SessionFilter.kt` (`filterSessions`) — package `com.foco.app.domain`; delete `data/Sessions.kt`; update imports in `UsageRepository` and tests.
- [x] 2.3 Move the session tests to `app/src/test/java/com/foco/app/domain/` (package + imports only, zero behavioral edits); verify `gradlew.bat test` green.

## 3. Adapters and presentation: data/ + ui/

- [x] 3.1 `data/`: move `UsageAccess.kt` from the root next to `UsageRepository.kt` (package `com.foco.app.data`).
- [x] 3.2 `ui/`: `App.kt` (routing composable extracted from `MainActivity`), `dashboard/` (`DashboardScreen.kt`, `HourChart.kt`, `AppUsageRow.kt`, `AppLabel.kt`, `AppIcon.kt`), `onboarding/OnboardingScreen.kt`, `metrics/` (`AppUsageEntry.kt`, `HourBucket.kt`, `DayMetrics.kt`, `UsageMetrics.kt`), `theme/Theme.kt` stays; `MainActivity.kt` keeps only the activity.
- [x] 3.3 Verify `gradlew.bat test` green (including both structure tests) and `gradlew.bat assembleDebug` builds.

## 4. Device + integration

- [x] 4.1 `gradlew.bat installDebug` and verify on the emulator: dashboard identical (total, chart, ranking, labels/icons) and onboarding intact.
- [ ] 4.2 Run the full gate with green evidence (`node scripts/gates/gate-check.mjs --run-tests`); update `gitref` to the branch tip and verify it is an ancestor of HEAD.

## Workflow follow-up

- Review the apply report, merge, and archive with `gate-check --pre-archive layered-structure`.
- Create the structural fact: `/diagram-sync` (there is no `ARCHITECTURE.dsl` yet — this change defines the layers it should mirror).
- `app-usage-detail` (draft) stays independent (human decision): whichever applies second adapts; its findings.md already lists the housekeeping for its own apply.
