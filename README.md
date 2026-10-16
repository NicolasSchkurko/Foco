# Foco

App de Android (proyecto personal) para:

1. **Trackear** cuánto y *cuándo* usás cada app (estilo Bienestar Digital, pero con tus propias reglas).
2. Poner **límites y fricción** (recordatorios, esperas forzadas) para reducir el uso automático.
3. **Bloquear Reels / Shorts** (contenido de scroll infinito) dentro de Instagram, TikTok y YouTube.

Stack: **Kotlin + Jetpack Compose**. Todo local, sin cuentas ni servidores.

---

## Estado actual

- ✅ **Fase 1 (este scaffold)**: tracker del día — total de uso, actividad por hora y ranking de apps, con onboarding del permiso de "Acceso de uso".
- ⏭️ Fases 2 a 5 (límites, fricción, bloqueo de reels): ver **[PLAN.md](PLAN.md)**.

## Requisitos

- [Android Studio](https://developer.android.com/studio) (última versión — incluye el JDK y el SDK).
- Un teléfono Android con Android 10 (API 29) o superior, con **depuración USB** activada.
  - El emulador sirve para ver la UI, pero **no** para datos de uso reales.

## Cómo abrirlo y correrlo

1. Android Studio → **Open** → seleccioná esta carpeta (`Foco`).
2. Esperá el primer *sync* de Gradle (descarga Gradle 9.6 y las dependencias; la primera vez tarda).
3. Conectá el teléfono por USB y aceptá "Permitir depuración USB" cuando aparezca.
4. Presioná **Run ▶**.

> El proyecto ya usa la misma pila que trae Android Studio 2026.2: AGP 9.4, Kotlin 2.4 (incorporado), Gradle 9.6 y Compose BOM 2026.09. No deberías ver pedidos de "Upgrade".

## Primer uso

1. Al abrir, Foco te pide el permiso **Acceso de uso** (es un permiso especial: Android no permite pedirlo con un diálogo, solo activarlo a mano).
2. Tocá **"Abrir Ajustes"**, buscá **Foco** en la lista y activá el permiso.
3. Volvé a la app: el dashboard se actualiza solo.

Si tu fabricante tarda en mostrar la app en esa lista, salí de Ajustes y volvé a entrar.

## Estructura del proyecto

```
Foco/
├── app/
│   ├── build.gradle.kts              # módulo: SDK, dependencias
│   └── src/main/
│       ├── AndroidManifest.xml       # permiso especial + queries + activity
│       ├── res/                      # ícono, tema base, strings
│       └── java/com/foco/app/
│           ├── MainActivity.kt       # entrada: onboarding ↔ dashboard
│           ├── UsageAccess.kt        # chequeo del permiso "Acceso de uso"
│           ├── data/
│           │   ├── UsageModels.kt    # modelos (día, app, hora)
│           │   └── UsageRepository.kt# consulta UsageStatsManager, arma sesiones
│           └── ui/
│               ├── OnboardingScreen.kt
│               ├── DashboardScreen.kt# gráfico por hora + ranking de apps
│               ├── Formatting.kt     # duración "3 h 24 min", hora "14:32"
│               └── theme/Theme.kt    # Material 3 (+ color dinámico)
├── gradle/libs.versions.toml         # catálogo de versiones
├── PLAN.md                           # plan de implementación por fases
└── gradlew / gradlew.bat             # wrapper de Gradle
```

## Comandos útiles (con el wrapper)

En la raíz del proyecto:

```powershell
.\gradlew.bat assembleDebug     # compila -> app/build/outputs/apk/debug/app-debug.apk
.\gradlew.bat installDebug      # compila e instala en el teléfono conectado
```

`adb` viene con el SDK (`%LOCALAPPDATA%\Android\Sdk\platform-tools`), o instalás *platform-tools* desde el SDK Manager.

## Cómo funciona el tracker (resumen técnico)

- `UsageStatsManager.queryEvents()` devuelve eventos del sistema (`ACTIVITY_RESUMED` / `ACTIVITY_PAUSED`).
- `UsageRepository` los convierte en **sesiones** (app, inicio, fin), descarta apps sin ícono de lanzador, home y Foco mismo, y arma dos vistas: ranking por app y distribución por hora.
- El permiso `PACKAGE_USAGE_STATS` se declara en el manifest y se verifica como **AppOp** (`UsageAccess.isGranted`).
- Los datos se consultan al vuelo (el sistema ya los guarda); la persistencia propia llega en la Fase 2 con Room.

## Problemas comunes

| Síntoma | Causa / solución |
|---|---|
| "Sin actividad registrada" | El permiso de uso no está activo, o es un teléfono recién formateado (el sistema tarda en acumular eventos). |
| El sync de Gradle falla por red | Reintentar: Tools → Sync Project. Si persiste, revisar proxy/firewall. |
| `SDK location not found` | Abrí el proyecto con Android Studio (genera `local.properties`) o definí `ANDROID_HOME`. |
| Compila pero no instala | ¿Depuración USB aceptada? ¿Conflicto con una versión ya instalada? `.\gradlew.bat installDebug -r`. |
