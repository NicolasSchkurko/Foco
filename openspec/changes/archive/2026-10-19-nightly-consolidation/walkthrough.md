# Walkthrough — nightly-consolidation (tech brief)

## Mechanism

Two triggers now write the same history, and one door serializes them.

**The schedule.** `MainActivity.onCreate` calls `ConsolidationScheduler.schedule(applicationContext)`
(`MainActivity.kt:56`) before it starts the Compose content. The scheduler builds a
`PeriodicWorkRequestBuilder<ConsolidationWorker>(24, TimeUnit.HOURS)` with an initial delay to the next
**03:00 local** (`ConsolidationScheduler.kt:19-27`) and enqueues it as the unique work
`"consolidate-and-prune"` with `ExistingPeriodicWorkPolicy.KEEP` (`:24`). `KEEP` is the whole
point: the second, third and hundredth app start find the work already enqueued and leave it alone, so
the 24 h clock is never restarted by opening the app.

**The job.** WorkManager wakes `ConsolidationWorker` (`ConsolidationWorker.kt:11-19`), a
`CoroutineWorker` whose only decision is the result: `ConsolidationOutcome.Done` → `Result.success()`,
`ConsolidationOutcome.Failed` → `Result.retry()` with WorkManager's default backoff. The worker takes
the day as `LocalDate.now()` and passes it to the runner; it does nothing else.

**The one door.** `ConsolidationRunner.run(today)` (`ConsolidationRunner.kt:25-47`) is single-flight:
the first caller creates a `Deferred` for the run and stores it in `inFlight` under a `Mutex`; a second
caller that arrives while the run is in progress finds that `Deferred` and awaits **it** instead of
starting another. When the run finishes, the `finally` clears `inFlight` under the same mutex. The
app-start path (`MainActivity.consolidateHistory`, `MainActivity.kt:79-87`) and the worker both call
`ConsolidationRunner.forApp(...)` (`ConsolidationRunner.kt:51-58`), which is a `@Volatile` + `synchronized`
singleton like `SessionHistoryStore.forApp` — so both triggers share one runner and one mutex.

**The store underneath is unchanged.** `run` calls `SessionHistoryStore.consolidate(day)`, the same
call the app-start path made before: pending days = `finishedDays(day) - storedDays()`, each pending
day read from the platform once, three writes (sessions → totals → count), and only then
`sessions.pruneBefore(rawCutoff(day))` (`SessionHistoryStore.kt:32-37`). A throw inside the loop
leaves the prune unreachable, which is what makes "a failed run prunes nothing" true without any new
code.

## Physical traceability

- **Where the schedule lives**: WorkManager's own database,
  `/data/user/0/com.foco.app/databases/androidx.work.workdb`, plus one entry in the system's
  JobScheduler for `androidx.work.systemjobscheduler@com.foco.app`. That is why the job survives a
  process kill and a reboot without a boot receiver: the system owns it, not the app.
- **Where the history lives**: unchanged — `foco.db` in private storage, `session_record`,
  `daily_aggregate`, `daily_unlock` (`SessionHistoryStore.forApp`, `SessionHistoryStore.kt:77-100`).
- **Who writes it**: the runner over the store, reached from exactly two call sites: the app start and
  the worker. There is no UI path.
- **Who reads it**: unchanged; the screens read the store on IO.
- **Lifecycle**: the schedule is created on the first start after install and kept; a user force-stop
  stops the job until the next launch (the system clears the package's jobs), and the next launch
  re-registers it with `KEEP` (which is a no-op if the entry survived).
- **Nothing leaves the device**: the job calls the platform reader and the local database; no network,
  no new permission, no manifest component beyond WorkManager's own service.

## Limits and failure modes

- **03:00 is a request, not a promise.** Doze and app-standby delay a periodic job; WorkManager runs it
  when the system allows. The consequence is a late run, never a lost one: the store consolidates
  whatever the platform still serves when it finally runs.
- **A day lost before the first run is gone.** The job only helps while the platform still serves the
  day (roughly a week). An app never opened for longer than that, with no job ever scheduled, keeps
  losing days — the schedule is created on first start by design.
- **A failure costs a retry, not a day.** A throw inside consolidation returns `Failed`, WorkManager
  retries with backoff, and nothing was pruned; the day stays pending and the next run (or the next
  app start) retries it.
- **Two triggers never overlap.** The single-flight runner makes the second waiter observe the first
  run's result; it does not queue a second run. The store is idempotent anyway (`REPLACE` on all
  three tables), which is the second line of defense.
- **A cancelled worker** propagates `CancellationException` (not wrapped as `Failed`), so a stopped job
  is not reported as a failure.
- **The job does not wake the app's UI**; it runs in a background process with no activity.

## Touched APIs

- **`androidx.work:work-runtime-ktx` 2.12.0** (`libs.versions.toml:15,37-38`,
  `app/build.gradle.kts:71`): `CoroutineWorker`, `PeriodicWorkRequestBuilder`,
  `ExistingPeriodicWorkPolicy.KEEP`, `WorkManager.getInstance`.
- **`androidx.work:work-testing` 2.12.0** (`app/build.gradle.kts:81`): `TestListenableWorkerBuilder`,
  used by the device test only.
- **`kotlinx.coroutines.sync.Mutex` / `CoroutineStart.LAZY` / `Deferred`** in the runner: the
  single-flight is a mutex plus a lazily-started `async` that is assigned to `inFlight` **before** it
  starts, so a run that completes instantly cannot clear a slot that was not set yet.
- **`java.time.ZonedDateTime`/`Duration`** for the 03:00 delay, pure and testable without Android.

## Implicit decisions

- **`ConsolidationOutcome` lives in `domain/`**, not in `data/`: `LayerRulesTest` requires a file
  without Android imports to sit in the domain layer.
- **The worker exposes `runnerFor` as a `@Volatile` companion seam** (`ConsolidationWorker.kt:21-23`) so
  the device test can drive a seeded runner without the platform. It is the same kind of seam the
  dashboard uses for `loadDay`.
- **The app-start path lost its `AtomicBoolean`**: the runner's single-flight replaces it, and it is
  strictly stronger (it also covers the worker).
- **KEEP, not UPDATE**: a new app version that changes the schedule does not re-enqueue until the
  existing work finishes; the alternative (UPDATE) would reset the 24 h clock on every update.
- **No charging constraint**, as the requirement says: the job is a database write, not a heavy task.

## Cognitive sign-off

Answer in your own words (the agent must not fill these):

1. The app is opened three times in one day, and then left alone for two days. Describe what happens to
   the schedule at each start, why the third start does not move the 03:00 run, and what the job does
   when it finally runs.
   > **Answer (Nschkurko, 2026-10-19):** La primera vez que se abre genera el worker a las 03:00.
   > Después, el día 2 y el día 3 no mueven las 03:00 porque el `initialDelay` solo tiene efecto en el
   > momento de encolar; una vez encolado, el calendario lo maneja WorkManager.
2. The nightly job fires while the app-start consolidation is still reading the platform. Explain what
   the worker does, what the second trigger waits on, and what would happen to the stored rows if the
   runner did not do this.
   > **Answer (Nschkurko, 2026-10-19):** Entra a `run`, toma el gate y encuentra `inFlight != null`; en
   > vez de crear otro `async`, espera el mismo `Deferred` de la corrida en curso. El segundo disparador
   > espera sobre el resultado de la corrida que ya está en curso. Sin esto, las dos corridas llamarían
   > a `consolidate()` a la vez, ambas leerían `storedDays()` antes de que la otra escriba, verían los
   > mismos días pendientes y leerían la plataforma dos veces.
3. The job runs and the platform reader throws on the third day. State what the worker returns, what
   happened to the raw rows and the totals, and what the next run does about that day.
   > **Answer (Nschkurko, 2026-10-19):** Devuelve `Result.retry()` y reintenta. Los días 1 y 2 ya
   > quedaron escritos completos. En el siguiente día, si el día 3 sigue pendiente, lo vuelve a leer;
   > si la lectura ya funciona, lo escribe y recién entonces poda lo que quedó fuera de la ventana.
