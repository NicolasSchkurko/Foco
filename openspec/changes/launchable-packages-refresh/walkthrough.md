# Walkthrough — launchable-packages-refresh

## Mechanism

`UsageRepository` answers "which packages are user-facing apps" from `launchablePackages`
(`UsageRepository.kt:30-36`), a `by lazy` set built once from the launcher query. The repository
instance is owned by `UsageMonitorEvaluator`, which `UsageMonitorService` builds lazily in
`onCreate` and keeps while the service lives. `sync()` only re-delivers `onStartCommand`, so nothing
rebuilds the evaluator or its repository: an app installed after service start never enters the set,
`SessionFilter` drops its sessions at `:111`, `usedSecondsToday` reads 0, and the limit rule can
never hold for it. The fix turns the set into a re-resolved read (function per use, or cache with
explicit invalidation), so every consumer — monitor, dashboard, detail, limits picker — sees the
same current set at read time.

## Physical traceability

- `app/src/main/java/com/foco/app/data/UsageRepository.kt:30-36,52,111` — the set, its accessor,
  the session filter.
- `app/src/main/java/com/foco/app/data/UsageMonitorService.kt:44,159-167` — single evaluator per
  service life; `sync()` does not re-run `onCreate`.
- `app/src/main/java/com/foco/app/data/UsageMonitorEvaluator.kt:14` — the repository the monitor
  holds.
- `app/src/test/java/com/foco/app/data/SessionFilterTest.kt` — JVM proof the late package counts.

## Limits and failure modes

- A launcher query per read is I/O: acceptable on the monitor's 2-minute cadence and the UI's
  per-day read (both off the main thread). If profiling shows cost, the fallback is a cached set
  with an invalidation hook — recorded as a task, not silent.
- Packages disabled but not uninstalled disappear from the launcher query; sessions of a
  now-disabled app stop counting, which matches "only user-facing apps count".
- No persistence, no schema, no new dependency.

## Touched APIs

`PackageManager` launcher query only, through the existing adapter — same API the repository already
uses, only called more often. No network, no storage format change.

## Implicit decisions

- Re-query over package broadcasts: one code path instead of an event stream with its own filters
  (alternatives table in `proposal.md`).
- The fix lives entirely inside `UsageRepository`, so no other active change's targets are touched
  (the parallel monitor changes own `UsageMonitorService.kt` / the accessibility service).
