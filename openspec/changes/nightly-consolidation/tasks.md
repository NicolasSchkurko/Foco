# Tasks

## 1. Prove the dependency first (the risk is the toolchain, not the logic)

- [ ] 1.1 Add `androidx.work:work-runtime-ktx` (and `androidx.work:work-testing` for the device test) to `gradle/libs.versions.toml` and `app/build.gradle.kts`, pinned compatible with AGP 9.4 / Kotlin 2.4. Nothing else yet.
- [ ] 1.2 `gradlew.bat assembleDebug`: the WorkManager initializer must merge into the manifest and the build must stay green. Stop here if it does not — the alternatives are in the proposal.
- [ ] 1.3 By hand once: `adb shell dumpsys jobscheduler | findstr com.foco.app` shows the work registered after a start.

## 2. The runner (the logic, JVM-testable, before any Android type)

- [ ] 2.1 `data/ConsolidationRunner.kt`: wraps `SessionHistoryStore.consolidate(today)` behind one `Mutex`, takes the day as a parameter (a `LocalDate`, not `LocalDate.now()` inside), and returns `Done` or `Failed(cause)`. No Android imports.
- [ ] 2.2 `ConsolidationConcurrencyTest` (JVM, fakes): two overlapping calls run the store **once**; a second call after the first completes is a no-op for the days already replaced. Write it **before** the mutex and watch it fail.
- [ ] 2.3 `ConsolidationRetryTest` (JVM, fakes): a reader that throws leaves the store untouched and returns `Failed`; no prune call is reached; a successful run prunes only after the upserts.

## 3. Worker and schedule (Android types, thin)

- [ ] 3.1 `data/ConsolidationWorker.kt`: `CoroutineWorker` that calls the runner with `LocalDate.now()`; `Done` → `Result.success()`, `Failed` → `Result.retry()` (default backoff).
- [ ] 3.2 `data/ConsolidationScheduler.kt`: `PeriodicWorkRequestBuilder<ConsolidationWorker>(24, TimeUnit.HOURS)` with an initial delay to the next **03:00 local**, `enqueueUniquePeriodicWork("consolidate-and-prune", ExistingPeriodicWorkPolicy.KEEP)`. `KEEP` is the point: a start must not restart the 24 h clock.
- [ ] 3.3 `MainActivity.onCreate`: schedule on the same start (cheap, returns immediately) and keep the app-start trigger, now through the runner.

## 4. The device test

- [ ] 4.1 `ConsolidationWorkerTest` (`work-testing`, `TestListenableWorkerBuilder` + a seeded store): a run consolidates and prunes; a failing store returns `Result.retry`.
- [ ] 4.2 Keep the seeding through the store's fakes/DAO interfaces — the same seam the foundation uses — rather than the real platform.

## 5. Verify

- [ ] 5.1 `openspec validate --all --strict` and `node scripts/gates/gate-check.mjs --run-tests`.
- [ ] 5.2 Instrumented run for 4.1 (the known `UnlockCountTest` flake stays unrelated).
- [ ] 5.3 By hand: two consecutive app starts leave **one** job (`dumpsys jobscheduler`) and no duplicate rows.

## Workflow follow-up

- `tech_brief: true`: `walkthrough.md` (two writers, the mutex, what Doze does to the hour, failure modes) + comprehension sign-off before archiving.
- Depends on `usage-history-foundation`; lands after it is `verified`. Leave `status: draft` until the human accepts.
