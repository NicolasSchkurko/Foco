# post-block-recheck-undelivered-resume — tech brief

## Mechanism

The hole this change closes: after Foco finished a block and sent the app home, the user reopened
the blocked app and no accessibility window event ever arrived, so neither the event path
(`UsageMonitorAccessibilityService.kt:63-68`) nor the 2-minute tick fired, and the app stayed open
with no card. The fix does not wait for an event: right after Foco's own block it asks the platform's
usage log whether the blocked app is in front again.

Step by step:

1. The 3-second limit screen ends and calls back into the service: `onFinished = { finishBlock() }`
   (`app/src/main/java/com/foco/app/UsageMonitorAccessibilityService.kt:150`).
2. `finishBlock` reads the package it just blocked (`:157`), tells the foreground tracker the block
   finished (`:158` → `ForegroundAppState.kt:21-25`), removes the overlay (`:159`), and sends HOME
   (`:160`).
3. Still in `finishBlock`, if it has a package, it cancels any previous re-check job and arms a new
   one (`:161-170`). One coroutine, two ticks:
   - `delay(RECHECK_FIRST_DELAY_MS)` = 2 000 ms (`:165`, `:189`) → first re-check (`:166`);
   - `delay(RECHECK_SECOND_DELAY_MS)` = 4 000 ms more (`:167`, `:190`) → second re-check (`:168`),
     i.e. ≈ +6 s after HOME.
4. Each tick runs `recheckForeground(packageName)` (`:174-177`). It first bails out if a limit screen
   is already showing (`:175`, `blockHost.isShowing`, `BlockOverlayHost.kt:21`). Then it asks
   `evaluator.isForegroundNow(packageName)` (`:176`).
5. `isForegroundNow` (`app/src/main/java/com/foco/app/data/UsageMonitorEvaluator.kt:34-37`) reads
   today's sessions through the existing repository and calls
   `AppLimitRules.isForegroundAt(sessions, packageName, now)`.
6. The predicate (`app/src/main/java/com/foco/app/domain/AppLimitRules.kt:51-55`) answers yes if some
   session of that package satisfies `start <= at && end >= at` — the app covers the instant.
7. If the answer is yes, the service calls `evaluate(packageName)` (`:176` → `:93-120`). `evaluate`
   re-reads the window and the limit and today's used seconds (`:100-103`), re-decides the reason
   (`:104-109`) and, if there is one, cancels the boundary job and shows the card
   (`:114-115` → `showBlock`, `:139-154`). If there is none, it arms the next window boundary instead
   (`:110-113`). The re-check never forces a reason; the rule is computed from fresh data.
8. If the answer is no, nothing happens: no card, no new job, no retry loop.

## Physical traceability

Nothing new is stored. The only "data" of this change is the platform's usage event log, read
through the adapter that already existed:

- `UsageMonitorEvaluator.isForegroundNow` (`UsageMonitorEvaluator.kt:34-37`) → `UsageRepository`
  (`UsageRepository.kt:23-27`) → `UsageStatsManager.queryEvents(lookbackStart, effectiveEnd)`
  (`UsageRepository.kt:74-76`), from `loadToday(now)` (`:55-59`). The class doc states the policy:
  read once per check, nothing cached or duplicated (`UsageMonitorEvaluator.kt:9-10`).
- The events are mapped to a flat list: `ACTIVITY_RESUMED` → resumed (`UsageRepository.kt:89-92`),
  `ACTIVITY_PAUSED` / `ACTIVITY_STOPPED` → stopped (`:94-99`) — the modern names for the
  MOVE_TO_FOREGROUND / MOVE_TO_BACKGROUND pair.
- Those flat events are folded into sessions (`SessionBuilder.kt:11-59`), clamped to the day
  (`:68-77`), filtered to launchable / non-excluded packages (`UsageRepository.kt:103-113`), and
  merged (`:115` → `SessionMerge.kt:12-30`).
- `recheckJob` is the only state the change adds: an in-memory field on the service
  (`UsageMonitorAccessibilityService.kt:40`). Written in `finishBlock` (`:162-169`), cancelled by the
  next block's `finishBlock` (`:162`) and by `onDestroy` (`:75`); it dies with the process. The
  package name it carries is `blockedPackage` (`:41`), captured as `finishedPackage` (`:157`).

Why the predicate is inclusive on both ends, and does not require `start >= block`:

- A reopen that happens in the same local minute as the pre-block session is **merged** into one
  session (`SessionMerge.kt:40-42` truncates both ends to the minute), so that merged session starts
  **before** the block and still ends after it. Requiring `start >= blockInstant` would miss exactly
  the reported case. The test pins it: a merged session covering both ticks is foreground
  (`AppLimitRulesTest.kt:79-86`).
- The app that is in the front **right now** is emitted as an open session ending at `now`
  (`SessionBuilder.kt:59`), and `isForegroundNow` passes that same `now` to both `loadToday` and the
  predicate (`UsageMonitorEvaluator.kt:37`). So the covering session ends **exactly** at the check
  instant: `end >= at` must be inclusive or the answer would always be no. Pinned by
  `AppLimitRulesTest.kt:113-118`.
- The other side is equally pinned: a session that ended at HOME does not cover the tick
  (`AppLimitRulesTest.kt:88-94`), another package's session does not make this one foreground
  (`:96-106`), and no sessions means nothing is foreground (`:108-110`).

## Limits and failure modes

- **Only two ticks.** The job checks at ≈ +2 s and ≈ +6 s and then ends
  (`UsageMonitorAccessibilityService.kt:161-170`). A resume that produces no event and happens later
  than ≈ +6 s is not covered; the fallback is the existing 2-minute tick
  (`UsageMonitorService.kt:60`, `:154`) or any later window event. In the observed failure the
  missing event was a dispatch race at the transition right after our own HOME; ordinary later
  reopens do emit events.
- **A late card.** If UsageStats has not yet recorded the user leaving, the predicate can say yes
  while the app is already gone, and the 3-second card appears over the launcher. Bounded: `evaluate`
  re-reads the real data, HOME was just sent by us, and the next window event re-syncs the tracked
  foreground. Also, because the re-check calls `evaluate` with no forced reason
  (`UsageMonitorAccessibilityService.kt:176`), an app that is no longer over its limit simply gets a
  new boundary job instead of a card (`:110-113`).
- **No loop without the user.** The tick is a pure read: if no session of that app covers the
  instant, `evaluate` is never called. Only a real reopen can produce a card, and each reopen is
  meant to be blocked again (the spec scenario, `specs/app-limits/spec.md:34-38`).
- **No duplicate from a normal event-driven reopen.** The re-check bails at `:175` if a card is
  already up, and `evaluate` repeats the guard at `:98`; so an event-driven block that already
  started a countdown wins and the tick is a no-op.
- **Forced close / process death.** The pending job is memory only; it is cancelled on `onDestroy`
  (`:75`) and the scope is cancelled (`:79`). Nothing to recover or migrate: after a service restart
  the 2-minute monitor (`UsageMonitorService.sync`, `UsageMonitorAccessibilityService.kt:60`) plus
  window events are the only triggers, exactly as before this change.
- **Usage access revoked.** `queryEvents` then yields nothing usable, sessions come back empty, the
  predicate answers no (`AppLimitRulesTest.kt:108-110`), and the service simply does nothing — no
  crash, no card. Degradation is silent, not wrong.
- **Concurrency.** Everything lives on the service's single `CoroutineScope` with
  `Dispatchers.Main.immediate` (`:37`): `recheckJob` is written and cancelled only from the main
  thread (`:162`, `:75`), and the two ticks are sequential inside one coroutine, so the second tick is
  always 4 s after the first even if the first took long.

## Touched APIs

- `UsageStatsManager.queryEvents` — already used by `UsageRepository.loadDay`
  (`UsageRepository.kt:74-76`); this change adds a second caller path (`isForegroundNow`) but no new
  API, no new query shape and no caching. It is the only source that knows about foreground state
  without being told by an accessibility event.
- `UsageEvents.Event.ACTIVITY_RESUMED` / `ACTIVITY_PAUSED` / `ACTIVITY_STOPPED` — unchanged mapping
  in `UsageRepository.kt:89-99`; the predicate just trusts the resulting sessions.
- `CoroutineScope` + `delay` + `Job.cancel` on the service's existing scope
  (`UsageMonitorAccessibilityService.kt:22-28`, `:37`) — the same primitives `armBoundary` already
  uses (`:123-137`); this change just adds a second `Job` field (`:40`).
- `BlockOverlayHost.isShowing` (`BlockOverlayHost.kt:21`) — read-only guard, already existed and is
  already read inside `evaluate` (`UsageMonitorAccessibilityService.kt:98`).
- No permission added: `PACKAGE_USAGE_STATS` is already declared (`AndroidManifest.xml:11`) and
  granted by hand by the user; `canRetrieveWindowContent` stays `false`
  (`res/xml/accessibility_service_config.xml:10`), and `performGlobalAction` / `GLOBAL_ACTION_HOME`
  (`UsageMonitorAccessibilityService.kt:160`) is unchanged.

## Implicit decisions

- **Two ticks instead of one.** The reported case is an immediate reopen, so a fast tick at +2 s
  covers it; a user who looks at the launcher for a couple of seconds is covered by the +6 s tick.
  Two ticks buy the second window for one extra UsageStats read, with no polling and no change to the
  periodic cadence.
- **One coroutine, two delays.** Both ticks share a job, so a single `cancel` (`:162`) disarms both;
  the second delay is measured from the first tick (`:167`), which is why the doc calls it ≈ +6 s.
- **The guard sits in two places.** `recheckForeground` checks `isShowing` before querying (`:175`)
  so a card already up costs no UsageStats read; `evaluate` keeps its own guard (`:98`) because it is
  also reached from events and the monitor.
- **The decision lives in `AppLimitRules`, the timing in the service.** `isForegroundAt`
  (`AppLimitRules.kt:51-55`) is pure and framework-free, next to `usedSeconds` (`:58-64`), so it is
  JVM-testable; the delays and the job stay untested glue, exactly like `armBoundary` (`:123-137`).
- **The service never touches usage data directly.** It asks `UsageMonitorEvaluator`
  (`UsageMonitorEvaluator.kt:34-37`); the repository stays behind one adapter, per the existing
  layering (proposal, "What changes").
- **The 2-minute cadence was not touched** (`UsageMonitorService.kt:154`). The new trigger is a
  one-shot that exists only after Foco's own block, so it adds a trigger rather than a poll; polling
  was rejected as a contract change.
- **The re-check does not force the block reason.** It calls `evaluate(packageName)` with no
  `forcedReason` (`:176`), so the card's reason is re-derived from the same rule the event path uses;
  the monitor, by contrast, passes `BlockReason.LIMIT` (`UsageMonitorService.kt:98`).
- **Failure is silent.** Any throw inside `evaluate` is logged and swallowed
  (`UsageMonitorAccessibilityService.kt:116-118`); the re-check has no error channel of its own, and a
  failed tick simply leaves the 2-minute monitor as the fallback.

## Cognitive sign-off

1. Después de que el bloqueo manda la app a HOME, ¿dónde vive la decisión de "hay que volver a
   chequear"? ¿Quién la crea, quién la cancela y por qué no queda nada guardado en disco?
2. Si reabrís la app a los 10 segundos pero el registro de uso todavía no anotó ningún evento de ese
   regreso, ¿qué ves en pantalla, y qué camino te vuelve a bloquear más tarde?
3. Para agregar un tercer re-chequeo a +15 s, ¿qué archivo y qué líneas tocás? Y si el predicado
   exigiera `start >= momentoDelBloqueo`, ¿qué caso real dejaría de detectarse y qué test lo
   demostraría?

### Answers

1. La decisión "hay que volver a chequear" vive en `recheckJob`, un `Job` de coroutine en el scope del
   servicio (`Dispatchers.Main.immediate`), creado en `finishBlock()` después de
   `performGlobalAction(HOME)`: lanza dos delays (2 s y 4 s) que llaman `recheckForeground`. Lo cancela
   `onDestroy()` (`recheckJob?.cancel()`) y también antes de re-armarlo (`recheckJob?.cancel()`). No
   queda nada en disco porque es un job transitorio en memoria: es un re-chequeo efímero posterior al
   HOME, no estado persistente; si el servicio muere, desaparece.

2. A los 10 s el `recheckJob` ya terminó (2 + 4 = 6 s). `recheckForeground` llamó a
   `isForegroundNow(packageName)`; si UsageStats todavía no registró el regreso, devuelve `false` y no
   hace nada → no hay overlay, la app abre normal. Más tarde me vuelve a bloquear el tick de 2
   minutos de `UsageMonitorService` (lee el uso acumulado y llama `requestBlock`) o un evento de
   ventana de la app que dispara `evaluate`.

3. Para agregar un tercer re-chequeo a +15 s: toco `UsageMonitorAccessibilityService.kt`, en el
   bloque `recheckJob` de `finishBlock()` (~líneas 161-170) agrego otro `delay(...)` +
   `recheckForeground(finishedPackage)`, y en el companion object (~líneas 183-186) una constante
   nueva (p. ej. `RECHECK_THIRD_DELAY_MS = 9_000L`, para llegar a +15 s desde los +6 s). Si el
   predicado exigiera `start >= momentoDelBloqueo`, dejaría de detectarse el caso real de una sesión
   que empezó antes del bloqueo y sigue vigente al re-chequeo (reapertura fusionada en la sesión
   pre-bloqueo). Lo demuestra el test `AppLimitRulesTest` "a reopen merged into the pre-block session
   still counts as foreground at the re-check" (líneas 79-86): `mergedReopen` tiene
   `start = blockFinished - 60_000` y `end = blockFinished + 30_000`, y hoy `isForegroundAt` devuelve
   `true`; con `start >= blockFinished` daría `false`.

Verificado por el agente contra el código del working tree: las tres respuestas son consistentes con
`walkthrough.md` y con el diff del cambio.
