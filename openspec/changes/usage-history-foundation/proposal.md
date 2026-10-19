# Proposal — The history outlives the platform's retention

## Why

Phase 2 of `PLAN.md` exists because the tracker is a **lens on data it does not own**: `UsageRepository.loadDay` re-queries `UsageStatsManager.queryEvents` on every screen (`data/UsageRepository.kt:60-105`), and the platform only serves a few days. The day selector is capped at seven for that reason (`domain/AvailableDays.kt`, `docs/mechanisms/ui-flow.md:72`). Nothing is wrong with the reading — it is the writing that is missing: **a day the platform drops is gone**, and it is gone for good.

The same pass found the one privacy hole that already exists today, before any history: `android:allowBackup="true"` (`AndroidManifest.xml:31`) lets Auto Backup copy the app's data (settings, exclusions, and any future database) to the user's cloud account and restore it on another device. That contradicts the project's first principle ("everything local") in one attribute.

This change is the **foundation slice** of Phase 2: start accumulating history now, before the window rolls, and close the backup door in the same move. The Worker and the Week screen come later, when there is history to show.

## What Changes

- `usage-tracking` gains three requirements: the day's data outlives the platform's retention; the stored history is bounded (raw seven days, totals kept); stored usage data does not leave the device.
- A new `data/db/` with Room: `FocoDatabase`, `SessionRecord`, `DailyAggregate` and their DAOs (one type per file, as the repo demands).
- `data/SessionHistoryStore`: consolidates the days the platform still serves into the database and reads them back. It is an **adapter** — the pure fold (`buildSessions`), the window (`dayWindow`) and `availableDays` stay untouched.
- Consolidation runs once on app start, off the main thread, and only for days that are not stored yet, so the steady state costs one query and no platform read.
- `AndroidManifest.xml`: `android:allowBackup="false"`, pinned by a manifest test.
- New dependencies: `room-runtime` + `room-ktx` and the KSP plugin; `exportSchema` writes the schema to `app/schemas/`, which is committed as the migration baseline.

## State and persistence mechanism

- **Where**: a SQLite database (`foco.db`) in the app's private storage (`/data/user/0/com.foco.app/databases/`), owned by Room. Other apps cannot read it (per-uid sandbox); the device's file-based encryption covers it while the phone is locked. `app/schemas/<db>/1.json` is the schema, not the data.
- **What**: `session_record` (`packageName`, `start`, `end`; primary key `packageName + start`) and `daily_aggregate` (`packageName`, `day`, `totalMillis`; primary key `packageName + day`). The composite key is what makes a second consolidation of the same day an overwrite instead of a duplicate.
- **Lifecycle**: written once per app start for the previous six days (today stays live from the platform, its data is not final); raw sessions pruned after seven days; totals kept for every consolidated day.
- **Reuse**: the store only calls what already exists (`availableDays`, `loadDay`) and maps its output; no domain function grows a dependency, no UI reads the database yet.
- **Leaving the device**: only by uninstall. No backup (`allowBackup="false"`), no network permission (the app declares none), no export.

## Expected outcome

After eight days with the app opened at least once a day, a day the platform no longer serves still yields its sessions and its per-app total — and the app's data cannot be restored onto another device from a cloud backup. We will know it was worth it when the Week screen can be built without touching the platform ever again.

## Alternatives considered

| Option | Why yes | Why not / status |
|---|---|---|
| **Room + KSP, raw 7 days, totals beyond (chosen)** | SQL checked at compile time, `Flow` for the future Week screen, versioned migrations from v1 | Costs a dependency plus the KSP plugin, and version pinning against AGP 9.4 / Kotlin 2.4 has to be proven by a build — **chosen by the human (2026-10-19)** |
| `SQLiteOpenHelper` by hand | No new dependency; two tables and three queries fit | The SQL is not verified until runtime, migrations are hand-written, no `Flow` — rejected |
| Aggregates as JSON in DataStore | No new dependency; reuses the store pattern | Cannot query a range, rewrites the whole file, no real migrations — rejected |
| Raw for 14 days (retention) | Cheap re-derivation if the metric changes | Doubles the exposure window for data nobody reads — rejected |
| Everything forever | Maximum re-analysis | Maximum exposure, and the raw rows stop being useful once the totals exist — rejected |
| Full Phase 2 in one change (entities + Worker + Week + migrations) | Closes the PLAN item at once | Too many moving parts in one apply, and the history would start accumulating only when the Week screen lands — rejected for now (the user chose the foundation slice) |

## Non-goals

- **No Worker and no Week screen**: consolidation on app start is enough for a personal app opened daily; both arrive when there is data to show.
- **The day selector does not change**: `Day selection within system retention` stays true (the UI keeps offering the platform's window); the Week screen is what will revisit it, in a later change.
- **No encryption layer** (SQLCipher): file-based encryption already protects the device at rest, and the remaining exposure (an unlocked phone in someone's hands) is a UI-lock decision, not a storage one.

## Impact

- New dependency family (`room-*` + KSP): a build risk, not a logic risk — proven first thing in the tasks.
- `tech_brief: true`: this change writes to disk and reads a platform API, so it needs `walkthrough.md` plus the human's comprehension sign-off before archiving.
- No protected zone today (`openspec/config.yaml` → `protected: []`). If the human wants future changes to touch `data/db/` only with explicit approval, that glob belongs in `config.yaml` — a human edit, not part of this change.
