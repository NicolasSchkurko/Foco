# Spec Delta

## MODIFIED Requirements

### Requirement: Only user-facing apps count
The system SHALL keep only sessions whose package has a launcher icon, excluding the device home app and Foco itself, by filtering over an allowed/excluded package set.
[@test] `app/src/test/java/com/foco/app/domain/SessionFilterTest.kt`

#### Scenario: Internal packages are dropped
- **WHEN** sessions include a package with no launcher icon
- **THEN** it is not present in the returned sessions

#### Scenario: Home and self are dropped
- **WHEN** sessions include the home package or Foco's own package
- **THEN** neither is present in the returned sessions

#### Scenario: A package becomes launchable while Foco runs
- **WHEN** an app is installed or enabled after the filtering set was built, while the monitor is running
- **THEN** subsequent checks count its sessions and enforce its limit — the set is re-read, not frozen from service start
