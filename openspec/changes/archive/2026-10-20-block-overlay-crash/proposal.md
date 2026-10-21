# Proposal — block-overlay-crash

## Why

The block overlay never appears and the blocked app is never sent home. Evidence from the connected
Pixel (`adb logcat -b crash`, 2026-10-20):

```
IllegalStateException: Composed into the View which doesn't propagate ViewTreeSavedStateRegistryOwner!
  at AbstractComposeView.resolveComposeViewContext → attachedToWindow
IllegalStateException: You can 'consumeRestoredStateForKey' only after the corresponding
  component has moved to the 'CREATED' state
```

`UsageMonitorAccessibilityService.showBlock()` composes the block screen into a `ComposeView` whose
`LifecycleOwner`/`SavedStateRegistryOwner` is built by hand but never attached/restored nor moved to
`CREATED` before Compose consumes saved state. Adding the view throws, the app process dies, and
Android marks the accessibility service as crashed and stops binding it
(`dumpsys accessibility` → `Crashed services: com.foco.app/…UsageMonitorAccessibilityService`).
With the service down, neither the overlay nor `GLOBAL_ACTION_HOME` can run. The human confirms
this never worked: it is not a regression from a later change but a defect in the original overlay
host, which matches the crashes already logged before today's builds.

## What Changes

- Extract the overlay host from the service into a testable `BlockOverlayHost` (ComposeView +
  lifecycle/saved-state owners) that is attached and restored before composition.
- `UsageMonitorAccessibilityService.showBlock()` uses that host; a failed add is cleaned up instead
  of crashing the process.
- No behavior change in the block itself: 3-second countdown, then home.

## Expected outcome

On the Pixel, opening an app at/over its limit or outside its window shows the block overlay, counts
3, 2, 1, returns to home, and the accessibility service stays bound (next app is still evaluated).
Verified by the instrumented overlay test plus a manual run on the connected device.

## State and persistence mechanism

No new persistence: limits/windows stay in `limits.db` through `AppLimitsStore`. The overlay is a
transient `TYPE_ACCESSIBILITY_OVERLAY` window owned by the service; nothing is stored.

## Alternatives considered

| Option | Why yes | Why not | Recommendation |
| --- | --- | --- | --- |
| Fix the ComposeView host (attach + restore, CREATED before content) | Smallest diff; keeps `AppLimitBlockScreen` and its tests; no new dependency | Lifecycle plumbing must be exactly right | **Recommended** |
| Build the overlay with plain Android Views (TextView/LinearLayout + Choreographer) | No lifecycle/saved-state risk; very robust for an overlay | Duplicates the block UI and its strings; loses Compose theming | No |
| Transparent `Activity` shown over the app | Familiar lifecycle | Needs `SYSTEM_ALERT_WINDOW` or a full-screen intent; heavier and permission-fragile | No |

## Risks and limits

- The service currently crashes and is marked crashed by the system; after the fix the user may need
  to toggle the accessibility service once (Android keeps a crashing service disabled).
- No protected zones (`protected: []`) are touched.
- The fix touches platform SDKs (accessibility overlay, WindowManager, lifecycle) → tech brief.
