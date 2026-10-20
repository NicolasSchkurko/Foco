# Proposal — merge-contiguous-sessions

## Why

A stop and a resume at the same minute produce two sessions that the UI renders with the same
`HH:mm` at the boundary (for example 03:44–03:44), and a chain such as 04:00–04:30 followed by
04:30–04:45 reads as two rows even though it is one continuous use of the app. The user wants those
boundaries joined, while different apps and different days stay separate.

## What

Add a pure `mergeContiguousSessions(sessions, zone)` in `domain/`:

- group by package, sort by start;
- merge a session with the previous one of the same app when their local date is the same and the
  previous `end` and the next `start` have the same `HH:mm`;
- repeat transitively, taking the earliest start and the latest end;
- never merge different apps or different local days.

Apply it in `UsageRepository.loadDay` after the day clamp/filter (the live construction) and in
`appDayDetail` before `visibleSessions` (the stored read). The raw `session_record` rows and the
`daily_aggregate` totals are unchanged, so already-stored days are normalized on read without a
migration.

## Expected outcome

A 03:44 end followed by a 03:44 start is one row; 04:00–04:30 followed by 04:30–04:45 is one
04:00–04:45; 03:44 followed by 03:45, two different apps, or the same app on another day stay
separate. The detail's session count, average and longest change accordingly; the day total, hourly
chart and week aggregates do not.

## Alternatives considered

| Option | Why yes | Why not |
| --- | --- | --- |
| Read-time normalization + live construction (chosen) | Covers old stored rows and new days; no migration; one pure rule | The export needs a decision if it should see merged rows |
| Exact-timestamp-only merge | No accidental joins | Would not fix the cases the user sees as the same `HH:mm` |
| Rewrite/backfill at consolidation | One canonical stored shape | Needs a migration, rewrites history, and still needs a read rule until the backfill runs |
| Merge only in the app detail UI | Smallest visible change | Misses the live construction and any other consumer; duplicates the rule |

## Boundaries and prior decisions

- The merge is a post-fold normalization, not a new event boundary: it does not change how
  resume/pause/stop events establish `start`/`end` (`DECISIONS.md:258-263`).
- The midnight clamp stays in force: a merge never joins the end of one day with the start of the next
  (`DECISIONS.md:251-256`).
- `hide-zero-second-apps` still applies to the list after the merge; two contiguous sub-second
  pieces become one visible session only if their merged duration reaches 1 s.
- **Open decision:** the weekly JSON export currently plans raw rows. Should it emit the merged
  sessions too? The app views and the live construction use merged data either way.

## Impact

- `UsageRepository.loadDay`, `appDayDetail`, a new pure `SessionMerge`, and their tests.
- No protected zone (`openspec/config.yaml` → `protected: []`); no new dependency.
