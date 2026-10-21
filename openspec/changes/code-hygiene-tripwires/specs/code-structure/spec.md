# Spec Delta

## MODIFIED Requirements

### Requirement: Source files are UTF-8 and contain no mojibake
All source and documentation files SHALL be stored as UTF-8 (no BOM). String literals and comments SHALL NOT contain replacement characters (U+FFFD) or other mojibake; glyphs needed by the UI SHALL be provided through resources or drawables, not through broken literals. A change that introduces a replacement character in any source or tracked documentation file SHALL fail the quality gate.
[@test] `app/src/test/java/com/foco/app/structure/SourceEncodingTest.kt`

#### Scenario: Clean source
- **WHEN** the repository is scanned for U+FFFD across `app/src` and every tracked Markdown file, including the documents under `openspec/`
- **THEN** no occurrence is found

#### Scenario: Mojibake introduced
- **WHEN** a commit adds a replacement character to a `.kt` file or to a tracked Markdown document
- **THEN** the quality gate fails
