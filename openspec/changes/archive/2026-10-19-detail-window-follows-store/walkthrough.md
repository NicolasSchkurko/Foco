# Walkthrough — detail-window-follows-store (tech brief)

> For the human: this brief explains the mechanism of the change. Answer the questions in
> `## Cognitive Sign-off` yourself; the agent never fills them. The sign-off itself — the transcribed
> answers plus `understood_by` / `understood_at` — is recorded in `/spec-verify`, never by the agent.

## Mechanism

The change adds no table, no column, no write and no permission. It replaces **one read** — "which
days does the store have?" — with an **observation of the same query**, so that the startup backfill's
writes reach a detail screen that is never paused. Before this change the window was refreshed only by
a resume, and the residue that left is the one the audit filed
(`openspec/changes/archive/2026-10-19-detail-history-days/findings.md:16`: *while the screen is never
paused, the window is the one read at its last resume*).

The path, hop by hop:

**1. The value is one query.** The window is the distinct days of `daily_aggregate`:
`SELECT DISTINCT day FROM daily_aggregate`, now twice in the same DAO — the suspend twin
`DailyAggregateDao.storedDays()` (`DailyAggregateDao.kt:16-17`, text unchanged, kept for
`consolidate`) and the new `storedDaysFlow()` (`DailyAggregateDao.kt:19-21`) which returns the same SQL
as a `Flow<List<String>>`. `daily_aggregate` is one row per app and day, `day` in ISO-8601 text
(`DailyAggregate.kt:9-14`).

**2. The trigger is the app-start backfill, not the user.** `MainActivity.onCreate` calls
`consolidateHistory()` (`MainActivity.kt:57`), which guards the one-shot with an `AtomicBoolean`
(`:80`, field at `:98`) and runs on **IO** (`:82`), building the store over the platform reader and
calling `consolidate(LocalDate.now())` (`:84-85`), with every failure logged and swallowed
(`:86-93`). `consolidate` asks which days are stored (`SessionHistoryStore.kt:33`), subtracts them from
the six finished days (`:34`), consolidates each (`:35`) and prunes raw rows (`:36`). One day's writes
are raw sessions, totals, count (`:63-65`); the totals write is `aggregates.upsert(...)`
(`SessionHistoryStore.kt:64`).

**3. That write invalidates Room's tracker.** Because `storedDaysFlow` is a `@Query` returning a
`Flow`, Room registers it against the `daily_aggregate` table and re-runs the query every time that
table is written — no polling, no timer. The query runs on Room's query executor (the builder
configures none, so it is Room's default IO-backed one: `SessionHistoryStore.kt:87-91`), and the new
`List<String>` is emitted to the collector. One `upsert` is one invalidation, so a backfill that
consolidates six days can deliver up to six emissions, the window growing one day at a time.

**4. The store maps it to the screen's type.** `SessionHistoryStore.storedDaysFlow()`
(`SessionHistoryStore.kt:43-45`) parses each ISO day into a `LocalDate` and collects into a
`Set<LocalDate>` — the type the screen already consumed. The `map` runs in the collector's context, so
it is seven tiny parses on the composition's dispatcher, not on Room's executor.

**5. The screen collects it once, for as long as it is composed.** In `AppDetailScreen`:

- `storedDays` starts as `null` (`AppDetailScreen.kt:70`);
- `LaunchedEffect(store)` (`:77`) collects `store.storedDaysFlow().readOr(emptySet<LocalDate>())`
  (`:78`) and assigns `storedDays = it` — the assignment is the only writer of that state;
- the key is the **store instance** the screen remembered (`:68`), not a lifecycle owner (the file no
  longer imports any lifecycle API, `AppDetailScreen.kt:3-57`).

Until the first emission — and after a failed stream — `storedDays` is null/empty, so
`availableDays(today, emptySet())` answers **today alone** (`AvailableDays.kt:13-14`): the screen is
usable immediately, with the forward arrow disabled at today.

**6. The window is recomputed from it, and only the tail of the list grows.** `days` is
`remember(today, storedDays, date)` (`AppDetailScreen.kt:81-89`): `availableDays(today, storedDays)`
(`:82` → `AvailableDays.kt:10-15`) plus, when a `date` was requested from outside that window but
inside `today - 6 … today`, that day (`:84-86`). `availableDays` computes the oldest offered day
inside the retention floor and maps `0 .. n` to `today - i` (`AvailableDays.kt:12-14`), i.e. **index i
is always `today - i`**. The ‹ arrow increments `selectedIndex` and is enabled while
`selectedIndex < days.lastIndex` (`AppDetailScreen.kt:176-181`); › decrements and is enabled while
`selectedIndex > 0` (`:193-198`). An emission therefore enables exactly one disabled arrow: ‹ at the
floor.

**7. An emission cannot land the selector on the wrong day.** Three reasons, all structural:

- the list's prefix never changes: index `i` is `today - i` for every `i`
  (`AvailableDays.kt:14`), so appending older days at the tail leaves `selectedIndex` pointing at the
  same date;
- the set only grows within a process: nothing in `DailyAggregateDao` deletes (`DailyAggregateDao.kt:11-24`)
  and the prune touches `session_record` only (`SessionHistoryStore.kt:36` → `SessionRecordDao.kt:18-19`),
  so `storedDays` cannot lose a day and the list cannot shrink under the index;
- the requested `date` is applied **once**, and only after a real emission (`storedDays != null`,
  `AppDetailScreen.kt:94-103`): the effect finds the requested day in `days`, sets the index and sets
  `requested = null` (`:98-100`), so later emissions and later taps are never overruled.

**8. The day's own read follows the same state.** `LaunchedEffect(packageName, selectedDate, storedDays)`
(`AppDetailScreen.kt:106`) keys on `storedDays`, so a day that just arrived is read the moment it
becomes readable: `daySource(selectedDate, today, storedDays)` decides Live / Stored / Absent
(`:112` → `DetailDaySource.kt:9-13`), and the read is `repository.loadDay` / `store.sessionsFor` /
`emptyList()` (`AppDetailScreen.kt:113-115`). Today's live read is unaffected: `daySource` tests
`date == today` **before** looking at the store (`DetailDaySource.kt:9-10`), and today is never
consolidated (it is not in `finishedDays`, `AvailableDays.kt:18-20`, which `consolidate` filters at
`SessionHistoryStore.kt:34`), so the store's days can never demote today to a stored read.

**9. What replaced the lifecycle.** The old code re-read the days inside
`repeatOnLifecycle(RESUMED)` — a re-read per resume, described in
`openspec/changes/archive/2026-10-19-detail-history-days/findings.md:16`. Now the collection is a
coroutine of the **composition**, keyed on the store (`AppDetailScreen.kt:77-79`): pausing the app
triggers nothing (no re-read, no restart), and the collection ends when the screen leaves the
composition (navigate away, activity destroyed) — not when it pauses. An emission that arrives while
the app is paused still updates `storedDays`, so the window is already current when the user returns.
No `Dispatchers.IO` is written for this read: Room runs the query on its own executor
(`tasks.md:49-51`, adjustment 2.2).

**10. The degrade path, and why the stream has its own helper.** `ui/common/ReadOr.kt` now holds two
siblings:

- `suspend fun <T> readOr(fallback, read)` (`ReadOr.kt:12-20`): a failure logs `Day read failed` and
  answers the fallback; `CancellationException` is rethrown first (`:15-16`), so a leaving screen still
  cancels. The detail's day read uses it (`AppDetailScreen.kt:111`).
- `fun <T> Flow<T>.readOr(fallback)` (`ReadOr.kt:26-30`): `catch` logs the failure, `emit(fallback)`,
  and the stream **ends** — the KDoc says "a cancelled collection ends without a value"
  (`ReadOr.kt:22-25`).

The KDoc is worded that way because of what `Flow.catch` actually does: it lets the **cancellation of
the collecting coroutine** through and hands everything else to the block — including a
`CancellationException` thrown by the upstream flow, which is therefore treated like any other
failure. That was found while applying this change (`tasks.md:41-45`, adjustment 2.1), and it is why
`AppDetailReadTest` proves the rule with a real cancellation — a `launch` that is then `cancelAndJoin`ed
(`AppDetailReadTest.kt:70-77`) — instead of throwing one: the throwing version asserted the wrong thing
and failed on device.

**11. What proves it, and what it does not.** The JVM suite covers the mapping half only:
`HistoryConsolidationTest`'s `the stored days stream re-emits after a consolidation`
(`HistoryConsolidationTest.kt:78-89`) opens a second collector, consolidates and asserts the two
emissions `[{} , {yesterday}]` — that is the store's `map` plus the fake's invalidation stand-in
(`HistoryFakes.kt:46-47`), **not** Room's reactivity, which only SQLite can show. On device,
`AppDetailReadTest` (4 tests) covers both helpers, and `AppDetailNavigationTest` (5 tests) covers the
screen; its propagation case is the change's own regression:

- `aDayStoredWhileTheDetailIsOpenBecomesReachable` (`AppDetailNavigationTest.kt:137-176`) launches the
  app, opens the detail and walks with ‹ while the arrow is enabled (`:146-151`), so it stands on the
  window's floor where only a store write can enable ‹ again;
- it computes **every** day the window is missing down to the retention floor
  (`:153-157`) and seeds all of them into the app's own store through `consolidate`
  (`:158-161` → `seedInStore`, `:179-188`);
- then it waits, with **no lifecycle change and no fixed delay**, for ‹ to enable itself
  (`composeRule.waitUntil(timeoutMillis = 10_000) { backArrowEnabled() }`, `:164`; the 10 s is a failure
  guard, not a window), and asserts the walk grew by exactly the seeded days (`:173`) and that it ends
  on the floor (`:174`).

The reported runs of the apply: JVM `gradlew.bat test` green (79 tests), `AppDetailReadTest` OK (4),
`AppDetailNavigationTest` OK (5) on a clean store, full instrumented suite OK (35). The test also runs
on a device whose store already holds the whole window — and there `assumeTrue` skips it
(`AppDetailNavigationTest.kt:24,157`) with `assumption failed: the store already holds the whole
window` in the runner log. That skip is honest but it is also the zero-coverage case: see the limits.

The gate's green evidence is recorded for this change: `openspec/evidence.json` →
`green_tests` at `2026-10-19T20:26:47.747Z`, `mode: tests`, `ok: true`, over the fingerprint
`96257f00558aee3d` — the same `--run-tests` run that produced the numbers above.

## Physical traceability

- **Where the state lives.** One SQLite file, `foco.db` (`DATABASE_NAME`, `SessionHistoryStore.kt:78`),
  in the app's private storage, opened once per process by `Room.databaseBuilder` behind a `@Volatile`
  + `synchronized` singleton (`SessionHistoryStore.kt:81-99`). This change adds no table, no column and
  no version: the schema is still version 2 with the three entities (`FocoDatabase.kt:12-16`), and no
  migration is touched (`FocoDatabase.kt:26-34`).
- **The one table this change reads.** `daily_aggregate` — one row per `(packageName, day)`, `day` as
  ISO text, `totalMillis` (`DailyAggregate.kt:9-14`). It is the table the day list comes from, and the
  table whose writes drive the screen (`DailyAggregateDao.kt:19-21`). The screen never reads
  `session_record` for the window, only for a chosen stored day (`SessionHistoryStore.kt:47-53`).
- **Who writes it.** Nobody in this change. The only writer is the startup backfill
  `consolidateDay` → `aggregates.upsert` (`SessionHistoryStore.kt:64`), reached from `consolidate`
  (`:32-37`), reached from `MainActivity.consolidateHistory` on IO (`MainActivity.kt:57,82-85`). There
  is no UI path and no second backfill; a `SessionHistoryStore` built by the detail screen never calls
  `consolidate`.
- **Who reads it.** Three readers, all through the same DAO: `consolidate` itself (the suspend
  `storedDays()`, `SessionHistoryStore.kt:33`), the device test `FocoDatabaseTest.kt:57`, and — the new
  one — the detail's stream (`SessionHistoryStore.kt:44-45` → `AppDetailScreen.kt:78`). The week reads
  `totalsFor` from the same table (`SessionHistoryStore.kt:40-41`), a different query.
- **The lifecycle of the observation.** One collector per detail composition, created with the
  `LaunchedEffect` (`AppDetailScreen.kt:77`) and cancelled with it; each emission is a fresh
  `List<String>` from Room, mapped to a `Set<LocalDate>` and dropped when the next one arrives. Nothing
  is cached on disk, nothing is kept in memory beyond the current `storedDays` value.
- **What the user's screen holds.** Four pieces of composition state, all in `remember`
  (`AppDetailScreen.kt:69-72`): `today` (frozen at mount, `:69`), the window `storedDays` (`:70`), the
  index `selectedIndex` (`:71`) and the day's `sessions` (`:72`). The day's data itself is never
  stored by the screen; it is read on each selection (`:106-119`).
- **Nothing leaves the device**: no network, no permission, no manifest change; `allowBackup="false"`
  keeps the database out of cloud backup (contract:
  `openspec/specs/usage-tracking/spec.md:245-247`).

## Limits and failure modes

- **Midnight is out of scope, as decided.** `today` is `remember { LocalDate.now() }`
  (`AppDetailScreen.kt:69`) and is not a key of anything except `days` (`:81`); a screen left open
  across midnight keeps the same "today" until it is left and re-entered. The stream is about the
  store's days, not the clock (`proposal.md:45`).
  `shortcut: today is frozen for the composition's life, when a day boundary crossed while the detail is open must be shown.`
- **A failed stream freezes the window at today alone for the rest of the screen's life.**
  `readOr(emptySet())` emits the fallback **and completes** (`ReadOr.kt:26-30`; the device test shows
  exactly two values and then the end, `AppDetailReadTest.kt:48-59`), so the collection ends: later
  writes to `daily_aggregate` have no collector and the arrows stay as they are until the screen is
  re-entered. The log line is `Log.w("Foco", "Day read failed", …)` (`ReadOr.kt:28`); the user sees a
  today-alone window, not an error.
  `shortcut: one failed emission ends the observation until the screen is re-entered, when a transient store failure must heal without leaving the screen.`
- **Every emission re-blanks the body.** The day's read effect restarts on a new `storedDays` and sets
  `sessions = null` before the IO read (`AppDetailScreen.kt:107`), whose branch is the spinner
  (`:205-212`). A backfill that writes several days while the detail is open can therefore flash the
  spinner once per day, even when the selected day's data does not change.
  `shortcut: a store emission re-shows the spinner (sessions = null at AppDetailScreen.kt:107), when the flicker while a backfill runs beside an open detail is user-visible.`
- **Every emission also re-reads the selected day, including live today.** `storedDays` is a key of the
  day's read (`AppDetailScreen.kt:106`), so a write while the detail shows today costs one extra
  `queryEvents` for a day whose source (`DaySource.Live`, `DetailDaySource.kt:9-10`) did not change.
  Same value, extra platform read.
- **The week screen and the dated dashboard keep their own once-per-entry read.**
  `WeekScreen` reads `store.week(today)` once per entry (`WeekScreen.kt:86-91`, its note at `:86`) and
  the dated `DashboardScreen` reads its day once per entry (`DashboardScreen.kt:103-127`, the stored
  branch at `:117-125`). Opening either during a backfill still freezes a partial view until it is
  re-entered — the residue this change closes only in the detail (`proposal.md:40,48`).
- **The retention caps the growth at seven days.** Only `today-1 … today-6` are ever consolidated
  (`AvailableDays.kt:18-20` filtered at `SessionHistoryStore.kt:34`) and `availableDays` clamps the
  floor to `today-6` (`AvailableDays.kt:12-13`), so even a perfectly reactive window cannot grow past
  seven entries, and a store that already has all six finished days has nothing to gain (the skip
  case).
- **The full-window device skips the proof.** `assumeTrue(..., gained.isNotEmpty())`
  (`AppDetailNavigationTest.kt:157`) means a device whose store already holds the whole window logs
  `assumption failed: the store already holds the whole window` and asserts nothing: the propagation
  is then **unproven on that run**. The case that always runs on any device is the rendering one
  (`aDayOnlyTheStoreHasStillShowsItsTotal`, `:104-135`), which says nothing about reactivity. The
  apply's green run is the clean-store one (`tasks.md:24`); a later run on a warm store is not
  evidence either way.
- **Seeding more than the floor day was a correction, not the first draft.** Seeding only `today-6`
  proved nothing on a device that already had it and its step count flaked; the test now seeds every
  missing day so `gained.size == walked` states the rule on any device state (`tasks.md:36-40`,
  adjustment 3.2). The test also became two tests because rendering must run everywhere while
  propagation needs a day the store can gain (`tasks.md:29-36`).
- **Concurrency is a background writer against one reactive reader.** The backfill writes on IO
  (`MainActivity.kt:82`) while Room re-runs the query on its own executor and the screen collects on
  main; Room serializes the statements, so an emission always reflects committed rows — a day is
  either in the emitted set or not, never half-written. Multiple writes mean multiple emissions, and
  the last one wins.
- **Inside the device test there are two writers.** The launched `MainActivity` starts its own backfill
  (`AppDetailNavigationTest.kt:58` → `MainActivity.kt:57`), beside the test's `seedInStore`
  (`:179-188`). That cannot inflate the walk: every day either writer can store is one of
  `today-1 … today-6` (`AvailableDays.kt:18-20`), the same span the test seeds (`:153-157`).
- **Force close / data deleted.** Nothing is held across processes: the window is re-read on the next
  mount. Clearing storage or uninstalling removes `foco.db`; the next start opens a fresh file and
  consolidates only the days the platform still serves, so older days are gone for good — the store's
  rule, unchanged here.
- **No network anywhere**: a missing day is a missing row in a local table.
- **The doc comment that named the deleted read is the other file this change touched.**
  `DaySelectionTest.kt:15` said its set was "as `SessionHistoryStore.storedDays()` returns them"; the
  apply reworded it to "shaped as the store's day set is", so no comment names a function that is gone
  (the set is still built by hand at `:16`; the file joined `x-tests` because the change edits it).

## Touched APIs

- **Room `@Query` returning `kotlinx.coroutines.flow.Flow`** (`DailyAggregateDao.kt:19-21`) — the
  mechanism of the whole change: Room registers the query against `daily_aggregate` in its
  invalidation tracker and re-runs it on every write to that table, instead of the app deciding when to
  ask again. Supported by `androidx.room:room-ktx`, already on the main classpath
  (`app/build.gradle.kts:70-71`); no new dependency (`proposal.md:49`).
- **`kotlinx.coroutines.flow.map`** (`SessionHistoryStore.kt:18,44-45`) — turns the DAO's
  `Flow<List<String>>` into the `Flow<Set<LocalDate>>` the screen consumes; the parse runs per emission
  in the collector's context.
- **`kotlinx.coroutines.flow.catch`** (`ReadOr.kt:6,26-30`) — the stream half of the degrade: logs and
  emits the fallback, then completes. Chosen over `try/catch` at the call site because the failure can
  happen on any emission, not only on the first.
- **`LaunchedEffect(store)` + `collect`** (`AppDetailScreen.kt:77-79`, `kotlinx.coroutines.flow.collect`
  imported at `:56`) — the composition owns the collector; the key is the remembered store, so the
  effect starts once per screen and is cancelled with it.
- **Removed from this screen: `LocalLifecycleOwner` + `repeatOnLifecycle(Lifecycle.State.RESUMED)`** —
  the previous trigger for this read (`findings.md:16`). Both APIs remain in the project for the week
  (`WeekScreen.kt:46,94-101`) and the dashboard (`DashboardScreen.kt:44,99-110`), so nothing was
  dropped from the dependency list.
- **JUnit `Assume.assumeTrue`** (`AppDetailNavigationTest.kt:24,157`) — makes the propagation test skip
  instead of lying when the device's store already holds the whole window; the runner reports the
  assumption failure, which is visible in the log.
- **`MutableStateFlow` as the fake's invalidation stand-in** (`HistoryFakes.kt:10,37,47`) — the JVM DAO
  increments a counter on `upsert` and `map`s it to the current day list, so the store's stream behaves
  like Room's without SQLite.
- **`withTimeout` + `async` + `yield` + `take`** (`HistoryConsolidationTest.kt:7-12,80-86`) — the test
  opens the collector first, yields so it is subscribed, then writes and awaits two values; `withTimeout`
  keeps a broken stream from hanging the suite.
- **`launch` + `cancelAndJoin` + `CompletableDeferred` + `awaitCancellation`**
  (`AppDetailReadTest.kt:9-11,15,64-77`) — a real cancellation of a real collection, which is the only way
  to observe that the helper does not answer the fallback on cancellation.

## Implicit decisions

- **The suspend DAO read stayed.** Only the *store's* suspend `storedDays()` was deleted, because the
  detail was its only caller; `consolidate` still needs a one-shot read to compute the pending days
  (`SessionHistoryStore.kt:33`) and `FocoDatabaseTest.kt:57` asserts the SQL directly, so
  `DailyAggregateDao.storedDays()` keeps its line (`DailyAggregateDao.kt:16-17`), task 1.1.
- **The stream lives on the store, not on the screen.** The DAO returns strings and the store parses
  them (`SessionHistoryStore.kt:44-45`), so the UI still talks in `LocalDate` and the persistence
  detail (ISO text) stays in `data/`.
- **`storedDays` is nullable rather than starting empty.** `null` is "no emission yet" and `emptySet()`
  is "the store has nothing / the read failed"; the requested-day effect tests `!= null`
  (`AppDetailScreen.kt:96`) so it cannot apply a request to the today-alone placeholder — the same
  reason the previous change recorded (`week-screen`, step 11).
- **The collection is keyed on the store, not on the lifecycle owner**, and runs without an explicit
  `Dispatchers.IO` (`tasks.md:49-51`): Room owns the query's thread, and the screen's only job is to
  assign the result on its own dispatcher.
- **The window is allowed to change while the day read is in flight.** `selectedDate` is captured per
  effect run (`AppDetailScreen.kt:105-106`), so an old read finishing late cannot overwrite a newer
  selection: a new key cancels the old effect. The cost is the extra re-read documented in the limits.
- **The failure fallback is the same value as the empty store.** `emptySet()` rather than a distinct
  error state (`AppDetailScreen.kt:78`): a failed read and an empty store look the same on screen, which
  is the degradation the living contract already accepts
  (`openspec/specs/usage-tracking/spec.md:287-292`), and it is why the helper, not the screen, owns the
  log line.
- **The fake reads its map at emission time instead of caching the day list** (`HistoryFakes.kt:37-47`,
  `tasks.md:46-48`, adjustment 1.3): `HistoryWeekTest` and `HistoryRetentionTest` write
  `aggregates.rows` directly, and a cached list would have hidden those rows from the stream.
- **The device proof became two tests** (`tasks.md:29-36`, adjustment 3.2): the rendering case must run
  on any device, the propagation case needs a day the store can gain. The cost is the `assumeTrue` skip
  on a warm device, accepted deliberately instead of asserting a premise.
- **The delta states freshness without stating a frequency** — "the offered window SHALL follow the
  store while the detail is open" plus the scenario *A day stored while the detail is open*
  (`openspec/changes/detail-window-follows-store/specs/usage-tracking/spec.md:6,27-29`). The living
  requirement (`openspec/specs/usage-tracking/spec.md:265-269`) is unchanged until this change is
  archived; the delta does not touch the other four scenarios or the `[@test]` list, because
  `AppDetailNavigationTest` already carries the new case.
- **No new seam was introduced for the tests.** The propagation proof uses the app's real
  `MainActivity`, real database and real `consolidate` (`AppDetailNavigationTest.kt:51-67,179-188`);
  the only fake is the platform reader passed to `consolidate`, so the test proves the database's own
  reactivity rather than a stub's.

## Cognitive sign-off

Answer in your own words (the agent must not fill these):

1. The backfill writes a finished day while the detail is open. Trace the value from
   `aggregates.upsert` to the ‹ arrow becoming enabled: which table, which query, which three pieces of
   screen state it passes through, and why the index cannot end up on a different day than the one the
   user was looking at.
   - **Answer (Nschkurko, 2026-10-19):** Se lee la query registrada y se aumenta el largo de `days`.
2. The stream fails once (the store is busy) while the detail is open. Say what the window shows, what
   the screen does with later writes, how the user gets a full window back — and name the operator and
   file that decide each of those three.
   - **Answer (Nschkurko, 2026-10-19):** Emite el fallback y se termina el flow completo.
3. You want the week screen and the dated dashboard to follow the store too. Name the file and function
   where each one's read would have to become a stream, and say what "midnight is out of scope" would
   then mean for `today` in the detail while it stays open.
   - **Answer (Nschkurko, 2026-10-19):** (a) `WeekScreen.kt:86-91` (el `LaunchedEffect` que llama
     `store.week`) y `DashboardScreen.kt:117-125` (el branch `else` del dashboard datado).
     (b) Porque si cambia `today` y la selección sigue guardada como índice, se redefine el mapeo y el
     usuario salta de día.
