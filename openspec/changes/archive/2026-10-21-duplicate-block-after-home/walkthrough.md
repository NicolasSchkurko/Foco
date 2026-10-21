# duplicate-block-after-home — tech brief

## Mechanism

The accessibility service is the only component that sees window changes, so it owns the answer to
"which app is in front". The duplicate came from the block itself: after the countdown sent YouTube
home, the launcher was tracked, and ~0.2–0.8 s later YouTube's transition emitted another
`TYPE_WINDOW_STATE_CHANGED` with class `android.view.ViewGroup`. The service treated it as a new
foreground entry, evaluated it, and showed a second block over the launcher.

The final rule combines the block moment with the event class:

- `showBlock` records the package being blocked (`blockedPackage`).
- `finishBlock` calls `ForegroundAppState.blockFinished(package)`, which stores the package and the
  time, and clears `current` if it still pointed at that package. Then it removes the overlay and
  sends HOME.
- On every window event the service classifies the class name: `isViewWindow` is true only for
  `android.view.ViewGroup` (the observed late transition class).
- `onWindowEvent` ignores a **view-group** event from the blocked package for a **1-second settling
  window** (`BLOCK_COOLDOWN_MS`). The launcher event still tracks the launcher; the late YouTube
  `ViewGroup` event does not replace it, so `evaluate` never runs a second time.
- A real reopen — `MainActivity` or `FrameLayout` in the device logs — is not a view-group event: it
  is tracked and evaluated immediately, **including inside the settling window**. The user gets a
  fresh block instead of an app left open until the 2-minute monitor.
- After the window expires, even a view-group event is tracked again; the latch only covers the
  narrow transition that produced the duplicate.

The clock is injected into `ForegroundAppState` (`now: () -> Long`) so the JVM tests control time;
the service uses the default `System::currentTimeMillis`.

## Revision after device evidence

The first version filtered events by activity class. On the device, YouTube's first window-state
event on a normal open arrived as `android.widget.FrameLayout` — a view, not an activity. The filter
ignored it, so the app was never tracked and **the overlay did not appear at all**. That version was
removed.

The second version ignored **every** event from the blocked package for one second. It fixed the
duplicate, but on the device a user who closed and immediately reopened the app inside that second
was ignored too: the app stayed usable until the 2-minute monitor. The final rule keeps the
settling window but scopes it to `android.view.ViewGroup`; the event class distinguishes the late
transition from a real launch, and the immediate reopen is evaluated.

## Physical traceability

- `app/src/main/java/com/foco/app/domain/ForegroundAppState.kt:17-18` — `blockedPackage` and
  `blockedAt`.
- `app/src/main/java/com/foco/app/domain/ForegroundAppState.kt:20-25` — `blockFinished`: package,
  time, clear `current`.
- `app/src/main/java/com/foco/app/domain/ForegroundAppState.kt:27-48` — `onWindowEvent` with
  `isViewWindow`; the class-scoped cooldown check is `:42`; the expiry is `:45`; `:11` is the
  injected clock; `:58` is the 1-second constant.
- `app/src/main/java/com/foco/app/UsageMonitorAccessibilityService.kt:62-66` —
  `onAccessibilityEvent` classifies `android.view.ViewGroup` (`:64`) and passes it to the state.
- `app/src/main/java/com/foco/app/UsageMonitorAccessibilityService.kt:137-152` — `showBlock` records
  the package; `:154-158` calls `blockFinished` before removing the overlay and sending HOME;
  `:167` holds the `VIEW_GROUP` constant.
- `app/src/main/java/com/foco/app/data/UsageMonitorService.kt:82-98` — unchanged: reads
  `currentPackage()` and calls `requestBlock` every 2 minutes; `accepts` still drops a stale
  request.
- `app/src/test/java/com/foco/app/domain/ForegroundAppStateTest.kt:62-108` — four regression tests:
  late view-group at +400 ms is ignored; a real reopen at +400 ms is tracked; a real reopen at
  +1.5 s is tracked; a view-group event at +1.5 s is tracked.

## Threads and lifecycle

`blockFinished` and `onWindowEvent` both run on the accessibility service's main thread;
`blockedPackage` and `blockedAt` are main-thread-only. `current` stays `@Volatile` because the
2-minute monitor reads it from `Dispatchers.Default`. The settling state is process-local: a
service restart loses it, which is correct (there is no block to settle). No persistence, no new
permission, no window-content retrieval.

## Limits and failure modes

- A genuine reopen whose first event is reported as `android.view.ViewGroup` inside the 1-second
  window would be ignored; the app would stay open until a later event or the monitor tick. The
  observed launches report `MainActivity` or `FrameLayout`, so this is the residual risk of the
  class-scoped rule.
- A late transition with a different class (never observed; all late events were `ViewGroup`) would
  still re-track and block. The window is sized with margin over the 0.2–0.8 s evidence.
- An app already open when the limit is crossed still waits for the monitor tick; the cadence is
  untouched.
- `finishBlock` records the block **before** HOME, so the cooldown starts at the moment the app is
  sent away, not when the next event arrives.

## Touched APIs

- `AccessibilityEvent` / `TYPE_WINDOW_STATE_CHANGED` (unchanged); `event.className` is read
  (available without window-content retrieval).
- `performGlobalAction(GLOBAL_ACTION_HOME)` (unchanged; one call per block).
- `ForegroundAppState.blockFinished` and the `isViewWindow` parameter (new); `accepts`, `clear`, the
  monitor and the overlay are unchanged.
- No `canRetrieveWindowContent`, no `SYSTEM_ALERT_WINDOW`, no new dependency.

## Implicit decisions

- A class-scoped settling window instead of a package-wide one: the device showed both the late
  transition class (`ViewGroup`) and the launch classes (`MainActivity`/`FrameLayout`); only the
  former needs suppressing.
- One second instead of a longer cooldown: the observed late events were 0.2–0.8 s; a longer window
  would delay a genuine `ViewGroup` reopen for no benefit.
- The Android class-name decision stays in the service; the domain receives a boolean, so the
  domain tests remain framework-free.
- The state is not persisted: a block cannot survive a service restart, so neither should its
  settling window.

## Evidence

- JVM, latest class-scoped revision: `gradlew.bat test ktlintCheck` green (exit 0); 175 tests,
  0 failures, 2 skipped.
- Device, Pixel 8 Pro / Android 17, 2026-10-21, YouTube with an active window outside its hours
  (the class filter was already gone; these runs used the package-wide settling window):
  - `01:25:53.122` event `MainActivity` → `evaluate`; usage 44 ms; overlay window added
    `53.194` (~72 ms after the event); content composed `53.233` (~111 ms). HOME `56.295`;
    launcher `56.449`; late YouTube `ViewGroup` `56.681` — no `evaluate` after it; one HOME, no
    second overlay.
  - `01:26:18.068` (reopen after the settling window) → usage 118 ms; overlay added `18.213`;
    content composed `18.256`; HOME `21.308`; launcher `21.464`; late `ViewGroup` `21.710` — no
    `evaluate`; one HOME, no second overlay.
  - `01:21:40.846` first event was `android.widget.FrameLayout` → usage 73 ms; overlay added
    `40.974`; content composed `41.216`. This is the path the activity filter used to skip.
- Clean APK (no diagnostic logs) `01:27:46`: one overlay, launcher after, one HOME.
- Final class-scoped build, device quick-reopen pass on Instagram (it is limited and was outside its
  10:00–21:30 window; YouTube was inside its 07:30–11:00 window and therefore allowed):
  - launch `1792585189658`; overlay shown `1792585190326` (count 1, Instagram focused).
  - block finished `1792585193417` (count 0, launcher `com.dino.simple` focused).
  - reopen `1792585193604` — **187 ms after the block finished**; app foreground `1792585193983`.
  - second overlay `1792585194245` — **828 ms after the block finished**, count 1, Instagram
    focused — then HOME at `1792585197352`; count 0 and no third overlay one second later.
  - Reopen after the settling window (`09:20:48`): overlay shown and HOME sent; count 0 after.
  - This is the behavior the package-wide cooldown lost: the genuine reopen inside the second is
    evaluated and blocked, while the late `ViewGroup` transition stays suppressed (JVM test).
- Before the fix: second overlay over the launcher 0.4–0.8 s after the first and two HOME intents.

## Cognitive sign-off

1. El primer evento de YouTube al abrir puede llegar como `android.widget.FrameLayout`, no como una
   activity. ¿Por qué el filtro por activity dejaba el overlay sin aparecer y por qué el cooldown por
   `android.view.ViewGroup` no tiene ese problema?
2. ¿Cómo distingue el servicio el evento tardío que causaba el segundo bloqueo de una reapertura
   real? ¿Qué pasa si reabrís dentro de ese segundo?
3. ¿Qué cambió en la cadencia de 2 minutos y en los permisos? ¿Por qué el cooldown no necesita
   `canRetrieveWindowContent`?

### Answers

1. El filtro por activity dejaba el overlay sin aparecer porque el primer evento de YouTube al abrir
   puede ser un FrameLayout (no una activity declarada), así que se descartaba como “no es app
   window” y no se evaluaba hasta que llegara un evento de activity (o el tick de 2 min). El cooldown
   limitado a android.view.ViewGroup no tiene ese problema porque no descarta todos los eventos
   no-activity: solo ignora las ventanas de vista del paquete bloqueado durante 1 s. Las aperturas
   normales (FrameLayout, etc.) siguen trackeándose y evaluándose al instante.
2. El servicio distingue el evento tardío del segundo bloqueo porque es un evento de vista
   (android.view.ViewGroup) del mismo paquete bloqueado y llega dentro del cooldown de 1 s tras
   blockFinished (ForegroundAppState.kt:42). Una reapertura real llega como activity (no ViewGroup)
   o fuera del segundo, así que se trackea y evalúa. Si reabrís dentro de ese segundo, el evento
   ViewGroup se ignora (no se vuelve a bloquear) y la app queda sin track hasta que expire el
   cooldown o llegue un evento de activity; si la reapertura viene como activity, no se demora.
3. La cadencia de 2 minutos no cambió (el monitor sigue leyendo currentPackage() y llamando
   requestBlock cada 2 min) y no se agregó ningún permiso. El cooldown no necesita
   canRetrieveWindowContent porque solo mira event.className del AccessibilityEvent (si es
   "android.view.ViewGroup"); no lee el árbol de ventanas ni requiere acceso al contenido.

Signed by: Nschkurko — 2026-10-21
