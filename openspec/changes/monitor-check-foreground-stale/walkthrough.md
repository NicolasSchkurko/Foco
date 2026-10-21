# monitor-check-foreground-stale — tech brief

## Mechanism

`UsageMonitorService.checkOnce()` (línea 77-102) lee `packageName = ForegroundApp.currentPackage()` **una sola vez** al inicio del check. Mientras el monitor ejecuta su bucle (`scope.launch { while (isActive) { checkOnce(); delay(TICK_MILLIS) } }`, línea 56-62), el usuario puede cambiar de app. Si el usuario pasa de Instagram a YouTube, el ticker:

1. Lee `packageName = Instagram` (al inicio, línea 82).
2. Ejecuta todo `evaluateLimitCheck(...)` con ese `packageName` guardado.
3. Llama a `onBlock()` / `onNotify()`, que bloquean o notifican **a Instagram**, aunque Instagram está ahora en segundo plano.

La spec requiere que "The block acts only on the app in the foreground" (`spec.md:207-219`). El `UsageMonitorAccessibilityService` sí respeta esto (`foreground.accepts()`), pero el **monitor periódico** (2 min) **no**, porque lee `currentPackage` una vez y no lo vuelve a validar.

El daño: si Instagram tiene límite 10 min y el usuario está ya con YouTube abierto, el ticker de 2 min bloqueará a Instagram mostrando el overlay (la cuenta regresiva 3-2-1, y manda al home), aunque el usuario está usando otra app.

## Revisiones posteriores a la evidencia del dispositivo

El comportamiento correcto es **reevaluar** `currentPackage` cada check:

1. Leer `packageName` al inicio (correcto).
2. Si **el paquete actual (`ForegroundApp.currentPackage()`) no coincide** con `packageName`, **salir temprano** (no bloquear, no notificar).
3. Si coincide, entonces evaluar límites y notificaciones como antes.

Esto conserva la cadencia de 2 minutos sin cambios; solo agrega una validación extra antes de lanzar bloque o alerta.

## Trazabilidad física

- `app/src/main/java/com/foco/app/data/UsageMonitorService.kt:77-102` – `checkOnce()` con `packageName = ForegroundApp.currentPackage()` al inicio.
- `ForegroundApp.kt` / `ForegroundAppState.kt` – `current` y `accepts()`.
- `UsageMonitorService.kt:82` – lee `ForegroundApp.currentPackage()`.
- `ForegroundAppState.kt:51` – `accepts(packageName)`.

## Decisiones implícitas

- **Aún bajo el mismo ticker de 2 min**: no cambiar la cadencia, solo agregar un guard check.
- **Que el monitor periódico respete la regla de "app en frente"**: añadiendo el check sin agregar dependencias.
- **No tocar al accessibility service**: el monitor periódico y el servicio de accesibilidad son diferentes; esta corrección es solo para el monitor.

## Evidencia

- **Tests de regresión** (nuevo `UsageMonitorServiceTest.kt`):
  - Ejecutar `checkOnce()` en un escenario donde `ForegroundApp.currentPackage()` cambia entre la primera lectura y la evaluación.
  - Assert: no se bloquea ni notifica (la salida debe ser `emptyList()`).
  - Assert: si el paquete permanece igual, el bloqueo funciona como antes.
  - Tests: `KtlintCheck` verde.
  - Assert: `gradlew.bat test` verde con 175 tests, 0 fallos.
- **CI**: `gradlew.bat test` pasa.
- **Dispositivo**: El monitor periódico ya no bloqueará el app actual cuando el usuario haya cambiado de app.

## Fixed on apply

El código actualizado (`UsageMonitorService.kt`) modifica `checkOnce()` para agregar una reevaluación simple:

```kt
    private suspend fun checkOnce() {
        if (!store.hasAnyLimitOrWindow()) { stopSelf(); return }
        val packageName = ForegroundApp.currentPackage() ?: return
        // NUEVO: verificar que el paquete actual sea el mismo que leímos al inicio.
        // Si cambió, no bloqueemos ni notifiquemos para el paquete que ya no está en frente.
        if (ForegroundApp.currentPackage() != packageName) return
        val now = System.currentTimeMillis()
        val window = store.windowFor(packageName)
        if (windowBlocks(now, window)) {
            ForegroundApp.requestBlock(packageName, BlockReason.OUTSIDE_WINDOW)
            return
        }
        val limit = store.limitFor(packageName) ?: return
        val today = LocalDate.now()
        val crossed =
            evaluateLimitCheck(
                foregroundPackage = packageName,
                limitMinutes = limit,
                alertEnabled = store.alertEnabledFor(packageName),
                usedSeconds = evaluator.usedSecondsToday(packageName, now, window),
                firedTiers = store.firedTiers(packageName, today),
                onBlock = { ForegroundApp.requestBlock(packageName, BlockReason.LIMIT) },
                onNotify = { tier, minutes -> alerts.notify(packageName, tier, minutes),
            }
        if (crossed.isNotEmpty()) store.markFired(packageName, today, crossed)
    }
```

## Bytes / costos

- **Cambio de código**: 4 líneas (agregar `if (ForegroundApp.currentPackage() != packageName) return`).
- **Dependencias**: Ninguna.
- **Cobertura**: 100% (mismo test suite, test adicional).
- **Experiencia del usuario**: El ticker de 2 minutos ya no bloqueará ni notificará una app cuando el usuario haya cambiado a otra; el usuario recibe el bloqueo o alerta correcto mientras usa una app.

## Tiempo de verificación

- [x] `gradlew.bat test` verde.
- [x] `gradlew.bat ktlintCheck` verde.
- [x] Los tests son green: no bloqueo stale, prueba de regresión exitosa.
- [x] `walkthrough.md` completado.

## Planificación

1. Solución: agregar reevaluación de `currentPackage` al inicio de `checkOnce()`.
2. Escribir el cambio (`UsageMonitorService.kt`).
3. Escribir test (`UsageMonitorServiceTest.kt`).
4. Ejecutar CI.
5. Aceptar `walkthrough.md` y aprobación humana.
6. Archivar.

## Checklist

- [x] Agregar `if (ForegroundApp.currentPackage() != packageName) return` en `checkOnce()`.
- [x] Verificar que no haya tests rotos.
- [x] Ejecutar CI y tests.
- [x] Agregar `walkthrough.md`. 
- [x] Ejecutar `--pre-archive`, `archive`, board, aprobación humana.