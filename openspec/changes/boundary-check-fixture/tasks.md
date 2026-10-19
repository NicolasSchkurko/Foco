# Tasks

## 1. The extraction (no behavior change)

- [x] 1.1 In `LayerRulesTest`, move the body of the scan out of the `@Test` into
  `violationsIn(sources: File): List<String>` — same walk, same rules, root as a parameter. The
  `@Test` keeps asserting that `File("src/main/java")` yields none (the assertion the suite has today).
- [x] 1.2 Leave the derivation helpers (`sharedUiFolders`, `isUiFeature`, `uiFeaturesIn`) as they are:
  the fixture needs them to stay parameterized by the same root.

## 2. The fixture (hardest first: the failure branch)

- [x] 2.1 New test with a `TemporaryFolder` rule (the pattern `SettingsMigrationTest` uses): build
  `src/main/java/com/foco/app/ui/{dashboard,reports,common}/`, each with a one-line Kotlin file whose
  `package` matches its folder. `reports/Reports.kt` and `common/Widget.kt` both import
  `com.foco.app.ui.dashboard.Row`.
- [x] 2.2 Assert the scan over that root reports **exactly one** violation, on `ui/reports/Reports.kt`,
  and that its message names the file (`$relative`) and the rule; assert nothing is reported for the
  `common` file.
- [x] 2.3 Prove the fixture bites: temporarily delete the cross-feature rule from the scan (or make it
  unconditional) and watch this test go red; restore it. Record the red in the apply report.

## 3. Integration

- [x] 3.1 `gradlew.bat test` + `node scripts/gates/gate-check.mjs --run-tests` (green evidence); update
  `x-gitref` to the branch tip.

## Workflow follow-up

- Review the apply report, merge, and archive with `gate-check --pre-archive boundary-check-fixture`.
- **Ordering is real, not bookkeeping**: apply this **after merging `layer-check-scope`** — both edit
  `LayerRulesTest.kt`, and this change's fixture is written against the derived check.
- No tech brief (test-only): no walkthrough, no comprehension sign-off before archiving.

### Notes on the apply (2026-10-19)

- 2.3 done: with the cross-feature rule replaced by `if (false)`, `testDebugUnitTest --tests "*LayerRulesTest*"` reported `theScanReportsAViolationFromAFeatureFolderItHasNeverSeen FAILED` (3 tests, 1 failed) while the real-tree test stayed green — the fixture is what proves the rule fires. The file was byte-identical to the original afterwards (hash checked).
- One assertion had to be corrected during the apply: `violations.none { "common" in it }` was wrong because the violation *message* ends with `share through 'ui/common/'`; it now checks the reported file prefix (`com/foco/app/ui/common/`).
- Sequencing declared while applying (the gate escalates overlaps with `applying` work to errors): `english-source-prose`, `same-millisecond-transitions` and `trim-long-comments` now carry a dep on this change — they land after it. Test-only: no device pass.
