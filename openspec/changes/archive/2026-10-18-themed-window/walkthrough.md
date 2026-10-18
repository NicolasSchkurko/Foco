# Walkthrough — Themed window: the chosen appearance reaches the theme and the window

## What this change touches

Three small places, all in the app module:

- `data/AppAppearance.kt` (new) — turns the stored appearance into appcompat's day/night mode.
- `MainActivity.kt` — applies that mode before the first frame, applies it again when the choice changes, and paints the window backdrop with the theme's `surface` color.
- `test/.../AppAppearanceTest.kt` (new) and `androidTest/.../SettingsScreenTest.kt` — pin the mapping and the mode the app ends up in.

No new storage and no new dependency: the choice is already stored by `app-settings`.

## Mechanism

The two user-visible flashes both come from the same gap: until now the in-app appearance only decided Compose's `darkTheme`. The **activity theme**, its resources and the **window backdrop** kept following the *system*, so relaunching the activity (a language change) or a transition could show the opposite scheme for a moment.

**Cold start, with something already stored**

1. `MainActivity.onCreate` reads `foco_settings` once, blocking, before any UI exists (the same read that already applied the language).
2. `AppAppearance.applyToSystem` maps the stored value to one of appcompat's night-mode constants and hands it to `AppCompatDelegate.setDefaultNightMode`.
3. appcompat resolves the activity theme and resources in that mode; if the mode in force was different, it re-creates (or re-configures) the activity *while it is still being created*, so the first visible frame is already correct.
4. The value read in step 1 is also used as the initial value of the Compose state, so the first composition does not briefly apply the system default.
5. `FocoTheme` resolves the Material 3 color scheme from that state (`AUTO` follows the device, otherwise the choice wins).
6. `WindowBackgroundEffect`, inside the theme, paints the window backdrop with `colorScheme.surface` — the exact color Compose is about to draw, dynamic colors included.

**Picking an appearance while the app is open**

7. The settings row writes the new value to `foco_settings` (unchanged code).
8. The flow in `MainActivity` re-emits, Compose recomposes with the new value, and the `LaunchedEffect` inside the composition calls `AppAppearance.applyToSystem` on the main thread — the choice reaches the theme immediately, and Compose and the window backdrop follow in the same pass.
9. If the mode really changed, appcompat re-creates the activity; the re-created instance repeats step 1 to 6 with the stored value, so it comes back in the right scheme. Applying the mode that is *already* in force is a no-op (appcompat compares it first), which is what stops a recreate loop.

**Switching the language**

10. `AppCompatDelegate.setApplicationLocales` re-creates the activity (this already existed). Because the theme now carries the chosen scheme, the frame shown during the relaunch comes from the themed window/theme instead of the static system-following one — and once Compose paints, `WindowBackgroundEffect` has already set the backdrop to the same `surface` color.

## Physical traceability

| What | Where it lives | Who touches it | Lifecycle |
|---|---|---|---|
| The appearance choice | Preferences DataStore file `foco_settings`, key `appearance` (`"auto"`/`"light"`/`"dark"`), app-private storage | Written by `SettingsStore.setAppearance` (settings screen); read by `SettingsStore.appearance` (Compose state) and by `MainActivity` at startup | Survives restarts; unchanged by this change |
| appcompat's night mode | Process memory: a static field inside `AppCompatDelegate`, no file | Set by `AppAppearance.applyToSystem` from `MainActivity` | Lives as long as the process; re-derived from the stored choice on every cold start |
| The window backdrop | The Activity's own `Window`, holding a `ColorDrawable` built from the theme color | Set by `WindowBackgroundEffect` from the Compose theme | Lives exactly as long as that Activity instance; re-derived on every composition that changes the color; never persisted |
| The Compose state that threads it together | In memory, inside the `MainActivity` composition | `collectAsState`, initialised with the value read at startup | Lives as long as the activity; the DataStore flow keeps it current |

## Limits and failure modes

- **Process killed / device rebooted.** Nothing extra is stored: the cold start re-reads the choice from `foco_settings` and re-applies it, so the fix survives exactly as the rest of the settings do.
- **Missing or unknown stored value.** `AppearanceOption.fromWire` falls back to `AUTO` (follow the system) — same behaviour as before this change.
- **Re-creation cost.** A mode change re-creates the activity. Screen state held by the Activity (which screen is on top, scroll position) is not restored, so after picking an appearance the app can be back on the dashboard — the same thing that already happens today when the language changes. Nothing in Foco holds unsaved user input.
- **No loop.** `setDefaultNightMode` with the mode that is already in force does nothing, so the re-created activity's startup apply is a no-op. At most one re-creation happens per real mode change.
- **Offline.** No network or account is involved anywhere in this path.
- **What the window backdrop covers.** It is the frame behind the UI. Any screen that paints its own background paints over it; the effect is not a substitute for a screen background.
- **Dynamic colors.** The scheme is read from `colorScheme.surface` on each recomposition that changes it. The backdrop follows a *wallpaper* change only when Compose recomposes (e.g. the next re-creation), not instantly.
- **Not machine-checked here.** The window backdrop has no public getter (`Window` exposes `setBackgroundDrawable` but no `getBackground`), so the instrumented test asserts only appcompat's mode; the backdrop itself is covered by the manual visual pass (task 1.5) on the API 36 AVD.

## Touched APIs

- `AppCompatDelegate.setDefaultNightMode(int)` — appcompat's process-wide day/night override. It is the one call that makes the *activity theme*, its resources and the theme's static window background follow the user's choice; it also re-creates a running activity when the mode actually changes.
- `AppCompatDelegate.getDefaultNightMode()` — reads the mode back. Used by the instrumented test as the observable proof of the pick.
- `AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM / MODE_NIGHT_NO / MODE_NIGHT_YES` — the three constants the mapping chooses among (`-1`, `1`, `2`); being `static final` ints, a JVM test can assert against them without the framework.
- `Window.setBackgroundDrawable(Drawable)` — paints the Activity window's backdrop. Chosen over the decor view's background so the color applies at the window level, under Compose.
- `android.graphics.drawable.ColorDrawable` — a solid single-color drawable; the simplest holder for the theme color.
- `Color.toArgb()` (Compose UI graphics) — converts the theme's `Color` into the ARGB int the platform drawable needs.
- `SideEffect` (Compose runtime) — runs the backdrop paint after a successful composition; it only runs when the theme color actually changed, so the drawable is not re-allocated on every frame.
- `LaunchedEffect(appearance)` (Compose runtime) — runs the appcompat apply on the main thread when the stored choice reaches the composition; keyed on the value, so it runs once per real change.
- `MaterialTheme.colorScheme.surface` — the Compose color the window matches; dynamic colors included because it comes from the resolved scheme.
- `isSystemInDarkTheme()` — used unchanged: with the appcompat override in place, it already reflects the app's mode rather than the device's.

## Implicit decisions

- **Mapping in `data/`, split in two.** `AppAppearance` sits next to `AppLanguage` (same layer, same job, same shape: `applyToSystem`). It exposes `nightModeFor` separately because the platform call cannot be made in a JVM test (`android.jar` is a stub) while the mode constants can be asserted there.
- **Startup apply inside the existing blocking read.** The language and the appearance are applied in the same pre-content read; the value read is reused as the Compose state's initial value so the first composition is already correct instead of briefly applying `AUTO`.
- **One `LaunchedEffect` instead of touching the settings screen.** Reacting to picks lives in `MainActivity` (the single place that owns startup application) rather than adding a second call beside the DataStore write in `SettingsScreen` — one owner for "the appearance reaches the platform", and no duplicate apply on the startup path.
- **Both A and B are implemented**, as the proposal adds them: A makes the theme/resources/window follow the choice, B makes the pixels exact for the dynamic scheme. A alone leaves a shade of difference between the static window background and the dynamic scheme; B alone leaves the theme and the static window wrong during the very first frames.
- **The window paint is re-derived, never stored.** No new key, no migration, nothing to go stale.
- **Instrumented assertions kept to what the platform exposes.** The dark-scheme assertion is on `getDefaultNightMode()`; the backdrop is verified by eye (task 1.5), not by a fragile screenshot or an unfetchable drawable.

## Cognitive sign-off

Answered by the human on 2026-10-18 — answers transcribed verbatim, one question at a time (repo convention). Questions 2 and 3 still pending.

1. On a cold start with **Oscuro** stored on a **light** system, the app calls `setDefaultNightMode` *after* `super.onCreate()` has already built that activity's theme and resources. Where does the correct scheme come from before the first visible frame — and where does the chosen appearance live between the DataStore write and the next cold start?
   - "the choice of Oscuro (dark) lives on datastore, mainactivity reads the datastore and brings the correct apparence, after that sets the appapparence to oscuro" … "the appcompat applies the mode before anything is drawn" — the human, 2026-10-18
   - Noted: both halves land. The choice is the `appearance` key of the `foco_settings` DataStore file, written by `SettingsStore.setAppearance` (`app/src/main/java/com/foco/app/data/SettingsStore.kt:28-30`) and read back at every startup by `MainActivity` (`app/src/main/java/com/foco/app/MainActivity.kt:42-46`). The scheme of the first frame comes from appcompat having the mode applied before anything is drawn: the call is made before `setContent` (`:48-49`), so by the time the frame is composed the activity's resources are already in that mode — which is why the re-creation shows up instead of a wrong-coloured first frame (`app/src/androidTest/java/com/foco/app/ui/SettingsScreenTest.kt:184-185`).
2. Picking Oscuro with the app already open changes appcompat's mode and the activity is re-created. What is lost in that re-creation, and what exactly prevents an endless recreate loop when the re-created activity applies the mode again?
   - "A rebuild throws away the screen itself: the activity instance is destroyed and a new one is made. Everything that lived only in that instance goes with it — which screen you were on, where you had scrolled, the state the Compose tree was holding. That's why the test has to re-open Settings after picking an appearance (SettingsScreenTest.kt:184-185). What does not go away: your choice (it's in DataStore), the exclusions, and appcompat's mode (it's process-wide, not per-screen). And nothing you typed is lost, because Foco has no unsaved input anywhere. Because asking for the mode that is already in force is a no-op. setDefaultNightMode starts by comparing what you're asking for with the mode currently in force, and if they're equal it returns without doing anything — no re-apply, no rebuild. The rebuilt screen asks for Oscuro, Oscuro is already in force, so nothing happens and it stays put. A rebuild can only happen when the mode really changes, i.e. when you pick something different: at most one rebuild per real choice." — the human, 2026-10-18
   - Noted: correct, and recorded as written — the human wrote it back while the explanation in the chat was still on screen, so read this as "follows the mechanism and reproduces it", not as unaided recall. Against the code: the re-creation is visible in the test's need to re-open Settings (`app/src/androidTest/java/com/foco/app/ui/SettingsScreenTest.kt:184-185`); what survives is the DataStore (`data/SettingsStore.kt:16-36`) and the process-wide mode (`data/AppAppearance.kt:15-17`); the no-op guard is appcompat's own comparison, relied on by `MainActivity.kt:51-54` and by the walkthrough's *Limits and failure modes* ("No loop").
3. A language switch and an appearance pick both relaunch the activity. Which of the two pieces (A: the night mode / B: the window backdrop) keeps the relaunch frame from showing the opposite scheme — and to make the window follow a *different* color source (say a custom brand color instead of `surface`), which functions would you change?
   - (a) "the a half". (b) after two stabs at the wrong place (`AppAppearance`), the human pointed at the code itself: "this section `FocoTheme(darkTheme = appearance.isDark(isSystemInDarkTheme())) { WindowBackgroundEffect(window = window, surface = MaterialTheme.colorScheme.surface) App() }`" — the human, 2026-10-18
   - Noted: (a) correct — the frames shown during a relaunch come from the activity's own theme/window background, which the night mode (A) drives; the backdrop (B) only gets its turn once a composition exists and the theme can be read, which is what makes the color exact afterwards and covers transitions. (b) correct once located, and the walk to it is the useful part: the color is decided at the call site (`app/src/main/java/com/foco/app/MainActivity.kt:56-62`, the `surface = MaterialTheme.colorScheme.surface` argument) and painted by `WindowBackgroundEffect` (`:73-78`); `data/AppAppearance.kt` is **not** involved, because it only deals in appcompat mode numbers and never sees a color. Recorded as given — answered by pointing at the two places rather than in prose.
