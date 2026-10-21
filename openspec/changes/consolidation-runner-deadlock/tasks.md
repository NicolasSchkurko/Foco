# Tasks — consolidation-runner-deadlock

## Riskiest first vertical slice

- [ ] 1. `ConsolidationRunner`: modificar `gate` / `inFlight` para evitar reentrancy:
  - Usar `Mutex(locking = LockingMode.NON_REENTRANT)` explícitamente, o reestructurar para que `finally` no necesite bloquear (lo hace el `withLock` externo).
  - Reemplazar `gate.withLock { inFlight = null }` con `inFlight = null` (el `withLock` externo en `run()` lo protege).
  - Probar en JVM que `run()` retorna `ConsolidationOutcome.Failed` cuando `consolidate` lanza.
- [ ] 2. Agregar `consolidation-runner-deadlock` a `.openspec.yaml` (`x-kind: fix`, `x-targets` a `app/src/main/java/com/foco/app/data/ConsolidationRunner.kt`).
- [ ] 3. Enviar `gate-check --run-tests` con `consolidation-runner-deadlock` incluido, en `app/src/test/...` si se requiere una `[@test]` nueva.
- [ ] 4. Corregir `gitignore` si cambia la construcción; `gradlew.bat test`, `gradlew.bat ktlintCheck` → green.
- [ ] 5. [--pre-archive, archive, regenerate board, commit]

## Atajos

- La mejor solución es reestructurar `ConsolidationRunner` para que el bloqueo sea **externo** (el `run()` ya hace `gate.withLock`, solo `inFlight` necesita un campo.
- El `finally` se mueve fuera del `withLock` externo.

## Decisiones

- Usar `supervisorJob()`: La rama del worker y la rama de app-start comparten el **mismo** `ConsolidationRunner` (`shared`).
- Solo se necesita un `ReentrantMutex` si mantenemos `gate` para `inFlight`.
- `inFlight` debe estar siempre bajo `gate.withLock` (la única protección).

## Variantes

- Mantener `gate` pero hacerlo reentrant usando `withLock` mutex que permite reentrancy (Kotlin/Multihost): `ReentrantMutex()`.
- Mantener `gate` pero permitir que `finally` haga `withLock` si el código externaliza la protección de `inFlight` con un respaldo para la verificación de `await()`.

## Dependencias

- El cambio declara un cambio de prueba en `app/src/test/java/com/foco/app/data/ConsolidationRunnerTest.kt` (si existe) o agrega uno nuevo.
- `consolidation-runner-deadlock` tiene deps: [] (sin) porque ningún cambio activo depende de él todavía.
- El cambio depende de `usage-history-foundation` (`archived`), `nightly-consolidation` (draft), `schema-drift-guard` (draft) a través de `.openspec.yaml`.

## Tarea de tiempo

- Asegurar que no haya fugas de bloqueo: JMeter 100 sprints.
- Asegurar que un error en la consolidación limpia todos los nulls.
- Asegurar la cadencia del test runner.

## Acta de aceptación

- [ ] Aceptar `consolidation-runner-deadlock` (draft) con `tech_brief: true`.
- [ ] Aplicar `walkthrough.md` y comprender la aprobación humana antes de `--pre-archive`.
- [ ] Después de `verified`, registrar `ARCHIVE.md` -> `BOARD.md` actualizado.

## Edición del CHANGELOG

Agregado en `openspec/changes/consolidation-runner-deadlock/` bajo `DECISIONS.md`.

## Hoja de ruta

1. Preparar el fix del cambio (archivos listos).
2. Abrir PR para aprobación humana.
3. Después de la aprobación, aplicar a dev, CI, green.
4. Corrección del test runner.
5. Llevar a `--pre-archive` + `walkthrough.md` + aprobación humana.

## Lista de pendientes

- [ ] Modificar `ConsolidationRunner.kt` para evitar reentrancy del mutex.
- [ ] Agregar test de regresión para el caso de error con bloqueo.
- [ ] Ejecutar la suite completa de tests.
- [ ] Asegurar la seguridad de las pruebas estructuradas.
- [ ] Generar walkthrough.md con la explicación y rastreo.