# Proposal — app-limits

## Why

Foco measures usage but cannot enforce a self-imposed daily budget. The requested behavior is a
per-app daily limit configured in Settings in 15-minute steps; an app with no limit is never blocked.
The monitor cannot rely on UsageStats alone: it needs a foreground service to check today's usage and
an accessibility service to see the current app and show the block screen. The tick is **2 minutes**
to keep the battery cost bounded; accessibility events cover the app-open case immediately.

## What Changes

- Settings gains an **App limits** section: every launchable app shows its current limit and a picker
  with **No limit**, **Block immediately (0 min)** and 15-minute steps up to 24 h.
- Limits live in a dedicated Room database in private storage (`limits.db`, one `app_limit` table
  keyed by package). No row means no limit; `0` means block as soon as the app comes to the foreground.
- `UsageMonitorService` runs as a foreground service with the `specialUse` type on Android 14+ (a
  plain foreground service below 34) while at least one limit exists. It checks the current foreground
  app every **2 minutes** and on accessibility foreground events, using today's UsageStats sessions.
- `UsageMonitorAccessibilityService` provides the foreground package and hosts a full-screen
  accessibility overlay when a limited app is at or over its limit: it names the app, shows a
  **3-second countdown**, then removes the overlay and sends the app home.
- The app-entry gate also requires the accessibility service; onboarding explains both permissions
  and offers both shortcuts.
- The monitor starts when the first limit is saved, restarts after boot, and stops when no limit
  remains. Accessibility is mandatory; if it is revoked, enforcement stops and the gate asks for it
  again.
- 100 % local: no network permission, no upload, `allowBackup` stays `false`.

## Expected outcome

The user sets Instagram to 30 min; after 30 min of today's use, the next 2-minute check (or the next
time Instagram comes to the foreground) shows the limit screen for 3 seconds and returns home. An app
without a limit is never blocked. The limit and today's usage survive a reboot, and the monitor does
not poll in a tight loop.

## State and persistence mechanism

Limits are a small package → minutes map. They live in a **dedicated Room database** `limits.db` in
the app's private storage, created on first use and living as long as the app; the `app_limit` table
is keyed by `packageName`. The existing history database `foco.db` and its eight-day retention are
untouched, so there is no migration and no overlap with the consolidation changes. No limit is the
absence of a row; `0` is stored explicitly and means block immediately. The day's used time is read
from the existing `UsageRepository` UsageStats query, never duplicated. No network and no new
dependency.

## Alternatives considered

| Option | Why yes | Why not | Recommendation |
| --- | --- | --- | --- |
| Dedicated Room `limits.db` | Room as requested; one small table; no migration of the history DB; no overlap with consolidation changes | A second database instance | **Chosen** |
| `app_limit` table in the existing `foco.db` | One database; transactional with history | Version-4 migration, schema export and a shared DB provider; collides with `consolidation-raw-reset` (draft) on `SessionHistoryStore`/DAO | No |
| DataStore `limits` | Smallest change, existing `ExcludedAppsStore` pattern | The human explicitly chose Room, not settings | No |
| FGS 2-minute tick + accessibility | Bounds battery while keeping mid-use overshoot small; accessibility makes app-open immediate | FGS, notification and permission complexity; tech brief | **Chosen** |
| FGS 5-second tick | Tight mid-use blocking | Wastes battery; the requested cadence is 2 minutes | No |
| WorkManager 15-minute job | Reuses the existing scheduler | Cannot meet a 2-minute check or draw/close an app | No |
| Accessibility overlay | Requested; can draw and send home without `SYSTEM_ALERT_WINDOW` | Accessibility is a sensitive permission; mandatory onboarding | **Chosen** |
| `SYSTEM_ALERT_WINDOW` overlay | No accessibility | Extra permission; cannot close the app; not requested | No |
| Keep `minSdk 29` with a 34+ `specialUse` fallback | Works on Android 10–13 and 14+ | Manifest/start nuance on 29–33; verify on the API 36 AVD | **Chosen** |
| Raise `minSdk` to 34 | Simpler foreground-service type | Drops Android 10–13 | Only if the fallback fails |

## Risks and limits

- A normal app cannot force-stop another app. "Close" means remove the overlay and
  `GLOBAL_ACTION_HOME`; the limited app goes to the background.
- `specialUse` is API 34+. The plan keeps `minSdk 29` and starts the service without a type below 34;
  if a device rejects it, raise `minSdk` as the human allowed.
- Mid-use enforcement is bounded by the 2-minute tick; app-open enforcement is immediate through
  accessibility.
- The ongoing notification is subject to `POST_NOTIFICATIONS` on Android 13+; the service still runs
  in the system's active-apps surface if the permission is denied. The settings flow requests it.
