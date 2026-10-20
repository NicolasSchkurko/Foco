# Proposal — app-limit-alerts

## Why

`app-limits` stops a limited app at 100 %. The user wants an optional, actionable warning before that
point: a per-app toggle that alerts at 50 %, 75 % and 90 % of the app's daily limit. It must not
alert for an app without a limit or with a 0-minute limit, and the only action is **Entendido**. The
2-minute monitor is already the place where today's usage is known; this change adds the threshold
decision and the notification, not a second poller.

## What Changes

- Settings adds a **per-app alert toggle** next to the limit. It is off by default and can only be
  enabled for an app with a positive limit.
- Alerts ride the **existing 2-minute check**: one cumulative today's-usage read per tick, no extra
  timer and no alert from an accessibility event alone. The check may report a threshold up to 2
  minutes after it is crossed.
- While the limited app is in the foreground, that check computes the highest threshold crossed
  (50, 75 or 90) and shows at most one system notification for it. A jump from 60 % to 90 % shows
  only 90 %; the lower crossed tiers are marked fired.
- The notification names the app, the threshold and the limit, and has exactly one action:
  **Entendido**, which dismisses it. It does not block or close the app; the 100 % block remains the
  `app-limits` overlay.
- Fired tiers are stored per app and local day, so each threshold fires once and resets at midnight.
  Changing the limit or the toggle clears that app's fired tiers for the day.
- No alert for an app without a limit, for a 0-minute limit, or once the block at 100 % has started.
- `POST_NOTIFICATIONS` is respected: when notifications are disabled, no alert is shown and the
  toggle explains the permission state.

## Expected outcome

With Instagram at 30 min and alerts on, the user gets one notification at the first 2-minute check
at or after 15, 22.5 and 27 minutes, never a duplicate, and never a notification after the block
appears. Turning alerts off, removing the limit, or setting 0 minutes produces no alerts. The user is
warned **before** the block.

## State and persistence mechanism

The alert flag and the fired tiers live in the existing `limits.db` Room database created by
`app-limits`: a boolean column on `app_limit` and a small `app_limit_alert_fired` table keyed by
package, local day and tier (50/75/90). Version 2 adds both with a non-destructive migration from
version 1. No DataStore, no duplicate usage store, no network, no new dependency.

## Alternatives considered

| Option | Why yes | Why not | Recommendation |
| --- | --- | --- | --- |
| Per-app toggle (chosen) | The user asked for it; each app can be different | More rows in Settings | **Chosen** |
| Global toggle | One switch | Cannot silence only one app | No |
| Always-on for every limited app | No toggle work | Not optional; noisy | No |
| System notification with one action (chosen) | Visible while using the limited app; non-blocking; supports **Entendido** | Needs `POST_NOTIFICATIONS` | **Chosen** |
| In-app popup | Simple when Foco is open | Not visible while another app is in front | No |
| Accessibility overlay | Uses the existing service | Intrusive for a warning; would compete with the block overlay | No |
| Highest crossed tier only (chosen) | Avoids a burst after a jump | Skips the intermediate message | **Chosen** |
| Notify every crossed tier | More complete | 60 %→90 % would fire twice at once | No |
| Fired tiers in Room `limits.db` (chosen) | Survives reboot; per-day reset; same DB as limits | Version 2 migration | **Chosen** |
| Fired tiers in DataStore | No migration | Splits alert state from the limit config | No |

## Risks and limits

- The alert rides the 2-minute check and reads cumulative usage; a threshold can be reported up to 2
  minutes late. Tick deltas are never summed.
- Alerts are evaluated only while the limited app is in the foreground. If it is closed before the
  check, no alert is posted at that moment; the next check while the app is open evaluates the tier.
- `app-limits` is verified and archived (2026-10-20); this change builds on its exported version 1
  schema.
- Notification permission may be denied; the alert is skipped and the toggle shows the state.
- The 100 % block is the existing overlay; alerts never replace or delay it.
