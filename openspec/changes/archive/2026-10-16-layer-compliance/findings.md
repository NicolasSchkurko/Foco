# Findings — layer-compliance

Semantic pass, 2026-10-16 (see the `/spec-verify` report on this change):

- [x] ARCHITECTURE.dsl misdescribes the applied structure: the `data` component still lists `availableDays` (and the `domain` description omits it), and the `ui -> platform` edge is still drawn — the helper now lives in `domain/` and icon/label reads go through `data/AppPackageInfo` — evidence: `ARCHITECTURE.dsl:11,13,31` vs `app/src/main/java/com/foco/app/domain/AvailableDays.kt:1`, `app/src/main/java/com/foco/app/data/AppPackageInfo.kt:12`, `app/src/main/java/com/foco/app/ui/dashboard/AppIcon.kt:31` — action: run `/diagram-sync` before `--pre-archive` (structure owner; `cap:` tags are unaffected). **Resolved 2026-10-16:** `/diagram-sync` applied (full diff — components and edges reconciled, including `nav -> platform` and `data -> storage`; `entry -> platform` added).
