# Walkthrough — startup-history-gate (tech brief)

## Mechanism

The startup consolidation no longer runs beside the home: it runs **before** it, behind the usage-access
gate, and the app shows a loading state until it ends.

**Where the run moved.** `MainActivity.onCreate` now only schedules the nightly job
(`MainActivity.kt:49`); the store's startup run lives in `App` (`App.kt:47-52`), which asks the same
process-wide `ConsolidationRunner` the worker uses, with `LocalDate.now()`.

**The three states.** `App` keeps `hasUsageAccess` (seeded by `UsageAccess.isGranted`, re-read on every
`ON_RESUME` through a lifecycle observer, `App.kt:54-63`) and `startupReady` (`:45`). It renders one of
three things (`:78-82`):

- no permission → `OnboardingScreen()`;
- permission but not ready → `StartupLoadingScreen()` (`:86-98`, spinner + the `loading` string);
- ready → `FocoNavHost()`.

**The gate.** `LaunchedEffect(hasUsageAccess)` (`:65-76`): without permission it sets `startupReady =
false` and returns immediately; with permission it sets `false`, runs `loadStartup()` on **IO**
(`withContext(Dispatchers.IO)`, `:72`) and sets `startupReady = true` in a `finally`.

**Granting from onboarding.** The `ON_RESUME` observer flips `hasUsageAccess` to `true`, which changes
the key of the `LaunchedEffect` and re-runs it — so the run starts right then and the home follows
without a process restart.

**Failure.** `ConsolidationRunner.run` returns `Done` or `Failed` instead of throwing for a store
failure, and the `finally` marks the startup ready regardless. The app becomes usable and the next
start runs the gate again; the pending days are still pending because the store only writes a day when
it finishes it.

## Physical traceability

- **What the gate waits on**: the existing `ConsolidationRunner` (`data/ConsolidationRunner.kt`), which
  is single-flight, so the app start and the nightly worker never run at once.
- **Where the data lands**: unchanged — `foco.db` through `SessionHistoryStore.consolidate`.
- **Where the loading state lives**: in memory, in `App`'s composition; nothing is persisted about it,
  so a process restart simply re-runs the gate.
- **No new store, schema, dependency or permission**: the nightly scheduler stays as it was.

## Limits and failure modes

- **The first open can wait**: up to six days of consolidation run before the home mounts, and there is
  no timeout — a slow reader means a longer spinner.
- **A failed run does not trap the app**: the loading state ends and the app mounts; the cost is that
  the week/detail may look incomplete until the next start (or the nightly job) consolidates the
  pending days.
- **The gate covers the startup run only**: a later nightly consolidation updates the store while the
  app is open; the home's ring follows `storedDaysFlow`, and a week read after that sees the new days.
- **Permission revoked while running**: the next `ON_RESUME` flips `hasUsageAccess` to `false`, the
  effect resets `startupReady` and the app returns to onboarding.
- **A cancelled composition** (process death, activity gone) cancels the effect; the `finally` runs, but
  the process is going away anyway, and the store's writes are per-day and idempotent.

## Touched APIs

- **Compose `LaunchedEffect` / `rememberUpdatedState` / `DisposableEffect`** (`App.kt:43-76`).
- **`LifecycleEventObserver` on `ON_RESUME`** (`:55-63`) — the permission re-check without a restart.
- **`Dispatchers.IO` + `withContext`** (`:72`) — the run off the main thread.
- **`CircularProgressIndicator` + the existing `loading` string** (`:86-98`).

## Implicit decisions

- **The gate lives in `App`, not in `MainActivity`**: it is a UI state (what to mount), and the runner is
  already a process singleton.
- **`startupLoad` is a seam** (`:40`), so the instrumented tests drive the loading and failure paths
  without consolidating real history.
- **The failure path never blocks**: `finally` marks ready even when the run failed; the contract says a
  failure costs a retry, not the app.
- **Nothing is persisted about the gate**: a restart re-runs it, which is what makes a failed run
  self-heal on the next start.

## Cognitive sign-off

Answer in your own words (the agent must not fill these):

1. The app starts with usage access granted and the consolidation still running. What does the user see,
   and which state change is what makes the home appear?
   > **Answer (Nschkurko, 2026-10-20):** El usuario ve la pantalla de carga: spinner centrado y el texto
   > de `loading` (`App.kt:86-98`). Arranca con `hasUsageAccess = true` y `startupReady = false`
   > (`App.kt:44-45`), así que el `when` cae en `StartupLoadingScreen` (`:80`); el `LaunchedEffect` corre
   > la consolidación en IO y, cuando termina, el `finally` pone `startupReady = true` (`:73-74`), que es
   > el cambio de estado que hace aparecer el inicio (`FocoNavHost`, `:81`). Hasta ese momento el home ni
   > se monta.
2. The user grants usage access from onboarding. Explain why the app does not need to be restarted, and
   name the code that notices the permission and re-runs the load.
   > **Answer (Nschkurko, 2026-10-20):** No hace falta reiniciar porque al volver de los Ajustes del
   > sistema se dispara `ON_RESUME` y el `LifecycleEventObserver` registrado en el `DisposableEffect`
   > vuelve a leer `UsageAccess.isGranted` y actualiza `hasUsageAccess` (`App.kt:55-62`). Como
   > `hasUsageAccess` es la clave del `LaunchedEffect(hasUsageAccess)` (`:65`), el cambio relanza el
   > efecto, que ahora sí ve el permiso y corre `loadStartup()` en IO (`:71-72`). El que se entera es el
   > observer de `ON_RESUME`; el que vuelve a lanzar la carga es el `LaunchedEffect` keyed por ese estado.
3. The startup run fails. Say what happens to the loading state and to the home, and why the days are
   retried on the next start instead of being lost.
   > **Answer (Nschkurko, 2026-10-20):** Si la corrida falla, el `ConsolidationRunner` devuelve `Failed`
   > en vez de tirar, y el `finally` igual pone `startupReady = true` (`App.kt:73-74`): el spinner termina
   > y el home se monta, quizá con la semana o el detalle incompletos hasta la próxima corrida. Los días
   > no se pierden porque el marcador se escribe último (`SessionHistoryStore.kt:66-68`); un día que falla
   > queda sin marcador, `consolidate` calcula pendientes justamente por ausencia de marcador (`:33-34`)
   > y, como el estado del gate no se persiste, el próximo arranque vuelve a correr la carga, recalcula
   > pendientes y reintenta esos días.
