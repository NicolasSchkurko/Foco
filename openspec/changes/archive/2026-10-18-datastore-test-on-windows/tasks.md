# Tasks

## 1. The exclusion

- [x] 1.1 `SettingsMigrationTest`: declare the Windows exclusion with a named reason (the DataStore `File.renameTo` branch taken when `Build.VERSION.SDK_INT < 26` under a mocked `android.jar`), so `copiesTheSettingsClearsTheLegacyAndLeavesTheExclusionsAlone` and `runningItAgainIsANoOp` skip on Windows instead of failing.
- [x] 1.2 Keep the exclusion surgical: `aFreshInstallIsUntouched` still runs on Windows, and the two guarded tests still run on a non-Windows platform (verified by the same suite on CI/Linux, or by temporarily forcing the guard's condition). — Windows leg verified here: `tests=27 failures=0 skipped=2`, with `aFreshInstallIsUntouched` running unskipped. The non-Windows leg follows from the guard's own condition (`os.name` does not contain "win") and stays covered on CI/Linux.
- [x] 1.3 Regression: on Windows the two tests do not FAIL — they pass or are skipped with the named reason; `tests completed, 0 failed`.

## 2. Integration

- [x] 2.1 `gradlew.bat test` + `node scripts/gates/gate-check.mjs --run-tests` → suite green and evidence recorded; update `x-gitref` to the branch tip.

## Workflow follow-up

- Review the apply report, merge, and archive with `gate-check --pre-archive datastore-test-on-windows`.
- Ordering: waits for `structure-housekeeping` (declared dep).
- Tech brief: complete `walkthrough.md` **and** the human's comprehension sign-off before archiving.
