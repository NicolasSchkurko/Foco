# Spec Delta

## ADDED Requirements

### Requirement: JVM tests are portable across development platforms
The JVM unit suite SHALL be deterministic on every supported development platform: a test SHALL NOT fail because a framework path behaves differently per platform. Where such a path makes a test unrunnable on a platform, the test SHALL declare that platform exclusion explicitly and name the framework reason; a silent or incidental failure SHALL NOT stand in for it.
[@test] `app/src/test/java/com/foco/app/data/SettingsMigrationTest.kt`

#### Scenario: A framework path that only works on another platform
- **WHEN** a test exercises a framework branch that cannot succeed under the developer's platform — for example DataStore's `Build.VERSION.SDK_INT < 26` fallback, which moves the scratch file with `File.renameTo` and cannot replace an existing file on Windows
- **THEN** the test declares the excluded platform together with its framework reason, and the rest of the suite still runs

#### Scenario: The suite on Windows
- **WHEN** the JVM suite runs on Windows
- **THEN** every test passes or is skipped with a named reason, and none fails
