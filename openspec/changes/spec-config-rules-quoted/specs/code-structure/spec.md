# Spec Delta

## ADDED Requirements

### Requirement: Declared rules stay effective
The repository's own configuration SHALL keep its declared rules effective: a `rules:` item that the tooling would silently drop (an unquoted scalar that YAML reads as a mapping) SHALL fail a check that names the file, the line and the item.
[@test] `app/src/test/java/com/foco/app/structure/SpecConfigRulesTest.kt`

#### Scenario: A rules item the tool would ignore
- **WHEN** an item under `rules.<artifact>` starts unquoted and contains `: ` (so YAML reads it as a mapping)
- **THEN** the check fails and names the file, the line and the item

#### Scenario: Every declared rule reaches the tooling
- **WHEN** every item under `rules.<artifact>` is a quoted scalar
- **THEN** the check passes, and creating a change no longer reports that the artifact's rules are being ignored

#### Scenario: The rule text is untouched
- **WHEN** the quoting is added
- **THEN** the wording of every rule is the same as before — including the tech-brief rule, which keeps its colon
