# Proposal — settings-about-and-pickers

## Why

Settings opens straight into the Language section and says nothing about the app itself: which
build is running, who makes it, or where the source lives. Language and appearance also spend
three radio rows each, so the screen is long for four settings that only need one value at a time.

## What

- **Identity block**, first content in settings, above the Language section and horizontally
  centered: `Foco <version>` on one line, the credit `Open source project by Nicolas Schkurko`,
  and a `Git repository` link. Tapping the link hands
  `https://github.com/NicolasSchkurko/Foco` to the system browser.
- **Language** becomes one row that shows its current choice (`Automatic` / `Spanish` /
  `English`, localized) and opens an anchored dropdown menu; picking applies and persists as today.
- **Appearance** gets the same shape (`Automatic` / `Light` / `Dark`).
- **MIT `LICENSE`** at the repo root, so the "open source" credit is backed by an actual grant.
- No new dependency, no new store, no new screen, no network from Foco.

Position and order of the remaining sections (Language, Appearance, Excluded apps) stay as they
are; only the three selector rows collapse into one row per setting.

## Expected outcome

Opening Settings shows the app's identity and build version above Language, centered, and the
repository link reaches GitHub in the browser. Each of language and appearance is a single row
that states its current value and opens a dropdown; both still apply immediately, survive restarts
and keep following the system under `Automatic`.

Worth it when a reader holding the phone can tell which build they have and where the code lives
without leaving the app, and changing theme or language takes two taps instead of scanning six
radio rows.

## State and persistence mechanism

No new persistence. Language and appearance keep writing to the existing DataStore through
`SettingsStore.setLanguage` / `SettingsStore.setAppearance` (`data/SettingsStore.kt:16-31`) and
keep the same apply-side effects (`AppLanguage.applyToSystem`, the scheme resolved in
`FocoTheme`). The open/closed dropdown is composition state only (`remember { mutableStateOf(false) }`).

The version is read from the installed package (`PackageManager`) through a `data/` adapter and is
never stored or cached on disk; the repository URL is a constant in the same adapter. Tapping the
link builds an `ACTION_VIEW` intent in `data/` (the `UsageAccess.settingsIntent()` precedent,
`data/UsageAccess.kt:25`) and `ui/` launches it: the browser, not Foco, does the network work, so
the 100%-local posture and `allowBackup="false"` are unchanged.

## Alternatives considered

### Selector shape (chosen: anchored dropdown menu)

| Option | Why yes | Why not |
| --- | --- | --- |
| Modal `AlertDialog` with the three options, check on the current one | Three options fit; large targets; Material 3 is already on the classpath | Takes over the screen while open; the living instrumented test has to open and close a dialog |
| Anchored `DropdownMenu` on the row (chosen) | Lighter, no scrim, stays in context; the row keeps stating its value | Small targets; row and menu look alike; a menu near the screen edge clips |
| Keep the radio rows | No test rewrite; two living requirements stay green untouched | Exactly what the request rejects; six rows for two settings |

### Version source

| Option | Why yes | Why not |
| --- | --- | --- |
| `PackageManager.getPackageInfo().versionName` through a `data/` adapter (proposed) | The installed truth; no build flag; leaves `app/build.gradle.kts` alone (claimed by `weekly-json-export`); follows the existing adapter rule | One platform read; `versionName` is nullable, so the block needs a fallback to the name alone |
| `BuildConfig.VERSION_NAME` | Compile-time constant, no read at all | `buildConfig` is off today (`app/build.gradle.kts:38-40`), so it forces a build change on a file another in-flight change claims |
| Literal string in `strings.xml` | Zero code | Drifts from `versionName` silently, and the requirement forbids it |

### Repository link dispatch

| Option | Why yes | Why not |
| --- | --- | --- |
| `ACTION_VIEW` intent built in `data/`, launched from `ui/` (proposed) | Leaves Foco; the system browser owns its own security; mirrors `UsageAccess.settingsIntent()` | One more adapter method |
| `LocalUriHandler.openUri` inside the composable | One line | A raw platform call in `ui/`, against `code-structure/spec.md:41` and the repo's adapter rule |
| In-app `WebView` | Stays inside Foco | New surface, wider permission surface, network from the app — against the 100%-local posture |

### Presenting "open source" without a license (chosen: add MIT)

| Option | Why yes | Why not |
| --- | --- | --- |
| Add a `LICENSE` file (MIT) and keep the credit (chosen) | Makes the claim true; readers know their rights | Adds a file to the repo root and fixes the copyright line |
| Reword to "Personal project by Nicolas Schkurko" | True today without new files | Drops the wording the human asked for |

## Open decisions

Tracked in `.openspec.yaml` → `open_decisions`; it blocks `verified` until the human answers:

1. Version source: `PackageManager` adapter (proposed), `BuildConfig`, or a literal.

Resolved on 2026-10-19 (recorded in `DECISIONS.md`): the selector is an anchored dropdown; the
repository ships an MIT `LICENSE`; the change declares `weekly-json-export` as a dep so the shared
string files are applied in a declared order.

## Prior decisions

No conflict. `DECISIONS.md:30` keeps base resources English and the Spanish text in `values-es`;
the credit line is the same proper noun in both. `DECISIONS.md:211` approved `app-settings`
without ever fixing the control form ("SHALL offer" in `openspec/specs/app-settings/spec.md:25`,
`:41`), so a dropdown does not reverse it. `DECISIONS.md:317` / `:324` require an explicit human
choice per dependency; this change adds none (the MIT license is a repo file, not a library).

## Overlaps in flight

`weekly-json-export` (draft) claims `app/src/main/res/values/strings.xml` and
`app/src/main/res/values-es/strings.xml`, plus `app/build.gradle.kts`. The about block needs new
keys in both string files; the `PackageManager` route keeps `app/build.gradle.kts` out of this
change. This change now declares `weekly-json-export` in `x-deps`, so the gate reports the shared
string files as **sequenced by deps** (warning, not a conflict) and the apply order is fixed:
`weekly-json-export` lands first.
