# Tasks

## 1. The moves (behavior-preserving)

- [x] 1.1 `data/AvailableDays.kt` → `domain/AvailableDays.kt` (package `com.foco.app.domain`); update the import in `ui/dashboard/AppDetailScreen.kt`; move `DaySelectionTest.kt` to `app/src/test/java/com/foco/app/domain/` (package only — no test edits).
- [x] 1.2 `data/AppPackageInfo.kt` (new): context-scoped adapter owning `iconBitmap(packageName): Bitmap?` and `label(packageName): String` with the exact same `runCatching` fallbacks; `AppIcon`/`AppLabel` call it (same `produceState` + IO; the Compose conversion stays in the UI).

## 2. The tripwires (structure checks)

- [x] 2.1 Extend `LayerRulesTest`: a `data/` file without `android`/`androidx` imports fails; raw platform markers in `ui/` fail.
- [ ] 2.2 Verify the suite is green with both checks active (the previous layout would have failed both).

## 3. Integration

- [ ] 3.1 `gradlew.bat test` (JVM) + `assembleDebug` + the gate with green evidence (`node scripts/gates/gate-check.mjs --run-tests`); update `gitref`.

## Workflow follow-up

- Review the apply report, merge, and archive with `gate-check --pre-archive layer-compliance`.
- Tech brief: `walkthrough.md` (written at creation; platform SDK access is relocated) — **your** sign-off in `/spec-verify` before `--pre-archive`.
- Ordering (pre-declared per the A+ policy): `app-settings` waits for it; `app-entry-gate` follows transitively.
