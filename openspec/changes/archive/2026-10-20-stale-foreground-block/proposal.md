# Proposal — stale-foreground-block

## Why

Device evidence (Pixel 8 Pro, Android 17, 2026-10-20): with YouTube ReVanced at
"Block immediately (0 min)", opening YouTube shows the block overlay over YouTube (`Closing in 1`),
sends YouTube home, and the accessibility service stays bound. The overlay itself works and does
not need the "Display over other apps" permission.

The reported failure is the trigger: when a limit is crossed while an app is open, the 2-minute
monitor reads `ForegroundApp.currentPackage()` (`UsageMonitorService.kt:82`) and later calls
`requestBlock(packageName)` (`:86-98`). The accessibility host keeps the last non-Foco package when
Foco's own window comes to the front, because `onAccessibilityEvent` returns for
`packageName == this.packageName` without clearing the field
(`UsageMonitorAccessibilityService.kt:59-67`). Returning to Foco from recents or from the monitor
notification therefore lets the tick block the previous app **over Foco** and send Foco home.

The living spec is right: the monitor checks "the current foreground package"
(`openspec/specs/app-limits/spec.md:37-50`), the block names the limited app and sends it home
(`:76-108`), and alerts only fire while the limited app is in front (`:207-219`). The code is wrong.

## What Changes

- Extract the host's foreground state into a small pure `ForegroundAppState`: a window event for
  Foco's own package clears the tracked package; System UI and the input method leave it alone; a
  normal app window makes that app current.
- `UsageMonitorAccessibilityService` delegates `onAccessibilityEvent`, `currentPackage()` and the
  boundary check to that state.
- A block request from the 2-minute monitor is honored only when the requested package is still the
  tracked foreground. This closes the race between the monitor's read and Foco coming forward.
- No change to the overlay window, the countdown, the permissions or the onboarding.

## Expected outcome

With a limit crossed while an app is open, returning to Foco from recents or the monitor
notification never shows the block over Foco and never sends Foco home. The next time the limited
app comes to the front it is blocked immediately. Verified by the JVM regression test and a device
pass.

## State and persistence mechanism

No new persistence. The tracked package is process-local state in the accessibility service; limits
and windows stay in `limits.db` through `AppLimitsStore`. The state is `@Volatile` because the
2-minute monitor reads it from a background thread while accessibility events write it on the main
thread.

## Alternatives considered

| Option | Why yes | Why not | Recommendation |
| --- | --- | --- | --- |
| Clear the tracked package on Foco's own window event and guard monitor requests (chosen) | Smallest behavior change; keeps enforcement; makes the invariant JVM-testable | Needs a small state extraction | **Chosen** |
| Clear on every System UI and input-method window too | "No app in use" is literal | The shade or a keyboard would pause enforcement while the user is still in the app | No |
| Ask the platform for the current foreground app at each tick | No event gap | No public API without the usage-stats lag or window-content capability | No |
| Add `SYSTEM_ALERT_WINDOW` and draw a `TYPE_APPLICATION_OVERLAY` | The reporter's first hypothesis | The accessibility overlay already draws over YouTube; a new sensitive permission would not fix a stale trigger | No |

## Risks and limits

- The block can still be up to 2 minutes late while the app stays open: the spec's check cadence and
  out of scope here.
- The overlay stays a `TYPE_ACCESSIBILITY_OVERLAY`; if a future OS hides it over some app, that is a
  separate change.
- No protected zones (`protected: []`) are touched. Platform SDK plus cross-thread state means a
  tech brief.
