# Tasks

## 1. The store's day list as a stream

- [x] 1.1 `DailyAggregateDao`: `storedDaysFlow(): Flow<List<String>>` — the same `SELECT DISTINCT day` as `storedDays()`, returned as a Room `Flow` so a write to `daily_aggregate` re-emits. Keep the suspend one: `consolidate` and `FocoDatabaseTest` read it once.
- [x] 1.2 `SessionHistoryStore`: `storedDaysFlow(): Flow<Set<LocalDate>>` with the mapping the suspend `storedDays()` does today, and delete that suspend read — the detail was its only caller.
- [x] 1.3 `HistoryFakes.FakeDailyAggregateDao`: implement the new query over the same rows and emit on `upsert`, so the JVM suite keeps compiling and behaving like the Room DAO.

## 2. The detail follows it

- [x] 2.1 `ui/common/ReadOr.kt`: the flow sibling of `readOr` — a failure logs and answers the fallback; `CancellationException` propagates instead of being read as a failure.
- [x] 2.2 `AppDetailScreen`: collect the store's flow into `storedDays` and drop the `repeatOnLifecycle(RESUMED)` block; the requested-day effect and the day's session read keep their rules, and today-alone stays the state until the first emission.
- [x] 2.3 Nothing else reads the deleted suspend method, and the week/dashboard reads stay untouched (both are outside this change's targets).

## 3. The tests

- [x] 3.1 `HistoryConsolidationTest` (JVM): a case that the store's day stream emits the day `consolidate` just wrote — the mapping half of the mechanism.
- [x] 3.2 `AppDetailNavigationTest`: the H2 case stops forcing the lifecycle — seed the floor day while the detail is open, wait for `‹` to enable itself, walk to the new floor and assert it is the oldest day offered. Delete the `CREATED`/`RESUMED` pair and the bounded walk counter.
- [x] 3.3 `AppDetailReadTest` (device): one case for the stream — a failing flow answers the fallback, a cancelled one does not.

## 4. Verify

- [x] 4.1 `openspec validate --all --strict`; `node scripts/gates/gate-check.mjs --run-tests` for the green evidence.
- [x] 4.2 Device: `AppDetailNavigationTest` and `AppDetailReadTest` on the emulator, on a clean and on an already-populated DB (the case that grew by three days must not depend on what the store already had).
- [x] 4.3 The overlaps with the two deps stay warnings ("sequenced by deps"), and the change's `[@test]` files exist — the gate is green before `verified`.

## Adjustments (apply, 2026-10-19 — the plan against the real tree)

- **3.2 — the H2 case became two tests.** The stored-day rendering (the class-(c) finding's proof) must
  keep running on any device, while the propagation proof needs a day the store can *gain* — which a
  device holding the whole window cannot provide. So the rendering stays a test of its own and the
  propagation is `aDayStoredWhileTheDetailIsOpenBecomesReachable`: it walks to the window's floor (where
  the arrow is disabled), seeds **every** day the window is missing down to the retention floor, waits
  for the arrow to enable itself with no lifecycle change, and asserts the walk grew by exactly those
  days. `assumeTrue` skips it when the window is already full, and the device log shows that skip
  (`assumption failed: the store already holds the whole window`) instead of a green lie.
- **3.2 — why it seeds more than the floor day.** The first draft seeded only `today - 6`, which is
  what the old test did; on a device whose store already had that day the run proved nothing and its
  step count flaked — the same class of flake removed from `WeekScreenTest` earlier. Seeding every
  missing day makes the assertion `gained.size == walked` state the rule on any device state.
- **2.1 — `catch` and cancellations.** Applying this found that `Flow.catch` treats a *hand-thrown*
  `CancellationException` as an ordinary failure (it only lets a real job cancellation through), so the
  helper's KDoc claims what it does — "a cancelled collection ends without a value" — and
  `AppDetailReadTest` proves it with a `launch` + `cancelAndJoin`, not with a `throw` (that first
  version failed on device and its assertion was wrong, not the code).
- **1.3 — the fake's flow.** Some JVM tests write `aggregates.rows` directly (`HistoryWeekTest:25`,
  `HistoryRetentionTest:28`), so the fake counts writes and reads the map at emission time instead of
  caching the day list when `upsert` runs; a cached list would have hidden those rows from the stream.
- **2.2 — `LaunchedEffect(store)`**: the flow is keyed on the store instance the screen remembers, not
  on the lifecycle owner the resume read used; Room runs the query on its own executor, so the screen
  needs no `Dispatchers.IO` for it (the day's sessions still use the IO read they had).

## Workflow follow-up

- `tech_brief: true`: `walkthrough.md` + the human's comprehension sign-off before `--pre-archive`.
- Closes the residue of finding **H2** of the archived `detail-history-days` (`archive/2026-10-19-detail-history-days/findings.md:16`), which is the third item of the queue recorded in `DECISIONS.md` (2026-10-19, "Change approved: `stored-day-unlocks`").
- Leave `status: draft` until the human accepts; one apply, one branch, one change.
