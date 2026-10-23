# Repository constitution

Foco uses living specs and structured changes as the project's contract and design tracking.

## Layout

`openspec/specs/` (living specifications) · `openspec/changes/` (changes in flight and archive) · `openspec/schemas/` (schemas) · `docs/BOARD.md` (task/change tracking) · `docs/ARCHITECTURE.dsl` (system structure).

## Reuse first (code minimalism)

Before writing code, take the first option that fully works: (1) does it need to exist? skip what nobody asked for and name it; (2) already in this repo (helper, component, pattern)? use it the way the codebase does; (3) standard library or platform feature?; (4) installed dependency — never add one for a few lines; (5) can it be one clear line?; (6) otherwise, the minimum that works.

Be lazy about the solution, never about the change: finish every caller, test and fixture it breaks. Never cut trust-boundary validation, error handling that prevents data loss, security or accessibility. A shortcut with a known limit gets: `shortcut: <limit>, <when to upgrade>`.

## Code expressivity (generation rules)

- Never call platform/persistence/network APIs raw from domain or UI logic — wrap them in functions whose names state the effect and the medium (e.g. `saveThemeToLocalDataStore()`, not a bare `prefs.edit().putString`).
- Files that touch the OS, database, storage or network start with a short header: which physical resource, where it lives, its lifecycle. No other noise comments.
- No premature abstraction: explicit linear flow over indirection, unless requested.

## Project context

- **Foco**: personal Android app (Kotlin + Jetpack Compose, Material 3), 100% local — no accounts or servers. Phases in `docs/PLAN.md` (1 tracker ✅ · 2 Room history ✅ · 3 app limits and optional alerts · 4 modular friction at app open).
- Code in `app/src/main/java/com/foco/app/` (`model/`, `domain/`, `data/`, `ui/`). Build: Android Studio or `gradlew.bat assembleDebug`. Tests: JVM tests under `app/src/test/`; instrumented tests under `app/src/androidTest/`.
