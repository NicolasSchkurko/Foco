# Spec Delta

## ADDED Requirements

### Requirement: The committed schema cannot drift silently
The app SHALL verify that the database's declared version has a committed exported schema and that its committed identity hash matches the one the open database reports, so an entity edit without a version bump, or a bump without an export, fails the build.
[@test] `app/src/test/java/com/foco/app/data/db/SchemaDriftTest.kt`
[@test] `app/src/androidTest/java/com/foco/app/data/db/SchemaBaselineTest.kt`

#### Scenario: An entity edited without a version bump
- **WHEN** the committed schema of the declared version carries a different identity hash than the open database reports
- **THEN** the verification reports drift and names the version

#### Scenario: A version bumped without an exported schema
- **WHEN** the declared version has no committed schema file
- **THEN** the verification reports the missing export

#### Scenario: The baseline is consistent
- **WHEN** the declared version has its committed schema and both identity hashes agree
- **THEN** the verification reports no drift

### Requirement: Schema changes never wipe the stored history
The app SHALL NOT use destructive migrations — no `fallbackToDestructiveMigration` call in the production sources — and every version bump SHALL ship a migration while the previous schema stays committed.
[@test] `app/src/test/java/com/foco/app/data/db/NoDestructiveMigrationTest.kt`

#### Scenario: No destructive fallback
- **WHEN** the production sources are scanned
- **THEN** no call to `fallbackToDestructiveMigration` exists

#### Scenario: The check bites
- **WHEN** the scan runs against a source tree that contains the call
- **THEN** it fails and names the file
