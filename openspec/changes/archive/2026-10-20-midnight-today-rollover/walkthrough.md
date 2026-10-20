# Walkthrough — midnight-today-rollover

## What this change does

Before this change, the home, detail and week captured `LocalDate.now()` once with `remember`, and
the export passed a `today` but read the live sessions with `loadToday()`. If the app crossed
midnight, the screens could keep the previous date while the live read returned the new day.

Now there is one observable current date per screen. It refreshes:

1. on every `ON_RESUME`, and
2. at the next local midnight while the app stays open.

The home, detail, week and export all use that same date. The export reads the live sessions with
`repository.loadDay(today)`, so `weekEnd` and the live entry always describe the same day.

No store is added. The date is in-memory, derived from the device clock; the existing `UsageRepository`
and `foco.db` remain the data sources.

## Mechanism

`rememberCurrentDate(clock)` (`app/src/main/java/com/foco/app/ui/common/CurrentDate.kt:24`) owns the
state:

- the initial value is `LocalDate.now(clock)` (`CurrentDate.kt:25`);
- a lifecycle observer sets it again on `ON_RESUME` (`CurrentDate.kt:29-33`);
- a coroutine computes the next local midnight, delays until it, and sets the date again
  (`CurrentDate.kt:37-43`). It does not tick every minute; it waits once for the boundary.

The clock is a parameter with a system default, so the test can inject a mutable clock without
waiting for real midnight.

## Physical traceability

| Piece | Where | What it does |
| --- | --- | --- |
| Current date source | `app/src/main/java/com/foco/app/ui/common/CurrentDate.kt:24` | Resume + next-midnight updates; test clock seam |
| Home uses it | `app/src/main/java/com/foco/app/ui/dashboard/DashboardScreen.kt:115` | `today` is the source for title, ring, week and export |
| Home relabels/reloads | `DashboardScreen.kt:185` | `today` is in the live/stored effect key; an old day becomes stored |
| Home export | `DashboardScreen.kt:240` | Passes the same `today` to `WeekExportService` |
| Detail uses it | `app/src/main/java/com/foco/app/ui/detail/AppDetailScreen.kt:73` | Header, day window and live/stored choice follow it |
| Detail reloads | `AppDetailScreen.kt:110` | `today` is in the read key, so an old live day switches to stored |
| Week live read | `app/src/main/java/com/foco/app/ui/common/WeekContent.kt:67` | Uses `repository.loadDay(today)`, not `loadToday()` |
| Week reloads | `WeekContent.kt:81` | `today` is in the effect key, so the week follows the rollover |
| Export source | `app/src/main/java/com/foco/app/data/WeekExportService.kt:37` | Live sessions come from the same `today` as `weekEnd` |
| Regression test | `app/src/androidTest/java/com/foco/app/ui/TodayRolloverTest.kt:57,69` | Resume after midnight and midnight while open, with a fake clock |

## Implicit decisions

- The date is in-memory only; nothing new is persisted.
- The home does not jump to the new today: the old position becomes `Yesterday`, and the new today
  is a ring stop.
- The detail selector remains index-based, so its selected index can shift when the day window
  changes; the date and the live/stored source are correct.
- The export uses the date captured by the current composition; there is no second clock read inside
  the service.
- A manual clock or timezone change is picked up on the next resume or boundary, not instantly.

## Limits and failure modes

- **The midnight coroutine is not an alarm.** Doze, process suspension or a busy main thread can delay
  it. The next `ON_RESUME` is the guaranteed fallback; while the app stays visible, the delay is the
  normal trigger.
- **A manual clock or timezone change while the app is open** is picked up at the next resume or the
  next date boundary, not instantly. There is no dedicated timezone-change listener.
- **The home does not jump to the new today.** If the user was on yesterday when midnight passed, that
  position becomes `Yesterday` and is read from history; the new `Today` is in the ring. This is
  deliberate: the screen does not move under the user.
- **The detail selector is index-based.** When the day window changes at midnight, the selected index
  can point to a different date (the existing selector behavior). The date itself and the live/stored
  source are correct; a future change could preserve the selected date rather than the index.
- **The export uses the date captured by the current composition.** If the user taps export exactly at
  the boundary before the next recomposition, the old date is used consistently for that export; the
  next frame uses the new one. No second clock read exists inside the service.
- **The dashboard live read shares the date with the label.** It now calls
  `repository.loadDay(today)` (DashboardScreen.kt:116), so a live refresh cannot show the new day's
  numbers under the old label. The label itself can still lag until the helper updates; the read
  stays on the same day.
- **Process death** simply starts a new composition and reads the current date; nothing is persisted.

## Touched APIs and resources

`java.time.Clock`, `LocalDate`, `ZonedDateTime`, `Duration`; Compose `remember`, `DisposableEffect`,
`LaunchedEffect`; Lifecycle `LifecycleEventObserver` / `LocalLifecycleOwner`. No permission, network,
storage or schema change.

## Verification

- `gradlew.bat test` green.
- `TodayRolloverTest` green on the API 36 AVD: resume after midnight and midnight while open.
- `WeekScreenTest`, `AppDetailNavigationTest`, `DashboardRingTest` and `DashboardRefreshTest` green on
  the same AVD.
- `openspec validate midnight-today-rollover --strict` valid; gate green with the expected warnings.

## Cognitive sign-off

Recorded from the human's answers on 2026-10-20.

1. Which two events refresh the current date, and where is that implemented?

   > On every `ON_RESUME` and at the next local midnight while the app remains open. Both live in
   > `rememberCurrentDate` (`CurrentDate.kt:24-47`): the lifecycle observer sets
   > `today = LocalDate.now(latestClock)` on `ON_RESUME` (`:29-35`), and
   > `LaunchedEffect(latestClock.zone)` computes the next midnight, delays until it and sets the date
   > again (`:37-44`), without ticking every minute.

2. When the app was showing yesterday's `Today` at midnight, why does it become `Yesterday` instead
   of keeping the live label, and where does the stored read take over?

   > `stopKey` keeps pointing at the same date, so the user does not jump screens. The title in
   > `formatDayLabel(stop.date, today, …)` changes: the date that was today is now
   > `today.minusDays(1)`, so it shows “Yesterday”/“Ayer”. The data source changes in
   > `LaunchedEffect(lifecycleOwner, refreshIntervalMillis, dayDate, today)` when `today` changes:
   > `dayDate == today` is no longer true, so it falls into the else branch and reads from the store
   > with `store.sessionsFor(dayDate)` and `store.unlocksFor(dayDate)` instead of the live loop. The
   > ring recalculates with the new `today` (`:129`); the old day becomes yesterday, and because its
   > key still exists the position is not reset.

3. If the midnight coroutine is delayed by the system, what does the app do, and what is the user
   likely to see until then?

   > It does nothing special and does not hang: it keeps the last known date and the screen as it is
   > until the coroutine wakes or an `ON_RESUME` (the guaranteed fallback) reads `LocalDate.now`
   > again. In the meantime the user may see the previous day still labelled “Today” for a while.

   **Post-answer fix:** the answer also identified that a live refresh could show the new day's
   numbers under the old label before the date state rolled over. The dashboard live read now uses
   `repository.loadDay(today)` (`DashboardScreen.kt:116`), so that race is closed; the label may
   still lag until the helper updates, but the numbers stay on the same day.
