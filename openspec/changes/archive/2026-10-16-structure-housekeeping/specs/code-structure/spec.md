# Spec Delta

## MODIFIED Requirements

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

## ADDED Requirements

### Requirement: Shared UI widgets live in `ui/common/`
Widgets used by more than one screen SHALL live in `ui/common/`; each screen keeps its own widgets beside it. A file under a `ui/<feature>/` folder SHALL NOT import another `ui/<feature>/` package: cross-screen reuse goes through `ui/common/` (the `ui` root, `ui/metrics/` and `ui/theme/` stay shared).
[@test] `app/src/test/java/com/foco/app/structure/LayerRulesTest.kt`

#### Scenario: A feature importing another feature's widget
- **WHEN** a screen imports a widget that lives under another feature's folder (e.g. the settings screen importing from `ui/dashboard/`)
- **THEN** the boundary check fails and names the file

#### Scenario: A widget needed by two screens
- **WHEN** the same widget is needed by the dashboard and the settings screens
- **THEN** it lives in `ui/common/` and both import it from there
