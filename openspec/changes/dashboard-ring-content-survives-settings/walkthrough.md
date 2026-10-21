# Walkthrough — dashboard-ring-content-survives-settings

## Mechanism

The home ring keeps two pieces of state with different lifetimes: the position/history (saveable,
restored after recreation or a Settings trip) and the content map `ringContent`
(`DashboardScreen.kt:161`, a plain `remember` map of already-loaded day/week data). On return from
Settings the position is back but the map is empty, so the loading branch composes
`RingLoading()` until the read repopulates it — data the app held moments before. The fix gives
the content holder the same lifetime as the position (one hoisted, saveable/retained owner above
the `NavHost`), so position and content are restored as one unit and the placeholder only ever
appears on a genuine cold start.

## Physical traceability

- `app/src/main/java/com/foco/app/ui/dashboard/DashboardScreen.kt:161` — the `remember` map that
  dies with the composition; `:206-248` fill/evict logic; `:504-527` the placeholder branch.
- `app/src/main/java/com/foco/app/ui/FocoNavHost.kt` — where the holder moves above (the seam
  the position already crosses).
- `app/src/androidTest/java/com/foco/app/ui/DashboardRingTest.kt` — the Settings round-trip proof.
- Contract: `usage-tracking/spec.md:443-447,479-494`, `DECISIONS.md:415`.

## Limits and failure modes

- Retained content is bounded by the ring itself (today, week, seven days) — no unbounded growth.
- If the process dies (true cold start), saved state restores the position and the placeholder
  legitimately shows until the read: that is the "first load" case the spec allows.
- Eviction rules (`:240-243`) keep applying; restoring a position whose day left the window falls
  back through the existing path rather than a stale entry.

## Touched APIs

Compose state APIs (`remember`, saved instance state, the existing `NavHost` seam) — no platform
API, no persistence format, no new dependency.

## Implicit decisions

- Hoist, don't re-read: the defect is a lost in-memory cache, and re-reading would show exactly the
  placeholder this change removes.
- Content is not written to disk (no serialization of session data); survival covers configuration
  change and navigation, and cold start still re-reads — privacy and size stay as they are.
