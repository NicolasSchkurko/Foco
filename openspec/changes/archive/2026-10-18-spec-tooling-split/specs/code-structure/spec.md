# Spec Delta

## REMOVED Requirements

### Requirement: A whole-tree check proves its own failure branch
**Reason**: The rule is about how the repository verifies itself — a check proving its own failure branch — not about the shape of the source tree, so it moves to `spec-tooling`.
**Migration**: Same requirement text and the same `[@test]` (`app/src/test/java/com/foco/app/structure/LayerRulesTest.kt`); the owner becomes `cap:spec-tooling`.

### Requirement: The boundary check derives its feature folders from the tree
**Reason**: The rule governs a check's own scope, not the shape of the tree, so it moves to `spec-tooling`.
**Migration**: Same requirement text and the same `[@test]` (`app/src/test/java/com/foco/app/structure/LayerRulesTest.kt`); the owner becomes `cap:spec-tooling`.

### Requirement: JVM tests are portable across development platforms
**Reason**: The rule governs the test suite's portability, not the shape of the tree, so it moves to `spec-tooling`.
**Migration**: Same requirement text and the same `[@test]` (`app/src/test/java/com/foco/app/data/SettingsMigrationTest.kt`); the owner becomes `cap:spec-tooling`.

### Requirement: Declared rules stay effective
**Reason**: The rule governs the tooling's own configuration, not the shape of the tree, so it moves to `spec-tooling`.
**Migration**: Same requirement text and the same `[@test]` (`app/src/test/java/com/foco/app/structure/SpecConfigRulesTest.kt`); the owner becomes `cap:spec-tooling`.
