# Proposal — Settings screen: language, exclusions and appearance

## Why

The app has no configuration surface: the UI is hardcoded in Spanish (~15 user-facing strings across the dashboard, onboarding, chart and rows), excluded apps can only be added (long-press) — never reviewed, and the theme follows the system with no manual override (`Theme.kt:19`). The user wants a Settings section where the language, the appearance (Claro/Oscuro) and the excluded list are managed in one place.

## What Changes

- **Settings screen** reachable from the dashboard (gear in the header), on the app's navigation graph (the NavHost lands with `app-usage-detail`).
- **Language selector**: Automático (system) / Español / Inglés — applied immediately and persisted in the same DataStore the exclusions use. Mechanism (human decision): **`appcompat` backport** — `AppCompatDelegate.setApplicationLocales`, one API for 29–37; on 33+ Foco also appears in the system's per-app language list (`android:localeConfig`).
- **Appearance selector**: Automático (system) / Claro / Oscuro — applied immediately (resolved in `FocoTheme`; dynamic colors on Android 12+ keep working) and persisted in the same DataStore.
- **Excluded-apps manager**: lists the excluded packages (icon + label) and restores any of them; empty state when none. Management lives **only** here (human decision — `ranking-exclusions` keeps just the gesture and its undo).
- **i18n baseline**: all user-facing strings move to resources (default ES + `values-en`).
- Non-goals: languages beyond ES/EN; theme and other settings.

## Expected Outcome

"La app es tuya: tu idioma, tus excluidas en un solo lugar, y la ves como querés — siempre" — switching the language re-renders the whole UI (Automático follows the system); picking Claro/Oscuro applies instantly app-wide; both choices survive restarts; and the excluded list is reviewable/restorable from Settings without hidden gestures.

## Alternatives Considered

| Language mechanism | Summary | Why not chosen |
|---|---|---|
| **`appcompat` backport — chosen** (human decision) | One API for 29–37; integrated with the system's per-app language settings on 33+ | Adds `appcompat`; `MainActivity` becomes `AppCompatActivity` |
| Manual override (`createConfigurationContext` + CompositionLocal) | No dependencies; uniform across APIs | DIY surface to maintain; no system integration |
| Native-only (API 33+, `LocaleManager`) | Cleanest on modern devices | `minSdk 29` would lose the feature |

| Exclusions management | Summary | Why not chosen |
|---|---|---|
| **Settings-only — chosen** (human decision) | One home for the list; dashboard stays lean | — |
| Dashboard section + Settings manager | Quick access | Two places managing the same list |

Scope note (human decision): the light/dark selection was folded into this change instead of a separate `appearance-mode` change — same screen, store and tests; one less draft and apply in the queue.

## Capabilities

### New Capabilities

- `app-settings`: the settings surface — language selection (Auto/ES/EN) and the excluded-apps manager.

### Modified Capabilities

(none)

## Impact

- `app/src/main/java/com/foco/app/ui/**` — Settings screen, gear entry point, strings externalized, appearance resolved in `FocoTheme` (`ui/theme/Theme.kt`)
- `app/src/main/java/com/foco/app/data/**` — language and appearance preferences in the existing DataStore; reuse of the exclusions store
- `app/src/main/res/**` — `strings.xml` (ES default) + `values-en` + locales config for the system list
- `app/src/main/AndroidManifest.xml` — `android:localeConfig`
- `app/src/main/java/com/foco/app/MainActivity.kt` — `AppCompatActivity`
- `app/build.gradle.kts`, `gradle/libs.versions.toml` — `androidx.appcompat`
- `app/src/androidTest/**` — settings flow tests
- Ordering (human decision): last in the current queue — after `app-usage-detail` (NavHost) and `ranking-exclusions` (exclusions store); this change also moved exclusions management out of `ranking-exclusions`
