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
- **Decision:** (1) The full repo (including `.git`) lives in `Foco/`; the `E:\Proyectos\Foco` workspace is a container without git that hosts `.opencode/` (outside the repo, on purpose). (2) `spec-*` and `diagram-sync` commands adapted: they assume cwd in `Foco/`. (3) The constitution (`Foco/AGENTS.md`) is loaded in the container via `opencode.json` → `instructions: ["Foco/AGENTS.md"]`; there is a short guide in the container's `AGENTS.md`. (4) `app/build`, `build/`, `.gradle`, `.idea` and the install temp were deleted; `.kotlin/` was added to `.gitignore`. (5) OpenSpec's own commands/skills (`opsx-*`, `openspec-*`) were deleted: Spec Growth does not use them; they can be regenerated with `openspec update`.
- **Consequences:** Android Studio must be reopened at `E:\Proyectos\Foco\Foco` (it lost `.idea`). OpenCode sessions open in the container; the commands do `cd Foco`. The gate and the pre-commit hook live in the repo and keep working with cwd = repo root. The user's pending changes (`gradle.properties`, `gradle/gradle-daemon-jvm.properties`) stayed untouched and were committed separately.
- **Refs:** `E:\Proyectos\Foco\Foco` · `.opencode/commands/` · `opencode.json` (container) · `Foco/AGENTS.md`

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
