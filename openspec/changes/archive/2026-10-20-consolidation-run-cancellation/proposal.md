# Proposal — consolidation-run-cancellation

## Why

`ConsolidationRunner.run` creates the shared `async` inside the caller's `coroutineScope`. The app
startup effect and the nightly worker share that runner. If the app startup effect is canceled
(Activity destroyed) while the worker is waiting for the same run, the shared job is canceled too;
the worker's `run` receives cancellation, `ConsolidationWorker` does not turn that into a retry,
and the nightly consolidation is skipped. No data is lost, but the run the worker was promised to
wait for does not finish.

## What

Give the shared run a runner-owned lifetime instead of the first caller's: create it in a
`CoroutineScope` owned by the runner (a `SupervisorJob` plus IO), let callers await its `Deferred`,
and let their cancellation stop only their wait. A failure still returns `Failed` to all waiters;
a canceled caller does not cancel the work.

No persistence/schema change.

## Expected outcome

Canceling the startup wait does not cancel a consolidation the worker is waiting for; the worker
still receives `Done`/`Failed` and retries on failure; two overlapping triggers still execute the
store once.

## State and persistence mechanism

No new store. The in-flight `Deferred` and its scope live in the process-wide `ConsolidationRunner`;
the existing `foco.db` writes are unchanged. No network, backup or permission change.

## Alternatives considered

| Option | Why yes | Why not |
| --- | --- | --- |
| Runner-owned `SupervisorJob` scope (chosen) | The shared run outlives any single caller; waiters stay independent | The runner must own/cancel its scope deliberately |
| Keep the caller’s `coroutineScope` (current) | No extra scope | A caller cancellation cancels every waiter |
| `withContext(NonCancellable)` | Simple | It also prevents legitimate shutdown cancellation and hides failures |
