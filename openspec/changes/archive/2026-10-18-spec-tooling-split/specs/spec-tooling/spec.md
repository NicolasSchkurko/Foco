# Spec Delta

## Purpose

The repository's rules for how it verifies itself and stays honest: the tripwires (checks that prove their own failure branch and derive their own scope), the artifact budgets, the JVM suite's portability across development platforms, and the tooling configuration whose declared rules stay effective. Never application behavior, and never the shape of the source tree.

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

### Requirement: The boundary check derives its feature folders from the tree
The cross-feature boundary check SHALL derive the UI feature folders from the source tree — every folder under `ui/` except the shared ones (the `ui` root, `ui/common/`, `ui/metrics/`, `ui/theme/`) — so that a feature folder added later falls under the rule without editing the check.
[@test] `app/src/test/java/com/foco/app/structure/LayerRulesTest.kt`

#### Scenario: A feature folder added after the check was written
- **WHEN** a new folder appears under `ui/` and one of its files imports another feature's package
- **THEN** the boundary check fails and names the file, with no change to the check itself

#### Scenario: A shared folder is not treated as a feature
- **WHEN** a file under `ui/common/`, `ui/metrics/` or `ui/theme/` imports a feature's package
- **THEN** no cross-feature violation is recorded for it

### Requirement: JVM tests are portable across development platforms
The JVM unit suite SHALL be deterministic on every supported development platform: a test SHALL NOT fail because a framework path behaves differently per platform. Where such a path makes a test unrunnable on a platform, the test SHALL declare that platform exclusion explicitly and name the framework reason; a silent or incidental failure SHALL NOT stand in for it.
[@test] `app/src/test/java/com/foco/app/data/SettingsMigrationTest.kt`

#### Scenario: A framework path that only works on another platform
- **WHEN** a test exercises a framework branch that cannot succeed under the developer's platform — for example DataStore's `Build.VERSION.SDK_INT < 26` fallback, which moves the scratch file with `File.renameTo` and cannot replace an existing file on Windows
- **THEN** the test declares the excluded platform together with its framework reason, and the rest of the suite still runs

#### Scenario: The suite on Windows
- **WHEN** the JVM suite runs on Windows
- **THEN** every test passes or is skipped with a named reason, and none fails

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
