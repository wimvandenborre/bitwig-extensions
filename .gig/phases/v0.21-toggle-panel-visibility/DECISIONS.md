# Decisions

> Append-only log. Never delete entries — amend or revise instead.
> Active decisions for the current phase live here.
> When a phase completes, its decisions are archived to `phases/`.

## 2026-03-13 — Scope: Which panel toggle methods to expose

**Decision:** Expose all 7 Application-level panel toggles as parameterless RPC methods: `app/toggleInspector`, `app/toggleDevices`, `app/toggleMixer`, `app/toggleNoteEditor`, `app/toggleAutomationEditor`, `app/toggleBrowser`, `app/toggleFullScreen`. Also expose sub-panel navigation: `app/previousSubPanel`, `app/nextSubPanel`. Skip panel focus methods (focusPanelToLeft/Right/Above/Below) — these are keyboard-navigation aids, not useful for scripted control.
**Rationale:** The 7 toggles directly address the workflow script need (show device panel without changing layout). Sub-panel navigation lets scripts cycle between note editor/device/automation in the bottom panel. Panel focus methods are for keyboard-driven UI navigation and don't serve scripted workflows.
**Alternatives considered:** (1) Only toggleDevices — too narrow, other toggles are equally useful. (2) Include panel focus methods — over-scoped, not useful for RPC. (3) Use deprecated Mixer/Arranger toggle methods — deprecated, already have alternatives via existing arranger RPCs.
**Status:** ACTIVE
**ID:** D-21.1

## 2026-03-13 — Naming: RPC method naming convention

**Decision:** Use `app/toggle{Panel}` naming matching the Bitwig API method names. Exception: `toggleBrowserVisibility` → `app/toggleBrowser` for brevity (consistent with existing `browser/` namespace). Sub-panels: `app/previousSubPanel`, `app/nextSubPanel`.
**Rationale:** Direct mapping to Bitwig API names makes the RPC discoverable. Shortening "BrowserVisibility" to "Browser" avoids redundancy — all toggles affect visibility.
**Alternatives considered:** (1) `app/showDevicePanel` / `app/hideDevicePanel` — can't distinguish show vs hide (it's a toggle). (2) `panel/toggle{name}` namespace — inconsistent with existing `app/` methods.
**Status:** ACTIVE
**ID:** D-21.2

## 2026-03-13 — Testing: Unit tests + tool definitions + manual verification

**Decision:** Add unit tests for all 9 new RPC methods (verify Application method calls). Add tool definitions to claude-tools.json. Update system-prompt.md with panel toggle documentation. Update manual scripts to use `app/toggleDevices` instead of layout switching where appropriate.
**Rationale:** Follows established pattern — every new RPC needs unit test, tool definition, and system prompt coverage. Manual script updates demonstrate the practical value.
**Alternatives considered:** (1) Skip tool definitions — breaks Claude tool-use workflow. (2) Skip manual script updates — misses the whole point of the phase.
**Status:** ACTIVE
**ID:** D-21.3
