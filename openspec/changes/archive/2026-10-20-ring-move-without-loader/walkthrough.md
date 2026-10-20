# Walkthrough — ring-move-without-loader

## What this change does

A ring move used to blank the day's data before reading the next one. The spinner and the
“Loading…” subtitle could appear for a frame or two inside the 320 ms slide. Now the home keeps the
position already on screen until the requested position has its content, then animates the swap. A
loading placeholder remains only when the home has never shown a position (cold start).

## Mechanism

- `RingContent` (`app/src/main/java/com/foco/app/ui/dashboard/RingContent.kt:7`) is the ready data for
  one position: `Day(data, unlocks)` or `Week(totals)`.
- The home keeps two positions:
  - `requestedKey` is what the arrows, swipes, bars, back history and calendar move
    (`DashboardScreen.kt:132`, `:156-179`).
  - `shownKey` is what the header and `AnimatedContent` render (`DashboardScreen.kt:133`, `:439`).
- A `mutableStateMapOf<String, RingContent>` caches ready content (`DashboardScreen.kt:143`). The
  cache is pruned to the ring, today, the requested and the shown keys (`:246-249`).
- Stored days load through `SessionHistoryStore.sessionsFor`/`unlocksFor` (`:222-233`). The week
  loads `SessionHistoryStore.week` (`:235-243`).
- Today/Week keep the live read under `repeatOnLifecycle(RESUMED)` with the existing interval
  (`:207-220`). It fills today's cache and the week's live day; a week request waits for both.
- `shownKey` advances only when `requestedReady` is true (`:251-255`). Until then the previous
  `shownKey` and header stay visible; the outgoing cache entry stays for the exit side of the
  transition.
- The header title, usage line and week-only export action derive from `shownKey` and its cached
  content (`:295-360`). The old “Loading…” subtitle fallback is gone.
- `RingLoading` (`:588-596`) is the only placeholder and is shown only when a shown position has no
  content. It carries `ring_loading` for the regression test.
- `WeekContent` (`app/src/main/java/com/foco/app/ui/common/WeekContent.kt:45`) is now a renderer:
  `today`, `totals`, `todayData`, `excludedPackages`, callbacks. It no longer reads the repository,
  store or exclusions, and has no spinner.

## Physical traceability

| Piece | Where | What it proves |
| --- | --- | --- |
| Ready content type | `RingContent.kt:7-12` | Day/Week data shape |
| Requested vs shown | `DashboardScreen.kt:132-133`, `:156-179` | Navigation moves requested; rendering follows shown |
| Cache | `DashboardScreen.kt:143`, `:246-249` | Ready content and bounded lifetime |
| Day load | `DashboardScreen.kt:222-233` | Stored day reads sessions/unlocks |
| Week load | `DashboardScreen.kt:235-243` | Week totals read once per day |
| Live read | `DashboardScreen.kt:207-220` | Today/week cadence and shared live data |
| Readiness gate | `DashboardScreen.kt:251-255` | Shown advances only when ready |
| Header/export | `DashboardScreen.kt:295-360` | Title, usage and export follow shown |
| Cold placeholder | `DashboardScreen.kt:588-596` | Only loading surface, tagged |
| Week renderer | `WeekContent.kt:45-109` | No reads or spinner |
| Regression | `DashboardRingTest.kt:181-209` | A move waits with no `ring_loading` |

## Limits and failure modes

- **Cached stored days can be stale.** A store update while a stored day is cached does not reload it
  until the requested position is revisited after the cache entry is pruned or the composition is
  recreated. The week cache reloads when the ring/store changes or the requested week is entered.
- **The live read stops on a stored day.** Moving to a stored day cancels the live cadence; entering
  today or the week starts it again. The cache keeps the last live day until then.
- **Cold start only.** If there is no shown content at all, `ring_loading` is visible; every move
  during a session keeps the previous content.
- **A week request waits for today.** Week totals and today's live day are both required; if today's
  read is slow, the previous position stays on screen with no spinner.
- **Midnight while the week is shown.** The week can briefly lack the new day's live entry after the
  date rolls over; this is a data-refresh moment, not a user move, and the placeholder may appear.
- **No persistence.** The cache is in-memory and bounded by the ring; process death starts from the
  restored requested position and reads again.
- **The export action follows the shown week.** Tests and users see it only after the week is shown;
  the existing export test now waits for it.
- **The ring's shortcut day** (from an absent week bar) remains outside the ring and keeps its own
  cache entry while it is the requested/showed position.

## Touched APIs and resources

Compose `mutableStateMapOf`, `rememberSaveable`, `AnimatedContent`, `LaunchedEffect`,
`repeatOnLifecycle`; existing `UsageRepository`, `SessionHistoryStore`, `ExcludedAppsStore`,
`RingMotion` and `DayData`/`DailyAggregate`. No dependency, permission, schema, network or new
storage.

## Implicit decisions

- Requested and shown are separate; a move does not mutate the rendered position until its content is
  ready.
- The cache is capped to the ring plus the two positions and today; there is no disk cache.
- A stored day is read once per cache lifetime; the live day and week keep their existing cadence.
- The header is derived from shown, never from requested.
- The export action is week-only and follows shown.
- The loading placeholder is only for the cold start; the move itself never composes it.
- The existing 320 ms slide/fade and reduced-motion behavior are unchanged.

## Verification

- `gradlew.bat test` green.
- API 36 AVD: `DashboardRingTest`, `WeekScreenTest`, `DashboardRefreshTest` and
  `WeekExportShareTest` green; the new regression holds `loadDay` and finds no `ring_loading` during
  the move.
- `RingMotionTest` (JVM) green.
- `openspec validate ring-move-without-loader --strict` valid; gate green.

## Cognitive sign-off

Recorded from the human's answers on 2026-10-20.

1. Why does the home keep `requestedKey` and `shownKey` apart, and which one do the arrows and the
   header follow?

   > One is where the user wants to go; the other is what can already be shown. `requestedKey` is
   > the intention: the arrows, swipe, bars, calendar and back move it and trigger the loads.
   > `shownKey` is what actually renders: the header, the subtitle, the export action and
   > `AnimatedContent(targetState = shownKey)`. The arrows follow `requestedKey`; the header follows
   > `shownKey`, and it advances only when `requestedReady` is true. That is why tapping an arrow
   > does not jump the header immediately: it waits for the destination content.

2. When the user moves to a position whose data is not cached, what stays on screen and what makes
   the new position appear?

   > `requestedKey` changes immediately, but `shownKey` stays on the previous position: the old
   > content and header remain visible, with no spinner. Meanwhile the destination loads: a stored
   > day through `sessionsFor` + `unlocksFor` (`:222-233`); the week through `store.week` plus
   > today's live day (`:235-243` and `:207-220`). When that enters `ringContent`, `requestedReady`
   > becomes true, the `LaunchedEffect` sets `shownKey = requestedKey` (`:253-255`), and only then
   > does `AnimatedContent` animate the change. The old entry is kept for the exit side of the
   > transition (`:246-249`). The week also waits for today's live day (`:251-252`); if it is slow,
   > the previous position stays.

3. Where can `ring_loading` still appear, and why is that not a regression in a normal move?

   > `ring_loading` is in `RingLoading` (`:588-596`) and is composed only when the shown position has
   > no cached content: on cold start (empty cache and initial `shownKey`), after process death or
   > recreation (keys restored by `rememberSaveable` but the cache is in memory), and at midnight
   > with the week shown, when `todayKey` changes and the new day's live entry is not there yet. It
   > is not a regression in a normal move because during a move `shownKey` still points to the
   > previous cached position; the destination becomes `shownKey` only when its content is ready, so
   > `RingLoading` is not composed. The regression test holds `loadDay` and verifies that
   > `ring_loading` does not appear during the move (`DashboardRingTest.kt:181-209`).
