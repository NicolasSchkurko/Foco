# Proposal — alert-notification-distinct-ids

## Why

Alert notifications share a colliding id space:
`AppLimitAlertNotifier.kt:102` posts with `1000 + packageName.hashCode() % 1000`. Two packages whose
hashes land in the same slot get the **same** notification id → the second alert posted replaces
the first, whose tier was already marked as fired (`Fired tiers reset per day`,
`app-limits:240-257`) — so for that app **the alert of that day is never seen**.

The contract describes the alert as "a system notification naming the app, the reached tier and the
limit" (`app-limits:222-238`) but is silent on notification identity across apps (🟡): nothing says
each app's alert must be its own notification. The result of the gap is exactly the silent loss
above.

## What changes (proposed contract)

- New requirement in `app-limits`: each app's alert uses its own notification id derived from its
  package, so alerts for different apps stand side by side; `Entendido` dismisses only its own;
  a later tier of the same app may update that app's own notification.
- Implementation: id = stable per-package value (full hash as int, or a counter table — chosen at
  apply, uniqueness is the contract), replacing the `% 1000` truncation.

## Alternatives

| Option | Effect | Verdict |
|---|---|---|
| Id from the full package hash (stable, collision astronomically unlikely) | one line; stable across posts | **Proposed** |
| Monotonic counter per package held in the store | survives nothing extra the hash doesn't already; more state | not proposed |
| One id per tier | three notifications per app at once — noisy, no added information | not proposed |

## Risks

- The id becomes an implementation detail of the contract "one notification per app": a future
  scheme change re-posts rather than updates (harmless: alerts are informational).

## Origin

Filed 2026-10-21 by the anchored bug-finder audit (finding 14, severity low, 🟡 spec-silent);
`kind: feature` via /spec-add. Awaiting human acceptance — no code written.
