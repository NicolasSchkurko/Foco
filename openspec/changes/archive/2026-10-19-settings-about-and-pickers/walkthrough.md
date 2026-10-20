# Walkthrough — settings-about-and-pickers (tech brief)

## Mechanism

Settings gains an identity block and turns its two selectors from radio rows into one row with an
anchored dropdown each.

**The About block.** `SettingsScreen` builds `AppIdentity(context.applicationContext)` once
(`SettingsScreen.kt:61`) and renders `AboutBlock(version = identity.versionName(), onOpenRepository = …)`
as the **first** item of the list (`:91-97`). `AboutBlock` (`:169-196`) is a centred column: the
credit line, `settings_about_version` formatted with the version (or `settings_about_version_unknown`
when the package manager does not resolve it), and a `TextButton` that starts
`identity.repositoryIntent()`.

**Where the version comes from.** `AppIdentity.versionName()` (`AppIdentity.kt:14-17`) asks the
`PackageManager` for `getPackageInfo(context.packageName, 0).versionName` inside `runCatching`, so an
unresolvable package yields `null` instead of a crash. It is the installed truth, not a build flag: a
debug build and a release build both report what the system sees.

**The selectors.** Each setting is a `DropdownRow` (`:200-236`): a row whose title is the section
name, whose second line is the **active** label, with a trailing chevron; the row is clickable and
opens a `DropdownMenu` anchored to it. The menu lists the option labels, marks nothing inline (the
row already shows the active value), and on a pick calls the option's action. The labels come from
`languageLabel` / `appearanceLabel`, the single source shared by the row and the menu.

**What a pick does.** Language (`:99-113`): `store.setLanguage(option)` inside
`withContext(NonCancellable)`, then `AppLanguage.applyToSystem(option)` — which re-creates the
activity. Appearance (`:115-129`): the same shape with `store.setAppearance(option)`. The
`NonCancellable` is deliberate: the pick is what triggers the re-creation, and a write cancelled by
that re-creation would lose the user's choice.

**The license.** The MIT `LICENSE` was added by the change's first commit; the credit line in the
About block is now backed by an actual grant.

## Physical traceability

- **Where the version lives**: nowhere in the app — it is read from the installed package
  (`PackageManager`), so there is no copy to go stale.
- **Where the settings live**: unchanged — `DataStore` through `SettingsStore` (`AppLanguage`,
  `AppAppearance`), the same keys the notification-free screens already used.
- **Where the license lives**: `LICENSE` at the repository root, not shipped in the APK.
- **No new storage, no new permission, no network**: the only new intent is `ACTION_VIEW` on the
  repository URL, started by the user's tap.

## Limits and failure modes

- **A `null` version** (package manager refuses to resolve the package) renders "unknown" rather than
  an empty line.
- **The menu is a popup**: it dismisses on an outside tap or on the system back action, and picking an
  option closes it. It is not a dialog — the screen behind stays interactive-looking but the popup
  owns the touch until dismissed.
- **The language pick re-creates the activity**; the test has to tolerate the gap between the two
  hierarchies (that is what `ensureSettings` does now).
- **The manual tap on the repository link is not covered by an automated test**: `AppIdentity`'s
  intent is asserted (`ACTION_VIEW` + the exact URL), but that the browser opens and Foco stays in the
  background is a one-time manual check, and it stays as an open task.
- **Nothing here is exported**: no `FileProvider`, no share, no storage permission.

## Touched APIs

- **`PackageManager.getPackageInfo`** (`AppIdentity.kt:14-17`) — the installed `versionName`.
- **`Intent.ACTION_VIEW` + `Uri.parse`** (`AppIdentity.kt:20`) — the repository link.
- **Material 3 `DropdownMenu` / `DropdownMenuItem`** (`SettingsScreen.kt:224-236`) — the anchored
  popup, with `expanded` + `onDismissRequest`.
- **`kotlinx.coroutines.NonCancellable`** (`SettingsScreen.kt:107,123`) — the write survives the
  activity re-creation the pick itself triggers.

## Implicit decisions

- **`AppIdentity` is a class with a `Context`**, not a top-level function: it is platform access, and
  the layer rules send a file without Android imports to `domain/`.
- **The About block is the first item of the `LazyColumn`**, above Language, so the identity reads
  before the settings it describes.
- **The row shows the active value instead of a checkmark in the menu**: one line to read when the
  menu is closed, which is the state the screen is in most of the time.
- **`NonCancellable` on the store write, not on the whole coroutine**: the locale/night-mode
  application must still be able to run after the write completes, and the write must not be lost if
  the screen goes away first.
- **The test scopes the option click to the popup** (`hasAnyAncestor(isPopup())`): "Automático" is also
  the appearance row's visible value, so an unscoped text match clicked the wrong node.

## Cognitive sign-off

Answer in your own words (the agent must not fill these):

1. The About block shows "Version 0.1.0". Where does that string come from, what happens on a device
   where the package manager cannot resolve Foco, and why is it not read from a build constant?
   > **Answer (Nschkurko, 2026-10-19):** El `0.1.0` sale de `versionName = "0.1.0"` en
   > `app/build.gradle.kts:19`, que queda grabado en el paquete instalado. Si el package manager no
   > resuelve Foco, el `runCatching` devuelve `null` y se muestra `settings_about_version_unknown`
   > («desconocida» / «unknown»). No se lee de una constante de build porque sería una copia que puede
   > quedar vieja; preguntarle al paquete instalado muestra lo que realmente está instalado, igual en
   > debug y release.
2. You tap Language, the menu opens, you pick Spanish. Describe what happens to the stored value, what
   the activity does, and why the write is wrapped in `NonCancellable`.
   > **Answer (Nschkurko, 2026-10-19):** El ítem del menú ejecuta `onSelect()` y cierra el popup.
   > Guarda el lenguaje seleccionado y después lo aplica. Usa `NonCancellable` porque la corrutina vive
   > en el scope de la pantalla y la recreación que dispara el propio pick la cancelaría.
3. The instrumented test picks "Automático" from the language menu. What could go wrong if the test
   searched for that text across the whole screen, and what does the popup-scoped matcher change?
   > **Answer (Nschkurko, 2026-10-19):** «Automático» es el valor visible de la fila de Apariencia; el
   > matcher acotado `hasText(option) and hasAnyAncestor(isPopup())` limita la búsqueda al `DropdownMenu`
   > abierto, así que clickea el ítem real que llama `setLanguage(AUTO)` y no depende de lo que muestren
   > las otras filas.
