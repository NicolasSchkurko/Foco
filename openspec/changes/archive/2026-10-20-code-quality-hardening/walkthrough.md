# Walkthrough — code-quality-hardening

## What this change does

It removes the code smells that were making the tree hard to trust: the dashboard's navigation
history no longer lives as a joined `String` inside the composable, warnings now fail the build, a
static-analysis task runs in CI, encoding is checked mechanically, and startup can no longer block
the main thread forever.

## Mechanism

- `DashboardState` (`ui/dashboard/DashboardState.kt`) is a plain Kotlin class over
  `MutableStateFlow<Snapshot>`. It owns the typed ring (`DayRingState`), the shown stop and the
  motion flags, and exposes `moveTo` (`:28-51`), `moveStep` (`:53-65`), `goBack` (`:67-72`) and
  `retain` (`:74-86`). No Android or Compose import is needed to instantiate it.
- `DashboardScreen` is now a view over that holder: `rememberSaveable` restores it through
  `dashboardStateSaver` (`DashboardScreen.kt:588-610`), and `collectAsState` reads the snapshot
  (`:148`). The old `historyKeys` string, `setHistory` and direct `requestedKey`/`shownKey` writes
  are gone; the composable calls `moveTo`/`moveStep`/`goBack`/`retain`/`showCurrent`
  (`:167`, `:171`, `:175`, `:188`, `:238`).
- Repository and store creation moved behind `createRepository` / `createHistoryStore` parameters
  (`DashboardScreen.kt:121-130`). The `loadDay` and `clock` test seams stay as they were.
- `withStartupTimeout` (`domain/StartupSettings.kt:7-16`) bounds the startup work with
  `withTimeoutOrNull` and converts timeout or failure into the fallback. `MainActivity` runs the
  legacy migration and first settings reads inside it with a 2 s bound and logs the fallback
  (`MainActivity.kt:32`, `:49-58`).
- `warningsAsErrors` is enabled for every Kotlin compilation whose name does not contain
  `AndroidTest` (`app/build.gradle.kts:62-66`); the deprecated `unsafeCheckOpNoThrow` call became
  `checkOpNoThrow` (`UsageAccess.kt:17`) and `WeekExportJson` opts in to the experimental API
  (`WeekExportJson.kt:1`).
- ktlint 14.2.0 is applied in `app/build.gradle.kts:55-59` with a committed baseline and runs in CI
  (`.github/workflows/ci.yml:42`). Existing violations are captured by `app/ktlint-baseline.xml`; a
  new violation fails.
- `SourceEncodingTest` scans `app/src` and the top-level `*.md` files for U+FFFD and BOM
  (`app/src/test/java/com/foco/app/structure/SourceEncodingTest.kt`); it found and we removed a BOM
  from `app/src/androidTest/java/com/foco/app/ui/AppLimitBlockTest.kt`.
- `BuildHygieneTest` asserts the build fails on warnings and that CI runs `ktlintCheck`;
  `DashboardStateTest` covers push/pop, ring bounds, direction and shortcut retention;
  `MainActivityStartupTest` covers the fast, slow and failing startup branches.

## Physical traceability

| Piece | Where | What it proves |
| --- | --- | --- |
| Typed state holder | `DashboardState.kt:13-86` | History is a `List<DayStop>`, not serialized text |
| Save/restore | `DashboardScreen.kt:588-610` | Process death keeps the position |
| Thin view | `DashboardScreen.kt:145-238` | The composable delegates transitions |
| Injected factories | `DashboardScreen.kt:121-130` | Store/reader creation is testable |
| Bounded startup | `MainActivity.kt:32`, `:49-58`; `StartupSettings.kt:7-16` | Timeout and failure fall back |
| Warnings as errors | `app/build.gradle.kts:62-66` | main + unit test compilations |
| Static analysis | `app/build.gradle.kts:55-59`; `ci.yml:42` | ktlint runs in CI |
| Encoding gate | `SourceEncodingTest.kt` | U+FFFD and BOM fail the suite |
| Startup tests | `MainActivityStartupTest.kt` | Timeout, success and failure paths |
| State tests | `DashboardStateTest.kt` | Push/pop, bounds, direction, shortcut |

## Limits and failure modes

- **The ktlint baseline masks existing violations.** The task passes today because the baseline
  records the current debt; only violations outside it fail. Re-generating the baseline is the
  place to start reducing it.
- **`warningsAsErrors` excludes AndroidTest only.** Main, release and unit-test compilations fail on
  warnings; instrumented tests keep compiling with the many Compose test deprecations.
- **The startup bound is a hard 2 s.** A slower device loses the stored language/appearance and gets
  `AUTO`; the app still opens, and the log records why. The bound is a constant, not a setting.
- **`checkOpNoThrow` can throw a `SecurityException`** if the package/uid is not the caller's; this
  call uses the app's own uid and package, and the check already lives behind the usage-access
  onboarding.
- **The state holder is not Compose state.** It is a `StateFlow`; the screen collects it. Direct
  writes to the old `mutableStateOf` fields are gone, so every read goes through `collectAsState`.
- **The encoding scan covers `app/src` and top-level `*.md` only.** The `openspec/` documents still
  contain display-mojibake; changing them is a separate change.
- **The ktlint plugin is a new build-time dependency.** It runs in CI only, not at app runtime, and
  no other dependency changed.

## Touched APIs and resources

Kotlin coroutines (`StateFlow`, `withTimeoutOrNull`), Jetpack Compose saveable/state, the Gradle
build and CI workflow, and the AppOpsManager read. No database, network, permission or app data
changed.

## Implicit decisions

- Keep `DayRingState` as the typed core and wrap it with motion in `DashboardState`.
- Expose the state as `StateFlow`, not `mutableStateOf`, so the holder stays Android/Compose-free.
- Keep `rememberSaveable` with a `listSaver` of keys; restore falls back to today when a key is
  unreadable.
- Put the startup helper in `domain/` (pure, no platform import) rather than in the activity.
- Choose ktlint over Detekt and commit a baseline instead of fixing the whole tree in this change.
- Enable `warningsAsErrors` by task name rather than globally, to keep instrumented-test deprecations
  from blocking the suite.

## Verification

- `gradlew.bat test` green (full JVM suite, including the new tests).
- `gradlew.bat ktlintCheck` green with the committed baseline.
- The instrumented dashboard/week/settings classes pass on a clean device.
- `openspec validate 2026-10-20-code-quality-hardening --strict` valid.
- Gate `--run-tests` green.

## Cognitive sign-off

Answer these in your own words; the sign-off is recorded only after your explicit confirmation.

1. Where does the home's back stack live now, and why does that make it unit-testable without a
   device?
   > El back stack vive en DashboardState (ui/dashboard/DashboardState.kt:12-26), un holder Kotlin puro sobre MutableStateFlow<Snapshot>; el historial es tipado, List<DayStop> dentro de DayRingState (:46, :79), y lo mueven moveTo/moveStep/goBack/retain. No importa Android ni Compose, así que DashboardStateTest lo instancia en la JVM sin dispositivo ni emulador. DashboardScreen es una vista fina que lo restaura con dashboardStateSaver (DashboardScreen.kt:588-608) y lo observa con collectAsState (:148).
   >
   > — the human, 2026-10-20

2. What happens when the startup migration takes longer than the bound, and what does the user see?
   > withStartupTimeout corre la migración y las lecturas iniciales con un withTimeoutOrNull de 2 s (StartupSettings.kt:7-16, MainActivity.kt:32, :48-60). Si se pasa o falla, devuelve el fallback: loguea "Settings startup timed out or failed; using defaults" y usa AppearanceOption.AUTO; la app abre igual, con apariencia y lenguaje por defecto (siguiendo al sistema), en vez de quedarse bloqueada en el arranque. La migración que no llegó a completarse se reintenta en el próximo arranque.
   >
   > — the human, 2026-10-20

3. Which violations does the committed ktlint baseline hide, and what would it take to make them
   fail again?
   > El baseline (app/ktlint-baseline.xml, 1374 errores; app/build.gradle.kts:55-59) oculta todas las violaciones actuales ya registradas, no una regla concreta. Con ignoreFailures=false, solo falla lo nuevo que no está en el baseline. Para que vuelvan a fallar: quitar el baseline.set(...) (o borrar el archivo, o regenerarlo vacío después de arreglar); entonces ktlintCheck vuelve a fallar con esas 1374 y el build se rompe hasta corregirlas.
   >
   > — the human, 2026-10-20
