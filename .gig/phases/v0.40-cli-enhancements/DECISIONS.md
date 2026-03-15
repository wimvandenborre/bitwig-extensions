# Decisions

> Append-only log. Never delete entries — amend or revise instead.
> Active decisions for the current phase live here.
> When a phase completes, its decisions are archived to `phases/`.

## 2026-03-15 — CLI: Focus on highest-value missing commands

**Decision:** Add 4 new CLI command groups: `scene`, `action`, `mixer`, and `project`. These cover the most commonly needed operations not yet in the CLI. Skip arranger/browser/groove/arpeggiator (niche for CLI use).
**Rationale:** Scene management is essential for live performance workflows. Action invoke is the universal escape hatch. Mixer and project are common monitoring tasks. Arranger/browser operations are better done via RPC or Claude tools than CLI.
**Alternatives considered:** Adding all missing namespaces — rejected as over-engineering. Most users interact with arranger/browser/groove via Bitwig UI or Claude tools, not CLI.
**Status:** ACTIVE
**ID:** D-1.1

## 2026-03-15 — CLI: Add `watch` command for WebSocket streaming

**Decision:** Add a `watch` command that connects via WebSocket and streams `state/changed` notifications to stdout. Supports `--topics` flag for subscription filtering. Runs until Ctrl+C.
**Rationale:** This is the CLI interface to the new WebSocket subscription feature. Essential for monitoring state changes in real-time (e.g., watching transport while a script runs). The Java-WebSocket library is already a dependency.
**Alternatives considered:** HTTP polling loop — rejected because WebSocket is more efficient and we just built subscription support.
**Status:** ACTIVE
**ID:** D-1.2

## 2026-03-15 — CLI: Add config file support

**Decision:** Support a `~/.gig/config.json` file with `host` and `port` defaults. CLI flags override config file. Config file is optional — missing file means defaults (localhost:8787).
**Rationale:** Eliminates `--host` and `--port` flags for non-default setups. Simple JSON format matches the rest of the tool. `~/.gig/` is a reasonable config location.
**Alternatives considered:** Environment variables — rejected as less discoverable. YAML — rejected as an extra dependency.
**Status:** AMENDED
**ID:** D-1.3

## 2026-03-15 — CLI: Config path changed to ~/.gig-maestro/

**Decision:** Config file path is `~/.gig-maestro/config.json` (not `~/.gig/`).
**Rationale:** User override — `~/.gig` is too generic and conflicts with the project's `.gig/` workflow directory. `~/.gig-maestro/` is specific to this tool.
**Alternatives considered:** Original `~/.gig/` — rejected by user as too generic.
**Status:** ACTIVE
**ID:** D-1.3a

## 2026-03-15 — CLI: Scene command details

**Decision:** `scene` command with subcommands: `list`, `launch INDEX`, `create`, `create-from-playing`, `delete INDEX`, `rename INDEX NAME`, `set-color INDEX COLOR`.
**Rationale:** Mirrors the SceneHandler RPC methods. Scene management is essential for clip-launcher workflows and live performance.
**Alternatives considered:** Minimal (only launch) — rejected because scene management is a common workflow need.
**Status:** ACTIVE
**ID:** D-1.4

## 2026-03-15 — CLI: Action command details

**Decision:** `action` command with subcommands: `list [--category CAT]`, `categories`, `invoke ID`.
**Rationale:** Direct CLI wrapper for the action/list, action/listCategories, action/invoke RPCs. The `--category` filter keeps output manageable.
**Alternatives considered:** Single `run` command — rejected because discovery (list/categories) is equally important.
**Status:** ACTIVE
**ID:** D-1.5
