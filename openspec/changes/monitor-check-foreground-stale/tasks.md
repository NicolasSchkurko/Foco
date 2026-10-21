# Tasks — monitor-check-foreground-stale

## Prueba más arriesgada:

- [ ] 1. `UsageMonitorService.checkOnce()`: reevaluar `currentPackage` en el ticker antes de `evaluateLimitCheck`:
  - Guardar `packageName = ForegroundApp.currentPackage()` (ya es correcto).
  - Si `packageName != current` (el `foreground.current` grabado al inicio del check) salir temprano.
  - Corregir: mover la lectura de `packageName` hacia el principio, cerca del `if (!store.hasAnyLimitOrWindow())`.
  - Agregar un test en `app/src/test/java/com/foco/app/data/UsageMonitorServiceTest.kt` que verifica el caso donde el usuario cambia de app entre la primera lectura y la evaluación.
  - El test deberá capturar los eventos apropiados (`UsageStats`, `ForegroundApp`, etc.) para simular el cambio de app.
- [ ] 2. Agregar `monitor-check-foreground-stale` a `.openspec.yaml`:
  - `x-kind: fix`
  - `x-targets` a `app/src/main/java/com/foco/app/data/UsageMonitorService.kt`
  - `x-tests` a `app/src/test/java/com/foco/app/data/UsageMonitorServiceTest.kt`
  - `x-tech_brief: true` (si se necesita `walkthrough.md`)
- [ ] 3. Unificar `walkthrough.md` si es necesario (el cambio es muy pequeño).
- [ ] 4. Ejecutar `gradlew.bat test` y `gradlew.bat ktlintCheck`; todo verde.
- [ ] 5. `--pre-archive`, `archive`, registrar el board, la aprobación humana para `walkthrough.md`.

## Ideas

- El código que reevalúa `currentPackage` antes de bloquear es mínimo, pero importante.
- Reevaluar `usedSeconds` solo cuando el paquete coincide.
- Plan de validación: CI en cada cambio.

## Decisión

- Agregar `x-deps: []` (no es necesario).
- Si `tech_brief` se activa, agregar `walkthrough.md`.
- Asegurar que el `gate-check --run-tests` agregue la prueba como `[@test]`.

## Dependencias

- Ninguna (informe independiente).

## Pasos

1. Modificar `UsageMonitorService.kt` para reevaluar el paquete actual.
2. Agregar test o probar cambios.
3. Ejecutar CI.
4. Completar `walkthrough.md` si es necesario.
5. Procesar `/spec-triage` y `--pre-archive`.

## Lista de pendientes

- [ ] Modificar `UsageMonitorService.kt`.
- [ ] Agregar test unitario.
- [ ] Ejecutar tests.
- [ ] Verificar CI.
- [ ] Agregar `walkthrough.md` (si es necesario).
- [ ] Actualizar `DECISIONS.md`.

## Risks

- Lo anterior está bien cubierto.

## Log

- 2026-10-21: Aceptado por /spec-triage como fix para el monitor stale. 

## Checklist

- [ ] Modificar `UsageMonitorService.kt`.
- [ ] Agregar test y validaciones.
- [ ] Ejecutar CI y pruebas.
- [ ] Agregar `walkthrough.md` si es necesario.
- [ ] Generar `BOARD.md` y `DECISIONS.md`.

## Riskiest First Vertical Slice

- [ ] 1. Reevaluar el paquete en el ticker (el cambio crucial).
- [ ] 2. Asegurar los tests.
- [ ] 3. Corregir CI y tests.
- [ ] 4. Validar `--pre-archive`.

## Tareas

- [ ] Preparar el script de modificación.
- [ ] Preparar el test.
- [ ] Asegurar la verificación de CI.
- [ ] Validar la aprobación.
- [ ] Asegurar los checklists finales.