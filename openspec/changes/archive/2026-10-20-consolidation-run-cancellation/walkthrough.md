# Walkthrough — consolidation-run-cancellation

## What this change does

`ConsolidationRunner` shares one consolidation between the app start and the nightly worker. Before
this change that shared `async` lived in the first caller's `coroutineScope`. If the activity died,
the startup caller was cancelled and the worker waiting on the same job was cancelled with it; the
worker did not receive `Done`/`Failed`, so the nightly run was skipped. The run now belongs to a
runner-owned scope, and each caller only waits for its `Deferred`.

## Mechanism

- The runner owns `CoroutineScope(SupervisorJob() + Dispatchers.IO)`
  (`ConsolidationRunner.kt:23`); the shared job is created there (`:29`).
- `gate` still serializes job creation: the first caller starts the job and stores it in `inFlight`;
  later callers take the same `Deferred` (`:27-41`).
- `job.await()` is the caller's only suspension (`:42`). Cancelling the caller cancels that `await`,
  not the job in the runner's scope.
- The job still maps a thrown failure to `ConsolidationOutcome.Failed` and rethrows a
  `CancellationException`; its `finally` clears `inFlight` under the gate (`:30-39`).
- `ConsolidationWorker.doWork` is unchanged: `Done` → success, `Failed` → retry. Its own
  cancellation is still WorkManager's legitimate shutdown.
- The regression starts a startup caller, waits for the store to start, starts a worker caller,
  cancels the startup, and asserts the worker receives `Done` with one store run
  (`ConsolidationConcurrencyTest.kt:35-48`).

## Physical traceability

| Piece | Where | What it proves |
| --- | --- | --- |
| Runner-owned scope | `ConsolidationRunner.kt:23` | The run outlives one caller |
| Shared job | `ConsolidationRunner.kt:27-41` | One run for overlapping triggers |
| Caller wait | `ConsolidationRunner.kt:42` | Cancellation stops only the wait |
| Failure mapping | `ConsolidationRunner.kt:30-36` | `Failed` still reaches waiters |
| Worker mapping | `ConsolidationWorker.kt:13-17` | Failure still asks for retry |
| Regression | `ConsolidationConcurrencyTest.kt:35-48` | Cancelled startup, worker completes |

## Limits and failure modes

- **The runner scope has no shutdown.** It lives for the process, like the runner singleton; process
  death ends both the scope and any work, which is the platform's normal lifetime.
- **An internal `CancellationException` still cancels the job.** If `consolidate` itself throws
  cancellation, it is rethrown and waiters see cancellation, not `Failed`. The store does not
  currently cancel intentionally; a failure returns `Failed`.
- **A caller cancelled before it gets the gate starts no run.** There is no job yet, so there is
  nothing to keep alive; the next caller starts it.
- **A worker cancelled by WorkManager stops waiting.** That is the worker's own shutdown, not a
  sibling caller cancelling it; WorkManager owns that decision.
- **`inFlight` is cleared after the job finishes**, under the same gate, so a later trigger starts a
  fresh run. No result is cached.
- **No persistence change.** The `foco.db` writes, retry semantics and schema are unchanged.

## Touched APIs and resources

Kotlin coroutines (`CoroutineScope`, `SupervisorJob`, `Deferred.await`), the process-wide
`ConsolidationRunner`, and `ConsolidationWorker`'s existing result mapping. No dependency, storage,
network, permission or schema change.

## Implicit decisions

- The shared run gets a runner-owned `SupervisorJob` + IO scope, not `NonCancellable`.
- Callers await one `Deferred`; they do not own or cancel it.
- A failure is still a value (`Failed`), so every waiter gets it and the worker retries.
- The runner does not cache outcomes; completed runs are forgotten.
- The worker's own cancellation remains cancellation, not a retry.

## Verification

- `gradlew.bat test` green, including the new cancellation test.
- `ConsolidationConcurrencyTest` green (overlap, sequential, cancellation).
- `openspec validate consolidation-run-cancellation --strict` valid.
- Gate `--run-tests` green.

## Cognitive sign-off

Answer these in your own words; the sign-off is recorded only after your explicit confirmation.

1. Why does the shared job live in the runner's scope instead of the first caller's `coroutineScope`,
   and what happens to the job when the startup caller is cancelled?
   > Antes el async compartido era hijo del coroutineScope del primer llamador: si ese llamador se cancelaba (por ejemplo, la activity muriendo), la cancelación bajaba por structured concurrency y mataba el job que el worker estaba esperando. Ahora el job se crea en el scope propio del runner, CoroutineScope(SupervisorJob() + Dispatchers.IO) (ConsolidationRunner.kt:23, :29), y cada llamador solo hace await() del Deferred (:42). Cancelar el arranque cancela únicamente su espera; el job sigue corriendo y su finally limpia inFlight cuando termina (:37-39).
   >
   > — the human, 2026-10-20

2. Which caller still receives `Done`/`Failed` after the startup wait is cancelled, and why does the
   worker retry only on `Failed`?
   > El worker nocturno —o cualquier llamador posterior que se sume al mismo Deferred— sigue recibiendo el resultado, Done o Failed. El worker mapea Done → success y Failed → retry (ConsolidationWorker.kt:14-18). Reintenta solo con Failed porque una cancelación no es una falla: si WorkManager cancela el worker, es un apagado legítimo y reintentar sería incorrecto; además el runner re-lanza CancellationException en vez de convertirla en Failed (ConsolidationRunner.kt:33-34).
   >
   > — the human, 2026-10-20

3. What kind of cancellation still stops the work, and why is that not the bug this change fixes?
   > Detiene el trabajo una cancelación del propio trabajo: un CancellationException lanzado dentro de consolidate (se re-lanza, :33-34) o la cancelación del scope del runner / muerte del proceso. Eso no es el bug que arregla este cambio: el bug era que la cancelación de otro llamador (el arranque) se propagara al job compartido. La cancelación interna es cooperativa y significa que el trabajo mismo debe parar, no que un hermano se fue.
   >
   > — the human, 2026-10-20
