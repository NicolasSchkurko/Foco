# Spec Delta

## ADDED Requirements

### Requirement: The boundary check derives its feature folders from the tree
The cross-feature boundary check SHALL derive the UI feature folders from the source tree — every folder under `ui/` except the shared ones (the `ui` root, `ui/common/`, `ui/metrics/`, `ui/theme/`) — so that a feature folder added later falls under the rule without editing the check.
[@test] `app/src/test/java/com/foco/app/structure/LayerRulesTest.kt`

#### Scenario: A feature folder added after the check was written
- **WHEN** a new folder appears under `ui/` and one of its files imports another feature's package
- **THEN** the boundary check fails and names the file, with no change to the check itself

#### Scenario: A shared folder is not treated as a feature
- **WHEN** a file under `ui/common/`, `ui/metrics/` or `ui/theme/` imports a feature's package
- **THEN** no cross-feature violation is recorded for it
