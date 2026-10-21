# Proposal

## Why

The migration copies legacy settings into the new store without checking whether the target already holds a value, so it can clobber a newer choice.

## What Changes

- Skip copying a key when the target already has it.

## Capabilities

### Modified Capabilities
- `code-structure`: migration preserves existing target values.

## Impact

- `app/src/main/java/com/foco/app/data/SettingsMigration.kt`

## Expected outcome

An existing target value survives the migration.