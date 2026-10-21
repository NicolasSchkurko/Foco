# Tech Brief — dashboard-ring-content-survives-settings

## Mechanism
The home's ring position survives recreation (rotation, Settings round-trip) through
`rememberSaveable`, but its content map (`ringContent: MutableStateMap<String, RingContent>`)
was held in a plain `remember`, so every recreation dropped the already-loaded content and
`RingLoading()` reappeared for positions the user had already seen — a regression of
`DECISIONS.md:415` (`ring-move-without-loader`), which reserves the placeholder for the
cold start only.

The fix makes the content map saveable by giving `RingContent` a `Saver` that serialises each
entry to a list of primitives (sessions, unlock timestamps, unlock count for `Day`; totals
for `Week`) and restores them into a fresh `mutableStateMapOf`. The map's *keys* are the ring
position keys, so a restored position renders its content immediately, with no placeholder
and no silent re-read.

## Physical traceability
- `app/src/main/java/com/foco/app/ui/dashboard/DashboardScreen.kt:161` —
  `val ringContent = rememberSaveable(saver = ringContentSaver) { mutableStateMapOf<String, RingContent>() }`
  (was `remember { ... }`).
- `app/src/main/java/com/foco/app/ui/dashboard/RingContent.kt:26` — new
  `ringContentSaver: Saver<SnapshotStateMap<String, RingContent>, Any>`:
  - `save` (line 27): maps each entry to a tagged list (`"D"` for Day with sessions/unlockTimes/count,
    `"W"` for Week with totals).
  - `restore` (line 43): rebuilds the map, skipping malformed entries (`return@forEach`).
- `app/src/main/java/com/foco/app/ui/dashboard/RingContent.kt:1-16` — imports `Saver`,
  `SnapshotStateMap`, `mutableStateMapOf`, `UsageSession`, `DayData`, `DailyAggregate`.
- Contract: `openspec/specs/usage-tracking/spec.md` — new requirement "A restored position
  renders its content without a placeholder" (ADDED, merged from the delta).

## Limits and failure modes
- State that crosses recreation: the ring content map (sessions + unlock timestamps + count
  for up to 8 positions). Previously in-memory only; now saved into the saved-state Bundle
  via `rememberSaveable`.
- Size bound: today + week + up to 6 finished days → bounded, same as the map already had.
- Restore path runs on the main thread during composition; entries are small (primitive lists),
  so no `withContext(IO)` needed. Malformed entries are skipped, never crash.
- No schema/DB change; no concurrency change (the map is still single-owner inside
  `DashboardScreen`); no new dependency.
- Failure mode: if the Bundle is too large or restore partially fails, entries are skipped
  silently (`return@forEach`), so the affected position falls back to `RingLoading()` — the
  cold-start behavior, which is the safe degradation.

## Touched APIs
- `androidx.compose.runtime.saveable.Saver`
- `androidx.compose.runtime.saveable.rememberSaveable`
- `androidx.compose.runtime.snapshots.SnapshotStateMap`
- `androidx.compose.runtime.mutableStateMapOf`
- `com.foco.app.model.UsageSession`
- `com.foco.app.model.day.DayData`
- `com.foco.app.data.db.DailyAggregate`
- `rememberSaveable(saver = ringContentSaver) { mutableStateMapOf<String, RingContent>() }`

## Implicit decisions
- Saved the full content (sessions + unlock timestamps + count) rather than just keys, so a
  restored position renders without a placeholder and without a silent re-read — matching
  `DECISIONS.md:415` (`ring-move-without-loader`).
- Used a tagged-list encoding (`"D"`/`"W"`) instead of a sealed Saver, because `RingContent`
  is a sealed interface and Compose's `Saver` requires a saveable return type; lists of
  primitives are saveable by default.
- Malformed entries are skipped rather than failing the whole restore, so a single corrupt
  entry degrades one position to `RingLoading()` instead of crashing the screen.
- The week position's totals are re-saved on every recomposition that touches the map; this is
  acceptable because the map is bounded and the data is already in memory.

## Cognitive sign-off

Answered by the human on 2026-10-21 — answers transcribed verbatim (repo convention). The
sign-off was recorded on 2026-10-21 (`understood_by`/`understood_at` in `.openspec.yaml`).

1. `save` returns a `List` of tagged entries (`"D"`/`"W"`) made only of saveable primitives
   (`String`, `Long`, `Int`, nested `List`s). Compose can put that in a Bundle because the
   Saver exists precisely to convert the domain objects (`RingContent`, `UsageSession`,
   `DailyAggregate`) into those primitives. On device, after a rotation or a Settings
   round-trip, a position already shown must render its content with no `RingLoading`
   spinner.

2. `return@forEach` skips only the malformed entry: that position is left out of the restored
   map and falls back to `RingLoading()` until it reloads, while the rest of the map is
   restored intact. Discarding the whole map would empty every already-loaded position
   because of one bad entry, so skipping one is the right degradation.

3. Yes, it can go stale: the Saver snapshots the in-memory map, so if a consolidation lands
   between save and restore, the restored week total is the snapshot's, not the freshly
   consolidated one — the map is a cache in memory and does not observe the database. The
   re-save on every recomposition keeps the in-memory photo fresh, but it is still not live.
   Accepted as a bounded cache.

## unclear:
- Whether `rememberSaveable` for a `SnapshotStateMap` with a custom `Saver` survives process
  death (not just rotation). The Saver returns a `List`, which is saveable, so it should; but
  this was not exercised on device in this session.
- The `DashboardRingTest` (androidTest) was not re-run on the emulator for this change; only
  `:app:compileDebugKotlin` and `:app:testDebugUnitTest` (JVM) were green. Recommend running
  `connectedDebugAndroidTest` for `DashboardRingTest` before archiving.
