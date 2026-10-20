# Proposal — weekly-json-export

## Why

The home's week position already reads the rolling seven-day window — today live from the platform and the previous six days from the store — but those records cannot leave the app. The user wants a formatted JSON file grouped by day, app and session to open on a computer.

## What

Add an **Export JSON** action to the home's week position, in the header while the Week stop is showing, for the same seven-day window. The document is:

```json
{
  "weekStart": "2026-10-13",
  "weekEnd": "2026-10-19",
  "days": [
    {
      "date": "2026-10-19",
      "totalMillis": 1234,
      "apps": [
        {
          "packageName": "com.example.app",
          "appName": "Example",
          "totalMillis": 1234,
          "sessionsAvailable": true,
          "sessions": [
            {
              "start": "2026-10-19T09:00:00+02:00",
              "end": "2026-10-19T09:00:01.234+02:00",
              "durationMillis": 1234
            }
          ]
        }
      ]
    }
  ]
}
```

- All seven days are present; a day without activity has `"apps": []`.
- Sessions use the normalized records: contiguous same-app sessions that meet at the same `HH:mm` are merged, while standalone sub-second sessions are kept. The export does **not** apply the screen's 1 s visibility rule or the excluded-apps preference; it is a record, not the week ranking.
- If a day has a stored total for an app but its raw rows are gone, the app keeps its stored total with `"sessionsAvailable": false` and `"sessions": []`.
- Timestamps are ISO-8601 with the local offset; durations are milliseconds. The JSON is pretty-printed with two spaces.

Delivery: write a transient UTF-8 file `foco-week-<weekEnd>.json` under `cacheDir/week-exports/`, expose it read-only through a `FileProvider`, and open the Android share sheet (`ACTION_SEND`, `application/json`). No storage permission, no network, no backup change, no database change.

## Expected outcome

The week position offers **Export JSON**. The shared file opens in a JSON viewer on a computer and contains all seven days, each app's day total, and every session with its start, end and duration. A read failure shows an error and shares no partial file.

## State and persistence mechanism

No new persistent store. The exporter reads the existing `daily_aggregate` rows (`SessionHistoryStore.week`), the raw `session_record` rows (`sessionsFor`), and today's live `UsageRepository.loadDay`; names come from `AppPackageInfo`. The only new state is a transient cache file under `cacheDir/week-exports/`, shared read-only through `FileProvider` and evictable by the system. `allowBackup="false"` and the no-network posture stay unchanged.

## Alternatives considered

### Delivery (the human chose the share sheet)

| Option | Why yes | Why not |
| --- | --- | --- |
| Share sheet + `FileProvider` + cache file | No storage permission; the user picks the target; the file is a real `application/json` attachment | Needs one provider entry and a paths resource; the cache file is transient |
| SAF `ACTION_CREATE_DOCUMENT` | Persistent file chosen by the user; no provider | Different UX from the chosen share flow; the user must pick a folder |
| Share as `EXTRA_TEXT` | No file or provider | Recipients get text, not a `.json` file |
| Clipboard | No file | Not an export; loses the file contract |

### Serialization (the human chose `kotlinx.serialization`)

| Option | Why yes | Why not |
| --- | --- | --- |
| `kotlinx.serialization` (chosen) | Idiomatic, type-safe; the BOM is already pinned at 1.8.1; JVM tests can encode and decode the same model | Adds the compiler plugin and the JSON runtime artifact; touches `app/build.gradle.kts` and `gradle/libs.versions.toml`, claimed by other changes, so this change is sequenced after them |
| Small pure JSON builder in `domain/` | No new dependency; full control of field order and indentation | Manual string escaping must be tested; rejected after the human chose the library |
| `org.json` | Built into Android | JVM tests cannot use it without a test dependency or Robolectric; still needs manual pretty-printing |

The dependency is `org.jetbrains.kotlinx:kotlinx-serialization-json` (version from the existing 1.8.1 BOM) with `org.jetbrains.kotlin.plugin.serialization` 2.4.21. The export model carries ISO-8601 strings, so `@Serializable` needs no custom date serializer.

### Data source

| Option | Why yes | Why not |
| --- | --- | --- |
| Raw sessions + stored-total fallback (chosen) | Preserves per-session detail and does not lose an app whose raw rows were pruned | Needs `sessionsAvailable` to be honest about the fallback |
| Week aggregates only | Reuses `week(until)` directly | No session durations or timestamps |
| Re-read the platform for all seven days | One source | The platform may no longer serve older days; the store exists for that |

## Prior decisions

No conflict with `usage-tracking/spec.md:245-251`: `allowBackup="false"` forbids backup/restore, not a user-initiated share. The archived “no export” non-goal is not a living requirement. The rolling seven-day window from the archived week-screen change is preserved.
