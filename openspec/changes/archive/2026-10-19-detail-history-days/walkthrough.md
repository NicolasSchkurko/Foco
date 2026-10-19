# Walkthrough — detail-history-days (tech brief)

> For the human: this brief explains the mechanism of the change. Answer the questions in
> `## Cognitive sign-off` yourself; the agent never fills them.

## Mechanism

The detail screen no longer asks the platform for past days. It asks its own database, and keeps the
platform read only for today. The screen is opened by tapping an app row in the dashboard:
`FocoNavHost.kt:21` navigates to `detail/<package>`, the route is declared at `FocoNavHost.kt:30-37`
and calls `AppDetailScreen(packageName)` (`AppDetailScreen.kt:67`).

One day's read, hop by hop:

1. The screen builds its two read doors (`AppDetailScreen.kt:70-71`): a `UsageRepository` (the
   platform) and a `SessionHistoryStore.forApp(context) { date -> repository.loadDay(date) }` (the
   database; the lambda is the store's way back to the platform, `SessionHistoryStore.kt:74-84`). It
   freezes the day it considers "today" (`AppDetailScreen.kt:72`) and starts with no stored days
   (`:73`).
2. On IO, and again every time the screen resumes, it loads which days the store has
   (`AppDetailScreen.kt:80-87`) →
   `SessionHistoryStore.kt:38-40` → the SQL `SELECT DISTINCT day FROM daily_aggregate`
   (`DailyAggregateDao.kt:15-16`).
3. The window: `availableDays(today, storedDays)` (`AppDetailScreen.kt:89`) →
   `AvailableDays.kt:10-15`. The floor is the oldest stored day inside `today - 6 … today - 1`
   (`:12-13`, falling back to today when nothing is stored), and the answer is every day from today
   down to that floor (`:14`) — a contiguous run, seven entries at most.
4. The index and the two arrows (`AppDetailScreen.kt:74,90`): the list is newest first, `0` is today;
   ‹ moves one day back and disables at `days.lastIndex` (`:161-166`), › moves forward and disables at
   `0` (`:178-183`), and the middle label names the selected day (`:167-177`).
5. The day's read re-runs on every day change (`AppDetailScreen.kt:91-104`). The source is the pure
   rule (`:97` → `DetailDaySource.kt:9-13`): today is `Live` (`:98` → `UsageRepository.kt:60-67` →
   `UsageStatsManager.queryEvents` at `:66`), a day the store has is `Stored` (`:99` →
   `SessionHistoryStore.kt:42-48` → the SQL `WHERE start >= :from AND start < :to`, half-open on
   `start`, `SessionRecordDao.kt:15-16`), and anything else is `Absent` → an empty list (`:100`).
   Neither read can kill the screen: both go through `readOr` (`:84`, `:96`), which rethrows
   cancellation, logs the failure and answers the fallback (`AppDetailScreen.kt:287-299`).
6. The whole day's sessions become that one app's detail: `appDayDetail(sessions, packageName,
   ZoneId.systemDefault())` (`AppDetailScreen.kt:106-108`) → `AppDetail.kt:10-12`: it filters by
   package, sorts by start, and returns **null** when that app has no session that day.
7. The screen renders (`AppDetailScreen.kt:188-282`): the spinner while `sessions` is null
   (`:190-197`), the empty state when the derivation is null (`:199-210`, `R.string.detail_empty`),
   otherwise the hour chart, the summary rows and the session list (`:212-281`).

The one-line version: **today is read live, every day back to the oldest stored one is offered and
read from the database, and a day with rows nowhere shows the empty state** — both paths hand the
existing derivations the same `List<UsageSession>` they already consumed.

## Physical traceability

- **Where the state lives**: one SQLite database, `foco.db`, in the app's private storage
  (`/data/data/com.foco.app/databases/` on device). The name is `DATABASE_NAME`
  (`SessionHistoryStore.kt:68`) and it is built once per process by `Room.databaseBuilder` behind a
  `@Volatile` + `synchronized` singleton (`SessionHistoryStore.kt:71-83`): created on first use,
  living as long as the app process.
- **The two tables** (`FocoDatabase.kt:10-15`, version 1, exported schema under `app/schemas/`):
  - `session_record` (`SessionRecord.kt:10`): one raw session, key `(packageName, start)`, an index on
    `start`. **This is what this change reads** for a past day (`SessionRecordDao.kt:15-16`, used from
    `SessionHistoryStore.kt:46`).
  - `daily_aggregate` (`DailyAggregate.kt:9`): one per-app, per-day total, key `(packageName, day)`,
    `day` as ISO text. **This change reads it only to know which days exist**
    (`DailyAggregateDao.kt:15-16` from `SessionHistoryStore.kt:38-40`); it never reads the totals
    themselves.
- **Who writes them**: nobody in this change. The only writer is the startup backfill
  `SessionHistoryStore.consolidate(today)` (`SessionHistoryStore.kt:27-32`), called from
  `MainActivity.onCreate` via `consolidateHistory()` (`MainActivity.kt:57,79-95`), on IO (`:82`) and
  once per process guarded by an `AtomicBoolean` (`:80,98`). It reads the platform through `readDay`,
  writes raw rows first and totals second (`SessionHistoryStore.kt:50-56`), then prunes.
- **Lifecycle / retention**: raw rows are deleted when they leave the seven-day window —
  `sessions.pruneBefore(rawCutoff(today))` (`SessionHistoryStore.kt:31`), the SQL being
  `DELETE FROM session_record WHERE end < :cutoff` (`SessionRecordDao.kt:18-19`), with the cutoff at
  the start of `today - 6` days on the system zone (`SessionHistoryStore.kt:64-65`, `SIX_DAYS` at
  `:69`). Totals are never deleted: `DailyAggregateDao` has no delete query, so a day's totals
  outlive its raw rows (the stored-history contract, `openspec/specs/usage-tracking/spec.md:245-251`).
- **Nothing leaves the device**: no network call, no new permission, no write in this change, and
  `allowBackup="false"` keeps the database out of cloud backup
  (`openspec/specs/usage-tracking/spec.md:257-263`).

## Limits and failure modes

- **The window is at most seven days** (`AvailableDays.kt:12`, `windowDays = 7` by default), because
  raw rows only live seven days (`SessionHistoryStore.kt:64-65`). Today counts inside those seven:
  today plus six stored days. `DaySelectionTest.kt:19-26` pins the full window, `:40-48` pins that a
  stored day older than it does not extend it.
- **A day inside the window with no stored rows is offered and renders the empty state.** That is the
  normal path now, not a corner case: the list is contiguous (`AvailableDays.kt:14`), the day is not
  in `storedDays`, so the rule answers `Absent` (`DetailDaySource.kt:11-12` → `AppDetailScreen.kt:100`)
  and the screen shows `R.string.detail_empty` (`:199-210`). Pinned by `DaySelectionTest.kt:28-37`
  ("a day with no totals inside the window is still offered") and exercised on device: the seeded day
  sits three days back and the test proves one step back is *yesterday*, not the seeded day
  (`AppDetailNavigationTest.kt:117-126`). A gap day exists because `consolidateDay` writes nothing for
  a day the platform served no sessions for (`SessionHistoryStore.kt:52`).
- **Today is never consolidated**: `finishedDays` is `today - 1 … today - 6`
  (`AvailableDays.kt:18-21`) and `consolidate` stores only those, so a row for today says nothing. The
  rule checks today **first** (`DetailDaySource.kt:9-10`), so today is `Live` even when the store
  happens to have a row for it (`DetailDaySourceTest.kt:18-21`).
- **A day whose raw rows were pruned renders the empty state**: `sessionsFor` is a plain query, no rows
  means an empty list (`SessionHistoryStore.kt:42-48`), `appDayDetail` returns null (`AppDetail.kt:12`)
  and the screen shows the empty state (`AppDetailScreen.kt:199-210`). This is reachable only if the
  device date moves **backwards** after a prune (the read bounds by `start` at the start of
  `today - 6`, the prune deletes `end < start(today - 6)`, so while the date only moves forward an
  offered day keeps its rows), or if the database is touched outside the app.
- **An empty (or still loading) store leaves the selector at today alone**: `storedDays` starts null
  and `availableDays` falls back to today (`AppDetailScreen.kt:73`, `AvailableDays.kt:13`), so both
  arrows are disabled and the screen stays usable (`DaySelectionTest.kt`, "nothing stored offers today
  alone").
- **The index is never re-clamped**, which is safe because the run can only grow from `[today]`
  (`AppDetailScreen.kt:89`): a resume re-reads the stored days and only ever appends older days, so
  the selected index still points at the same day. The arrows stop at `0` and `lastIndex`
  (`:161-166`, `:178-183`), so it cannot be pushed out of range.
- **A failing read degrades instead of crashing.** Both reads go through `readOr`
  (`AppDetailScreen.kt:287-299`): `CancellationException` is rethrown (`:294-295`), any other failure
  is logged (`:297`) and the fallback is answered — which days are stored degrades to today alone
  (`:84`), and a day's sessions degrade to an empty day (`:96`), i.e. the screen shows the empty state
  for that day. What is lost: that day's numbers until the day changes again (there is no retry).
  Before this hardening an exception from Room escaped the `LaunchedEffect` and killed the app. It has
  its own requirement in the delta and its own device test (`AppDetailReadTest`): the fallback is
  answered and cancellation still propagates (task 7.1); the logger's shape is the backfill's catch
  (`MainActivity.kt:86-93`).
- **The window refreshes on resume, not continuously**: `storedDays` is re-read every time the screen
  reaches `RESUMED` (`AppDetailScreen.kt:80-87`), so a day the startup backfill stores while the
  detail sits open waits for the next resume — pinned on device, where a day seeded with the screen
  open becomes reachable after a resume and the walk grows by exactly those days
  (`AppDetailNavigationTest.kt:139-155`). A resume that lands before the backfill finishes can still
  read a short window, and the following resume corrects it.
- **The day is frozen for the screen's lifetime**: `today = remember { LocalDate.now() }`
  (`AppDetailScreen.kt:72`). Leaving the app open across midnight keeps the old day as "today" until
  the screen is recreated.
- **Force close / kill**: nothing here is written, so nothing is lost. Concurrency is a background
  backfill writing and pruning on IO while this screen reads on IO; Room serializes them, and the worst
  case is a read that sees a day's rows just before they are pruned — one frame of a day that is
  leaving the window anyway. The `AtomicBoolean` (`MainActivity.kt:80,98`) keeps two backfills from
  running at once, and the DAO upserts are `REPLACE` on a composite key, so a retried day cannot
  duplicate rows (`SessionRecord.kt:10`).
- **No network at all**: a missing day is a missing row in a local table.

## Touched APIs

- **`androidx.room` `@Query` (Room 2.8.5, already in the project)** — no new dependency.
  `SessionRecordDao.sessionsBetween(from, to)` (`SessionRecordDao.kt:15-16`) gets its first production
  caller here; it was declared as scaffolding by the foundation
  (`openspec/changes/archive/2026-10-19-usage-history-foundation/tasks.md:13`) and flagged as having
  no caller in that change's `findings.md:15`. It is half-open on `start`, which is why the store
  passes `dayStart` and `nextDayStart` (`SessionHistoryStore.kt:44-46`).
- **`DailyAggregateDao.storedDays()`** (`DailyAggregateDao.kt:15-16`) — `SELECT DISTINCT day`, the only
  way the app knows which days have totals. Already used by `consolidate` (`SessionHistoryStore.kt:28`);
  this change reuses it through the store wrapper (`:38-40`) instead of writing a second query.
- **Platform read, kept for today only**: `UsageRepository.loadDay` (`UsageRepository.kt:60-67`) →
  `UsageStatsManager.queryEvents` (`:66`), with the day window built by `dayWindow`
  (`DayWindow.kt:18-26`). Same `ZoneId.systemDefault()` calendar the store's bounds use
  (`SessionHistoryStore.kt:44-45,64-65`), so "a day" means the same thing on both sides.
- **`android.util.Log`** for the degraded read (`AppDetailScreen.kt:297`), the same tag and shape the
  backfill uses (`MainActivity.kt:90`).
- **Compose state APIs**: `remember`, `mutableStateOf`/`mutableIntStateOf`,
  `LaunchedEffect(packageName, selectedDate, storedDays)` (`AppDetailScreen.kt:72-104`) — the read is
  keyed on the day, not on a callback, so switching days re-runs exactly the one read — and
  `LaunchedEffect(lifecycleOwner)` (`:81-87`) for the resume re-read. `Dispatchers.IO` + `withContext`
  (`:83`, `:93`) keeps both reads off the main thread.
- **`androidx.lifecycle`** (`repeatOnLifecycle`, `LocalLifecycleOwner`; already in the project through
  `lifecycle-runtime-compose`): how the stored days are re-read when the screen comes back
  (`AppDetailScreen.kt:80-87`). No new dependency.
- No new permission, no manifest change, no network, no writes, no new dependency.

## Implicit decisions

- **One calendar, `ZoneId.systemDefault()`**, for the stored day bounds (`SessionHistoryStore.kt:44-45`),
  the prune cutoff (`:64-65`) and the rendering zone (`AppDetailScreen.kt:107`). No zone is passed in, so
  crossing time zones shifts the day boundary by hours — consistent with the retention decision
  recorded for the foundation (`DECISIONS.md:317-318`).
- **Half-open day bounds on `start`** in the store read (`SessionHistoryStore.kt:44-46`), matching the
  DAO contract and never double-counting a midnight boundary. Note the asymmetry that already exists in
  the store: the **read** bounds by `start`, the **prune** bounds by `end` (`SessionRecordDao.kt:15-16`
  vs `:18-19`).
- **The window comes from the totals table, not from the raw rows** (`SessionHistoryStore.kt:38-40`),
  even though the detail needs raw sessions: it is the cheap "which days exist" question, and the same
  source `consolidate` uses to decide what is already done (`:28`). The floor is therefore a day with
  totals, and the run never reaches further back than `today - 6`.
- **The window walks every day, by decision after the human's review** (`AvailableDays.kt:14`). The
  first cut offered only the days with totals and skipped the gaps; the human asked for the day-by-day
  walk, so the list is now contiguous and a gap day shows the empty state (the scenario added at
  `openspec/changes/detail-history-days/specs/usage-tracking/spec.md:24-26`). Why: a ‹ › selector that
  jumps two days reads as a bug, and the empty state already existed for exactly this.
- **`windowDays` is a parameter, not a constant** (`AvailableDays.kt:10`); changing retention means
  changing it and `SIX_DAYS` (`SessionHistoryStore.kt:69`) together.
- **The plan's seam parameter on the screen (task 3.3) was deliberately dropped.** The screen keeps its
  original signature, `AppDetailScreen(packageName: String)` (`AppDetailScreen.kt:67`), so the
  instrumented test seeds the app's **real** store instead
  (`AppDetailNavigationTest.kt:159-169`: `SessionHistoryStore.forApp(targetContext) { … }` +
  `consolidate`) and launches the real activity. The assertion runs against the same database the
  shipped screen reads, which a seam would have replaced with a fake.
- **A failing read degrades, by decision after the human's review** (`readOr`,
  `AppDetailScreen.kt:287-299`): an exception from Room used to kill the app; now it costs that day's
  numbers and logs, and `CancellationException` is rethrown so coroutine cancellation still works.
  After the audit it is `internal` and carries a device test (`AppDetailReadTest`), and the delta
  declares the requirement (task 7.1).
- **The stored days are re-read on every resume, by decision after the audit** (task 7.2,
  `AppDetailScreen.kt:80-87`): the startup backfill writes while the user is already reading, so the
  window read once at mount could stay short for the life of the screen. `repeatOnLifecycle(RESUMED)`
  fixes it with APIs already in the project — no seam, no new dependency.
- **`DaySource` is a plain enum in `domain/`** (`DaySource.kt:4`) and the rule is a pure function
  (`DetailDaySource.kt:9-13`): no Android types, so it is JVM-testable (`DetailDaySourceTest.kt`) and
  the screen keeps a `when` with no branching logic of its own.
- **The empty state stayed as it was**: no new string, no "day not stored" variant, no distinction
  between "no data" and "data pruned" (`AppDetailScreen.kt:199-210`).

## Cognitive sign-off

Answer in your own words (the agent must not fill these):

1. Open the detail on today: which of the two reads answers for it, and why can today not come from the
   app's own database even when the database has a row with today's date?
   > **Answer (human, 2026-10-19):** "Today's read comes from the OS through the activity query. It
   > cannot come from the database because it has to be up to date to the moment the app is opened;
   > otherwise it would not show the data up to that moment."
   > Checked against the code: **correct.** The rule answers `Live` for today before it looks at the
   > store (`DetailDaySource.kt:9-10`) and consolidation only stores finished days
   > (`AvailableDays.kt:18-21`), so a stored row for today would necessarily be stale.
2. You go one step back from today and the screen shows "no data for this day" while the arrows still
   move. Which table is missing that day's rows, and which part of the code turns that missing day into
   the empty state?
   > **Answer (human, 2026-10-19):** "The one without rows is `session_record`. And the part is the
   > `when`: `DaySource.Absent -> emptyList()` inside the day's read."
   > Checked against the code: **correct, with one refinement.** Both tables lack a gap day, but what
   > turns it into an empty list is the rule answering `Absent` (`DetailDaySource.kt:9-13`), because
   > `storedDays` — which comes from `daily_aggregate` (`SessionHistoryStore.kt:38-40`) — does not list
   > that day; `session_record` is not even queried for it (`AppDetailScreen.kt:96-102`). The empty list
   > then makes `appDayDetail` return null (`AppDetail.kt:12`) and the screen draws `R.string.detail_empty`
   > (`AppDetailScreen.kt:199-210`).
3. The database read fails once. What does the screen show for that day, what did the user lose, and
   why is that better than the app closing?
   > **Answer (human, 2026-10-19):** "The user sees 'no data' (not an error), the app stays alive, and
   > what is lost are that day's numbers until the day changes — there is no retry. It is a read of local
   > data: failing costs information, not the app; closing costs everything, including the other days
   > that may read fine."
   > Checked against the code: **correct.** The failure is caught in `readOr`
   > (`AppDetailScreen.kt:287-299`), so the exception no longer escapes the `LaunchedEffect` (`:91-104`)
   > the way it did before hardening; the log line stays in logcat (`:297`) and never reaches the screen;
   > the day's read answers an empty list (`:96`), so `appDayDetail` returns null (`AppDetail.kt:12`) and
   > the screen draws the empty state it already had (`AppDetailScreen.kt:199-210`); `CancellationException`
   > is rethrown so cancellation still works (`:294-295`).

## Comprehension sign-off

The sign-off is the human's; the agent never fills it. Recorded in `/spec-verify`.

understood_by: Nschkurko
understood_at: 2026-10-19

This sign-off was recorded before the audit's five corrections (tasks 7.1-7.5). Those changed the
window's freshness and the degraded read's evidence and wordings, none of which touches the mechanism
the three answers describe (today read live, past days read from the store, a failed read degrading to
the empty state), so the answers stand and were not re-asked.
