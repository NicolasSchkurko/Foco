# PLAN — Foco (tracker + límites + bloqueo de reels)

Plan de implementación por fases. Cada fase termina con algo usable y un criterio
concreto de "terminado". El objetivo es aprender Android construyendo, no al revés.

## Idea madre

Ganarle al scroll infinito no se hace con muros (se puentean), sino con
**fricción**: cada decisión de abrir la app cuesta unos segundos y requiere
intención. El tracker es el espejo; la fricción es la herramienta.

## Principios de diseño

1. **Fricción antes que muro.** Esperas, respiraciones, "escribí por qué" — cambiar
   la decisión, no declararle la guerra al usuario.
2. **Todo local.** Nada de cuentas ni servidores: los datos de uso quedan en el teléfono.
3. **El sistema manda.** Android no tiene API para "bloquear parte de otra app".
   Todo lo de Reels se construye leyendo la UI, es heurístico y hay que mantenerlo.
   El diseño asume eso (motor de reglas, no lógica hardcodeada).
4. **Fases usables.** Nada de tres meses sin probar nada en el teléfono.

## Arquitectura objetivo (dónde va cada cosa)

| Componente | Responsabilidad | Fase |
|---|---|---|
| UI Compose (`ui/`) | Dashboard, límites, ajustes | 1+ |
| `UsageRepository` (`data/`) | Leer UsageStatsManager → sesiones | 1 ✅ |
| Room (`data/db/`) | Histórico propio, presupuestos, reglas | 2 |
| `UsageMonitorService` (FGS) | Seguir uso en curso, disparar límites | 3 |
| `FrictionOverlay` | Pantalla de fricción sobre la app objetivo | 3 |
| `LimitEngine` | Presupuestos por app/hora + notificaciones | 3 |
| `ReelsBlockerService` (Accessibility) | Detectar Reels/Shorts y actuar | 4 |
| `rules.json` (`assets/`) | Selectores y acciones por app/versión | 4 |

---

## Fase 0 — Entorno

- [ ] Instalar Android Studio; abrir `Foco`; primer build en el teléfono.
- [ ] Depuración USB en el teléfono; aceptar el prompt de la PC.
- [ ] (Para la Fase 3) Desactivar la optimización de batería para Foco.

**Terminado cuando:** el dashboard corre en tu teléfono y ves tu uso real.

## Fase 1 — Tracker del día ✅ (scaffold incluido)

Qué hay: onboarding del permiso, total del día, actividad por hora (24 barras),
ranking de apps con ícono, duración y hora del último uso.

Archivos clave:

- `MainActivity.kt` — decide onboarding ↔ dashboard y re-chequea el permiso en `ON_RESUME`.
- `UsageAccess.kt` — el permiso `PACKAGE_USAGE_STATS` se verifica como AppOp.
- `data/UsageRepository.kt` — eventos → sesiones → ranking + buckets por hora.
- `ui/DashboardScreen.kt` — todo el dashboard.

Extensiones naturales (cuando quieras jugar): selector de fecha, comparación
con ayer, exportar CSV, widget de "hoy".

**Terminado cuando:** ✅ ya funciona.

## Fase 2 — Histórico y persistencia (Room)

Objetivo: no depender de lo que el sistema retiene (limitado) y tener base para límites.

- [ ] Entidades: `SessionRecord` (pkg, inicio, fin) y `DailyAggregate` (pkg, día, total).
- [ ] `WorkManager`: job nocturno que consolida el día y limpia crudo viejo.
- [ ] Pantalla **Semana**: barras por día + totales por app.
- [ ] Migración de esquema desde el día 1 (buena costumbre).

Conceptos nuevos: Room (DAO + Flow), WorkManager, migraciones.

**Terminado cuando:** historial de 7+ días visible, sobrevive reinicios.

## Fase 3 — Límites, recordatorios y fricción

Objetivo: presupuestos por app y fricción real al pasarse. **Acá aparece el
foreground service.**

- [ ] Presupuesto por app (ej. Instagram 30 min/día), configurable en Ajustes.
- [ ] `UsageMonitorService` (foreground, tipo `specialUse` — obligatorio desde Android 14):
      tick cada 15-30 s con UsageStats + accessibility para tiempo real.
      Alternativa liviana: `WorkManager` cada 15 min (alcanza para límites, no para instantaneidad).
- [ ] Notificaciones: canal propio + permiso `POST_NOTIFICATIONS` (Android 13+).
- [ ] **Overlay de fricción** (`SYSTEM_ALERT_WINDOW` + `TYPE_APPLICATION_OVERLAY`):
      pantalla completa sobre la app objetivo con:
        - cuenta regresiva de 10 s antes de habilitar "Abrir igual",
        - opción "escribí para qué la vas a abrir",
        - modo excepción "5 minutos más" con costo (espera + registro).
- [ ] Patrones de referencia de UX: **one sec**, **ScreenZen**.

**Terminado cuando:** si te pasás del límite de Instagram, el teléfono te frena
de verdad y ceder cuesta (un poquito).

## Fase 4 — Bloqueo de Reels / Shorts (la parte artesanal)

Objetivo: que abrir Reels no sea una opción. No hay API: se lee la UI con
**AccessibilityService**.

- [ ] `ReelsBlockerService` con `accessibility-service` config XML.
- [ ] Detección: `onAccessibilityEvent` (`TYPE_WINDOW_STATE_CHANGED` /
      `TYPE_WINDOW_CONTENT_CHANGED`) → BFS acotado sobre `rootInActiveWindow`,
      matcheando por `viewIdResourceName` (regex), texto y `contentDescription`.
- [ ] **Motor de reglas** en `assets/rules.json` (así no recompilás por cada cambio de Instagram):
      ```json
      {
        "package": "com.instagram.android",
        "match": {
          "viewIdRegex": ".*clips.*",
          "textAny": ["Reels"],
          "descAny": ["Reels", "Clips"]
        },
        "action": "back"
      }
      ```
- [ ] Acciones: `BACK` y, si a los ~300 ms sigue bloqueado, `HOME`.
- [ ] Anti falsos positivos: whitelist (visor de stories, DM), debounce de 1 s,
      decisión registrada en un log local (últimas N).
- [ ] Anti-impulso: desactivar el servicio requiere PIN.
- [ ] Sobreversión (Android 13+): una app instalada a mano necesita
      "Permitir ajustes restringidos" para poder activar Accesibilidad.
- [ ] Probar con logcat + prueba real por app (Instagram, TikTok, YouTube).

Referencias open source: `duartebarbosadev/Scrolless`,
`JacobX0126/shorts-blocker-android` (este último documenta exactamente estas
decisiones, incluso el escalado BACK→HOME y el PIN).

**Terminado cuando:** abrir Reels te devuelve al feed de forma consistente, sin
romper el resto de la app (chats, stories, perfil).

## Fase 5 — Endurecimiento (opcional)

- [ ] **Device owner** (una vez, por adb: `adb shell dpm set-device-owner`):
      `DevicePolicyManager.setPackagesSuspended()` bloquea apps a nivel sistema,
      sin heurísticas. Ojo: tu app debe poder desactivarse sola después
      (`clearDeviceOwnerApp()`).
- [ ] O root + LSPosed si te picó el bicho (hook directo a la pantalla de Reels:
      lo más robusto que existe, pero requiere root).
- [ ] Distribución: `adb install` o compilar APK firmado. La verificación de
      desarrollador de Google (en curso 2026-2027) **no afecta** instalaciones
      por adb ni el sideload directo por ahora; para compartir con hasta 20
      dispositivos hay "limited distribution account" gratis.

---

## Riesgos y cosas que este plan no promete

- **OEMs:** algunos fabricantes matan servicios en segundo plano con saña; por eso
  Foco se excluye de optimización de batería y el paso de límites no depende de tiempo real exacto.
- **Instagram cambia la UI:** por eso reglas en JSON + log de decisiones; esperar
  mantenimiento periódico de la Fase 4.
- **Play Store:** accessibility + overlay juntos es zona de alto escrutinio. Este
  proyecto es de uso personal: se instala por USB, no se publica.
- **Batería:** el costo lo paga el foreground service de la Fase 3; se mide y se ajusta ahí.

## Recursos

- Curso oficial gratis: *Android Basics with Compose* (developer.android.com).
- Docs clave: `UsageStatsManager`, `AppOpsManager`, `AccessibilityService`,
  `TYPE_APPLICATION_OVERLAY`, `DevicePolicyManager.setPackagesSuspended`,
  foreground service types (`specialUse`).
- Apps para estudiar su UX: **one sec**, **ScreenZen**, **Scrolless**.
