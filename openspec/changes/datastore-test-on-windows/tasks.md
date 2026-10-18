# Tasks

## 1. The exclusion

- [ ] 1.1 `SettingsMigrationTest`: declare the Windows exclusion with a named reason (the DataStore `File.renameTo` branch taken when `Build.VERSION.SDK_INT < 26` under a mocked `android.jar`), so `copiesTheSettingsClearsTheLegacyAndLeavesTheExclusionsAlone` and `runningItAgainIsANoOp` skip on Windows instead of failing.
- [ ] 1.2 Keep the exclusion surgical: `aFreshInstallIsUntouched` still runs on Windows, and the two guarded tests still run on a non-Windows platform (verified by the same suite on CI/Linux, or by temporarily forcing the guard's condition).
- [ ] 1.3 Regression: on Windows the two tests do not FAIL — they pass or are skipped with the named reason; `tests completed, 0 failed`.

## 2. Integration

- [ ] 2.1 `gradlew.bat test` + `node scripts/gates/gate-check.mjs --run-tests` → suite green and evidence recorded; update `x-gitref` to the branch tip.

## Workflow follow-up

- Review the apply report, merge, and archive with `gate-check --pre-archive datastore-test-on-windows`.
- Ordering: waits for `structure-housekeeping` (declared dep).
- Tech brief: complete `walkthrough.md` **and** the human's comprehension sign-off before archiving.
