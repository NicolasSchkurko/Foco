# Proposal — app-limits-window

## Why

`app-limits` gives each app a daily minute budget but lets it be used at any hour. The user wants a
second, independent restriction: a recurring local-time window. For example, Instagram can have a
30-minute limit **and** be usable only between 09:00 and 23:00. Outside that window the app is
blocked; time outside the window does not count toward the 30 minutes; threshold alerts are evaluated
only inside it.

## What Changes

- Settings adds a **per-app use window**: No window (default) or a start/end in the local zone, on a
  15-minute grid. Overnight windows are allowed (e.g. 22:00–06:00); equal start and end is rejected
  (clear the window to allow all day).
- The window is recurring daily and independent of the minute limit. A package can have a window
  without a minute limit, a minute limit without a window (today), or both.
- Outside the window the monitor blocks the app with the existing 3-second full-screen overlay and a
  distinct **“Outside allowed hours”** message; the user cannot skip it. At app open the accessibility
  event blocks immediately when either the window is outside or the daily limit is already reached.
  If the app is already open when the boundary arrives, a one-shot boundary check blocks it then;
  the 2-minute check remains the fallback if the process was delayed.
- The daily minute budget counts **only the portions of today's sessions inside the window**; outside
  time is never added. The budget still resets at local midnight, and the window recurs daily.
- The 50/75/90 alerts are evaluated only inside the window; outside, the block is the only feedback.
- No new service or permission: the existing `UsageMonitorService`, accessibility service and block
  overlay are reused.

## Expected outcome

With Instagram at 30 min and a 09:00–23:00 window, opening Instagram at 08:50 shows the “outside
allowed hours” screen for 3 seconds and returns home; opening at 09:10 allows use until 30 minutes of
in-window time, then the existing limit block appears. Overnight windows work; the window and limit
survive a reboot; no usage outside the window is counted.

## State and persistence mechanism

The window is a new `app_limit_window` table in the existing `limits.db` Room database, keyed by
package with `startMinute`/`endMinute` (minutes from local midnight). It is independent of
`app_limit`, so a window can exist without a minute limit. The database moves to version 3 after
`app-limits` v1 and `app-limit-alerts` v2; the 2→3 migration only creates the new table (no
destructive migration). No DataStore, no network, no new dependency.

## Alternatives considered

| Option | Why yes | Why not | Recommendation |
| --- | --- | --- | --- |
| Window table in `limits.db` (chosen) | Window can exist without a limit; tiny; versioned with limits | One more table and v3 migration | **Chosen** |
| Window columns on `app_limit` | Fewer tables | Forces a limit row; window-only apps cannot exist; nullable/tri-state | No |
| DataStore window | No migration | Splits the same per-app row across stores | No |
| Same 3-second overlay with a different message (chosen) | Consistent with the limit block; cannot be skipped | Reuses the block surface | **Chosen** |
| Notification only | Less intrusive | Does not stop usage; the user asked for a block | No |
| Home only without overlay | Simple | No explanation; unclear why the app closed | No |
| 15-minute anchored dropdowns (chosen) | Matches the existing picker pattern and limit grid | More taps than a modal time picker | **Chosen** |
| Material3 modal `TimePicker` | Precise | Contradicts the anchored-dropdown decision; no such picker exists today | No |
| Count only in-window usage (chosen) | Matches “no usage outside”; alert percentages are in-window | Needs clipping sessions to the window | **Chosen** |
| Count the whole day | Simpler | Usage before the window would consume the budget | No |
| One-shot boundary check + 2-minute fallback (chosen) | Blocks at the boundary without polling faster | OS scheduling can still delay background work | **Chosen** |
| 2-minute tick only | No new timing | Up to 2 minutes of use outside the window | No |

## Risks and limits

- A foreground service can arm a boundary delay, but the OS may delay it; the next 2-minute check is
  the fallback. The spec therefore asks for a boundary block and a bounded fallback, not an
  impossible real-time guarantee.
- Overnight and DST boundaries use the local wall clock (`ZoneId.systemDefault()`); the pure rules
  compute the window for the local day and its overnight tail.
- Equal start and end is invalid; the UI rejects it. No window is the way to allow all day.
- The window change depends on `app-limits` and `app-limit-alerts`; it cannot be verified or archived
  before both, and the schema lands at version 3.
- The block at 100 % and the outside-window block share the overlay but must show different messages;
  the block reason is part of the request to the accessibility service.
