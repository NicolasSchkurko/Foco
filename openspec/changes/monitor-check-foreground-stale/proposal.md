# Proposal — monitor-check-foreground-stale

## Why

`UsageMonitorService.checkOnce()` (línea 82-98) lee el `packageName` actual de `ForegroundApp.currentPackage()` **una sola vez** al inicio del check. Luego itera sobre el valor de `limit`, `alertEnabled`, `usedSeconds` e intenta bloquear o notificar. Mientras tanto, el usuario puede cambiar de app (ej., de Instagram a YouTube). El monitor periódico (ticker cada 2 minutos) **no valida nuevamente** `currentPackage` antes de `onBlock()` / `onNotify()`. La spec dice que "El block acts only on the app in the foreground" (`spec.md:207-219`).

El resultado:
- Una app que ya tiene un límite activo y **alcanzó** su límite, pero **está actualmente abierta** pero **con un límite agotado**, **bloquea** una vez que está abierta.
- El ticker de 2 min lee el `packageName` (Instagram) **mientras está fuera del frente**, espera el límite (1500 s alcanzado), y lanza bloque **sobre Instagram** aunque Instagram está actualmente abierto. Esto en realidad provoca un ciclo de vida de actividad extraño.
- El bloqueo de monitor basado en `currentPackage()` (el servicio de accesibilidad) es correcto, pero el monitor periódico está incorrectamente diseñado.

El mecanismo:
- `UsageMonitorService.checkOnce()` lee `packageName = ForegroundApp.currentPackage()` al inicio (línea 82) y lo guarda.
- Luego `evaluateLimitCheck(...)` (línea 91-100) valida `limitMinutes` y `usedSeconds` y decide el bloqueo o alerta.
- El `BackgroundAppEvent` se usa de forma similar; `UsageMonitorAccessibilityService` usa `foreground.accepts()` que `onWindowEvent` actualiza `currentPackage` correctamente.

El bug no es un bug en el sentido tradicional, es una **especificación** (la entrada para checkOnce solo se completa al inicio).

## Qué cambia

- `UsageMonitorService.checkOnce()` lo reevalúa tras cada `delay` de 2 minutos:
  - `current = ForegroundApp.currentPackage()` (es correcto).
  - Si `current == packageName` (leído previamente), continúa con `evaluateLimitCheck`.
  - Si el paquete actual es **diferente**, salimos temprano (no bloqueamos ningún paquete).
- Solo reavaluamos `usedSeconds` si el paquete actual coincide; de lo contrario, no bloqueamos nunca nada para ese paquete no activo.
- Agregar prueba: JVM y CI para verificar el cambio de monitor (primer test es un `JUnit5` unit test).

## Riesgos

- Si el `currentPackage()` cambia entre la primera lectura y la evaluación (`onBlock` / `onNotify`), podemos bloquear un paquete que ya no está en frente, provocando un ciclo de vida inesperado (la situación real que observamos).
- Los cambios en el test runner pueden afectar al monitor de CI.
- Es un ajuste muy pequeño; podría interrumpir la cadencia de 2 minutos, pero en realidad mantiene el ticker regular.

## Aceptación

Aceptado por el humano para /spec-triage como `fix`, con `tech_brief` si es necesario (es detección).

Signado por: AI • 2026-10-21