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
Sources SHALL live in the layer folder matching their package — `model/` (pure types), `domain/` (pure behavior), `data/` (Android platform access), `ui/` (Compose presentation) — dependencies SHALL point downward only (`ui` may use `data`; `data` may use `domain`; `domain` may use `model`; never the reverse), and `model/` and `domain/` SHALL NOT import Android or Compose APIs. Pure computation helpers SHALL live in `domain/`, never in `data/`; platform reads used by the UI (package-manager icon and label reads) SHALL be reached through `data/` adapters, never called raw from `ui/`.
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
The reorganization SHALL NOT change observable behavior: session and metrics computations keep their results and the screens render the same information, with the existing suite passing and edited only for package moves. Internal storage reorganizations SHALL also preserve the stored user data: splitting a store SHALL migrate the existing values, and the migration SHALL be idempotent.
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
- **THEN** the first startup copies them to the new file, clears them from the legacy one, and the UI opens with the same choices

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
