# Spec Delta

## ADDED Requirements

### Requirement: A whole-tree check proves its own failure branch
A check that scans the tree SHALL be callable on a given root, and its failure branch SHALL be exercised by a fixture — a synthetic tree that violates the rule — so the rule is proven to fire, not only proven silent on the real tree.
[@test] `app/src/test/java/com/foco/app/structure/LayerRulesTest.kt`

#### Scenario: A feature importing another feature
- **WHEN** the scan runs over a tree where `ui/reports/Reports.kt` imports `com.foco.app.ui.dashboard.…`
- **THEN** it reports exactly one violation, naming that file

#### Scenario: A shared folder importing a feature
- **WHEN** the scan runs over a tree where `ui/common/Widget.kt` imports `com.foco.app.ui.dashboard.…`
- **THEN** it reports no cross-feature violation for it

#### Scenario: The real tree
- **WHEN** the scan runs over `src/main/java` as it is today
- **THEN** it reports no violations — the same assertion the suite has always made
