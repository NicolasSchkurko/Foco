# Proposal — The detail's window follows the store while it is open

## Why

`/spec-verify` on `detail-history-days` (archived 2026-10-19) filed **H2**: the stored-days window was read once at mount, so opening the detail before the startup backfill finished froze a partial window. Task 7.2 fixed the freeze by re-reading the days on every resume (`app/src/main/java/com/foco/app/ui/detail/AppDetailScreen.kt:80-86`), and the findings left the rest explicit (`openspec/changes/archive/2026-10-19-detail-history-days/findings.md:16`): *what stays: while the screen is never paused, the window is the one read at its last resume.* The reader who opens the detail and stays there while the backfill writes never sees the days arrive — `‹` stays disabled at what was there a moment ago.

The living contract is silent about **when** the window must be fresh: *Day selection within the stored history* (`openspec/specs/usage-tracking/spec.md:265-266`) fixes the window's shape and never its freshness. The human chose the shape of this fix on 2026-10-19: (1) the mechanism is **Room's `Flow`** — the read follows the database's invalidation, not a resume tick; (2) a **midnight rollover while the screen is open is out of scope**; (3) the proof must **wait for the propagation, not for a fixed window**.

## What Changes

- `daily_aggregate`'s day list gets a **Room `Flow`** read (`DailyAggregateDao.storedDaysFlow()`): a write to the totals — the startup backfill's `consolidate` — invalidates the query and re-emits, so the reader sees the day while the screen never pauses. The suspend `storedDays()` stays: `consolidate` and `FocoDatabaseTest` read it once.
- `SessionHistoryStore.storedDaysFlow()` maps it to the `Set<LocalDate>` the screen already consumes; the suspend `SessionHistoryStore.storedDays()` goes away, since the screen was its only caller.
- `AppDetailScreen` collects that flow into `storedDays` instead of the `repeatOnLifecycle(RESUMED)` re-read; the failure fallback (today alone) and the cancellation rule stay exactly as `readOr` has them.
- The requirement *Day selection within the stored history* gains the freshness clause and one scenario: a day stored while the detail is open becomes reachable **without the screen being paused and resumed**.
- The H2 device test stops forcing the lifecycle: it seeds the floor day while the detail is open and waits for the arrow to enable itself.

## Regression (why this `[@test]`)

`kind: fix` needs a regression test, and H2's own witness is the one that changes: the case at `app/src/androidTest/java/com/foco/app/ui/AppDetailNavigationTest.kt:139-155`. Today it proves the *resume* re-read by driving `CREATED` → `RESUMED`; after this change it seeds the floor day while the detail keeps running and waits for `‹` to become enabled — a state only the store's write can produce, with no lifecycle change and no fixed wait of its own. Two more oracles carry the rest: `AppDetailReadTest` gains the flow's degraded case (a failing stream answers today alone, a cancelled one does not), and `HistoryConsolidationTest` gains a case that the store's day stream re-emits the day `consolidate` just wrote — the JVM half of the mechanism, since the reactive half is the database's own.

## State and persistence mechanism

- **Reads**: `daily_aggregate`'s day list, observed as a Room `Flow`. The write that makes the window grow is the startup backfill (`app/src/main/java/com/foco/app/MainActivity.kt:57,79-95`), which runs on IO beside the screen.
- **Writes**: none — the change is only in how an existing read is observed.
- **Lifecycle**: the flow is collected for as long as the screen is composed; pausing stops mattering, and nothing new runs while the app is away.
- **Degrade**: a failing read still answers today alone and is still logged; `CancellationException` is not a failure and is not swallowed.
- **Nothing leaves the device**: unchanged.

## Expected outcome

The detail stops showing a window that is older than the store. We will know it was worth it when the reader who opens the detail during the startup backfill watches `‹` come alive by itself instead of leaving the screen and coming back.

## Alternatives considered

| Option | Why yes | Why not / status |
|---|---|---|
| **Room `Flow` on the DAO, collected by the detail (chosen)** | The store's writes already invalidate the query, so the freshness rule falls out of the mechanism the database ships: no polling, no lifecycle bookkeeping | Touches the DAO seam and the JVM fakes, and the stream needs the same degraded-read story the suspend read has |
| Keep the resume re-read and note the limit as a `shortcut:` | Zero code | That is exactly the residue H2 named: a reader who never pauses keeps a stale window — **rejected (human)** |
| Poll `storedDays()` on a timer while the screen is open | Keeps the suspend read | Wastes a query on every tick whether the store changed or not, and answers worse than the invalidation Room already raises — **rejected** |
| Fix the same residue in the week screen and the dated dashboard as well | Both read the store once per entry, so they carry it too | Out of this finding's scope: the week's own note stands, and widening it would touch three screens instead of one — **rejected (human: the detail)** |
| Re-read on every recomposition | No new store API | Recomposition is a rendering event, not a signal about the store: it would read on every frame the screen draws — **rejected** |

## Non-goals

- **No midnight rollover**: a day boundary crossed while the screen is open is not re-derived; `today` stays what it was when the screen mounted. The flow is about the store's days, not about the clock.
- **No change to what is stored, retained or pruned** — the foundation and `stored-day-unlocks` own that.
- **No change to the live read**: today's rules are the ones `week-screen` shipped.
- **The week screen and the dated dashboard stay as they are**: each reads the store when it is entered; moving them onto the same stream is a later change, if the residue bites there.
- **No new dependency**: `androidx.room:room-ktx` is already on the main classpath (`app/build.gradle.kts:71`), which is what supports a DAO returning a `Flow`.

## Impact

- `tech_brief: true`: a reactive read over a database, with a lifecycle change and a degrade path worth a walkthrough and the human's comprehension sign-off.
- Deps: `stored-day-unlocks` (owns `SessionHistoryStore.kt`) and `week-screen` (owns the detail screen, `ReadOr.kt` and the navigation test) — both `verified`, so this change lands after them. Its branch bases on `week-screen`'s tip: the code it edits (`ui/common/ReadOr.kt`, the dated detail) only exists there.
- Tests: `HistoryFakes.kt` (the JVM DAOs must implement the new query) and `HistoryConsolidationTest.kt`, plus the two instrumented classes named above.
- No protected zone (`openspec/config.yaml` → `protected: []`) and no new dependency.
