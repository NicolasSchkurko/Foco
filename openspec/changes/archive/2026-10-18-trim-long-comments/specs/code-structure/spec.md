# Spec Delta

## ADDED Requirements

### Requirement: Code comments stay within a four-line budget
A comment block under `app/src/` SHALL be at most four lines. A longer explanation SHALL live in `docs/mechanisms/`, a walkthrough or an ADR, with a one-line pointer where it was removed.
[@test] `app/src/test/java/com/foco/app/structure/ShortCommentsTest.kt`
[@test] `app/src/test/java/com/foco/app/structure/LayerRulesTest.kt`
[@test] `app/src/androidTest/java/com/foco/app/ui/SettingsScreenTest.kt`

#### Scenario: A comment block over the budget
- **WHEN** a comment block under `app/src/` has more than four lines
- **THEN** the check fails and names the file, the line, the block's length and its first line

#### Scenario: The header the constitution asks for
- **WHEN** a file touches the OS, storage or network
- **THEN** its header names the physical resource, where it lives and its lifecycle, inside the four-line budget

#### Scenario: An explanation worth keeping
- **WHEN** the text being removed explains a why — a debugging note or a mechanism
- **THEN** it moves to `docs/mechanisms/`, the walkthrough or an ADR, and the code keeps a one-line pointer to it

#### Scenario: The sweep changes no behavior
- **WHEN** the sweep is complete and the suite runs
- **THEN** every test passes without behavioral edits — the tree, the domain results and the rendered text are the same as before
