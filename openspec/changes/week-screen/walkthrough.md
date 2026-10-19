# Walkthrough — week-screen (tech brief)

> For the human: this brief explains the mechanism of the change. Answer the questions in
> `## Cognitive Sign-off` yourself; the agent never fills them.

## Mechanism

The week view adds no storage and no write: it *reads* what two earlier changes already stored and
what the platform still answers for today, and it merges the two. Every number on the screen is one
of those two reads, run through pure functions in `ui/metrics/`.

**1. The entry.** On the dashboard — only when it is the undated (today) screen
(`DashboardScreen.kt:203`) — a `DateRange` icon calls `onOpenWeek` (`DashboardScreen.kt:204-209`)
and the nav root pushes `week` (`FocoNavHost.kt:30`), declared at `FocoNavHost.kt:34-40`. That route
composes `WeekScreen(loadDay = loadDay, onDayClick, onAppClick)` (`FocoNavHost.kt:35-39`).

**2. The screen's two doors and its frozen day.** `WeekScreen` builds a `UsageRepository` (the
platform) and a `SessionHistoryStore.forApp(...)` (the database, with a lambda back to the platform
for consolidation) (`WeekScreen.kt:76-77`), freezes the day it calls "today"
(`WeekScreen.kt:78`, in `remember`), and resolves its live reader once: the injected `loadDay` or,
in the app, `repository.loadToday()` (`WeekScreen.kt:79`).

**3. The six stored days — read once.** `LaunchedEffect(store)` runs a single IO read,
`store.week(today)`, degraded by `readOr(emptyList())` (`WeekScreen.kt:87-91`) →
`SessionHistoryStore.kt:38-39` → `DailyAggregateDao.totalsFor(from, to)`
(`DailyAggregateDao.kt:18-19`), a text range over ISO day strings from `today - 6` to `today`
inclusive (`SIX_DAYS = 6`, `SessionHistoryStore.kt:77`). What comes back is one row per app and day
(`DailyAggregate.kt:10-14`), i.e. the totals that survive pruning.

**4. Today — read once per resume.** `repeatOnLifecycle(RESUMED)` re-runs the live read each time
the screen comes back (`WeekScreen.kt:94-101`) → `load()` → `UsageRepository.loadToday()`
(`UsageRepository.kt:50-54`), which derives the date from the clock and calls `loadDay`
(`UsageRepository.kt:60-105`): `queryEvents` over a window that starts the previous day and ends at
`now` (`:61-66`), folded into sessions and clamped to the day (`:93-104`), plus that day's
`KEYGUARD_HIDDEN` marks (`:76-78`). There is **no interval**: the dashboard's
`DEFAULT_REFRESH_INTERVAL_MILLIS` (`DashboardScreen.kt:65`) is deliberately not imported here, and
it could not be — a `ui/week/` file importing `ui/dashboard/` is exactly what the `code-structure`
contract forbids (`openspec/specs/code-structure/spec.md:61-62`). A live read failure answers
`DayData(emptyList(), emptyList())` (`WeekScreen.kt:98`, fallback at `ReadOr.kt:10-18`).

**5. The derivation — where each bar's number comes from.** `deriveWeekMetrics(storedTotals, today,
todayData, excludedPackages)` runs in `remember` (`WeekScreen.kt:107-114`) →
`WeekMetrics.kt:17-23`. Bars are built oldest first over `windowDays - 1 downTo 0`
(`WeekMetrics.kt:26`), and each day takes exactly one branch:

- `date == todayDate` → `Live`, total = `dayTotal(today.sessions)`, i.e. `sumOf { end - start }`
  over today's live sessions only (`WeekMetrics.kt:28-29` → `UsageMetrics.kt:9-10`);
- the store has rows for that day (`storedTotals.filter { it.day == date.toString() }`) → `Stored`,
  total = the **sum of that day's per-app totals** (`WeekMetrics.kt:31,35`);
- the store has **no** row for that day → `Absent` with `0L`, and the state marks it
  (`WeekMetrics.kt:32-33`, `WeekDayBar.kt:6-12`).

Today is checked **first**, so a stray stored row for today could never override the live number.

**6. The ranking — both sides merged by package.** The stored rows are grouped by package and
summed, with `daysUsed` counted as the number of distinct stored days
(`WeekMetrics.kt:40-43`); the live sessions are grouped the same way, contributing their own sum and
`1` day (`WeekMetrics.kt:45-48`); then the key sets are unioned and each package gets
`stored + live` for both numbers (`WeekMetrics.kt:50-58`). Merging by package is what makes one row
per app: the same app appears on both sides (yesterday's totals, today's sessions) and must not be
listed twice, and its week total must be one number. Exclusions filter both sides of the ranking
(`:41`, `:46`) but not the bars — the test pins that (`WeekMetricsTest.kt:97-107`).

**7. The bars on screen.** `WeekBars` (`WeekScreen.kt:205-225`) computes the tallest non-absent bar
(`:206-209`; an absent day never becomes the scale reference) and lays out seven `WeekBar`s in one
`Row` with `SpaceBetween` (`:211-224`). Each bar (`WeekScreen.kt:232-281`): a 28×120 dp track
(`:254-259`), a filled box whose height is `max(4.dp, 120.dp * total/max)`
(`:245,263`), a **4 dp muted marker** and a `surfaceVariant` colour instead of `primary` when the
day is absent (`:263,266-268`), then the weekday (`formatWeekday`, `Formatting.kt:25`) and the day of
month (`WeekScreen.kt:272-280`). The whole column is one clickable node whose `contentDescription`
is either `week_bar` ("Today · 3 h 20 min") or `week_bar_absent` ("Mon · no data")
(`WeekScreen.kt:239-243,247-252` → `strings.xml:60-61`, `values-es/strings.xml:60-61`), the day
label being `formatDayLabel` so the strings "Today"/"Yesterday" follow the app language
(`Formatting.kt:33-42`).

**8. The ranking rows.** `metrics.ranking` renders through `AppUsageRow` from `ui/common/`
(`WeekScreen.kt:183-193`), `maxMillis` being the top app's total (`:176`) so the bars are relative
to the week's leader, and the caption supplied by the caller as the `week_days_used` plural
(`WeekScreen.kt:187-191` → `strings.xml:63-66`). Empty ranking → `week_empty`
(`WeekScreen.kt:157-169`, `strings.xml:59`).

**9. A bar opens its day.** `onDayClick(bar.date)` (`WeekScreen.kt:221`) → nav `day/$date`
(`FocoNavHost.kt:37`), route declared at `FocoNavHost.kt:46-56`, argument parsed back with
`LocalDate.parse` (`:50-52`) and handed to the *same* `DashboardScreen`, now with `date`
(`:51-55`). The ranking rows of the week open the detail **without** a date
(`FocoNavHost.kt:38`), i.e. today, as decided.

**10. The dated dashboard reads once, from the right place.** `requestedDate = date`
(`DashboardScreen.kt:85`) and one `when` decides the source (`:103-127`):

- `null` → today, the old loop with the refresh interval (`:105-110`), untouched;
- `requestedDate == today` → **live**, one `load()` (`:113-115`);
- any other date → **the store**, in one IO block: `DayData(store.sessionsFor(date), emptyList())`
  plus `store.unlocksFor(date) ?: 0` (`:117-125`).

The stored unlock count lives in its own state, `storedUnlockCount` (`DashboardScreen.kt:94`),
because the day view needs a *count* while `DayData.unlockTimes` is a list of marks
(`DayData.kt:9-12`) and the stored day has no marks to count. The header picks per branch: stored
date → `storedUnlockCount`, otherwise `day.unlockTimes.size` (`DashboardScreen.kt:186-201`). The
stored date read is `SessionHistoryStore.unlocksFor` (`SessionHistoryStore.kt:53-54`,
`DailyUnlockDao.kt:15-16`), where `null` means "no count was stored" and is rendered as `0`
(`DashboardScreen.kt:121`, and the apply note in `tasks.md:50-52`): a day without sessions is never
consolidated (`SessionHistoryStore.kt:57-64`, the early return at `:59`), so it has neither
sessions nor unlocks. `sessionsFor` is the raw read, half-open on `start` on the system zone
(`SessionHistoryStore.kt:46-51`), because the dated screen needs sessions for the hourly chart and
the ranking, not totals.

**11. An app row there opens the detail for that day.** `FocoNavHost.kt:54` navigates to
`detail/$package?date=$date`; the route declares a nullable `date` argument
(`FocoNavHost.kt:58-67`) and `AppDetailScreen(packageName, date)` receives it
(`FocoNavHost.kt:69-74`, `AppDetailScreen.kt:66`). The requested day starts as a *pending request*
(`AppDetailScreen.kt:101`), and is applied only once `storedDays != null`
(`AppDetailScreen.kt:98-110`): the stored day set arrives asynchronously
(`AppDetailScreen.kt:80-86`) and the first window is today alone
(`AvailableDays.kt:10-15`), so applying the request too early would land on the wrong day when the
list grows. If the requested day falls inside `today - 6 … today` but is not in the store's window,
the list is extended with it (`AppDetailScreen.kt:88-96`) — that is what makes an **absent** bar
openable. The day's read then follows the existing rule: today `Live`, stored `Stored`, else
`Absent` → empty list (`AppDetailScreen.kt:113-126`, `DetailDaySource.kt:9-13`), so a bar whose day
has no stored data opens on the empty state of the dated dashboard
(`DashboardScreen.kt:239`, `strings.xml:62`).

**12. The two moves, and why.** `AppUsageRow` now lives in `ui/common/`
(`ui/common/AppUsageRow.kt:25-32`; the `dashboard/` folder holds only `DashboardScreen.kt`), because
the week and the dashboard both use it and the `code-structure` contract makes `ui/common/` the only
place a widget shared by more than one screen may live and forbids a `ui/<feature>/` file importing
another feature (`openspec/specs/code-structure/spec.md:61-62`). Its caption became a parameter
(`AppUsageRow.kt:29`) because the week ranks totals whose last-used time it does not have, and the
caller now supplies it: last-used on the dashboard (`DashboardScreen.kt:264-267`), days-used on the
week (`WeekScreen.kt:187-191`). `readOr` moved to `ui/common/` (`ui/common/ReadOr.kt:10-18`) because
it now has three callers (dashboard, week, detail), and it stays `internal` so `AppDetailReadTest`
still exercises it (`AppDetailReadTest.kt:5,20-38`).

**13. The tests' seam.** `FocoNavHost(loadDay = ...)` is a defaulted parameter documented as the
tests' seam (`FocoNavHost.kt:16-21`); it is handed to the dashboard (`:27`), the week (`:36`) and the
dated day view (`:53`), so one injection drives the whole chain. `WeekScreenTest` does exactly that:
`setContent { FocoNavHost(loadDay = { liveDay }) }` (`WeekScreenTest.kt:70-72`), the stored days
seeded in the app's **real** store by running `consolidate` behind a fake platform reader
(`WeekScreenTest.kt:93-102`), and the walk is dashboard → week → day → detail
(`WeekScreenTest.kt:110-197`). The platform is never queried; the assertions read the same database
the shipped screens read (`WeekScreenTest.kt:104-108`).

## Physical traceability

- **The database.** One SQLite file, `foco.db`, in the app's private storage, built once per process
  behind a `@Volatile` + `synchronized` singleton (`SessionHistoryStore.kt:76-97`). Three tables
  matter here: `daily_aggregate` — one row per `(packageName, day)`, `day` as ISO text, the totals
  that survive pruning (`DailyAggregate.kt:9-14`), **the only table the week's bars and ranking
  read**; `session_record` — raw sessions, read by `sessionsFor` for a dated day view
  (`SessionHistoryStore.kt:46-51`); `daily_unlock` — one count per consolidated day, read by
  `unlocksFor` (`DailyUnlockDao.kt:8-16`).
- **Who writes them.** Nobody in this change. The only writer is the startup backfill
  `consolidate(today)` (`SessionHistoryStore.kt:30-35`), called from `MainActivity.onCreate` on IO,
  once per process, guarded by an `AtomicBoolean` (`MainActivity.kt:57,79-95`). It writes raw rows,
  totals and the unlock count per day (`SessionHistoryStore.kt:60-64`) and prunes raw rows older
  than `today - 6` (`:34,72-73`).
- **The other store.** The user's exclusions live in the `excluded_apps` Preferences DataStore file
  (`ExcludedAppsStore.kt:10-21,38`); the week only *collects* that flow
  (`WeekScreen.kt:103-105`) and passes the set to the derivation, which uses it for the ranking
  only (`WeekMetrics.kt:15,41,46`).
- **Today has no storage at all.** It is re-queried from `UsageStatsManager` through
  `UsageRepository` (`UsageRepository.kt:24-25,60-105`); nothing today is persisted, which is why
  the week has to ask the platform at all.
- **Lifecycle.** The stored read happens once per entry to the screen (`WeekScreen.kt:87-91`); the
  live read happens once per resume (`:95-101`); the derived `WeekMetrics` is recomputed whenever
  either read or the exclusions change (`:107-114`). Nothing the week touches is written, so force
  close loses nothing.
- **Nothing leaves the device**: no network, no permission, no backup
  (`openspec/specs/usage-tracking/spec.md:245-251`).

## Limits and failure modes

- **The stored read happens once per entry and is not retried.**
  `shortcut: one store read per screen entry, no resume re-read, when a week must survive the backfill finishing while the screen is open.`
  `LaunchedEffect(store)` keys on the store only (`WeekScreen.kt:87-91`), unlike the detail, which
  re-reads on every resume (`AppDetailScreen.kt:80-86`). If the startup backfill is still writing
  when the week opens, the days it has not reached yet read **absent** and stay absent until the
  user leaves and re-enters the screen.
- **The seven bars are a fixed, non-scrolling row.**
  `shortcut: seven 28 dp tracks in one SpaceBetween row with no horizontal scroll, when a device or font scale is found where the labels collide.`
  `WeekBars` lays out every bar in a single `Row` (`WeekScreen.kt:211-224`) with fixed 28 dp tracks
  (`:254-257`), and the weekday/day-of-month labels are text under them (`:272-280`): nothing wraps
  or scrolls, and a very large font scale or a very narrow window squeezes the gaps to nothing.
- **The week refreshes only the live day.**
  `shortcut: only the live bar refreshes (on resume, no interval), when a stored day can change while this screen stays in the foreground.`
  `repeatOnLifecycle(RESUMED)` with no interval
  (`WeekScreen.kt:94-101`): while the screen stays in the foreground, today's bar is frozen; going
  to another screen and back refreshes it. The stored six do not change by design — they are
  finished days.
- **`today` is frozen for the screen's lifetime** (`WeekScreen.kt:78`). Worse, the live reader is
  `loadToday()`, which re-derives *its own* today from the clock (`UsageRepository.kt:50-54`). A week
  left open across midnight therefore keeps the same seven bar dates, while the resume read returns
  the **new** day's sessions: the last bar (labelled with the old date) shows the new day's total,
  and yesterday's now-finished day is not read until the screen is entered again.
- **A failed live read is a zero, not an absence.** `readOr(DayData(emptyList(), emptyList()))`
  (`WeekScreen.kt:98`) leaves the bar `Live` with `0L`, so it draws the 4 dp minimum bar in the
  active colour and announces "… · 0 s" (`WeekScreen.kt:239-243,263`) — indistinguishable from a day
  with no usage. Nothing logs to the user; the failure is a `Log.w` (`ReadOr.kt:16`), and there is
  no retry until the next resume. The six stored bars are unaffected.
- **A failed store read empties the week.** `readOr(emptyList())` (`WeekScreen.kt:89`) makes all six
  past days `Absent` and the ranking shrink to today's apps; if today is also empty, the screen
  shows `week_empty`. Again: no message beyond the log, no retry without re-entering.
- **Nothing is written, so nothing can be corrupted.** Force close, a killed process or a wiped
  cache of the *app* (the DataStore's exclusions) costs at most the exclusion list; the database is
  the backfill's, and it is rebuilt on the next start for the days the platform still serves.
- **Concurrency is a background writer versus these readers.** The backfill writes on IO while the
  week reads on IO; Room serializes them, so a read sees whole statements — either a day's rows or
  none. The visible consequence is the first limit above, not a crash.
- **A day with no stored data is not a zero, at every layer**: `consolidateDay` returns before
  writing anything when the platform served no sessions (`SessionHistoryStore.kt:57-59`), so the day
  has no totals *and no unlock row*; `unlocksFor` answers `null` for it (`DailyUnlockDao.kt:15-16`);
  the bar's derivation checks for "no rows" rather than "zero milliseconds"
  (`WeekMetrics.kt:31-33`); the bar draws a muted marker instead of a filled bar (`WeekScreen.kt:263,266-268`)
  and its accessibility text says "no data" (`WeekScreen.kt:239-241`).
- **No network anywhere**: a missing day is a missing row in a local table.

## Touched APIs

- **`rememberUpdatedState`** (`WeekScreen.kt:79`) — keeps the latest `loadDay`/`loadToday` lambda
  visible to the long-lived resume effect without restarting it; the dashboard already used it for
  the same reason (`DashboardScreen.kt:86`).
- **`LocalLifecycleOwner` + `repeatOnLifecycle(Lifecycle.State.RESUMED)`** (`WeekScreen.kt:94-96`) —
  runs the live read exactly while the screen is RESUMED and once per return. Already in the project
  through `lifecycle-runtime-compose`; no new dependency.
- **`LocalOnBackPressedDispatcherOwner`** (`WeekScreen.kt:75,127`) — the header arrow triggers the
  system back so it lands on the dashboard with the same behavior as the gesture. Same pattern as
  the detail (`AppDetailScreen.kt:68,143`).
- **`pluralStringResource`** (`WeekScreen.kt:187-191`) — renders the `week_days_used` plural
  (`strings.xml:63-66`, `values-es/strings.xml:63-66`); the same API the dashboard uses for unlocks
  (`DashboardScreen.kt:196`).
- **`Modifier.semantics { contentDescription = … }`** (`WeekScreen.kt:251`) — makes the whole
  column, not only the labels, one announced node whose text carries the date and either a duration
  or "no data". This is the seam the instrumented test asserts on
  (`WeekScreenTest.kt:80-84,149-153`).
- **`androidx.navigation` `NavType.StringType` with a nullable `navArgument`**
  (`FocoNavHost.kt:58-67`) — an optional query-style argument, so `detail/{package}` keeps working
  undated while `detail/{package}?date=…` carries a day; `LocalDate.parse` reads it back (`:71-73`).
- **`combinedClickable(onClick, onLongClick)` with a nullable long click**
  (`ui/common/AppUsageRow.kt:36`) — the week passes no `onLongClick`, the dashboard passes the
  exclusion action (`DashboardScreen.kt:269`), so one widget serves both without a second variant.
- **`IconButton` + `Icons.Filled.DateRange`** (`DashboardScreen.kt:204-209`) — the week entry,
  content description `week_title` (`strings.xml:58`); hidden on a dated dashboard by the
  `requestedDate == null` guard (`DashboardScreen.kt:203`).
- **Room `@Query`** `totalsFor` (`DailyAggregateDao.kt:18-19`) and `countFor`
  (`DailyUnlockDao.kt:15-16`) — both get their first production callers here, through the store
  wrappers (`SessionHistoryStore.kt:38-39,53-54`); no new dependency, no schema change.
- **`java.time` formatters** — `EEE` for the bar's weekday (`Formatting.kt:22-25`) and the existing
  `formatDayLabel` (`Formatting.kt:33-42`) for the announced day.
- No new permission, no manifest change, no network, no new dependency.

## Implicit decisions

- **The store's window includes today** (`week(today)` → `:38-39`), even though today is never
  consolidated: the bar builder tests `date == todayDate` **before** looking at the store
  (`WeekMetrics.kt:28-29`), so an unexpected stored row for today could not win. The ranking,
  however, sums every stored row it is given (`:40-43`) — today has none, so the two agree.
- **`daysUsed` for the live side is `1`, not a count of sessions** (`WeekMetrics.kt:48`): today
  contributes at most one day, whatever the number of sessions. The caption therefore reads "N days"
  for distinct days, which is what the user means.
- **Absent bars keep `totalMillis = 0L`** (`WeekMetrics.kt:33`) — the value is inert; the `State`
  carries the meaning. The screen uses it only for height, and an absent bar never sets the scale
  (`WeekScreen.kt:206-209`).
- **Exclusions never touch the bars**, only the ranking (`WeekMetrics.kt:15,41,46`): excluding an app
  hides its row, not the time it contributed to the day's total. Pinned by
  `WeekMetricsTest.kt:97-107`.
- **`windowDays` is a parameter defaulting to 7** (`WeekMetrics.kt:22`), not a constant, mirroring
  `availableDays` (`AvailableDays.kt:10`): changing retention means changing this, `SIX_DAYS`
  (`SessionHistoryStore.kt:77`) and `availableDays` together.
- **The visible label and the announced label differ**: the bar shows the locale's short weekday and
  the day of month (`WeekScreen.kt:272-280`), its content description uses `formatDayLabel`, so a
  screen reader hears "Today" or "Yesterday" (`WeekScreen.kt:233-238`).
- **The screen shows a spinner until *both* reads answer** (`WeekScreen.kt:144-152`), so a slow first
  live read holds the stored bars back instead of drawing them and growing the ranking afterwards.
- **A read failure degrades to a plausible value, never to an error**: empty totals, an empty live
  day, an empty day view (`WeekScreen.kt:89,98`; `DashboardScreen.kt:114,119-121`), all through
  `readOr`, which rethrows `CancellationException` so a screen that leaves mid-read still cancels
  (`ReadOr.kt:10-18`). The log line is `Log.w("Foco", "Day read failed", …)` (`:16`).
- **The dated dashboard keeps its single read and no refresh** (`DashboardScreen.kt:103-127`): the
  `LaunchedEffect` keys are `lifecycleOwner`, `refreshIntervalMillis` and `requestedDate`, and only
  the `null` branch loops — so a past day is read once when the back-stack entry is created, which
  is what "read-only" means here.
- **The requested detail day is applied once and then abandoned**
  (`AppDetailScreen.kt:98-110`): after the index is set, `requested` becomes `null`, so later taps on
  ‹ › win and the screen is not dragged back on the next resume.
- **The week's ranking rows open today's detail** (`FocoNavHost.kt:38`), while the day view's rows
  carry their date (`:54`): the week ranks a window, so there is no single day to open, and the
  detail's own selector still walks the stored window.
- **The seam is the dashboard's, carried forward** (`FocoNavHost.kt:21`), not a new one on the week
  or the day view: the apply note records that the detail change's planned seam was dropped
  (`tasks.md:55-58`), so one defaulted parameter at the nav root is the only injection point — and
  the instrumented test needs exactly that to drive the chain
  (`WeekScreenTest.kt:70-72`).

## Cognitive Sign-off

Answer in your own words (the agent must not fill these):

1. You tap **today's** bar and the day view shows today's live numbers; tap yesterday's and it shows
   the store's numbers — while the week's own today bar also read live. Why can today's dated view
   not simply read `sessionsFor(today)`, and what would the screen show if it did?
   - TODO(human)
2. A stored day's day view says "5 desbloqueos", read from `unlocksFor` and falling back to `0`. What
   exactly does that stored number carry, and what can it *not* tell you (or a future feature) —
   including the case where the read answers `null`?
   - TODO(human)
3. The week opens, the store read succeeds, but the live read fails once. Describe what each of the
   seven bars shows, and say whether the user can tell today's bar apart from a day that genuinely
   has no data — and when it would be corrected.
   - TODO(human)
