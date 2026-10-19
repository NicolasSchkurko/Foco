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

### Requirement: Code comments stay within a four-line budget
A comment block under `app/src/` SHALL be at most four lines. A longer explanation SHALL live in `docs/mechanisms/`, a walkthrough or an ADR, with a one-line pointer where it was removed.
[@test] `app/src/test/java/com/foco/app/structure/ShortCommentsTest.kt`
[@test] `app/src/test/java/com/foco/app/structure/LayerRulesTest.kt`
[@test] `app/src/androidTest/java/com/foco/app/ui/SettingsScreenTest.kt`

#### Scenario: A comment block over the budget
- **WHEN** a comment block under `app/src/` has more than four lines
- **THEN** the check fails and names the file, the line, the block's length and its first line

#### Scenario: The header the constitution asks for
- **WHEN** a file touches the OS, storage or network
- **THEN** its header names the physical resource, where it lives and its lifecycle, inside the four-line budget

#### Scenario: An explanation worth keeping
- **WHEN** the text being removed explains a why — a debugging note or a mechanism
- **THEN** it moves to `docs/mechanisms/`, the walkthrough or an ADR, and the code keeps a one-line pointer to it

#### Scenario: The sweep changes no behavior
- **WHEN** the sweep is complete and the suite runs
- **THEN** every test passes without behavioral edits — the tree, the domain results and the rendered text are the same as before
