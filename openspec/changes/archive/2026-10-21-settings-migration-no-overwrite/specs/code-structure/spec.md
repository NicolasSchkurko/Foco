# Spec Delta

## MODIFIED Requirements

### Requirement: Reorganization preserves behavior
The reorganization SHALL NOT change observable behavior: session and metrics computations keep their results and the screens render the same information, with the existing suite passing and edited only for package moves. The settings migration SHALL NOT overwrite a value already present in the target store.
[@test] `app/src/test/java/com/foco/app/data/SettingsMigrationTest.kt`

#### Scenario: Suite green after the moves
- **WHEN** the reorganization is complete and the suite runs
- **THEN** all tests pass without behavioral edits

#### Scenario: Stored settings survive the storage split
- **WHEN** the app upgrades with a language and an appearance already stored in the legacy file
- **THEN** the first startup copies them to the new file, clears them from the legacy one, and the UI opens with the same choices — and running the migration again changes nothing (it is idempotent)

#### Scenario: Same dashboard on device
- **WHEN** the app runs after the reorganization
- **THEN** the dashboard shows the same numbers and layout as before

#### Scenario: The migration does not clobber an existing target value
- **WHEN** the target store already holds a language or appearance
- **THEN** the migration leaves that value untouched