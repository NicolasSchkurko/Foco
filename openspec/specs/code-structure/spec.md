# code-structure Specification

## Purpose
Keep the source tree organized so that location is predictable: one declaration per file, explicit layers whose pure ones stay free of framework dependencies, and dependencies that always point downward.

## Requirements

### Requirement: One type per file
Every source file SHALL contain at most one top-level type declaration — class, data class, enum, interface or object — and when it contains exactly one, the file SHALL be named after it. Groups of top-level functions are exempt.
[@test] `app/src/test/java/com/foco/app/structure/OneTypePerFileTest.kt`

#### Scenario: File with several types
- **WHEN** a source file declares two or more top-level types
- **THEN** the structure check fails and names the file

#### Scenario: Type in a differently named file
- **WHEN** a file declares a single type whose name does not match the file name
- **THEN** the structure check fails

### Requirement: Explicit layers with a pure core
Sources SHALL live in the layer folder matching their package, and dependencies SHALL point downward only (`ui` may use `data`, `data` may use `domain`, `domain` may use `model`, never the reverse). `model/` and `domain/` SHALL stay free of Android and Compose APIs, pure helpers SHALL live in `domain/`, and platform reads the UI needs SHALL be reached through `data/` adapters.
[@test] `app/src/test/java/com/foco/app/structure/LayerRulesTest.kt`

#### Scenario: File in the wrong folder
- **WHEN** a file's package does not match its folder under `app/src/main/java`
- **THEN** the layout check fails and names the file

#### Scenario: Framework import in a pure layer
- **WHEN** a file under `model/` or `domain/` imports `android.*` or `androidx.*`
- **THEN** the purity check fails and names the file

#### Scenario: Dependency pointing upward
- **WHEN** a layer imports from a layer above it (e.g. `domain/` importing `com.foco.app.data`)
- **THEN** the direction check fails and names the file

#### Scenario: A pure helper in the data layer
- **WHEN** a file under `data/` imports neither `android.*` nor `androidx.*` (like the old `availableDays`)
- **THEN** the boundary check fails and names the file

#### Scenario: A platform read called raw from the UI
- **WHEN** a `ui/` file calls a platform read directly (`packageManager.`, `getSystemService(`, `queryEvents(`, `unsafeCheckOpNoThrow(`)
- **THEN** the boundary check fails and names the file

### Requirement: Reorganization preserves behavior
The reorganization SHALL NOT change observable behavior: session and metrics computations keep their results and the screens render the same information, with the existing suite passing and edited only for package moves.
[@test] `app/src/test/java/com/foco/app/domain/SessionBuilderTest.kt`
[@test] `app/src/test/java/com/foco/app/data/SettingsMigrationTest.kt`

#### Scenario: Suite green after the moves
- **WHEN** the reorganization is complete and the suite runs
- **THEN** all tests pass without behavioral edits

#### Scenario: Same dashboard on device
- **WHEN** the app runs after the reorganization
- **THEN** the dashboard shows the same numbers and layout as before

#### Scenario: Stored settings survive the storage split
- **WHEN** the app upgrades with a language and an appearance already stored in the legacy file
- **THEN** the first startup copies them to the new file, clears them from the legacy one, and the UI opens with the same choices — and running the migration again changes nothing (it is idempotent)

### Requirement: Shared UI widgets live in `ui/common/`
Widgets used by more than one screen SHALL live in `ui/common/`; each screen keeps its own widgets beside it. A file under a `ui/<feature>/` folder SHALL NOT import another `ui/<feature>/` package: cross-screen reuse goes through `ui/common/` (the `ui` root, `ui/metrics/` and `ui/theme/` stay shared).
[@test] `app/src/test/java/com/foco/app/structure/LayerRulesTest.kt`

#### Scenario: A feature importing another feature's widget
- **WHEN** a screen imports a widget that lives under another feature's folder (e.g. the settings screen importing from `ui/dashboard/`)
- **THEN** the boundary check fails and names the file

#### Scenario: A widget needed by two screens
- **WHEN** the same widget is needed by the dashboard and the settings screens
- **THEN** it lives in `ui/common/` and both import it from there

### Requirement: A whole-tree check proves its own failure branch
A check that scans the tree SHALL be callable on a given root, and its failure branch SHALL be exercised by a fixture — a synthetic tree that violates the rule — so the rule is proven to fire, not only proven silent on the real tree.
[@test] `app/src/test/java/com/foco/app/structure/LayerRulesTest.kt`

#### Scenario: A feature importing another feature
- **WHEN** the scan runs over a tree where `ui/reports/Reports.kt` imports `com.foco.app.ui.dashboard.…`
- **THEN** it reports exactly one violation, naming that file

#### Scenario: A shared folder importing a feature
- **WHEN** the scan runs over a tree where `ui/common/Widget.kt` imports `com.foco.app.ui.dashboard.…`
- **THEN** it reports no cross-feature violation for it

#### Scenario: The real tree
- **WHEN** the scan runs over `src/main/java` as it is today
- **THEN** it reports no violations — the same assertion the suite has always made

### Requirement: The boundary check derives its feature folders from the tree
The cross-feature boundary check SHALL derive the UI feature folders from the source tree — every folder under `ui/` except the shared ones (the `ui` root, `ui/common/`, `ui/metrics/`, `ui/theme/`) — so that a feature folder added later falls under the rule without editing the check.
[@test] `app/src/test/java/com/foco/app/structure/LayerRulesTest.kt`

#### Scenario: A feature folder added after the check was written
- **WHEN** a new folder appears under `ui/` and one of its files imports another feature's package
- **THEN** the boundary check fails and names the file, with no change to the check itself

#### Scenario: A shared folder is not treated as a feature
- **WHEN** a file under `ui/common/`, `ui/metrics/` or `ui/theme/` imports a feature's package
- **THEN** no cross-feature violation is recorded for it

### Requirement: JVM tests are portable across development platforms
The JVM unit suite SHALL be deterministic on every supported development platform: a test SHALL NOT fail because a framework path behaves differently per platform. Where such a path makes a test unrunnable on a platform, the test SHALL declare that platform exclusion explicitly and name the framework reason; a silent or incidental failure SHALL NOT stand in for it.
[@test] `app/src/test/java/com/foco/app/data/SettingsMigrationTest.kt`

#### Scenario: A framework path that only works on another platform
- **WHEN** a test exercises a framework branch that cannot succeed under the developer's platform — for example DataStore's `Build.VERSION.SDK_INT < 26` fallback, which moves the scratch file with `File.renameTo` and cannot replace an existing file on Windows
- **THEN** the test declares the excluded platform together with its framework reason, and the rest of the suite still runs

#### Scenario: The suite on Windows
- **WHEN** the JVM suite runs on Windows
- **THEN** every test passes or is skipped with a named reason, and none fails

### Requirement: The day's query window is computed by a pure function
The timestamps a day's query uses — the day's start and end, the lookback start and the effective end — SHALL be computed by a pure function in `domain/`, so the numbers the platform adapter asks for are pinned by a JVM test instead of being readable only inside it.
[@test] `app/src/test/java/com/foco/app/domain/DayWindowTest.kt`

#### Scenario: The day is still running
- **WHEN** the window is computed for today with a clock inside the day
- **THEN** the effective end is that clock value, the day's end is the next midnight, and the lookback is one day before the day's start

#### Scenario: The day is past
- **WHEN** the window is computed for a past day
- **THEN** the effective end is the day's own end, not the clock

#### Scenario: The lookback is exactly one day
- **WHEN** the day's start moves by a day
- **THEN** the lookback moves with it, one day before it — never further

### Requirement: Declared rules stay effective
The repository's own configuration SHALL keep its declared rules effective: a `rules:` item that the tooling would silently drop (an unquoted scalar that YAML reads as a mapping) SHALL fail a check that names the file, the line and the item.
[@test] `app/src/test/java/com/foco/app/structure/SpecConfigRulesTest.kt`

#### Scenario: A rules item the tool would ignore
- **WHEN** an item under `rules.<artifact>` starts unquoted and contains `: ` (so YAML reads it as a mapping)
- **THEN** the check fails and names the file, the line and the item

#### Scenario: Every declared rule reaches the tooling
- **WHEN** every item under `rules.<artifact>` is a quoted scalar
- **THEN** the check passes, and creating a change no longer reports that the artifact's rules are being ignored

#### Scenario: The rule text is untouched
- **WHEN** the quoting is added
- **THEN** the wording of every rule is the same as before — including the tech-brief rule, which keeps its colon
