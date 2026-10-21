# block-overlay-crash — tech brief

## Mechanism

The accessibility service used to build a `LifecycleOwner`/`SavedStateRegistryOwner` inline and
compose the block screen into it. Compose reads saved state as soon as it composes, so it called
`consumeRestoredStateForKey` on a registry that was never attached/restored and whose lifecycle was
still INITIALIZED: the process threw and died, and Android disabled the accessibility service.

Now `OverlayLifecycleOwner` owns the two registries and `prepare()` runs `performAttach()` +
`performRestore(null)` and moves the lifecycle to CREATED **before** any content is set;
`resume()` moves it to RESUMED once the view is attached. `BlockOverlayHost` sets both owners on a
`ComposeView`, adds it through `OverlayWindow` (the data-layer adapter that owns `WindowManager`),
and on failure destroys the owner instead of leaving it half-built.

Two more defects surfaced once the overlay ran on the device:

- **The host reused one owner.** `dismiss()` destroys the owner, so the second block tried
  `DESTROYED → RESUMED`, threw `IllegalStateException`, and killed the service: the first block could
  work and every later one failed. The host now creates a fresh owner inside `show()`.
- **The text was black on the dark surface.** `MaterialTheme` does not set `LocalContentColor`, so
  the `Text` fell back to black over a dark `background(surface)`; the block looked like a blank
  screen. The screen wraps its content in a `Surface`, which provides `onSurface`.

## Physical traceability

- `app/src/main/java/com/foco/app/UsageMonitorAccessibilityService.kt` — `showBlock()` calls `blockHost.show { … }`; `removeOverlay()` calls `blockHost.dismiss()`; failures are logged.
- `app/src/main/java/com/foco/app/ui/limits/BlockOverlayHost.kt` — one fresh owner and one `ComposeView` per block.
- `app/src/main/java/com/foco/app/ui/limits/OverlayLifecycleOwner.kt` — attach/restore/CREATED/RESUMED/DESTROYED.
- `app/src/main/java/com/foco/app/ui/limits/AppLimitBlockScreen.kt` — the read-only block screen inside a `Surface`.
- `app/src/main/java/com/foco/app/data/OverlayWindow.kt` — `TYPE_ACCESSIBILITY_OVERLAY` params, add/remove, and the rejection log.
- `app/src/androidTest/java/com/foco/app/ui/AppLimitBlockTest.kt` — pins the owner contract, including that a destroyed owner refuses RESUMED.

## Limits and failure modes

- If `WindowManager.addView` refuses the window, `show()` returns false, the owner is destroyed and
  the rejection is logged; the service stays alive and retries on the next evaluation.
- If `evaluate` throws, it is logged (`evaluate failed for <package>`) instead of killing the
  process.
- The overlay is not focusable (`FLAG_NOT_FOCUSABLE`): the foreground app keeps input focus while
  the block is shown; the countdown ends with `GLOBAL_ACTION_HOME`.
- Android keeps a crashed accessibility service disabled; after a crash the user re-enables it once.
- The instrumented suite cannot run on the connected Android 17 device
  (`NoSuchMethodException: InputManager.getInstance`); the on-device check was done with screenshots
  and `dumpsys window`.

## Touched APIs

- `AccessibilityService` / `TYPE_ACCESSIBILITY_OVERLAY` (unchanged permission model).
- `WindowManager.addView` / `removeView` (behind `data/OverlayWindow`).
- `SavedStateRegistryController.performAttach` / `performRestore(null)`.
- `LifecycleRegistry` CREATED → RESUMED → DESTROYED, one instance per block.
- `performGlobalAction(GLOBAL_ACTION_HOME)` (unchanged).

## Implicit decisions

- Keeping Compose for the block screen (option 1: fix the host, not the UI toolkit).
- The host lives in `ui/limits` and the window adapter in `data/`, so `LayerRulesTest` stays green.
- A new owner per block instead of resetting one owner: the lifecycle cannot leave DESTROYED.
- No new permission, dependency, storage or service.

## Cognitive sign-off

Signed by Nschkurko on 2026-10-20, verbatim answers:

1. The host reused one `LifecycleOwner`: `dismiss()` destroyed it, so the second block moved a
   DESTROYED owner to RESUMED, threw `IllegalStateException` and killed the service; the fix is a
   fresh owner inside `show()`.
2. `MaterialTheme` does not set `LocalContentColor`, so `Text` fell back to black over the dark
   `background(surface)` and the screen looked empty; the content now sits in a `Surface`, which
   provides `onSurface`.
3. If the `WindowManager` rejects the window, `show()` returns false, the owner is destroyed, the
   rejection is logged, and the service stays alive to retry on the next evaluation.
