# Spec Delta

## ADDED Requirements

### Requirement: The week can be exported as formatted JSON
When the home is on the week position, it SHALL offer an export action for a UTF-8, pretty-printed JSON document covering the same rolling seven-day window: today live and the previous six stored days. The document SHALL contain `weekStart`, `weekEnd`, and one `days` entry per day, oldest first.
[@test] `app/src/test/java/com/foco/app/domain/WeekExportTest.kt`
[@test] `app/src/androidTest/java/com/foco/app/ui/WeekScreenTest.kt`

#### Scenario: Seven days, including empty ones
- **WHEN** the user exports the week and one day has no activity
- **THEN** the JSON has seven `days` entries, oldest first, and that day has `totalMillis` 0 and `apps: []`

#### Scenario: Today comes live
- **WHEN** today has live sessions
- **THEN** the today entry contains them, while the previous six entries come from stored history

### Requirement: Each app and session is broken down
Each day entry SHALL contain `totalMillis` and `apps`; each app SHALL contain `packageName`, the resolved `appName`, its day `totalMillis`, `sessionsAvailable`, and its `sessions`; each session SHALL contain `start` and `end` as ISO-8601 strings with the local offset and `durationMillis`. An app's total SHALL be the sum of its sessions; a day's total SHALL be the sum of its app totals. Sessions SHALL be ordered oldest first.
[@test] `app/src/test/java/com/foco/app/domain/WeekExportTest.kt`

#### Scenario: App total and sessions
- **WHEN** an app has two sessions in a day
- **THEN** its `totalMillis` is their sum and `sessions` has both, oldest first, each with offset timestamps and its millisecond duration

#### Scenario: Stored total without raw rows
- **WHEN** a day has a stored total for an app but no raw session rows
- **THEN** the app is present with the stored total, `sessionsAvailable: false` and `sessions: []`

#### Scenario: Raw records, not the ranking
- **WHEN** a session lasts less than 1 s or belongs to an app excluded from the ranking
- **THEN** it is still present in the JSON; the export does not apply the screen's visibility or exclusion filters

#### Scenario: A name that needs escaping
- **WHEN** an app label contains a quote, a backslash or a control character
- **THEN** the emitted JSON escapes it and remains valid JSON

### Requirement: The export is shared, not uploaded
The export SHALL be written as a transient file in the app's cache and offered through the Android share sheet as `application/json` using a read-only `FileProvider` URI. It SHALL NOT request storage permissions, use the network, or change `allowBackup`; a failed read SHALL share no partial file and SHALL show an export error.
[@test] `app/src/androidTest/java/com/foco/app/ui/dashboard/WeekExportShareTest.kt`
[@test] `app/src/androidTest/java/com/foco/app/ui/WeekScreenTest.kt`

#### Scenario: Share intent
- **WHEN** the export is ready
- **THEN** the share intent has `ACTION_SEND`, type `application/json`, a `content://` stream and the read-grant flag

#### Scenario: A read fails
- **WHEN** a history or live read fails during export
- **THEN** no file is shared and the week position shows the export error
