# Findings — usage-history-foundation

Verified in the `/spec-verify` pass of 2026-10-19 against the code at `030f105`. None of them
breaks a requirement of the delta spec: all three requirements are implemented and tested. They are
gaps the tests do not cover.

- [x] `week(until)` is production API with no requirement and no test — evidence: `app/src/main/java/com/foco/app/data/SessionHistoryStore.kt:36-37` has no caller in `app/src/main` (only the planned consumer at `openspec/changes/week-screen/tasks.md:14`), and the foundation's delta spec has no requirement about reading stored days.
- [x] `HistoryRetentionTest` claims a retry it never performs — evidence: `app/src/test/java/com/foco/app/data/HistoryRetentionTest.kt:51-57` is named "a day without usage is left for the next run" but only asserts that nothing is written; no second consolidation is run, so the retry is unproven.
- [x] `consolidate` skips today by position, not by contract — evidence: `app/src/main/java/com/foco/app/data/SessionHistoryStore.kt:30` uses `availableDays(today).drop(1)`, and only `app/src/main/java/com/foco/app/domain/AvailableDays.kt:12` (comment) says today comes first; nothing at the call site names that coupling.
- [x] The store's `zone` only moves the prune cutoff while the days come from another calendar — evidence: `SessionHistoryStore.kt:54` uses the injected `zone`, while the reader builds its window with `ZoneId.systemDefault()` (`app/src/main/java/com/foco/app/data/UsageRepository.kt:61`); passing any other zone silently mixes two calendars.
- [x] A backfill failure is swallowed with no trace — evidence: `app/src/main/java/com/foco/app/MainActivity.kt:87-89` catches `Exception` and ignores it, so a permanent failure (e.g. a schema mismatch after a future version bump) shows only as missing history.

## Dropped on review

- `SessionRecordDao.sessionsBetween` has no production caller: deliberate scaffolding, declared in
  `tasks.md` 2.3, exercised by `FocoDatabaseTest` and consumed by the next change
  (`detail-history-days`). Not a finding.

## Disposition proposed

- First item: **decision request** (add the requirement + test here, or let `week-screen` own it).
- The rest: low-severity polish, one small follow-up (test rename + one real retry assertion, a named
  helper for "today is not final", one comment tying both zones, a one-line trace on failure).
- No waivers, no conflicts: no requirement is contradicted and no ADR is reversed.

## Closure

Closed on 2026-10-19 by `history-storage-followups` (`refactor`): the window read is now a requirement with its test, the retry is exercised, `finishedDays` names the rule, the store keeps one calendar, and a failed backfill logs. None of the five was a contract violation, so no waiver was needed.
