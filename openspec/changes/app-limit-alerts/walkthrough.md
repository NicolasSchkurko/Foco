# Walkthrough — app-limit-alerts (tech brief)

> For the human: this brief explains the mechanism of the change. Answer the questions in
> `## Cognitive sign-off` yourself; the agent never fills them.

## Mechanism

A daily limit already lives in `limits.db` (Room). Version 2 adds one boolean column,
`app_limit.alertEnabled` (default `0`, so every existing limit starts with alerts off), and a small
`app_limit_alert_fired` table keyed by package, local day (ISO-8601 text) and tier (50/75/90). The
1→2 migration is non-destructive: it alters the existing table and creates the new one; the exported
schema is `app/schemas/com.foco.app.data.db.AppLimitDatabase/2.json`.

The store (`AppLimitsStore`) owns the rules around that state. `setLimit` keeps an existing alert
flag when the minutes change, forces it off for a 0-minute limit, and clears the current day's
fired tiers. `setAlertEnabled` refuses to enable alerts without a positive limit and also clears
today's fired tiers. `clearLimit` removes the row and its fired tiers. Fired tiers are read and
marked per package and local day.

The threshold rules are pure (`AppLimitRules.crossedAlertTiers`): for a positive limit and usage
below 100 %, they return the crossed tiers in increasing order (`50`, `50,75`, `50,75,90`). No
limit, a 0-minute limit, no usage, and usage at or over 100 % return none — the 100 % block owns
that boundary.

The monitor is still `UsageMonitorService`, on its existing two-minute tick and with the same
cumulative `UsageMonitorEvaluator.usedSecondsToday` read. Each tick asks `ForegroundApp` for the
current package, reads that package's limit and alert flag, and calls the pure
`evaluateLimitCheck`. That function blocks first when the limit is reached (the `app-limits` overlay
path), otherwise it computes the highest crossed tier, skips it when already fired, and calls the
notifier. The notifier returns whether it actually posted; only then does the function return the
crossed tiers and the service mark all of them fired. The accessibility service never posts alerts:
it still only blocks, so an accessibility event alone cannot produce a warning.

`AppLimitAlertNotifier` creates the `limit_alerts` channel on first use, builds one notification
naming the app, the tier and the formatted limit, with exactly one action (`limit_alert_ack`, shown
as "Got it" in English and "Entendido" in Spanish). The action is a `PendingIntent` to
`AppLimitAlertDismissReceiver`, which cancels the notification. `canNotify()` checks both the app's
notification permission and the alert channel's importance, so a blocked channel is treated like a
denied permission: nothing is posted and no tier is marked fired.

Settings collects `limitsFlow` and `alertsFlow` and renders a `Switch` per app. The switch is off by
default, enabled only for a positive limit, persists through `setAlertEnabled`, and shows a caption
when Foco notifications or the alert channel are off. Enabling it requests `POST_NOTIFICATIONS` on
Android 13+; the permission state is refreshed on `ON_RESUME` and after the system dialog.

## Physical traceability

- `app/src/main/java/com/foco/app/data/db/AppLimit.kt` — the limit row with `alertEnabled` and its
  default.
- `app/src/main/java/com/foco/app/data/db/AppLimitAlertFired.kt`,
  `AppLimitAlertFiredDao.kt` — the fired-tier table and its per-package/per-day queries.
- `app/src/main/java/com/foco/app/data/db/AppLimitDatabase.kt` — version 2, the two DAOs and
  `MIGRATION_1_2`.
- `app/src/main/java/com/foco/app/data/AppLimitsStore.kt` — the store, `alertsFlow`, the
  set/clear/clear-fired rules and the singleton over `limits.db`.
- `app/src/main/java/com/foco/app/domain/AppLimitRules.kt` — `ALERT_TIERS` and
  `crossedAlertTiers`.
- `app/src/main/java/com/foco/app/data/UsageMonitorService.kt` — the tick wiring and the pure
  `evaluateLimitCheck` seam.
- `app/src/main/java/com/foco/app/data/AppLimitAlertNotifier.kt` — channel, content, action,
  permission/channel gate.
- `app/src/main/java/com/foco/app/data/AppLimitAlertDismissReceiver.kt` — the action's receiver.
- `app/src/main/java/com/foco/app/ui/settings/AppLimitsSection.kt` and `SettingsScreen.kt` — the
  per-app switch, the permission caption and the store wiring.
- `app/src/main/res/values/strings.xml`, `values-es/strings.xml` — the toggle, caption, channel,
  title, message and action strings.
- `app/src/main/AndroidManifest.xml` — the dismiss receiver.
- Tests: `AppLimitAlertRulesTest` (JVM); `AppLimitAlertStoreTest`, `AppLimitAlertMigrationTest`,
  `AppLimitAlertNotifierTest`, `AppLimitsSettingsTest`, `AppLimitsStoreTest`,
  `UsageMonitorServiceTest`, `AppLimitSchemaBaselineTest` (instrumented).
- Device state touched by tests: the notification permission (granted by the tests), the exported
  schema asset, and the test database files; each class removes its database.

## Limits and failure modes

- **Two-minute granularity.** An alert can arrive up to two minutes after the percentage is crossed.
  If the app is closed before the tick, no alert is posted at that moment; a later tick while the
  app is open evaluates the current cumulative usage.
- **Foreground only.** The alert path only runs when `ForegroundApp.currentPackage()` returns the
  package; background usage is never alerted. If the accessibility service has not seen a window
  yet, there is no package and no check.
- **Permission or blocked channel.** The notifier skips the post and `evaluateLimitCheck` returns
  no tiers, so the tier is not marked fired. Granting the permission (or unblocking the channel)
  lets the next tick alert the highest crossed tier.
- **One alert per tier per day.** The fired set is per package and local ISO day. Changing the limit
  or the toggle clears the current day's set; past days remain in the table but are never read.
  There is no pruning: at most three small rows per app per day.
- **Notification posted but mark lost.** If the process dies between posting and `markFired`, the
  next tick may post the same tier again: the behavior is at-least-once, not exactly-once.
- **Dismissal does not reset.** Tapping the action or swiping the notification only removes it; the
  tier stays fired for the day, so it does not reappear.
- **100 % wins.** At or over the limit the service requests the existing block and returns without
  alerting, even if a lower tier was never marked. A 0-minute limit blocks at open and never alerts.
- **Notification id collisions.** The id is a stable hash in `1000..1999`; two packages could
  collide and replace each other's alert. This is a small, accepted risk.
- **Midnight and timezone.** The day is `LocalDate.now()` at check time. A new local day starts with
  no fired tiers; a timezone change can restart or skip a local day.
- **Monitor availability.** Alerts depend on the same foreground service and accessibility grant as
  `app-limits`; a force-stop, OEM killer or lost accessibility stops alerts too.
- **No network.** The change adds no permission, dependency or backend; everything is local.

## Verification

- JVM suite (`gradlew.bat test`): 144 tests, 0 failures, 2 skipped (existing skips).
- `openspec validate app-limit-alerts --strict`: valid.
- Instrumented, API 36 AVD `medium_phone`, one class per run (or one `am instrument` batch):
  `AppLimitAlertStoreTest` (8), `AppLimitAlertMigrationTest` (1),
  `AppLimitAlertNotifierTest` (1), `AppLimitsSettingsTest` (8), `AppLimitsStoreTest` (5),
  `UsageMonitorServiceTest` (9), `AppLimitSchemaBaselineTest` (2) — all green.
- The notifier test needs `POST_NOTIFICATIONS`; it grants it in `@Before`, posts, checks the single
  action and sends it. The denied-permission path is covered by the monitor's
  `evaluateLimitCheck` test (a notifier that returns false leaves the tier unfired); revoking the
  permission in-process would kill the instrumentation process.
- `gate-check --run-tests` records the green evidence for this change.

## Touched APIs

- Room `@Database` version 2, `Migration`, `SupportSQLiteDatabase`, `MigrationTestHelper` and the
  exported schema asset.
- `NotificationChannel`, `NotificationManager`, `NotificationCompat.Builder`,
  `NotificationManagerCompat`, `POST_NOTIFICATIONS`, `PendingIntent.getBroadcast`.
- `BroadcastReceiver` for the single dismiss action.
- Compose `Switch` and `semantics { contentDescription = ... }`.
- `LocalDate` (ISO day keys) and the existing `UsageRepository`/`UsageStatsManager` read.
- Test-only device state: `pm grant` and `appops` for usage access; no production permission is
  added.

## Implicit decisions

- **Alerts ride the tick, not the accessibility event.** The accessibility path is the block at
  open; a warning while the app is already open belongs to the same two-minute check that reads
  cumulative usage. This avoids a second timer and a second usage query.
- **Mark only after a real post.** `evaluateLimitCheck` returns the crossed tiers only when the
  notifier reports success; this is what makes a denied permission recoverable on a later tick.
- **Highest only, all crossed marked.** A jump from 60 % to 90 % shows one notification at 90 % and
  marks 50/75/90, so the user is not hit by a burst.
- **Keep the flag across a limit change.** Changing 30 → 60 minutes keeps alerts on (unless the new
  limit is 0); the fired tiers are cleared so the new budget can alert again.
- **Flag in `app_limit`, fired tiers in their own table.** The flag is configuration; the fired
  tiers are per-day events with a composite key.
- **Localized action.** The only action is `limit_alert_ack` ("Got it" / "Entendido"); the English
  resource is a translation of the spec's **Entendido**, not a second action.
- **Channel check as part of `canNotify`.** The system can disable the alert channel while app
  notifications remain enabled; treating that as "cannot notify" keeps the fired set honest.
- **No pruning of past days.** Only the current day is queried; deleting old rows would add a write
  path for no user-visible behavior.

## Cognitive sign-off

Answer in your own words (the agent must not fill these):

1. Where is a threshold alert decided and posted, and why does an accessibility event alone not
   produce one?
2. Why are tiers marked fired only after `notify` succeeds, and what happens when the user later
   grants notifications or unblocks the alert channel?
3. What clears the fired tiers, and why is changing the limit or the toggle enough to re-arm the
   alerts for the day?
4. What is the worst-case delay for an alert, and what happens at 100 % or when the app is not in
   the foreground?
