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
