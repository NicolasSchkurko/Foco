# Tasks

## 1. The check

- [x] 1.1 `LayerRulesTest`: derive the feature folders from the tree — every sub-folder of
  `app/src/main/java/com/foco/app/ui/` except the shared ones (`common`, `metrics`, `theme`) — instead
  of the hardcoded `uiFeatures` set (`:40`).
- [x] 1.2 Use the derived set in the cross-feature check (`:87-88`), so any `ui/<folder>/` outside the
  shared list is treated as a feature.
- [x] 1.3 Regression: assert the derivation itself — a synthetic name that is not a shared folder
  (e.g. `reports`) is classified as a feature, and each shared folder is not. This assertion **fails
  against the hardcoded set**, which is what makes the fix a fix rather than a refactor.
- [x] 1.4 Sanity: the four existing feature folders still produce no violation for the current tree,
  and a feature importing another feature's package still fails and names the file.

## 2. Integration

- [x] 2.1 `gradlew.bat test` + `node scripts/gates/gate-check.mjs --run-tests` (green evidence);
  update `x-gitref` to the branch tip.

### Notes on 2.1 (apply of 2026-10-19)

- The red-before/green-after was demonstrated, not assumed: with `isUiFeature` reverted to the old hardcoded list, `testDebugUnitTest --tests "*LayerRulesTest*"` reported `theUiFeatureSetIsDerivedFromTheTreeNotFromAHandWrittenList FAILED` (2 tests, 1 failed); with the derivation restored, green. The file was byte-identical to the original afterwards (hash checked).
- Test-only change: no app code touched, so no device pass was run (the instrumented suite cannot see this file).


## Workflow follow-up

- Review the apply report, merge, and archive with `gate-check --pre-archive layer-check-scope`.

### Note on the merge ref (2026-10-19)

- `x-gitref: a8c14fe` is the apply commit; `x-merged_as: 1a6ae5e` is the main commit this change is verified against at archive time. Why both: the merge into `main` was `c01068c`, but the *dependent* change `boundary-check-fixture` had already extended this change's target file (`LayerRulesTest.kt`) before the archive, and `--pre-archive` compares every file the target glob covers between the ref and HEAD — so the ref had to move to a commit that contains both. Lesson for a change whose target is a glob: archive it before the next change touches the same files.
- Ordering: no dep on the three overlapping drafts (glob-only overlap, no file in common).
- No tech brief: the comprehension pass is not required before archiving.
