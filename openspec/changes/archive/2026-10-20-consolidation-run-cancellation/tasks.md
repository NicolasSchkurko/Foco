# Tasks — consolidation-run-cancellation

- [x] 1. Add `ConsolidationConcurrencyTest` coverage: cancel one caller while another waits; the
      shared run completes and the waiter receives `Done`.
- [x] 2. Run the shared job in a runner-owned `SupervisorJob` scope; callers await its `Deferred` and
      their cancellation stops only their wait.
- [x] 3. Ensure `ConsolidationWorker` still maps failure to `Result.retry()` and is not canceled by a
      caller.
- [x] 4. Re-run `ConsolidationConcurrencyTest`, `ConsolidationWorkerTest` and the JVM suite.
- [x] 5. Run `openspec validate consolidation-run-cancellation --strict` and the gate.
