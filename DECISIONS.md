# DECISIONS

Registro **append-only** de decisiones (ADR-lite). Acá van: el triage de bugs/discrepancias, las decisiones de arquitectura, las **resoluciones de decision requests** y los **waivers otorgados/rechazados**, y las **aprobaciones de zonas protegidas**. Formato:

## [YYYY-MM-DD] Título de la decisión
- **Contexto:** qué situación obligó a decidir
- **Decisión:** qué se eligió y por qué
- **Consecuencias:** qué implica / qué queda pendiente
- **Refs:** archivo:línea · specs · changes

---

## [2026-10-16] INIT de Spec Growth — desvíos de instalación en Windows

- **Contexto:** Se inicializó Spec Growth en este repo. El procedimiento asume el CLI `opencode mcp add` (no instalado: solo existe el desktop app) y `npx codebase-memory-mcp@<versión>` (roto en Windows en v0.12.0: su instalador npm rechaza el `.exe` nativo por superar su propio límite de 256 MiB — el binario pesa ~318 MiB).
- **Decisión:** (1) MCPs cableados editando directo `~/.config/opencode/opencode.json` (formato V2, `mcp.servers`), con backup del archivo. (2) `codebase-memory` instalado por la vía nativa oficial (`install.ps1 --skip-config`, release v0.12.0 verificado por checksum) en `%LOCALAPPDATA%\Programs\codebase-memory-mcp\`; el MCP apunta a ese `.exe` por ruta absoluta. (3) `structurizr` como MCP remoto (`https://mcp.structurizr.com/mcp`). (4) `openspec init --tools opencode --language spanish`; schema propio `spec-anchored` forkeado y parcheado según la skill.
- **Consecuencias:** Actualizar codebase-memory = re-correr `%LOCALAPPDATA%\Programs\codebase-memory-mcp\install.ps1`. El comando canónico `npx -y codebase-memory-mcp@<v>` no es usable en esta máquina hasta que el paquete npm corrija su extracción. Herramientas instaladas en la sesión: Node.js 24.20.0 LTS (winget) y OpenSpec 1.14.2 (npm, pinneado).
- **Refs:** `~/.config/opencode/opencode.json` · `openspec/config.yaml` · `openspec/schemas/spec-anchored/schema.yaml` · `AGENTS.md`

## [2026-10-16] Sanitización del workspace — repo a `Foco/`, tooling afuera

- **Contexto:** El repo tenía mezclados el código Android, el sistema Spec Growth y la tooling de OpenCode (`.opencode/`) en la misma raíz. Además había ~530 MB de artefactos regenerables (builds, `.idea`, temp de instalación de codebase-memory).
- **Decisión:** (1) El repo completo (incluido `.git`) vive en `Foco/`; el workspace `E:\Proyectos\Foco` queda como contenedor sin git y hospeda `.opencode/` (fuera del repo, a propósito). (2) Comandos `spec-*` y `diagram-sync` adaptados: asumen cwd en `Foco/`. (3) La constitución (`Foco/AGENTS.md`) se carga en el contenedor vía `opencode.json` → `instructions: ["Foco/AGENTS.md"]`; hay una guía breve en el `AGENTS.md` del contenedor. (4) Se borraron `app/build`, `build/`, `.gradle`, `.idea` y el temp de instalación; `.kotlin/` se agregó al `.gitignore`. (5) Se borraron los comandos/skills propios de OpenSpec (`opsx-*`, `openspec-*`): Spec Growth no los usa; se regeneran con `openspec update` si se los quiere.
- **Consecuencias:** Android Studio debe reabrirse en `E:\Proyectos\Foco\Foco` (perdió `.idea`). Las sesiones de OpenCode se abren en el contenedor; los comandos hacen `cd Foco`. El gate y el pre-commit viven en el repo y siguen funcionando con cwd = raíz del repo. Cambios pendientes del usuario (`gradle.properties`, `gradle/gradle-daemon-jvm.properties`) intactos y sin commitear.
- **Refs:** `E:\Proyectos\Foco\Foco` · `.opencode/commands/` · `opencode.json` (contenedor) · `Foco/AGENTS.md`
