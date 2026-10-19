# Proposal — The nightly consolidation job

## Why

`PLAN.md` Phase 2 asks for a `WorkManager` job that consolidates the day and cleans up old raw data. The foundation change made consolidation run **once per app start** (`MainActivity.onCreate`), which is enough only if the app is opened often: the raw window is seven days, so **eight days without opening Foco lose those days for good** — the platform drops them and nothing stored them, because consolidating was something only an app start could trigger.

This change closes that gap without touching the store's policy: the same `consolidate(today)` runs from a second trigger, and the two triggers are serialized.

## What Changes

- New dependency: `androidx.work:work-runtime-ktx` (and `work-testing` for the device test).
- `ConsolidationWorker` (`CoroutineWorker`): calls the shared runner and maps the outcome — success, or retry with backoff.
- `ConsolidationScheduler`: enqueues **one** unique periodic job (`24 h`, first run deferred to the next **03:00 local**, no charging constraint) with `ExistingPeriodicWorkPolicy.KEEP`, so a second app start does not reset the schedule.
- `ConsolidationRunner`: one `Mutex` around `SessionHistoryStore.consolidate(today)`, used by both the app-start trigger and the worker. A second trigger waits for the run in progress instead of starting another one.
- `MainActivity.onCreate`: keeps the start trigger (now through the runner) and schedules the job. The foundation's trigger is **extended, not replaced**.

## State and persistence mechanism

- **What WorkManager stores**: its own schedule in its private database (`/data/user/0/com.foco.app/databases/androidx.work.workdb`). It holds no usage data; it survives reboots and process death, which is why the job does not need a boot receiver.
- **What the job writes**: the same two tables the foundation owns — `session_record` (raw) and `daily_aggregate` (per-app, per-day totals) — through `SessionHistoryStore.consolidate(today)`. It reads the platform with the same reader the store already takes.
- **Order matters**: consolidation first, pruning second; a run that fails prunes nothing.
- **Nothing leaves the device**: the job runs in the app's process, uses no network, and `android:allowBackup="false"` (from the foundation) already keeps the database out of cloud backups.
- **Lifecycle**: the schedule is created on first app start and kept; there is no way to run it on demand from the UI (the app-start trigger is that).

## Expected outcome

History stops depending on the user opening the app. We will know it was worth it when, after **eight or more days without opening Foco**, the week view still shows every one of those days — and when `dumpsys jobscheduler` lists exactly one job for the package, however many times the app is opened.

## Alternatives considered

Option 0 is the contract this change reuses rather than rebuilds: `SessionHistoryStore.consolidate(today)` from `usage-history-foundation`, already idempotent and already the owner of the retention policy.

| Option | Why yes | Why not / status |
|---|---|---|
| **WorkManager periodic, 24 h, 03:00 hint, `KEEP`, plus the app-start trigger (chosen)** | Guaranteed deferrable work that survives reboots, no exactness it does not need, one dependency, and the store's policy untouched | The hour is a hint (Doze may run it hours later); adds a dependency and a second trigger that must be serialized — **chosen by the human (2026-10-19)** |
| `AlarmManager` with an exact alarm | Fires at the chosen minute | Needs `SCHEDULE_EXACT_ALARM` on Android 12+, a `BroadcastReceiver` plus a boot receiver, and wakes the device precisely for a job that only needs to happen "that night" — rejected |
| Keep only the app-start trigger | Zero dependency, zero new code | Loses every day beyond the raw window when the app is not opened — that is the problem being solved — rejected |
| `WorkManager` every 15 minutes, or a foreground service | Near-real-time consolidation | That mechanism belongs to Phase 3 (limits and monitoring); a permanent notification or 96 wake-ups a day to prune a personal database is disproportionate — rejected |

## Non-goals

- **No Week screen and no detail changes**: `week-screen` and `detail-history-days` consume the stored data; they do not depend on this job.
- **No change to retention** (seven days of raw, totals kept) or to the store's policy: the job is a trigger, not an owner.
- **No exact timing, no manual "sync now" control**, and no work while the app is force-stopped (Android cancels it until the next start).
- **No limits, reminders or monitoring**: Phase 3.

## Impact

- `tech_brief: true`: a second writer into the same tables, on a schedule the OS controls, is exactly the concurrency story a walkthrough owes.
- Depends on `usage-history-foundation`: it shares `MainActivity.kt`, `gradle/libs.versions.toml` and `app/build.gradle.kts` with it, so the gate treats those overlaps as sequenced.
- No protected zone (`openspec/config.yaml` → `protected: []`).
