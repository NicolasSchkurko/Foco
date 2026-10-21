# stale-foreground-block — tech brief

## Mechanism

The accessibility service is the only component that sees window changes, so it owns the answer to
"which app is in front". `ForegroundAppState` holds that answer in a `@Volatile` `current`:
`onWindowEvent` returns the app to evaluate for a normal package, clears `current` when Foco's own
package appears, and returns null for System UI and the enabled input methods (the app underneath
them is still the app in use).

The 2-minute `UsageMonitorService` reads `ForegroundApp.currentPackage()` from its background
thread and, when a limit or window is crossed, calls `ForegroundApp.requestBlock(package, reason)`.
`requestBlock` now asks `ForegroundAppState.accepts(package)` before evaluating: a request is
honored only while that package is still `current`. The block screen, the overlay window and the
countdown are untouched.

Both defenses are needed. Clearing on Foco removes the stale value in the normal flow (recents or
the monitor notification to Foco). `accepts` closes the race where the monitor read the package
just before Foco came forward and would otherwise still start the block. Without either, the block
ran over Foco and `GLOBAL_ACTION_HOME` sent Foco home.

## Physical traceability

- `app/src/main/java/com/foco/app/domain/ForegroundAppState.kt` — the tracked package, the window
  event rule, `accepts`, `clear`.
- `app/src/main/java/com/foco/app/UsageMonitorAccessibilityService.kt` — delegates
  `onAccessibilityEvent`, `currentPackage`, `requestBlock` and the boundary check to the state.
- `app/src/main/java/com/foco/app/data/UsageMonitorService.kt` — unchanged: reads
  `ForegroundApp.currentPackage()` and calls `requestBlock` every 2 minutes.
- `app/src/main/java/com/foco/app/domain/ForegroundApp.kt` — unchanged bridge between the monitor
  and the accessibility host.
- `app/src/test/java/com/foco/app/domain/ForegroundAppStateTest.kt` — pins the clear-on-Foco rule,
  the stale-request rejection, and the System UI/input-method exception.

## Threads and lifecycle

`onAccessibilityEvent` writes `current` on the main thread. The monitor reads it from
`Dispatchers.Default`, so the field is `@Volatile` (the same visibility the old service field had).
`requestBlock` runs on the monitor thread for the check and then launches the evaluation on the
service's main scope, as before. `onDestroy` clears the state before unregistering the tracker.

## Limits and failure modes

- The block can still be up to 2 minutes late while the app stays open: that is the spec's check
  cadence, not this change.
- System UI and input methods do not replace the tracked app, so opening the shade or a keyboard
  does not pause enforcement; a stale package is never introduced by them.
- A null event package changes nothing; only an explicit app window or Foco clears or replaces the
  track.
- The overlay remains a `TYPE_ACCESSIBILITY_OVERLAY` from the enabled service; no
  `SYSTEM_ALERT_WINDOW` and no onboarding change.

## Touched APIs

- `AccessibilityService.onAccessibilityEvent` / `TYPE_WINDOW_STATE_CHANGED` (same event type).
- `InputMethodManager.enabledInputMethodList` (read once, via the lazy set).
- `performGlobalAction(GLOBAL_ACTION_HOME)` (unchanged, now never called for Foco).

## Implicit decisions

- A small pure state class instead of fixing the field in place, because the regression must run on
  the JVM where the monitor and the service cannot be instantiated.
- Clear on Foco's own package, not on every System UI or IME window: those are overlays over the
  app in use, and clearing them would pause enforcement while the user is still in the app.
- The monitor request is rejected instead of being deferred; the next app foreground event re-reads
  the limit and blocks immediately if it is still over.

## Cognitive sign-off

Signed by Nschkurko on 2026-10-20, verbatim answers:

1. Porque ignorarlo dejaría el paquete stale en current: el próximo tick del monitor seguiría
   creyendo que esa app está adelante y la bloquearía (y GLOBAL_ACTION_HOME mandaría a Foco al
   home). Limpiar al ver el propio paquete de Foco elimina el valor stale en el flujo normal
   (recientes o la notificación del monitor hacia Foco).
2. Por la carrera: el monitor puede haber leído el paquete justo antes de que Foco pasara al
   frente y llamar requestBlock después. El clear ocurre en el evento de accesibilidad, que puede
   llegar después de esa lectura. accepts(package) rechaza la petición a menos que ese paquete siga
   siendo current, cerrando la carrera.
3. Un evento de la cortina de notificaciones o del teclado no reemplaza la app trackeada: System UI
   y los métodos de entrada habilitados se ignoran, porque son overlays sobre la app en uso; la app
   de abajo sigue siendo la que se usa, así que el enforcement sigue. Limpiar pausaría el
   enforcement mientras el usuario sigue en la app.
4. No toca la cadencia (tick de 2 minutos, UsageMonitorService queda sin cambios), la ventana del
   overlay (TYPE_ACCESSIBILITY_OVERLAY, BlockOverlayHost/OverlayWindow sin cambios) ni los permisos
   (ninguno nuevo, sin onboarding). Se ve en la trazabilidad física (UsageMonitorService.kt
   unchanged; ForegroundApp.kt unchanged) y en límites/failure modes (el overlay sigue siendo
   TYPE_ACCESSIBILITY_OVERLAY, sin SYSTEM_ALERT_WINDOW).
