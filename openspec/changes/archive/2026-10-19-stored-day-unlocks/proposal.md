# Proposal — The finished day's unlock count, stored

## Why

A finished day's sessions and per-app totals survive in the app's own database
(`daily_aggregate`), but the **unlock count** of that same day does not: it is derived live from the
platform's keyguard events (`UsageRepository.loadDay` → `DayData.unlockTimes`,
`openspec/specs/usage-tracking/spec.md:184-185`) and nothing stores it. The moment the platform stops
serving the day the count is gone, while the rest of the day is still readable. A view of a past day
can therefore only show a number it does not have — or a false zero.

## What Changes

- A new table, one row per consolidated day: the day (ISO text key, the shape `daily_aggregate`
  already uses) and its unlock count.
- The consolidation writes it with the day's totals, from the same platform read it already does; no
  second pass and no new permission.
- A read on the store for one day's count, answering "no stored count" distinctly from "zero
  unlocks".
- The count joins the **totals** side of retention: it is never pruned, because outliving the
  platform's window is its whole point.
- The schema leaves version 1: the first bump, with the migration that adds the table and a test
  that proves the upgrade keeps what was stored.

## State and persistence mechanism

- **Where**: the existing `foco.db` (private storage, built once per process by
  `SessionHistoryStore.forApp`) gains one table next to `session_record` and `daily_aggregate`.
  Nothing leaves the device; `allowBackup="false"` stays.
- **Writes**: only the consolidation path, which already receives the day's `DayData` from the
  platform reader — the count rides along with the totals it stores today.
- **Reads**: one query for one day, next to the existing `storedDays()` / `sessionsFor(date)` /
  `week(until)`.
- **Upgrade**: `version = 2` plus a `Migration(1, 2)` that creates the table; `exportSchema` keeps
  the new baseline committed, and there is no destructive fallback (the policy the foundation
  recorded).

## Expected outcome

A past day's unlock count is there to be shown, exactly like its sessions and totals, so the dated
day view can state the number instead of guessing it. We will know it was worth it when that view
shows the count of a day the platform has already forgotten.

## Alternatives considered

| Option | Why yes | Why not / status |
|---|---|---|
| **Own table, count per day, written by the consolidation, kept like the totals (chosen)** | The smallest datum that answers the only use; follows `daily_aggregate`'s shape; one writer, no second read of the platform | A migration is needed, and days consolidated before it keep no count until a later change re-considers them |
| Store the unlock timestamps (a list per day, as the live read has them) | Enables future questions (at what time did I unlock) | More rows, more sensitive data on disk, and nothing reads them today — **rejected by the human** |
| Do it inside `week-screen` | One change less | Mixes two ideas and drags a schema migration into a UI change — **rejected by the human** |
| Keep deriving it live and show nothing for past days | No migration at all | The dated day view could only show a number it does not have — the reason this change exists — **rejected** |
| A nullable column on `daily_aggregate` | No new table | That table is per app and day, so the count would be repeated per app or set to null on all but one row — **rejected** |

## Non-goals

- **No per-app unlocks and no timestamps**: the count per day, as chosen.
- **No display work**: how a past day shows it belongs to `week-screen`, which depends on this change.
- **No change to today**: the live read stays exactly as it is.
- **No retro-fill**: days consolidated before this change have no stored count; a later change can
  decide to re-consider them.

## Impact

- `tech_brief: true`: a new table, a version bump and a new test dependency are worth a walkthrough.
- Targets include `app/build.gradle.kts` and `gradle/libs.versions.toml` (the `room-testing` test
  dependency), which the two drafts `nightly-consolidation` and `schema-drift-guard` also claim — the
  gate sequences them by deps.
- No protected zone (`openspec/config.yaml` → `protected: []`).
- Reverses the non-goal recorded by the archived `unlock-counter`
  (`openspec/changes/archive/2026-10-16-unlock-counter/proposal.md:12`); the reversal is the human's
  decision, recorded in `DECISIONS.md`.
