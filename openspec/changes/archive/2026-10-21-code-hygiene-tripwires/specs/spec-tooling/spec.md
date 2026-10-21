# Spec Delta

## MODIFIED Requirements

### Requirement: Repository content is free of mojibake
The quality gate SHALL scan `app/src` and every tracked Markdown file in the repository, including the documents under `openspec/`, for U+FFFD and fail when any occurrence is found. The gate SHALL report the file and line of each occurrence.
[@test] `app/src/test/java/com/foco/app/structure/SourceEncodingTest.kt`

#### Scenario: Clean tree
- **WHEN** no replacement character exists
- **THEN** the gate passes

#### Scenario: Mojibake present
- **WHEN** a replacement character is found
- **THEN** the gate fails with the file and line

#### Scenario: A document outside `app/src`
- **WHEN** a Markdown file under `openspec/` carries a replacement character
- **THEN** the gate fails naming that file and line, which the previous top-level-only scan could not see
