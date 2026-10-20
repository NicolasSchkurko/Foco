# Walkthrough — app-limits (tech brief)

> For the human: this brief explains the mechanism of the change. Answer the questions in
> `## Cognitive sign-off` yourself; the agent never fills them.

## Mechanism

A daily limit is one row per package in a dedicated Room database (`limits.db`, version 1): no row
means no limit, `0` blocks as soon as the app comes to the foreground, `N` allows N minutes today.
The value is chosen in Settings in 15-minute steps (`AppLimitRules`, pure), persisted through
`AppLimitsStore`, and read against the current local day only — at midnight the day's sessions are
empty and the budget starts again.

Enforcement has two halves. The **accessibility service** is the only component that can see which
app is in front and draw over it: on every `TYPE_WINDOW_STATE_CHANGED` it records the package,
reads its limit and, at or over the budget, adds a full-screen `TYPE_ACCESSIBILITY_OVERLAY` window
hosting the Compose `AppLimitBlockScreen`; the screen counts 3, 2, 1 on the frame clock, removes the
window and performs `GLOBAL_ACTION_HOME`. The **foreground service** (`UsageMonitorService`,
`specialUse` type on Android 14+) re-checks the tracked package every two minutes so an app that
crosses the limit while already open is still stopped; it follows the limits through
`UsageMonitorService.sync()` (called from the entry gate's `ON_RESUME`, from Settings after a pick,
from the boot receiver and from the accessibility service connecting) and stops with the last limit
or when accessibility goes away.

The data layer never imports the UI or the entry layer: the accessibility host registers itself in
the pure `ForegroundApp` port (`domain/`), and the monitor asks the port for the current package and
for a block. Used time is `AppLimitRules.usedSeconds` over the day's sessions from the existing
`UsageRepository.loadToday` — the same platform read as the dashboard, clamped to the local day,
never a second query.

The entry gate now has two steps: usage access first, then the accessibility service
(`AccessibilityAccess` checks the secure settings list). Both are re-read on every return to the
foreground, and the startup consolidation runs only after both are granted. The accessibility
onboarding explains why the service is needed and links to its Settings page.

## Physical traceability

- `domain/AppLimitRules.kt` — 0/15-min grid, at-or-over rule, per-package seconds.
- `domain/ForegroundApp.kt`, `domain/ForegroundAppTracker.kt` — the port between monitor and
  accessibility host.
- `data/db/AppLimit.kt`, `AppLimitDao.kt`, `AppLimitDatabase.kt` — `limits.db` version 1, exported
  schema at `app/schemas/com.foco.app.data.db.AppLimitDatabase/1.json`.
- `data/AppLimitsStore.kt` — singleton over the private `limits.db`; set/clear/flow.
- `data/UsageMonitorEvaluator.kt` — today's seconds for one package via `UsageRepository`.
- `data/UsageMonitorService.kt` — the two-minute foreground service, its notification and the
  start/stop `sync()`.
- `data/UsageMonitorBootReceiver.kt` — restarts the monitor after a reboot.
- `AccessibilityAccess.kt` and `UsageMonitorAccessibilityService.kt` — root entry-layer files: the
  secure-settings check and the accessibility host (foreground tracking, overlay, HOME).
- `ui/limits/AppLimitBlockScreen.kt` — the countdown overlay's content.
- `ui/settings/AppLimitsSection.kt`, `ui/settings/SettingsScreen.kt` — the picker per launchable app.
- `ui/App.kt`, `ui/onboarding/OnboardingScreen.kt`, `MainActivity.kt` — the two-step gate and the
  monitor sync on resume.
- `app/src/main/AndroidManifest.xml`, `res/xml/accessibility_service_config.xml`, both
  `strings.xml` — declarations, permissions, labels and copy.
- Tests: `AppLimitRulesTest`, `NoNetworkPermissionTest` (JVM); `AppLimitsStoreTest`,
  `AppLimitSchemaBaselineTest`, `AppLimitBlockTest`, `AppLimitsSettingsTest`,
  `UsageMonitorServiceTest`, `AppEntryGateTest`, `AppEntrySeamTest`, `OnboardingContrastTest`
  (instrumented).
- Device state touched by tests: the usage-access AppOp, the accessibility secure settings and the
  test database file; each class restores what it changes.

## Limits and failure modes

- **Two-minute granularity.** An app opened before its budget is spent is blocked immediately by an
  accessibility event; if it stays open and crosses the limit later, the worst case is the length of
  the tick. The tick only acts on the package the accessibility service saw last.
- **Windows that are not the app.** System UI and enabled input methods are ignored so the keyboard
  does not mask the app; a window that is neither an app nor one of those (some OEM dialogs) can
  still become the tracked package until the next event.
- **Unconditional accessibility gate.** Entry asks for the accessibility service even before the
  first limit exists: the first limit then works without another trip to Settings. The cost is a
  permission request earlier than strictly needed. These are the dependent drafts' problem too
  (`app-limit-alerts`, `app-limits-window`).
- **Notification permission.** On Android 13+ the service still runs if `POST_NOTIFICATIONS` is
  denied, but its ongoing notification is hidden; the first pick asks for the permission.
- **OEM process killing.** A force-stop or an OEM task killer ends both services. `BOOT_COMPLETED`
  and the gate's `ON_RESUME` re-sync them; while both are gone, enforcement is off.
- **Accessibility enabled but not yet bound.** The monitor starts from `sync()` and may have no
  foreground package for a moment; the next window event fills it.
- **Blocking cost.** Each tick calls `UsageRepository.loadToday`, which queries the day's events.
  That is the price of not duplicating the query; it is bounded by the two-minute tick.
- **Seconds are floored.** `usedSeconds` divides milliseconds by 1000, so the block can fire up to
  one second after the exact boundary.
- **The overlay is a service window.** It uses the service context and a manually attached
  `LifecycleOwner`; `rememberSaveable` is not available there, and the composition is disposed when
  the window is removed.
- **Stale rows.** A limit for an uninstalled app stays in `limits.db` and never fires; the list only
  shows currently launchable apps. No cleanup is done.
- **Schema.** `limits.db` is version 1 with an exported schema and no destructive fallback; any
  entity change must bump the version and pass `AppLimitSchemaBaselineTest`.
- **Device coverage.** All instrumented classes ran on the API 36 AVD (`medium_phone`); real-device
  OEM behaviours (autostart blocks, aggressive battery savers) remain unverified.

## Verification

- JVM suite (`gradlew.bat test`): green.
- `openspec validate app-limits --strict`: valid.
- Instrumented, API 36 AVD `medium_phone` (`connectedDebugAndroidTest`, one class per run):
  `AppLimitSchemaBaselineTest` (2), `AppLimitsStoreTest` (5), `AppLimitBlockTest` (1),
  `AppLimitsSettingsTest` (3), `UsageMonitorServiceTest` (4), `AppEntrySeamTest` (7),
  `OnboardingContrastTest` (4), `AppEntryGateTest` (6), `SettingsScreenTest` (6),
  `AppDetailNavigationTest` (5), `ExcludedAppsTest` (1), `UnlockCountTest` (1) — all green.
- `UnlockCountTest` needs a swipe keyguard on the device (`locksettings set-disabled false`); the
  lock screen had been disabled on the AVD, which made the generated unlocks invisible.

## Touched APIs

- `AccessibilityService` (`TYPE_WINDOW_STATE_CHANGED`, `TYPE_ACCESSIBILITY_OVERLAY`,
  `GLOBAL_ACTION_HOME`, `InputMethodManager.enabledInputMethodList`).
- `Settings.Secure.ACCESSIBILITY_ENABLED` / `ENABLED_ACCESSIBILITY_SERVICES`, `ComponentName`.
- `UsageStatsManager` through the existing `UsageRepository`; no new platform read.
- Foreground service: `ServiceCompat.startForeground` with `FOREGROUND_SERVICE_TYPE_SPECIAL_USE`
  (34+), `PROPERTY_SPECIAL_USE_FGS_SUBTYPE`, `POST_NOTIFICATIONS`, `RECEIVE_BOOT_COMPLETED`.
- Room `AppLimitDatabase` (KSP), `limits.db` in private storage.
- Compose `ComposeView` + `LifecycleRegistry` + `setViewTreeLifecycleOwner`, `withFrameMillis`.

## Implicit decisions

- **Separate database.** Limits live in their own `limits.db` instead of extending `foco.db`: they
  are configuration, not measured history, and the history schema stays untouched.
- **Zero is a value.** `0` means "block at open", not "no minutes of use". The absence of the row is
  the only "no limit"; the picker's first option clears the row.
- **Two layers of enforcement.** Accessibility events cover "open the app"; the foreground service
  covers "already open and crossing the limit". Either one alone leaves the main scenario open.
- **Port instead of an upward import.** The monitor cannot import the accessibility host (data must
  not depend on UI); the pure `ForegroundApp` port keeps the direction downward.
- **Accessibility host in the entry layer.** It is a second system entry point (like
  `MainActivity`), so it lives at the root and wires data + UI; the block screen itself stays in
  `ui/`.
- **Foco is not in the list.** Its own usage is not measured, so a limit on it would never fire.
- **No network.** The manifest declares no network permission (`NoNetworkPermissionTest`); all
  state is local.

## Cognitive sign-off

1. Why does enforcement need the accessibility service on top of the foreground service, and what
   scenario breaks if only one of them exists?
   > **Answer (Nschkurko, 2026-10-20):** Porque cubren dos momentos distintos. La accesibilidad ve qué
   > app está adelante, puede dibujar encima y mandar al home. El foreground service no ve ventanas ni
   > dibuja; cada 2 minutos lee el paquete actual del port y, si está en o sobre el límite, pide el
   > bloqueo. Si solo tuvieras el FGS, abrir una app que ya pasó el límite no se bloquea hasta el próximo
   > tick (hasta 2 minutos; con límite 0, «bloquear al abrir» directamente no funciona). Si solo tuvieras
   > accesibilidad, una app que ya está abierta y cruza el límite sin cambiar de ventana no se bloquea
   > nunca; el tick es el que atrapa ese cruce. Resumen: accesibilidad cubre «abrir la app», FGS cubre
   > «ya está abierta y se pasa».
2. You reboot the phone with a limit set: which paths bring the monitor back, and why does the entry
   gate also matter even though the monitor is a service?
   > **Answer (Nschkurko, 2026-10-20):** Con un límite configurado, después del reboot lo levantan
   > `UsageMonitorBootReceiver`, `UsageMonitorService.sync`, el sistema re-bindea el servicio de
   > accesibilidad, abrir Foco hace sync (`MainActivity.kt:68-73`) y si el sistema mata el FGS,
   > `START_STICKY` lo reintenta. `sync` solo arranca si hay algún límite y accesibilidad concedida. El
   > gate importa igual porque un servicio no se auto-habilita: el boot puede estar bloqueado por el OEM,
   > un force-stop lo mata, y si revocás accesibilidad el FGS no arranca. El gate es el único camino que
   > te muestra el onboarding de accesibilidad, rechequea los dos permisos en cada `ON_RESUME` y, al
   > volver de Ajustes, `MainActivity.onResume` vuelve a sincronizar; sin gate, el monitor podría quedar
   > apagado sin forma de reactivarlo desde la app.
3. The two-minute tick is late while the app is already open. Which path still blocks it, and why
   does the block screen live in the accessibility host instead of the data layer?
   > **Answer (Nschkurko, 2026-10-20):** Si la app limitada entra al frente, la bloquea igual el camino
   > de accesibilidad: el `TYPE_WINDOW_STATE_CHANGED` dispara `onAccessibilityEvent` y evalúa en el acto
   > (`UsageMonitorAccessibilityService.kt:57-65`), sin esperar los 2 minutos. El tick solo es necesario
   > para el otro caso, la app que ya está abierta y cruza el límite sin cambiar de ventana; ahí el FGS
   > llama `ForegroundApp.requestBlock` y el host ejecuta. La pantalla vive en el host de accesibilidad
   > porque es una ventana `TYPE_ACCESSIBILITY_OVERLAY` que solo un `AccessibilityService` puede agregar
   > con `WindowManager`, montando un `ComposeView` con su lifecycle, y porque el cierre real es
   > `GLOBAL_ACTION_HOME` (`:94-131`). La capa de datos no puede dibujar ni cerrar apps y no debe
   > depender de `ui/`; por eso solo evalúa y pide el bloqueo a través del port `ForegroundApp`
   > (`domain/ForegroundApp.kt`), mientras el contenido visual queda en
   > `ui/limits/AppLimitBlockScreen.kt`, montado por el host.
