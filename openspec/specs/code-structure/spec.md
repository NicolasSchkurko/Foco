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
Sources SHALL live in the layer folder matching their package — `model/` (pure types), `domain/` (pure behavior), `data/` (Android platform access), `ui/` (Compose presentation) — dependencies SHALL point downward only (`ui` may use `data`; `data` may use `domain`; `domain` may use `model`; never the reverse), and `model/` and `domain/` SHALL NOT import Android or Compose APIs.
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

### Requirement: Reorganization preserves behavior
The reorganization SHALL NOT change observable behavior: session and metrics computations keep their results and the screens render the same information, with the existing suite passing and edited only for package moves.
[@test] `app/src/test/java/com/foco/app/domain/SessionBuilderTest.kt`

#### Scenario: Suite green after the moves
- **WHEN** the reorganization is complete and the suite runs
- **THEN** all tests pass without behavioral edits

#### Scenario: Same dashboard on device
- **WHEN** the app runs after the reorganization
- **THEN** the dashboard shows the same numbers and layout as before the change
