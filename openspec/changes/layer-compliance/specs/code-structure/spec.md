# Spec Delta

## MODIFIED Requirements

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
