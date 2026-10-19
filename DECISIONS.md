# DECISIONS

**Append-only** record of decisions (ADR-lite). It holds: bug/discrepancy triage, architecture decisions, **resolution of decision requests**, **granted/rejected waivers**, and **protected-zone approvals**. Format:

## [YYYY-MM-DD] Decision title
- **Context:** what situation forced the decision
- **Decision:** what was chosen and why
- **Consequences:** what it implies / what remains pending
- **Refs:** file:line · specs · changes

---

## [2026-10-16] Spec Growth INIT — installation deviations on Windows

- **Context:** Spec Growth was initialized in this repo. The procedure assumes the `opencode mcp add` CLI (not installed on this machine: only the desktop app exists) and `npx codebase-memory-mcp@<version>` (broken on Windows in v0.12.0: its npm installer rejects the native `.exe` for exceeding its own 256 MiB limit — the binary weighs ~318 MiB).
- **Decision:** (1) MCPs wired by editing `~/.config/opencode/opencode.json` directly (V2 format, `mcp.servers`), with a file backup. (2) `codebase-memory` installed through the official native path (`install.ps1 --skip-config`, release v0.12.0 verified by checksum) into `%LOCALAPPDATA%\Programs\codebase-memory-mcp\`; the MCP points to that `.exe` by absolute path. (3) `structurizr` as a remote MCP (`https://mcp.structurizr.com/mcp`). (4) `openspec init --tools opencode --language spanish`; own `spec-anchored` schema forked and patched per the skill.
- **Consequences:** Updating codebase-memory = re-running `%LOCALAPPDATA%\Programs\codebase-memory-mcp\install.ps1`. The canonical `npx -y codebase-memory-mcp@<v>` command is unusable on this machine until the npm package fixes its extraction. Tools installed in the session: Node.js 24.20.0 LTS (winget) and OpenSpec 1.14.2 (npm, pinned).
- **Refs:** `~/.config/opencode/opencode.json` · `openspec/config.yaml` · `openspec/schemas/spec-anchored/schema.yaml` · `AGENTS.md`

## [2026-10-16] Workspace sanitization — repo into `Foco/`, tooling outside

- **Context:** The repo had the Android code, the Spec Growth system and the OpenCode tooling (`.opencode/`) mixed at the same root. It also carried ~530 MB of regenerable artifacts (builds, `.idea`, codebase-memory install temp).
- **Decision:** (1) The full repo (including `.git`) lives in `Foco/`; the workspace is a container without git that hosts `.opencode/` (outside the repo, on purpose). (2) `spec-*` and `diagram-sync` commands adapted: they assume cwd in `Foco/`. (3) The constitution (`Foco/AGENTS.md`) is loaded in the container via `opencode.json` → `instructions: ["Foco/AGENTS.md"]`; there is a short guide in the container's `AGENTS.md`. (4) `app/build`, `build/`, `.gradle`, `.idea` and the install temp were deleted; `.kotlin/` was added to `.gitignore`. (5) OpenSpec's own commands/skills (`opsx-*`, `openspec-*`) were deleted: Spec Growth does not use them; they can be regenerated with `openspec update`.
- **Consequences:** Android Studio must be reopened at `Foco/` inside the container (it lost `.idea`). OpenCode sessions open in the container; the commands do `cd Foco`. The gate and the pre-commit hook live in the repo and keep working with cwd = repo root. The user's pending changes (`gradle.properties`, `gradle/gradle-daemon-jvm.properties`) stayed untouched and were committed separately.
- **Refs:** `Foco/` inside the container · `.opencode/commands/` · `opencode.json` (container) · `Foco/AGENTS.md`

## [2026-10-16] Repository docs moved to English

- **Context:** The repo is a development experiment with the Spec Growth method, pushed to GitHub. Its documentation was written in Spanish.
- **Decision:** README, PLAN, AGENTS.md, DECISIONS.md, the OpenSpec config (`Language: english` — future artifacts in English) and the `spec-*` commands were translated to English. Kotlin code and gate scripts stay as they are (Spanish comments and output).
- **Consequences:** New OpenSpec artifacts will be written in English. `BOARD.md` keeps generating Spanish section titles (produced by the gate scripts, left untouched by decision).
- **Refs:** `README.md` · `PLAN.md` · `AGENTS.md` · `DECISIONS.md` · `openspec/config.yaml` · `.opencode/commands/`

## [2026-10-16] Triage: README vs the real Compose BOM (2026.06.01)

- **Context:** The README claimed "Compose BOM 2026.09" while the project resolves `composeBom = "2026.06.01"` through the version catalog (`gradle/libs.versions.toml:7` → `app/build.gradle.kts:41`). First triage of the repo — no living specs yet, so the discrepancy was between documentation and configuration.
- **Decision (human):** the version catalog is the source of truth; the documentation was stale. The README is corrected to `2026.06` (the doc's own major.minor style).
- **Consequences:** documentation fix, no behavior change → no OpenSpec change and no `[@test]`. Rule applied: build files rule over docs. The unselected candidate (README says "Gradle 9.6" vs wrapper `9.6.1`) was left as is by decision.
- **Refs:** `README.md:35` · `gradle/libs.versions.toml:7` · `app/build.gradle.kts:41`

## [2026-10-16] openspec 1.14 flags bare custom keys — `x-` prefix adopted, gate accepts both

- **Context:** Creating the first change (`session-based-metrics`), `openspec validate --strict` failed: the custom `.openspec.yaml` keys (`kind`, `targets`, `tests`, `deps`, `gitref`, …) are flagged as unrecognized; openspec recommends the `x-<key>` form. The Spec Growth scripts (v0.8 assets) read only the bare keys.
- **Decision:** adopt `x-<key>` as the canonical form in `.openspec.yaml` and patch the gate to accept both: `scripts/gates/lib.mjs` (readChange normalizes `x-` → bare) and `scripts/gates/compact-archive.mjs` (kindOf matches `x-kind`). `AGENTS.md` rule 2, the `spec-anchored` schema instruction and the `spec-add` command updated to match.
- **Consequences:** `openspec validate --all --strict` passes clean while the gate keeps working. This is a documented divergence from the skill's stock scripts: if a future skill update ships native `x-` support, resync the scripts.
- **Refs:** `openspec/changes/session-based-metrics/.openspec.yaml` · `scripts/gates/lib.mjs` · `scripts/gates/compact-archive.mjs` · `AGENTS.md` · `openspec/schemas/spec-anchored/schema.yaml`

## [2026-10-16] Gate: dep-covered overlaps are warnings; glob-aware target existence

- **Context:** Applying `session-based-metrics` turned the gate red with 6 overlap errors: while a change is `applying`, the gate escalates any target overlap with another live change to error. `dashboard-auto-refresh` (draft) overlaps on purpose — both changes touch `ui/DashboardScreen.kt` and the build files — and its sequencing plan (declared `x-deps` on `session-based-metrics`) is exactly what the spec-add flow sanctions for target contention ("dependency or sequence"), but gate v0.8 (skill asset and repo copy) ignores `deps` for this check. The same run also flagged three directory-glob targets (`…/data/**`, `…/ui/**`, `app/src/test/**`) as "target no existe en disco" — a false positive: `existsSync` on a literal `**` path can never match.
- **Decision (human):** (1) In the repo's `gate-check.mjs`, an overlap between two changes with a declared `deps` in either direction is reported as a warning ("secuenciado por dep"), not an error; the ordering guarantee stays with check 13 (a change cannot reach `verified` while its dep is not verified/archived). (2) The target-existence check strips everything from the first `*` before `existsSync`; patterns starting with `*` are skipped as uncheckable.
- **Consequences:** Overlaps without a declared dep still error while work is active. Only the repo copy changes; the skill's global asset remains v0.8 — resync if upstream adopts an equivalent fix.
- **Refs:** `scripts/gates/gate-check.mjs` · `openspec/changes/dashboard-auto-refresh/.openspec.yaml` (`x-deps`) · `openspec/changes/session-based-metrics/.openspec.yaml` (`x-targets`)

## [2026-10-16] Change approved: `dashboard-auto-refresh`

- **Context:** Created on 2026-10-16, `dashboard-auto-refresh` (feature: fresh dashboard data on every app open — reload on return to foreground, periodic refresh while visible, manual "Actualizar" button removed) awaited human approval to leave `draft`.
- **Decision (human):** approved. The change moves to `accepted` (`x-accepted_by: Nschkurko`, `x-accepted_at: 2026-10-16`) and is ready for `/spec-apply`. No protected zones are involved.
- **Consequences:** Applying it requires its `x-deps` (`session-based-metrics`) archived or verified — it is already `verified` (archive pending). Overlaps with `layered-structure` and `app-usage-detail` stay informational (no ordering declared; whichever applies second adapts).
- **Refs:** `openspec/changes/dashboard-auto-refresh/.openspec.yaml` · `openspec/changes/dashboard-auto-refresh/proposal.md`

## [2026-10-16] Ordering: the other drafts depend on `dashboard-auto-refresh`

- **Context:** Starting the apply of `dashboard-auto-refresh` (status → `applying`) escalated the gate to **15 overlap errors**: its targets (`ui/**`, `app/src/androidTest/**`, `app/build.gradle.kts`, `gradle/libs.versions.toml`) collide with the two live drafts (`layered-structure`, `app-usage-detail`), which had been declared independent of each other and of it. The escalation rule — any overlap with an `applying` change is an error unless sequenced by a declared dep — is by design.
- **Decision (human):** option A — the drafts declare the dependency on the change that goes first: `layered-structure` and `app-usage-detail` add `dashboard-auto-refresh` to their `x-deps` (alongside `session-based-metrics`). Rationale: `dashboard-auto-refresh` is applied now, so "it first, the others after" is a fact; the dep is satisfied as soon as it reaches `verified` (archive not required by the gate).
- **Consequences:** Their overlaps with it report as warnings ("secuenciado por dep") and the gate stays green during the apply. No rule weakening: the repo gate keeps its current semantics. If the human later wants to apply `layered-structure` or `app-usage-detail` before `dashboard-auto-refresh` finishes, these deps must be revisited (new decision, append-only).
- **Refs:** `openspec/changes/layered-structure/.openspec.yaml` (`x-deps`) · `openspec/changes/app-usage-detail/.openspec.yaml` (`x-deps`) · `openspec/changes/dashboard-auto-refresh/.openspec.yaml`

## [2026-10-16] Reversal: gate scripts follow upstream v0.9 (English)

- **Context:** upstream Spec Growth now ships natively the two fixes this repo had patched locally (`x-` metadata support for OpenSpec 1.14 strict validation, and the glob-aware target-existence check), plus report-only `--scope`, conflicts, `refactor` kind and untracked-zone detection. The earlier decision "[2026-10-16] Repository docs moved to English" had left the gate scripts in Spanish; maintaining a divergent fork no longer has any benefit.
- **Decision (human):** revert that part of the decision. `scripts/gates/*` are replaced by upstream v0.9 (English output, dependency-free). The `x-` divergence is not a divergence anymore.
- **Consequences:** gate and BOARD messages print in English; future upstream updates apply to this repo without patching. Local resync flow stays: copy `assets/scripts/*` from the skill.
- **Refs:** `scripts/gates/*` (commit 377e5bd) · `DECISIONS.md` [2026-10-16 docs to English] · [2026-10-16 x- prefix]

## [2026-10-16] Test emulator: API ≤ 36 (Espresso breaks on API 37)

- **Context:** During the `dashboard-auto-refresh` apply, `connectedDebugAndroidTest` on the only available emulator (API 37 / Android 17) failed all 4 tests before executing any logic: Espresso's event injection reflects on `android.hardware.input.InputManager.getInstance`, a hidden method removed in Android 17. It is a known upstream issue (community workaround: API ≤ 36 emulators), unrelated to the change.
- **Decision (human):** option A — download the API 36 (Android 16) system image and create a dedicated test AVD (`Medium_Phone_API_36.0`) instead of deferring the tests with a `deferred-test` waiver. Rationale: proper fix, consistent with the project's stable `compileSdk 36`, and instrumented testing stays viable going forward.
- **Consequences:** Instrumented runs must target API ≤ 36 until Espresso supports API 37 (`Medium_Phone_API_36.0` is the repo's test AVD; noted in `dashboard-auto-refresh/tasks.md`). The API 37 AVD stays for manual smoke checks. All 4 instrumented tests pass on API 36.
- **Refs:** `app/src/androidTest/java/com/foco/app/ui/DashboardRefreshTest.kt` · `openspec/changes/dashboard-auto-refresh/tasks.md`

## [2026-10-16] Change approved: `layered-structure`

- **Context:** Created on 2026-10-16 as a draft: four-layer source layout (`model/` · `domain/` · `data/` · `ui/`), one type per file enforced by structure tests, zero behavior change. Its deps (`session-based-metrics`, `dashboard-auto-refresh`) are archived. The semantic pass re-verified its premises against the current code (still exactly `data/Sessions.kt` and `ui/metrics/UsageMetrics.kt` break one-type-per-file; no findings).
- **Decision (human):** approved. The change moves to `accepted` (`x-accepted_by: Nschkurko`, `x-accepted_at: 2026-10-16`) and is ready for `/spec-apply`. No protected zones are involved.
- **Consequences:** Its apply is unblocked (deps archived). It overlaps `app-usage-detail` (both drafts, independent): whichever applies first, the second adapts. After its apply, `/diagram-sync` creates `ARCHITECTURE.dsl`, which does not exist yet.
- **Refs:** `openspec/changes/layered-structure/.openspec.yaml` · `openspec/changes/layered-structure/design.md`

## [2026-10-16] Ordering: `app-usage-detail` depends on `layered-structure`

- **Context:** Starting the apply of `layered-structure` (status → `applying`) escalated the gate to 4 overlap errors with `app-usage-detail` (draft): their targets collide (`app/src/**` vs `ui/**`, `data/**`, `app/src/test/**`, `app/src/androidTest/**`) and no dependency was declared between them (both had been declared independent of each other).
- **Decision (human):** option A — `app-usage-detail` adds `layered-structure` to its `x-deps` (alongside the already-archived `session-based-metrics` and `dashboard-auto-refresh`). Rationale: `layered-structure` is applied now, so "it first, `app-usage-detail` after" is a fact; the dep is satisfied when it reaches `verified`. Same pattern as the earlier `dashboard-auto-refresh` ordering decision.
- **Consequences:** The overlaps report as "sequenced by deps" warnings and the gate stays green during the apply. `app-usage-detail` later lands on the four-layer layout (its findings.md already lists the housekeeping for its own apply).
- **Refs:** `openspec/changes/app-usage-detail/.openspec.yaml` (`x-deps`) · `openspec/changes/layered-structure/.openspec.yaml` · DECISIONS.md [2026-10-16 Ordering: the other drafts depend on `dashboard-auto-refresh`]

## [2026-10-16] Change approved: `app-usage-detail`

- **Context:** Created on 2026-10-16: per-app detail screen (hourly breakdown, day summary, session list) with a ‹ day › selector within system retention, on Navigation Compose; head of the current chain (`ranking-exclusions` and `app-settings` depend on it). Its deps (`session-based-metrics`, `dashboard-auto-refresh`, `layered-structure`) are archived. The semantic pass left two low-impact housekeeping items in its `findings.md` (stale line ref; test infra already landed).
- **Decision (human):** approved. The change moves to `accepted` (`x-accepted_by: Nschkurko`, `x-accepted_at: 2026-10-16`) and is ready for `/spec-apply`. No protected zones are involved.
- **Consequences:** Its apply opens the chain (`ranking-exclusions` → `app-settings` land after it). Overlaps with `app-entry-gate` stay informational; the two findings.md items should be addressed during the apply.
- **Refs:** `openspec/changes/app-usage-detail/.openspec.yaml` · `openspec/changes/app-usage-detail/findings.md`

## [2026-10-16] Ordering: `app-entry-gate` depends on `app-usage-detail`

- **Context:** Starting the apply of `app-usage-detail` (status → `applying`) escalated the gate to 2 overlap errors with `app-entry-gate` (draft): they collide on `App.kt` and `app/src/androidTest/**`, with no dependency between them (`app-entry-gate` had been declared independent).
- **Decision (human):** option A — `app-entry-gate` adds `app-usage-detail` to its `x-deps`. Rationale: `app-usage-detail` is applied now, so "it first, `app-entry-gate` after" is a fact; the dep is satisfied when it reaches `verified`. Same pattern as the two earlier ordering decisions (`dashboard-auto-refresh`, `layered-structure`).
- **Consequences:** The overlaps report as "sequenced by deps" warnings and the gate stays green during the apply. The whole in-flight queue is now ordered behind `app-usage-detail`.
- **Refs:** `openspec/changes/app-entry-gate/.openspec.yaml` (`x-deps`) · `openspec/changes/app-usage-detail/.openspec.yaml` · DECISIONS.md [2026-10-16 Ordering: `app-usage-detail` depends on `layered-structure`]

## [2026-10-16] Ordering: `app-entry-gate` queued behind the whole draft chain

- **Context:** Starting the apply of `ranking-exclusions` (status → `applying`) would escalate the gate again over `app-entry-gate` (draft), which overlaps on `App.kt` and `app/src/androidTest/**`; the same collision was going to repeat when `app-settings` applies (it also touches `App.kt` and the androidTest tree). Fourth occurrence of the pattern (`dashboard-auto-refresh`, `layered-structure`, `app-usage-detail`, `ranking-exclusions`).
- **Decision (human):** option A+ — `app-entry-gate` adds `ranking-exclusions` and `app-settings` to its `x-deps` in one go, encoding it **last** in the queue. Rationale: it was already the de-facto plan; one declaration removes this destrabe and the one that would arrive with `app-settings`' apply.
- **Consequences:** `app-entry-gate` formally waits for the whole chain (dep warnings until each change is verified/archived). No further ordering destrabes are expected for the current queue.
- **Refs:** `openspec/changes/app-entry-gate/.openspec.yaml` (`x-deps`) · `openspec/changes/ranking-exclusions/.openspec.yaml` · `openspec/changes/app-settings/.openspec.yaml`

## [2026-10-16] Ordering: `app-settings` lands after `detail-back-arrow` (pre-declared)

- **Context:** `/spec-verify` surfaced a user report: the detail screen has no visible way back (`KEYCODE_BACK` works; the affordance is missing). The new small change `detail-back-arrow` touches `AppDetailScreen.kt` (inside `app-settings`'s `ui/**` target) and `androidTest/**`, so the two would collide when the arrow applies. Under the A+ policy ("kill the destrabes"), the ordering is **pre-declared** instead of resolved mid-apply.
- **Decision:** `app-settings` adds `detail-back-arrow` to its `x-deps`; the arrow applies first (it is the direct response to the UX report). *Proposed by the agent under the A+ policy and flagged to the human in the same turn — reversible with a new entry if another order is preferred.*
- **Consequences:** No mid-apply destrabe when the arrow is applied; `app-settings` waits for it (dep warnings until then). The chain reads: `ranking-exclusions → detail-back-arrow → app-settings → app-entry-gate`.
- **Refs:** `openspec/changes/detail-back-arrow/.openspec.yaml` · `openspec/changes/app-settings/.openspec.yaml` (`x-deps`) · DECISIONS.md [2026-10-16 Ordering: `app-entry-gate` queued behind the whole draft chain]

## [2026-10-16] Ordering: `app-settings` lands after `detail-back-ux` (pre-declared)

- **Context:** A new fix change, `detail-back-ux` (Android 16 predictive-back gesture + Material back icon), overlaps `app-settings` (`ui/**`, `androidTest/**`, build files) and must land first: it fixes a user-reported bug. Per the A+ policy, the order is pre-declared.
- **Decision:** `app-settings` adds `detail-back-ux` to its `x-deps`; `app-entry-gate` follows transitively through `app-settings`. *Proposed by the agent under the A+ policy and flagged in the same turn — reversible with a new entry if another order is preferred.*
- **Consequences:** Chain: `detail-back-ux → app-settings → app-entry-gate`. No mid-apply destrabes expected.
- **Refs:** `openspec/changes/detail-back-ux/.openspec.yaml` · `openspec/changes/app-settings/.openspec.yaml` (`x-deps`)

## [2026-10-16] Ordering: `app-settings` lands after `unlock-counter` (pre-declared)

- **Context:** A new small feature, `unlock-counter` (dashboard unlock count), overlaps `app-settings` (`ui/**`, `data/**`, tests). The human chose "next after `detail-back-ux`" (it is small and wanted soon). Per the A+ policy the order is pre-declared.
- **Decision:** `app-settings` adds `unlock-counter` to its `x-deps`; `app-entry-gate` follows transitively. *Proposed by the agent under the A+ policy, flagged in the same turn — reversible with a new entry.*
- **Consequences:** Chain: `detail-back-ux → unlock-counter → app-settings → app-entry-gate`. No mid-apply destrabes expected.
- **Refs:** `openspec/changes/unlock-counter/.openspec.yaml` · `openspec/changes/app-settings/.openspec.yaml` (`x-deps`)

## [2026-10-16] Change approved: `detail-back-arrow`

- **Context:** Created from the `/spec-verify` finding the user reported: the detail screen has no visible way back (system back verified working with `KEYCODE_BACK`; the affordance was missing). Small feature: a back arrow on the detail header wired to the same back action; ordering pre-declared (`app-settings` depends on it).
- **Decision (human):** approved. Moves to `accepted` (`x-accepted_by: Nschkurko`, `x-accepted_at: 2026-10-16`) and is ready for `/spec-apply`. No protected zones are involved.
- **Consequences:** Its apply closes the user-reported gap; `app-settings` lands after it (declared dep). No conflict destrabes expected.
- **Refs:** `openspec/changes/detail-back-arrow/.openspec.yaml` · `openspec/changes/detail-back-arrow/proposal.md`

## [2026-10-16] Change approved: `ranking-exclusions`

- **Context:** Created on 2026-10-16 as a draft: long-press on a ranking row excludes the app from the ranking (its time keeps counting in the total and the hourly chart), with an **Undo** snackbar and a persisted list (DataStore Preferences — management lives in the `app-settings` change per the scope decision). Its dep (`app-usage-detail`) is archived.
- **Decision (human):** approved. The change moves to `accepted` (`x-accepted_by: Nschkurko`, `x-accepted_at: 2026-10-16`) and is ready for `/spec-apply`. No protected zones are involved.
- **Consequences:** Its apply heads the remaining chain (`app-settings` lands after it; `app-entry-gate` remains encoded behind `app-usage-detail`). It introduces the `datastore-preferences` dependency that `app-settings` reuses.
- **Refs:** `openspec/changes/ranking-exclusions/.openspec.yaml` · `openspec/changes/ranking-exclusions/proposal.md`

## [2026-10-16] Change approved: `detail-back-ux`

- **Context:** Created on 2026-10-16 as a draft fix: on Android 16 (API 36) the predictive-back default left the detail's back **gesture** without a listener (key and visible control verified working; measured in `/spec-verify`). The fix declares `android:enableOnBackInvokedCallback="true"` and swaps the `←` glyph for the Material back icon with a `contentDescription`. Comprehension sign-off already recorded (walkthrough 3/3). Its ordering is pre-declared at the head of the chain.
- **Decision (human):** approved ("detail-back-ux approve"). The change moves to `accepted` (`x-accepted_by: Nschkurko`, `x-accepted_at: 2026-10-16`) and is ready for `/spec-apply`. No protected zones are involved.
- **Consequences:** Its apply unblocks the queue (`unlock-counter` lands after it; `app-settings` and `app-entry-gate` follow transitively). The manifest flag becomes tripwire-pinned by `BackInvokedCallbackManifestTest`.
- **Refs:** `openspec/changes/detail-back-ux/.openspec.yaml` · `openspec/changes/detail-back-ux/walkthrough.md` · `openspec/changes/detail-back-ux/proposal.md`

## [2026-10-16] Change approved: `unlock-counter`

- **Context:** Created on 2026-10-16 as a draft feature: the dashboard subtitle shows the day's unlock count, derived from the `KEYGUARD_HIDDEN` events of the same daily query that feeds the sessions (no new permissions or dependencies; keyguard-dismissal is the platform's standard unlock semantics, fixed in the spec). Comprehension sign-off already recorded (walkthrough 3/3). Its ordering is pre-declared behind `detail-back-ux` (its dep).
- **Decision (human):** approved. The change moves to `accepted` (`x-accepted_by: Nschkurko`, `x-accepted_at: 2026-10-16`) and is ready for `/spec-apply` in its turn. No protected zones are involved.
- **Consequences:** Its apply follows the `detail-back-ux` close-out (dep + overlap sequencing); `app-settings` and `app-entry-gate` stay queued transitively behind it.
- **Refs:** `openspec/changes/unlock-counter/.openspec.yaml` · `openspec/changes/unlock-counter/walkthrough.md` · `openspec/changes/unlock-counter/proposal.md`

## [2026-10-16] Diagram sync: `nav` and `storage` join the C4 model (reality sync)

- **Context:** `/diagram-sync` (v1.2 background subagent) found real drift since the last diagram touch: Navigation Compose (`FocoNavHost`) and the `excluded_apps` DataStore were undocumented; `entry → data "Checks the permission"` was false (the gate lives in `ui/App.kt`); the UI resolves launcher icons/labels straight from `PackageManager` (undrawn); descriptions lacked the app-detail surface, day window and unlock count. The proposal was validated with Structurizr (parse OK; `cap:` tags check both ways).
- **Decision (human):** apply the full diff — `nav` (Navigation) and `storage` (Local store) become their own cap-tagged components; relationships updated, including `ui → platform` and `nav → platform`.
- **Consequences:** `ARCHITECTURE.dsl` reflects reality as of today. Two code-side questions stay flagged (not silently decided): the direct `ui → platform` icon/label access and `AvailableDays.kt` (pure, but living in `data/`) — candidates for `/spec-verify` or `/spec-triage`. Re-sync after `app-settings` and `app-entry-gate` land.
- **Refs:** `ARCHITECTURE.dsl` · `openspec/specs/code-structure/spec.md`

## [2026-10-16] Triage: layer boundaries — `availableDays` and the platform adapters

- **Context:** The `/diagram-sync` review flagged two silent discrepancies against `code-structure`. (1) `data/AvailableDays.kt:9` is pure (`java.time` only) but `data/` is defined as Android platform access; pure behavior belongs to `domain/`. (2) `ui/dashboard/AppIcon.kt:30` and `ui/dashboard/AppLabel.kt:18` call `PackageManager` raw from the UI; the spec was silent and the fresh diagram draws `ui → platform` (reality sync `4510657`).
- **Decision (human):** (1) the spec is right → `availableDays` moves to `domain/` (with its caller and test; no behavior change). (2) route the icon/label platform reads through a `data/` adapter — the UI stops touching the platform directly. Both land in one `kind: refactor` change (`layer-compliance`) whose delta clarifies the two boundary SHALLs and whose structure test pins them.
- **Consequences:** After the refactor, `ui` reaches the platform only through `data/`; the DSL's `ui → platform` edge gets re-checked at the next `/diagram-sync` (drawn as reality today; superseded by this fix). Regression proof: `DaySelectionTest` + the suite stay green.
- **Refs:** `openspec/changes/layer-compliance/` · `ARCHITECTURE.dsl` · `openspec/specs/code-structure/spec.md`

## [2026-10-16] Ordering: `app-settings` lands after `layer-compliance` (pre-declared)

- **Context:** The triage-born refactor `layer-compliance` (`availableDays` → `domain/`; platform reads behind `data/` adapters) overlaps `app-settings` (`data/**`, `ui/**`, `test/**`). Per the A+ policy the order is pre-declared instead of resolved mid-apply.
- **Decision:** `app-settings` adds `layer-compliance` to its `x-deps`; `app-entry-gate` follows transitively. *Proposed by the agent under the A+ policy, flagged in the same turn — reversible with a new entry if another order is preferred.*
- **Consequences:** Chain: `layer-compliance → app-settings → app-entry-gate`. No mid-apply destrabes expected.
- **Refs:** `openspec/changes/layer-compliance/.openspec.yaml` · `openspec/changes/app-settings/.openspec.yaml` (`x-deps`)

## [2026-10-16] Change approved: `layer-compliance`

- **Context:** Created on 2026-10-16 from the `/diagram-sync` triage of the layer boundaries: `availableDays` (pure `java.time` math) moves out of `data/` into `domain/` with its caller and test, and the raw `PackageManager` icon/label reads in `AppIcon`/`AppLabel` move behind a context-scoped `data/` adapter (`AppPackageInfo`) — one `kind: refactor` change, zero behavior change. The structure test gains the two boundary tripwires; `DaySelectionTest` is the regression proof. Its ordering was pre-declared at the head of the chain (`app-settings` depends on it; `app-entry-gate` follows transitively). No deps, no protected zones.
- **Decision (human):** approved ("layer-compliance approve"). The change moves to `accepted` (`x-accepted_by: Nschkurko`, `x-accepted_at: 2026-10-16`) and is ready for `/spec-apply`.
- **Consequences:** Its apply heads the chain (`layer-compliance → app-settings → app-entry-gate`). Refactor proof: the suite stays green. The tech-brief comprehension sign-off remains for `/spec-verify` after the apply (the `walkthrough.md` is already written).
- **Refs:** `openspec/changes/layer-compliance/.openspec.yaml` · `openspec/changes/layer-compliance/proposal.md` · `openspec/changes/layer-compliance/walkthrough.md`

## [2026-10-16] Comprehension answers stay verbatim (the human's language)

- **Context:** The `layer-compliance` `/spec-verify` teach-back asked the 3 questions in Spanish (the human's language, for free recall) and transcribed the answers word-for-word into the otherwise-English `walkthrough.md`; the same format was used earlier for `app-entry-gate`. The human questioned whether that breaks the repo's English rule and confirmed the practice ("si viene de las consultas anteriores todo ok").
- **Decision:** keep it as the convention: the walkthrough structure, mechanism and questions stay English; the human's answers are quoted verbatim, in whatever language they answered, attributed `— the human, <date>`; the agent never translates or paraphrases them (the sign-off evidence is the human's).
- **Consequences:** Future `/spec-verify` runs follow the same format; the quoted answers are an explicit, scoped exception to the English rule for artifacts (`openspec/config.yaml` context) — everything else stays English.
- **Refs:** `openspec/changes/layer-compliance/walkthrough.md` · `openspec/changes/app-entry-gate/walkthrough.md` · `openspec/config.yaml`

## [2026-10-16] Diagram sync: `ARCHITECTURE.dsl` reconciled with `layer-compliance` (reality sync)

- **Context:** `/diagram-sync` (background subagent) found the diagram still describing the pre-refactor structure: `data` listed `availableDays`, `domain` omitted it, `ui -> platform` was drawn as icon/label resolution, and stale edges remained (`data -> storage`, `nav -> platform`). The proposal was validated with Structurizr (parse OK; lint with no new class of finding).
- **Decision (human):** apply the full diff ("si" to the proposal, including the optional store-key detail): `data` gains `AppPackageInfo`, `domain` gains `availableDays`; edges updated — added `ui -> domain`, `ui -> storage`, `entry -> platform`; removed `data -> storage`, `nav -> platform`; `ui -> platform` re-described (opens the usage-access settings screen and dispatches the back invocation).
- **Consequences:** `ARCHITECTURE.dsl` reflects reality as of today and the `layer-compliance` finding is resolved. Capability tags unchanged (`usage-tracking`, `code-structure`); `cap:app-entry` / `cap:app-settings` are added only after those changes are archived — re-run `/diagram-sync` after each archive.
- **Refs:** `ARCHITECTURE.dsl` · `openspec/changes/layer-compliance/findings.md` · `scripts/gates/gate-check.mjs` (cap-tags check)

## [2026-10-16] Change approved: `app-settings`

- **Context:** Created on 2026-10-16 as a draft feature: a Settings screen (gear in the dashboard) with language (Automático/ES/EN via the `appcompat` backport), appearance (Automático/Claro/Oscuro resolved in `FocoTheme`) and the excluded-apps manager (list + restore; management lives only here), persisted in the existing DataStore; the i18n baseline moves the hardcoded UI strings to resources. Its deps are all archived (`app-usage-detail`, `ranking-exclusions`, `detail-back-arrow`, `detail-back-ux`, `unlock-counter`, `layer-compliance`). No protected zones are involved.
- **Decision (human):** approved ("approve app-settings"). The change moves to `accepted` (`x-accepted_by: Nschkurko`, `x-accepted_at: 2026-10-16`) and is ready for `/spec-apply`.
- **Consequences:** It heads the remaining queue (`app-entry-gate` follows transitively, declared dep). Its tech-brief `walkthrough.md` is written during the apply and the comprehension sign-off happens in `/spec-verify` afterwards, before `--pre-archive`.
- **Refs:** `openspec/changes/app-settings/.openspec.yaml` · `openspec/changes/app-settings/proposal.md` · `openspec/changes/app-settings/specs/app-settings/spec.md`

## [2026-10-16] Change approved: `app-entry-gate`

- **Context:** Created on 2026-10-16 as a draft: blind the permission gate (`App` checks the usage-access AppOp on first composition and on every `ON_RESUME`) behind a seam — `App(isUsageAccessGranted = { ... })` — plus the two instrumented test layers (deterministic fake-predicate seam + real-`appops` E2E on the API 36 AVD). The comprehension sign-off was already recorded (walkthrough 3/3, 2026-10-16). Its deps — `app-usage-detail`, `ranking-exclusions`, `app-settings` — are all archived. No protected zones are involved.
- **Decision (human):** approved ("app-entry-gate"). The change moves to `accepted` (`x-accepted_by: Nschkurko`, `x-accepted_at: 2026-10-16`) and is ready for `/spec-apply`.
- **Consequences:** It heads the remaining queue; `structure-housekeeping`, `themed-window` and `back-arrow-inset` wait behind it (declared deps; their overlaps on `data/**`, `test/**` and `androidTest/**` stay sequenced as warnings). No further ordering destrabes are expected.
- **Refs:** `openspec/changes/app-entry-gate/.openspec.yaml` · `openspec/changes/app-entry-gate/proposal.md` · `openspec/changes/app-entry-gate/walkthrough.md`

## [2026-10-16] Change approved: `structure-housekeeping`

- **Context:** Created on 2026-10-16 as a `refactor`: shared widgets (`AppIcon`/`appLabel`) move from `ui/dashboard` to `ui/common`, `AppDetailScreen` to `ui/detail`; language/appearance move to their own DataStore (`foco_settings`) with a one-shot idempotent migration from the legacy `excluded_apps` file; plus an expressivity pass (physical-resource headers, `add/remove` → `exclude/restore`, the cross-feature boundary pinned by `LayerRulesTest`). Regression: `SettingsMigrationTest` (JVM, temp-file stores) + the structure tests. Tech brief: yes (persistence split; walkthrough written during the apply). Its dep `app-entry-gate` is accepted and goes first.
- **Decision (human):** approved ("approve structure-housekeeping"). The change moves to `accepted` (`x-accepted_by: Nschkurko`, `x-accepted_at: 2026-10-16`) and is ready for `/spec-apply` behind `app-entry-gate`.
- **Consequences:** Applying it requires `app-entry-gate` verified/archived first (declared dep; the `data/**`, `MainActivity.kt`, `test/**` and `androidTest/**` overlaps stay sequenced). The comprehension sign-off happens in `/spec-verify` after its apply (walkthrough covers the migration). `themed-window` and `back-arrow-inset` wait transitively behind it.
- **Refs:** `openspec/changes/structure-housekeeping/.openspec.yaml` · `openspec/changes/structure-housekeeping/proposal.md` · `openspec/changes/structure-housekeeping/specs/code-structure/spec.md`

## [2026-10-16] Tooling back out of the repo: the tracked `.opencode/` removed

- **Context:** The workspace sanitization put the OpenCode tooling outside the repo (`.opencode/` in the container, on purpose), but the `v1.1-token-diet` PR (`542308b`) re-added a snapshot of the 8 command files inside the repo and they stayed tracked — and stale: the container copies are the live, newer set (v1.2 background-mode bodies; e.g. `spec-gate.md` carries 2 lines the repo copy lacked).
- **Decision (human):** move the tooling back out — `git rm -r .opencode` from the repo (the container's `.opencode/` remains the single canonical home, untouched) — and ignore `.opencode/` in the repo so no future sync re-tracks stale copies.
- **Consequences:** Working from the container is unaffected; opening the project directly at `Foco/` no longer loads stale commands. Future spec-growth command syncs land only in the container's `.opencode/`.
- **Refs:** `.gitignore` (`.opencode/` ignored) · commit `542308b` (which re-added them) · the container's `.opencode/`

## [2026-10-18] Windows JVM suite red: a DataStore platform branch, not the app

- **Context:** `gradlew.bat test` fails on Windows in `SettingsMigrationTest` (2 of its 3 tests) with `java.io.IOException: Unable to rename ...preferences_pb.tmp`, raised from `androidx.datastore.core.FileStorageConnection.writeScope`. Investigation: the Android artifact of DataStore moves its scratch file through `FileMoves_androidKt.atomicMoveTo`, which branches on `Build.VERSION.SDK_INT` — `>= 26` to `java.nio Files.move(REPLACE_EXISTING, ATOMIC_MOVE)`, `< 26` to `File.renameTo`. In an AGP unit test the mocked `android.jar` reports `SDK_INT = 0`, so the `renameTo` branch always wins, and `File.renameTo` on Windows cannot replace an existing file — which is precisely what a second write to the same file requires. Standalone probes confirmed the mechanism (`renameTo` over an existing target -> false; `Files.move` -> OK) and the failure reproduced identically against datastore 1.1.7 and 1.2.1, ruling out a version regression; a JVM-artifact harness passes on both, which is how the artifact difference was isolated. The app is unaffected: on a device or emulator `SDK_INT >= 26`, so DataStore takes the `Files.move` path (Foco was installed and run on the API 36 AVD to confirm). Cost of the red suite: `gate-check --run-tests` reports GATE RED and `structure-housekeeping`'s `verified` has no local green backing.
- **Decision (human):** the test is the side to change (`kind: fix`), and the remedy is to declare the Windows exclusion explicitly inside `SettingsMigrationTest` — no new dependency, no production change. Robolectric (which would execute the test on Windows with a real `SDK_INT`) and moving the JVM suite to Linux/CI were considered and set aside. Recorded as the change `datastore-test-on-windows`.
- **Consequences:** on Windows the two guarded tests are skipped with a named reason instead of failing; on every other platform they run unchanged. The migration is not executed on Windows — the gap stays visible in the test report and is covered on Linux/CI and on device. A new `code-structure` requirement states the rule: a platform exclusion must be declared and named, never silent.
- **Refs:** `openspec/changes/datastore-test-on-windows/` · `app/src/test/java/com/foco/app/data/SettingsMigrationTest.kt` · `openspec/changes/structure-housekeeping/specs/code-structure/spec.md`

## [2026-10-18] Workspace moved off `E:` — the repo root is now the workspace root

- **Context:** The 2026-10-16 sanitization put the repo on an `E:` volume, inside a git-less container that also hosted `.opencode/` as the single canonical home of the `spec-*` commands (see the two entries above). That volume is gone. The working copy now lives in an ordinary user directory, and the workspace root **is** the repo root — there is no container layer left.
- **Decision (human):** the human asked to drop the stale location notes (2026-10-18) and, with them, any absolute local path from the tracked docs (this repo is public). The `spec-*` commands were reinstalled from the `spec-growth` skill assets into `.opencode/commands/` at the repo root; `.opencode/` stays git-ignored, so the "never re-track command copies" rule (entry above) still holds.
- **Consequences:** `AGENTS.md` no longer declares a location at all, and no tracked document of this change states an absolute local path. The container's `opencode.json` → `instructions` wiring is unnecessary: OpenCode auto-loads the repo's `AGENTS.md` now that the workspace root is the repo root. Sessions open at the repo root, and the `spec-*` commands assume cwd = repo root. `openspec` 1.14.3 is installed globally (`@fission-ai/openspec`, pinned).
- **Refs:** `AGENTS.md` · `.opencode/commands/` · `.gitignore`

## [2026-10-18] Triage: usage across midnight is dropped — the code is wrong

- **Context:** The `bug-finder` pass (anchored mode) found that Foco loses the usage of an app that was already in the foreground when the day's query window opened. `UsageRepository.loadDay` asks the system for `[startOfDay, min(dayEndExclusive, now)]` (`data/UsageRepository.kt:68,70`) and folds only those events (`:98`); the fold discards a pause/stop that has no session open (`domain/SessionBuilder.kt:38-44`). So an app open across midnight (resumed 23:50, still open at 00:05) produces no `RESUMED` inside the new day's window, its pause at 00:06 is dropped, and the day reports nothing for it: the time is silently undercounted. The trigger is ordinary — any app used across midnight, which is exactly what this app exists to measure — and the living requirement promises the opposite: `openspec/specs/usage-tracking/spec.md:8-22`, scenario "Session open at query time" (`:16-18`).
- **Decision (human):** *the code is wrong.* The fix seeds the already-open session instead of dropping it: the query looks back far enough to find the event that opened the session, and the resulting session is clamped to the day's start so the previous day is unaffected. Recorded as the change `cross-midnight-sessions` (`kind: fix`).
- **Consequences:** No ⚔ conflict follows: the window `[day start, min(day end, now)]` was written only inside the archived change `2026-10-16-app-usage-detail` (`proposal.md:12`, `tasks.md:5`) as its then-implementation — the living `usage-tracking` spec never fixed the query window, and no ADR mentions it. Widening the *lookback* therefore supersedes an implementation note rather than reversing a decision in force, and it is what finally satisfies the living scenario. The golden rule held: the triage did not add "the bug" to the spec; the change states the resulting contract with its regression test.
- **Refs:** `openspec/changes/cross-midnight-sessions/` · `app/src/main/java/com/foco/app/data/UsageRepository.kt:68,70,98` · `app/src/main/java/com/foco/app/domain/SessionBuilder.kt:38-44` · `openspec/specs/usage-tracking/spec.md:8-22` · `openspec/changes/archive/2026-10-16-app-usage-detail/proposal.md:12`

## [2026-10-18] Triage: same-millisecond transitions are swallowed — the code is wrong

- **Context:** The `bug-finder` pass found that `buildSessions` guards both of its transitions with a strict `>` against the open session's start: the resume guard (`app/src/main/java/com/foco/app/domain/SessionBuilder.kt:29`, `open != event.packageName && event.timestamp > openStart`) and the stop guard (`:40`, `event.packageName == open && event.timestamp > openStart`). A transition landing on the same millisecond as the session's start therefore falls through, with two outcomes: (a) a direct switch to another app does not close the previous session, so that app's time is attributed to the previous package — and if no further event arrives, the session stretches to the query time; (b) a pause/stop in the same millisecond leaves the session "open", so it closes at the query time instead of being the zero-length session it actually is. The living requirement states the opposite for (a): `openspec/specs/usage-tracking/spec.md:9` ("switching directly between apps closes the previous session at the new start"), with its scenario at `:12-14`. For (b) the living spec is mute — the only statement is the code's own comment (`SessionBuilder.kt:11`). The trigger (two events sharing a millisecond) is rare; the damage is not: a whole session attributed to the wrong package, or a day total that stays wrong.
- **Decision (human):** *the code is wrong.* The fix drops the strict `>` from both guards — the transition settles by event order, not by inequality — and the zero-length session is filtered where sessions are emitted rather than where transitions are recognised. The delta states that contract explicitly, including the half the spec never wrote: a pause/stop always closes the open session of its own package.
- **Consequences:** No ⚔ conflict follows: no ADR mentions the fold or its guards, and the change implements the living requirement instead of reversing it — the only addition is the STOPPED half, which was never stated. Sequencing: it touches `domain/**` (almost certainly the same file as `cross-midnight-sessions`) and `test/**`, so it declares `x-deps: [cross-midnight-sessions]` (one-way, so no deadlock) plus `datastore-test-on-windows` (already verified, therefore satisfied); it deliberately declares **no** dep on `themed-window`, whose overlap on `test/**` stays a plain warning instead of blocking this fix behind a draft. The golden rule held: no bug was added to the spec — the delta states the resulting contract with its regression test.
- **Refs:** `openspec/changes/same-millisecond-transitions/` · `app/src/main/java/com/foco/app/domain/SessionBuilder.kt:29,40` · `openspec/specs/usage-tracking/spec.md:9,12-14`

## [2026-10-18] Triage: the boundary check only covers four hardcoded folders — the test is wrong

- **Context:** The `/spec-verify` semantic pass found that `LayerRulesTest` — the `[@test]` the living requirement declares for the cross-feature boundary — enforces that boundary only for a hardcoded set: `uiFeatures = setOf("dashboard", "detail", "settings", "onboarding")` (`app/src/test/java/com/foco/app/structure/LayerRulesTest.kt:40`), and a violation is recorded only `if (feature != null && feature in uiFeatures)` (`:87-88`), against the general rule written in `openspec/specs/code-structure/spec.md:62` ("A file under a `ui/<feature>/` folder SHALL NOT import another `ui/<feature>/` package") with its scenario at `:65-67`. Mechanism: a file `com.foco.app.ui.reports.X` importing `com.foco.app.ui.dashboard.Y` yields `feature = "reports"`, which is not in the set, so the check is skipped and the test **passes** — the tripwire stops covering exactly the case (a feature folder that did not exist when the check was written) that the scenario describes. Today `ui/` holds those four folders plus the three shared ones (`common`, `metrics`, `theme`), so the hole is latent rather than active. Found by reading the predicate, not by a temp-file reproduction: the source set must compile before the test runs, which makes a probe file an intrusive way to prove a set-membership test.
- **Decision (human):** *the test is wrong.* The check derives the UI feature folders from the source tree — every folder under `ui/` except the shared ones (the `ui` root, `ui/common/`, `ui/metrics/`, `ui/theme/`) — and asserts that derivation, so a new feature folder cannot fall outside the rule. Recorded as the change `layer-check-scope` (`kind: fix`).
- **Consequences:** No ⚔ conflict follows: the change implements the living requirement instead of reversing it; it also settles a small ambiguity in the contract — the requirement's parenthetical names the `ui` root, `ui/metrics/` and `ui/theme/` as shared but not `ui/common/`, which the requirement's own first sentence establishes as the home of cross-screen reuse, so `common` cannot be a feature. The regression is provable: an assertion over the derived set fails against the hardcoded list *before* the fix. Nothing of the app changes — test-only, no dependency, no platform API, hence no tech brief. Overlap: `app/src/test/**` collides with `cross-midnight-sessions`, `same-millisecond-transitions` and `themed-window` as globs only (no file in common); just the already-verified `datastore-test-on-windows` is declared as a dep, so this small fix is not queued behind two drafts. Golden rule held: the delta states the resulting contract (the check derives its scope from the tree) with its regression test.
- **Refs:** `openspec/changes/layer-check-scope/` · `app/src/test/java/com/foco/app/structure/LayerRulesTest.kt:40,87-88` · `openspec/specs/code-structure/spec.md:61-67`

## [2026-10-18] `themed-window` accepted: draft → accepted

- **Context:** `themed-window` (`kind: fix`, `tech_brief: true`) sat as a draft with both declared deps — `app-entry-gate` and `structure-housekeeping` — already archived, i.e. satisfied, and with no protected zone touched (`openspec/config.yaml` → `protected: []`). It carried no decision request, no conflict and no waiver, so the only thing between it and implementation was the human's authorization (nothing on the BOARD's 🔒/⏸/⚔/⚠ lists pointed at it).
- **Decision (human):** approved. The human authorized the change to move `draft → accepted`; recorded as `status: accepted` + `accepted_by: Nschkurko` + `accepted_at: 2026-10-18`. The approval covers the change as proposed — it is **not** an implementation and not an approval of a protected zone (there is none to approve).
- **Consequences:** the change may now be applied, one branch and one change at a time (`/spec-apply`), with green `--run-tests` evidence before it can be `verified`; being a `fix`, it is bound to its regression test (`app/src/test/java/com/foco/app/data/AppAppearanceTest.kt`). Because it is a tech brief, `walkthrough.md` and the human's comprehension sign-off will be required before archiving (`/spec-verify`) — neither exists yet. `back-arrow-inset` declares a dep on `themed-window`; that edge stays *unverified* until `themed-window` is verified, so `back-arrow-inset` is still queued behind it. No other change in flight is affected.
- **Refs:** `openspec/changes/themed-window/.openspec.yaml` · `openspec/changes/themed-window/proposal.md` · `openspec/config.yaml` · `BOARD.md`

## [2026-10-18] CI on GitHub — the JVM suite on Linux, plus repository topics

- **Context:** The repository had no `.github/` at all, while the README already claimed that verification "runs in `scripts/gates/` (pre-commit and CI)" and two records leaned on CI as the place where the DataStore migration tests execute: the `datastore-test-on-windows` walkthrough ("still exercised on Linux/CI") and its `tasks.md` 1.2 ("the non-Windows leg ... stays covered on CI/Linux"). The claim had no support: on this Windows machine those two tests are skipped, so nothing ran them. The human also asked for repository topics.
- **Decision (human):** add CI and the topics, treated as **repository infrastructure — outside the Spec Growth flow** (there is no capability for it, and the change would touch no app behavior, so no `openspec/changes/` entry was opened). CI is minimal on purpose: `.github/workflows/ci.yml` runs `./gradlew test` on `ubuntu-24.04` for every push to `main` and every pull request. The JVM daemon is supplied as Temurin 25 (the version `gradle/gradle-daemon-jvm.properties` pins), the Android SDK package `platforms;android-36` is installed (licenses accepted, `compileSdk = 36`), and Gradle caching is delegated to `gradle/actions/setup-gradle`. The runner deliberately does **not** run `gate-check` (the human chose "just the tests"), so the README's "pre-commit and CI" parenthetical was corrected to "pre-commit". `gradlew` was committed as `100755`: it was `100644`, which fails as `Permission denied` on a Linux runner.
- **Consequences:** the Windows-only test gap is now covered on every push and PR; a red PR can no longer be merged on a green local suite alone. Limits worth knowing: if Gradle ever needs an SDK package beyond `platforms;android-36`, AGP downloads it on demand (the licenses are accepted) — slower runs, no failure; CI knows nothing about the Spec Growth gate, so a PR can be green here and still `blocked` on the BOARD (that is the pre-commit hook's job, and a `gate-check` job is the obvious next step if the human wants one); the runner is pinned to `ubuntu-24.04` rather than `ubuntu-latest` because the label migrates to Ubuntu 26 on 2026-10-19 and an image swap should not be what turns the suite red (the pin needs revisiting when 24.04 is retired). The actions are at their current majors (`checkout@v7`, `setup-java@v6`, `setup-android@v4`, `setup-gradle@v6`), which clears the Node 20 deprecation notices the first run raised. Nothing of the app changes — no dependency, no platform API — hence no tech brief and no `[@test]`. Repository topics (`android`, `kotlin`, `jetpack-compose`, `material3`, `screen-time`, `digital-wellbeing`, `spec-growth`, `openspec`) were set through the GitHub API; they are metadata on GitHub, not files in the repo.
- **Refs:** `.github/workflows/ci.yml` · `README.md:13,79` · `gradle/gradle-daemon-jvm.properties` · `app/build.gradle.kts:9,18` · `openspec/changes/datastore-test-on-windows/walkthrough.md:34` · `openspec/changes/datastore-test-on-windows/tasks.md:6`

## [2026-10-18] `cross-midnight-sessions` and `layer-check-scope` accepted: draft → accepted

- **Context:** Both sat as drafts whose single declared dep — `datastore-test-on-windows` — was already `verified`, so only the human's authorization stood between them and implementation. `cross-midnight-sessions` fixes a real undercount (an app already in the foreground when the day's window opens produces no session: the query window at `data/UsageRepository.kt:68-70` plus the fold that drops a pause with no open session at `domain/SessionBuilder.kt:38-44`) and is the root of a chain — `same-millisecond-transitions` declares a dep on it. `layer-check-scope` is the test-side `kind: fix` the `/spec-verify` semantic pass filed against `LayerRulesTest`'s hardcoded feature list (`app/src/test/java/com/foco/app/structure/LayerRulesTest.kt:40,87-88`). Neither touches a protected zone (`openspec/config.yaml` → `protected: []`), and neither carried a waiver, an open decision or a conflict: the BOARD's 🔒/⏸/⚔/⚠ sections were empty, so the only thing this session had to resolve was the authorization itself.
- **Decision (human):** approved both, in one sentence ("approve cross-midnight-sessions and layer-check-scope"), recorded on each as `status: accepted` + `accepted_by: Nschkurko` + `accepted_at: 2026-10-18`. This is the authorization, not the implementation: each still lands on its own branch, one at a time, with green `--run-tests` evidence before it can be `verified`.
- **Consequences:** `cross-midnight-sessions` may be applied now; when it reaches `verified` it unblocks `same-millisecond-transitions` — and since both edit `domain/SessionBuilder.kt`, applying them in that order is also what keeps the two branches from colliding. Being a tech brief, it will need `walkthrough.md` + the comprehension sign-off before archiving, neither of which exists yet. `layer-check-scope` is test-only, has no dependents, and is deliberately not queued behind the drafts it shares the `app/src/test/**` glob with (the choice recorded earlier today). Both are `fix`, so each stays bound to its regression test (`app/src/test/java/com/foco/app/domain/SessionBuilderTest.kt`, `app/src/test/java/com/foco/app/structure/LayerRulesTest.kt`). `datastore-test-on-windows` was already satisfied as a dep and is unaffected — it still awaits its own comprehension sign-off.
- **Refs:** `openspec/changes/cross-midnight-sessions/.openspec.yaml` · `openspec/changes/layer-check-scope/.openspec.yaml` · `openspec/specs/usage-tracking/spec.md:8-22` · `app/src/main/java/com/foco/app/data/UsageRepository.kt:68-70` · `app/src/main/java/com/foco/app/domain/SessionBuilder.kt:38-44` · `app/src/test/java/com/foco/app/structure/LayerRulesTest.kt:40,87-88` · `BOARD.md`

## [2026-10-19] `boundary-check-fixture` accepted and applied: the boundary check proves its failure branch

- **Context:** the `/spec-verify` pass on `layer-check-scope` filed one medium finding: the scenario that change declares says *"the boundary check fails and names the file"*, but no test ever made the check fail — both tests only ever proved the rule **silent** on the real tree (`app/src/test/java/com/foco/app/structure/LayerRulesTest.kt`). Same shape as the defect `layer-check-scope` itself closed: a tripwire whose business branch nobody exercises, so it can rot unnoticed. The human asked for it to be fixed ("arreglalo") and then approved the change ("pasalo a aprobado y aplicalo ya").
- **Decision (human):** approved and applied `boundary-check-fixture` (`kind: refactor`, test-only), recorded as `status: accepted` + `accepted_by: Nschkurko` + `accepted_at: 2026-10-19`, later `verified` with green evidence. As part of the same go-ahead, `layer-check-scope` was merged into `main` (merge `0ef1907`), which was a precondition and not a convenience: its rewrite of `LayerRulesTest.kt` existed only on its branch, so `main` still held the hardcoded list and every change depending on it failed to resolve there — that was the RED the gate showed on `main`.
- **Consequences:** the tree scan moves out of the `@Test` into `violationsIn(sources: File)` (same walk, same rules, the root as a parameter) and a `TemporaryFolder` fixture builds a synthetic tree where `ui/reports/Reports.kt` imports `com.foco.app.ui.dashboard.…`: exactly one violation, naming that file, and none for `ui/common/Widget.kt`. It was proven to bite rather than assumed: with the cross-feature rule replaced by `if (false)` the fixture fails while the real-tree test stays green. Test-only ⇒ no tech brief, no comprehension sign-off, no device pass. Sequencing declared while applying (the gate escalates an overlap with `applying` work to an error): `english-source-prose`, `same-millisecond-transitions` and `trim-long-comments` now carry a dep on this change, so they land after it. `layer-check-scope` is merged but still **unarchived** — its delta stays queued behind the others in `code-structure`.
- **Refs:** `openspec/changes/boundary-check-fixture/` · `app/src/test/java/com/foco/app/structure/LayerRulesTest.kt:68-200` · `openspec/changes/layer-check-scope/findings.md` · `openspec/changes/layer-check-scope/.openspec.yaml` · merge `0ef1907`
