# Proposal — duplicate-block-after-home

## Why

Device evidence (Pixel 8 Pro, Android 17, 2026-10-21): opening YouTube with an active limit or
window shows the block overlay over YouTube, counts down, sends YouTube home — and then shows the
same block **a second time over the launcher**, which sends home again. Two HOME intents fire
~3.5 s apart.

Logcat (`FocoBlock`) shows the sequence:

1. `TYPE_WINDOW_STATE_CHANGED` for YouTube → tracked → block.
2. The overlay's own `com.foco.app` `ComposeView` event → track cleared.
3. Countdown ends → overlay removed → HOME.
4. The launcher's event → launcher tracked, no block.
5. ~0.2–0.8 s later, a late YouTube `TYPE_WINDOW_STATE_CHANGED` (the app leaving the foreground)
   re-tracks YouTube → `evaluate` → second block over the launcher.

The late event is a consequence of the block itself. The app was just sent home; its transition
event must not count as a new foreground entry.

## What changes

- `ForegroundAppState` remembers the package a block just sent home (`blockFinished`) and ignores
  that package's late **view-group** window events (`android.view.ViewGroup`) for a **1-second
  settling window**. Any other window event - a real relaunch - is still evaluated, so an immediate
  reopen blocks again.
- The clock is injected (`now: () -> Long`), so the JVM tests control time.
- `UsageMonitorAccessibilityService` records the blocked package when it shows the block, classifies
  the event as a view-group transition, and calls `blockFinished` when the countdown ends, before
  HOME.
- JVM regression tests: the late view-group event after a block does not re-track the app; a real
  reopen **inside** the settling window does; a real reopen after it does; the existing
  Foco/System UI/IME rules still hold.

No persistence, permission, monitor-cadence or UI change.

## Revision after device evidence

The first version filtered events by activity class. The device disproved it: on a normal open,
YouTube's first window-state event arrived as `android.widget.FrameLayout` (a view, not an
activity), so the filter ignored it and **the overlay did not appear at all**. The class filter
was removed.

The second version ignored **every** event from the blocked package for one second. That fixed the
duplicate but also swallowed a real reopen: closing and immediately reopening the app left it
usable until the 2-minute monitor. The final rule keeps the settling window but scopes it to the
observed late transition class (`android.view.ViewGroup`); a real reopen reports its activity (or
`FrameLayout`) and is evaluated immediately.

## Alternatives considered

- **Activity-class filter**: tried and rejected by the device (above); it also would skip apps that
  report a view class for their main window.
- **Keep the overlay until the launcher settles**: adds a visible hold and relies on timing.
  Rejected.
- **Read the focused window**: requires `canRetrieveWindowContent=true`, expanding what the
  service can read. Rejected for privacy.

## Risks

- A genuine reopen reported as `android.view.ViewGroup` within the 1-second settling window would
  be ignored; the observed opens report `MainActivity` or `FrameLayout`, and the app blocks on the
  next event or the monitor tick.
- An app already open when the limit is crossed still waits for the monitor tick; this change does
  not alter that cadence.

## Acceptance

Accepted by Nschkurko on 2026-10-21; the settling-window revision keeps the same goal (one block
per app entry) after the device rejected the class filter.
