# Plan

> Living document — reflects what was done, not just what was intended.
> This file tracks the ACTIVE phase only. Completed phases are archived to `phases/`.

---

## Active Phase

### Phase 40 — CLI Enhancements (v0.40.x)

> Expand the gig CLI with 4 new command groups (scene, action, mixer, project), a WebSocket `watch` command for real-time streaming, and config file support for host/port defaults.

**Decisions:** D-1.1, D-1.2, D-1.3, D-1.4, D-1.5

| Batch | Version | Title | Delegation | Status |
|-------|---------|-------|------------|--------|
| 40.1 | `0.40.1` | Config file support + SceneCommand + ActionCommand | team | pending |
| 40.2 | `0.40.2` | MixerCommand + ProjectCommand | team | pending |
| 40.3 | `0.40.3` | WatchCommand (WebSocket streaming) | in-session | pending |
| 40.4 | `0.40.4` | CLI unit tests | in-session | pending |
| 40.5 | `0.40.5` | Build verification | in-session | pending |

### Batch 40.1 — Config file support + SceneCommand + ActionCommand

**Delegation:** team
**Decisions:** D-1.3, D-1.4, D-1.5
**Files:** `GigCli.java`, `SceneCommand.java` (NEW), `ActionCommand.java` (NEW)
**Work:**
- GigCli: Add config file loading from `~/.gig/config.json` (`{"host":"...", "port":...}`). CLI flags override config. Missing file = defaults.
- SceneCommand: 7 subcommands — list, launch (index), create, create-from-playing, delete (index), rename (index, name), set-color (index, color). All call scene/* and sceneBank/* RPCs.
- ActionCommand: 3 subcommands — list (--category filter), categories, invoke (id). Calls action/* RPCs.
- Register both in GigCli.
**Test criteria:** `./gradlew :gig-maestro:cliShadowJar` builds successfully
**Acceptance:** 10 new CLI subcommands, config file loading

### Batch 40.2 — MixerCommand + ProjectCommand

**Delegation:** team
**Decisions:** D-1.1
**Files:** `MixerCommand.java` (NEW), `ProjectCommand.java` (NEW), `GigCli.java`
**Work:**
- MixerCommand: subcommands — get-state, set-section (section, visible), zoom-in, zoom-out.
- ProjectCommand: subcommands — get-state, unmute-all, unsolo-all, unarm-all, set-cue-volume (value), set-cue-mix (value).
- Register both in GigCli.
**Test criteria:** `./gradlew :gig-maestro:cliShadowJar` builds successfully
**Acceptance:** 10 new CLI subcommands

### Batch 40.3 — WatchCommand (WebSocket streaming)

**Delegation:** in-session
**Decisions:** D-1.2
**Files:** `WatchCommand.java` (NEW), `GigCli.java`
**Work:**
- WatchCommand: connects to WebSocket (ws://host:port+1), optionally sends state/subscribe with --topics filter, prints each state/changed notification to stdout as JSON. Runs until Ctrl+C (shutdown hook).
- Uses Java-WebSocket client library (already a transitive dependency via the extension).
- Register in GigCli.
**Test criteria:** `./gradlew :gig-maestro:cliShadowJar` builds successfully
**Acceptance:** `gig watch` streams state changes, `gig watch --topics transport,device` filters

### Batch 40.4 — CLI unit tests

**Delegation:** in-session
**Decisions:** all
**Files:** CLI test files
**Work:**
- Test config file loading (with file, without file, CLI override)
- Test SceneCommand, ActionCommand, MixerCommand, ProjectCommand RPC call generation
- Test WatchCommand argument parsing
**Test criteria:** `./gradlew :gig-maestro:test` passes
**Acceptance:** Tests cover new commands

### Batch 40.5 — Build verification

**Delegation:** in-session
**Decisions:** all
**Files:** none
**Work:** Full build (shadowJar + cliShadowJar + test + offline smoke)
**Test criteria:** All commands exit 0
**Acceptance:** Clean build, all tests green

**Phase Acceptance Criteria:**
- [ ] 4 new command groups: scene, action, mixer, project
- [ ] Watch command for WebSocket streaming with topic filtering
- [ ] Config file support (~/.gig/config.json)
- [ ] CLI builds and all new commands parse correctly
- [ ] Unit tests cover new functionality
- [ ] Clean build

**Completion triggers Phase 41 → version `0.41.0`**
