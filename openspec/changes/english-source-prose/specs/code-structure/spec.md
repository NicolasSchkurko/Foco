# Spec Delta

## ADDED Requirements

### Requirement: Source prose is English
Comments, KDoc and test failure messages under `app/src/` SHALL be English; Spanish SHALL appear only as data — a string value or a selector for the Spanish UI — never as prose, and a machine check SHALL enforce it.
[@test] `app/src/test/java/com/foco/app/structure/SourceProseTest.kt`
[@test] `app/src/test/java/com/foco/app/structure/LayerRulesTest.kt`
[@test] `app/src/androidTest/java/com/foco/app/ui/SettingsScreenTest.kt`

#### Scenario: Spanish prose lands in the source
- **WHEN** a comment or KDoc line contains a Spanish marker (e.g. `código`, `cuando`, `para que`)
- **THEN** the check fails and names the file, the line and the marker

#### Scenario: Spanish that is data, not prose
- **WHEN** a test compares against `"Ajustes"` or `"Idioma"` to find the Spanish screen, or a string value is Spanish in a `values-*` resource
- **THEN** the check does not flag it (it inspects comments only)

#### Scenario: A Spanish phrase the marker list misses
- **WHEN** real Spanish prose slips past the markers
- **THEN** the marker list is extended and the check re-runs; the check is never weakened or skipped to make it pass

#### Scenario: The sweep changes no behavior
- **WHEN** the prose sweep is complete and the suite runs
- **THEN** every test passes without behavioral edits — the tree, the layers, the domain results and the rendered text are the same as before
