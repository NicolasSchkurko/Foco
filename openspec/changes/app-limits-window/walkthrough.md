# Walkthrough — app-limits-window

## What the change does

Each package can now have a recurring daily window (minutes from local midnight, end exclusive)
stored beside its minute limit. Outside the window the accessibility host blocks the app with the
existing three-second overlay and a different message; inside it the minute limit and the 50/75/90
alerts work as before, but only in-window time is added to the budget.

## Physical traceability

- Table `app_limit_window` in `data/db/AppLimitWindow.kt` and its DAO in `data/db/AppLimitWindowDao.kt`; stored in the private `limits.db` (Room version 3, `MIGRATION_2_3`) and exported as `app/schemas/com.foco.app.data.db.AppLimitDatabase/3.json`.
- Rules in `domain/AppLimitWindowRules.kt`; the value in `domain/AppWindow.kt`; the reason in `domain/BlockReason.kt`.
- Monitor in `data/UsageMonitorService.kt`; budget clipping in `data/UsageMonitorEvaluator.kt`; store in `data/AppLimitsStore.kt`.
- Host and overlay in `UsageMonitorAccessibilityService.kt` and `ui/limits/AppLimitBlockScreen.kt`; picker in `ui/settings/AppLimitsSection.kt`; strings in `res/values/strings.xml` and `res/values-es/strings.xml`.

## Mechanism

- **Storage**: a new `app_limit_window` table in `limits.db`, one row per package, independent of
  `app_limit` (`AppLimitWindow.kt`, `AppLimitWindowDao.kt`). `AppLimitDatabase` moves to version 3
  with `MIGRATION_2_3`, which only creates the table; the existing rows are untouched.
- **Reading**: `AppLimitsStore.windowFor(package)` maps the row to `AppWindow`; `windowsFlow`
  exposes every window to Settings.
- **Rules**: `AppLimitWindowRules` is pure. `isInside(now, window, zone)` handles a start after the
  end as an overnight window; `inWindowMillis(session, window, zone)` clips a session to the
  window's intervals on its local day; `nextBoundaryMillis(now, window, zone)` finds the next open
  or close.
- **Budget**: `UsageMonitorEvaluator.usedSecondsToday(package, now, window)` sums only the clipped
  portions when a window exists. No window keeps the old whole-session sum.
- **Monitor**: `UsageMonitorService.checkOnce()` reads the window first. Outside it requests a block
  with `BlockReason.OUTSIDE_WINDOW` and returns; inside it evaluates the limit and alerts with the
  in-window budget. `sync()` starts the service when a limit **or** a window exists.
- **Host**: `UsageMonitorAccessibilityService.evaluate()` recomputes the reason and, when the app is
  allowed, arms a one-shot `delay` to the next boundary (`armBoundary`). The 2-minute tick remains
  the fallback. `AppLimitBlockScreen` picks its title and message from the reason.
- **Settings**: `AppLimitsSection` adds a `WindowControls` row: the current range (or `No window`),
  two 15-minute dropdowns and a clear action. An equal pair is ignored so the previous window stays.

## Limits and failure modes

- The one-shot boundary uses coroutine delay; if the process is delayed or killed, the next
  2-minute tick blocks. The spec asks for prompt, not impossible real-time, enforcement.
- Wall-clock cuts use `ZoneId.systemDefault()`; a DST jump can move a boundary, but the rule reads
  the local clock each time, so the next check still agrees with the user's clock.
- A window without a limit blocks outside and is unrestricted inside; a limit without a window is
  unchanged. Equal start and end is rejected, so "all day" is expressed by clearing the window.
- No new service or permission; the same accessibility binding and overlay are reused.

## Touched APIs

Room (`@Entity`, `@Dao`, `Migration`), `UsageStatsManager` (indirectly through `UsageRepository`),
`AccessibilityService` (`TYPE_WINDOW_STATE_CHANGED`, overlay window), Compose (`DropdownMenu`,
`TextButton`), `kotlinx.coroutines.delay`, `java.time` (`Instant`, `ZoneId`).

## Verification

- `gradlew.bat test` green, including `AppLimitWindowRulesTest`.
- `gradlew.bat ktlintCheck` green (no baseline).
- `compileDebugUnitTestKotlin` and `compileDebugAndroidTestKotlin` green.
- `AppLimitWindowStoreTest`, `AppLimitWindowMigrationTest`, `UsageMonitorServiceTest`,
  `AppLimitsSettingsTest` and `AppLimitBlockTest` compile; the class-level runs need the API 36 AVD.
- `openspec validate app-limits-window --strict` and the static gate are green.

## Implicit decisions

- Keep the window in `limits.db` as its own table instead of columns on `app_limit`, so a window can
  exist without a limit.
- Recompute the reason in the host instead of trusting the monitor's request, so a stale tick cannot
  show the wrong message.
- Put the budget clipping in the pure rules; the evaluator only filters by package.

## Cognitive sign-off

Answer these in your own words; the sign-off is recorded only after your explicit confirmation.

1. Why is the window a separate table instead of two columns on `app_limit`, and what breaks if it
   were on the limit row?
   > Porque una ventana puede existir sin límite (bloquea fuera y deja pasar dentro) y un límite puede existir sin ventana. Si fueran dos columnas en app_limit, la ventana quedaría atada a la existencia de una fila de límite: no podrías tener “solo ventana” ni limpiar una sin tocar la otra, y clearLimit borraría la ventana de regalo. Tabla aparte (app_limit_window, un row por paquete) mantiene los dos conceptos independientes y con CRUD propio (AppLimitsStore.windowFor, windowsFlow).
   >
   > — the human, 2026-10-20

2. How does the budget avoid counting time outside the window, and what happens to a session that
   straddles the window's start?
   > El presupuesto suma solo las porciones dentro de la ventana: UsageMonitorEvaluator.usedSecondsToday(package, now, window) usa AppLimitWindowRules.inWindowMillis(session, window, zone), que recorta cada sesión a los intervalos de la ventana en su día local. Una sesión que cruza el inicio de la ventana cuenta solo desde ese inicio (lo anterior se descarta); si la ventana es nocturna (start > end), se manejan los dos intervalos del día. Sin ventana, se mantiene la suma completa de antes.
   >
   > — the human, 2026-10-20

3. The boundary check is a one-shot delay, not a faster poll: why, and what covers it if the delay
   does not run on time?
   > Es un delay one-shot a la próxima frontera (abrir/cerrar) porque el monitor ya tiene un tick de 2 minutos; sondear más rápido solo gastaría batería para un caso que se puede calcular exacto. Si el delay no corre a tiempo (proceso demorado o matado), el tick de 2 minutos es el fallback y bloquea igual. El spec pide “prompt, no real-time imposible”.
   >
   > — the human, 2026-10-20
