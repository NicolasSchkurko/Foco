# Spec Delta — code-structure

## MODIFIED Requirements

### Requirement: Source files are UTF-8 and contain no mojibake
All source and documentation files SHALL be stored as UTF-8 (no BOM). String literals and comments
SHALL NOT contain replacement characters (U+FFFD) or other mojibake; glyphs needed by the UI SHALL be
provided through resources or drawables, not through broken literals. A change that introduces a
replacement character in `app/src/**` or `*.md` SHALL fail the quality gate.
[@test] `app/src/test/java/com/foco/app/structure/SourceEncodingTest.kt`

#### Scenario: Clean source
- **WHEN** the repository is scanned for U+FFFD in `app/src` and top-level `*.md`
- **THEN** no occurrence is found

#### Scenario: Mojibake introduced
- **WHEN** a commit adds a replacement character to a `.kt` file
- **THEN** the quality gate fails

### Requirement: UI screens do not own navigation history as serialized text
A screen that maintains a back stack SHALL represent it as a typed list (`List<DayStop>` or equivalent)
in a testable state holder, not as a joined/parsed `String`. The state holder SHALL expose `moveTo`,
`moveStep` and `goBack` and SHALL be instantiable without Android or Compose, so its transitions can be
unit-tested. The composable SHALL be a thin view over that holder.
[@test] `app/src/test/java/com/foco/app/ui/dashboard/DashboardStateTest.kt`

#### Scenario: Push and pop
- **WHEN** `moveTo` is called and then `goBack`
- **THEN** the previous stop is restored and the history shrinks by one

#### Scenario: Ring bounds
- **WHEN** `moveTo` targets a stop outside the ring
- **THEN** the state is unchanged

#### Scenario: Direction
- **WHEN** `moveStep(forward = true)` is called
- **THEN** `slideDirection` is positive and `softMotion` is true

### Requirement: Build fails on warnings and runs static analysis
The module SHALL compile with `warningsAsErrors = true` for main and unit-test sources, and the
repository SHALL run a static analysis tool (Detekt or ktlint) in CI. A warning or a lint violation
SHALL fail the build.
[@test] `app/src/test/java/com/foco/app/structure/BuildHygieneTest.kt`

#### Scenario: Warning present
- **WHEN** a deprecated API is used without suppression
- **THEN** compilation fails

#### Scenario: Lint in CI
- **WHEN** a pull request is opened
- **THEN** the CI job runs the lint task and fails on violations