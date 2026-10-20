# Spec Delta — spec-tooling

## ADDED Requirements

### Requirement: Repository content is free of mojibake
The quality gate SHALL scan `app/src` and top-level `*.md` for U+FFFD and fail when any occurrence is
found. The gate SHALL report the file and line of each occurrence.
[@test] `app/src/test/java/com/foco/app/structure/SourceEncodingTest.kt`

#### Scenario: Clean tree
- **WHEN** no replacement character exists
- **THEN** the gate passes

#### Scenario: Mojibake present
- **WHEN** a replacement character is found
- **THEN** the gate fails with the file and line

### Requirement: Startup reads are bounded
`MainActivity.onCreate` SHALL not block the main thread indefinitely: the legacy-store migration and
initial settings reads SHALL run with a bounded timeout, and on timeout or failure the app SHALL fall
back to defaults and log the error instead of crashing.
[@test] `app/src/test/java/com/foco/app/MainActivityStartupTest.kt`

#### Scenario: Migration succeeds
- **WHEN** the legacy store migrates within the timeout
- **THEN** language and appearance are applied before the first frame

#### Scenario: Migration times out
- **WHEN** the migration exceeds the timeout
- **THEN** defaults are used, the error is logged, and the app still reaches the first frame