# Plan

> Living document — reflects what was done, not just what was intended.
> This file tracks the ACTIVE phase only. Completed phases are archived to `phases/`.

---

## Active Phase

### Phase 3 — LLM + CLI + WebSocket Push (v0.3.x)

> Generate Claude tool_use schemas and a system prompt that give an LLM agent full control of Bitwig Studio via Gig Maestro's 36 RPC methods. Build a thin Picocli CLI that mirrors the RPC surface for scripting and manual testing. Add WebSocket push notifications so connected clients receive delta change events without polling.

**Decisions:** D-3.1, D-3.2, D-3.3a, D-3.4, D-3.5, D-3.6

| Batch | Version | Title | Delegation | Status |
|-------|---------|-------|------------|--------|
| 3.1 | `0.3.1` | Tool schemas + system prompt | in-session | done |
| 3.2 | `0.3.2` | Interactive LLM validation | in-session | done |
| 3.3 | `0.3.3` | CLI project setup + core commands | in-session | done |
| 3.4 | `0.3.4` | WebSocket push notifications | in-session | done |
| 3.5 | `0.3.5` | Smoke test extension | in-session | done |

### Batch 3.1 — Tool schemas + system prompt

**Delegation:** in-session
**Decisions:** D-3.1, D-3.2
**Files:**
- `tools/claude-tools.json` (create)
- `tools/system-prompt.md` (create)

**Work:**
Generate `claude-tools.json` — an array of 36 Claude `tool_use` tool definitions, one per RPC method. Tool name uses underscore convention (`transport_play`, `track_setVolume`). Each definition includes `name`, `description`, `input_schema` (JSON Schema with types, descriptions, required fields, enums). `session_snapshot` is a tool so the LLM can explicitly request state. Descriptions should explain what the tool does in DAW terms, not just echo the method name.

Write `system-prompt.md` covering:
1. Viewport model — TrackBank is a 64-track window, clip slots are 8 per track, device params are 8 per page, scenes are 8
2. Perception-action loop — always call `session_snapshot` first, then act, then snapshot to verify
3. Value ranges — volume/pan are 0.0–1.0 normalized, tempo is raw BPM, position is beats
4. Cursor model — cursor track and device follow UI selection, navigate with `cursor_selectTrack` and `device_selectNext/Previous`
5. Index conventions — all indices are 0-based
6. A few-shot example workflow

**Test criteria:**
- `tools/claude-tools.json` is valid JSON, parseable, contains exactly 36 tool definitions
- Every tool name maps to a registered RPC method (underscore ↔ slash conversion)
- All parameter types match the handler source (int→integer, double→number, boolean→boolean, string→string)
- `tools/system-prompt.md` covers all 5 mental model sections
- `jq length tools/claude-tools.json` returns 36

**Acceptance:** Tool schemas and system prompt exist, are accurate against handler source, and are ready for interactive testing.

### Batch 3.2 — Interactive LLM validation

**Delegation:** in-session
**Decisions:** D-3.3a
**Depends on:** Batch 3.1
**Files:** None (interactive testing only)

**Work:**
Load `tools/system-prompt.md` and `tools/claude-tools.json` into a Claude conversation. Test with real tasks:
1. "Set the tempo to 128 and solo track 2"
2. "Tell me the project name and current tempo"
3. "Create an empty clip on track 1, slot 0, 4 beats long, then launch it"
4. "Navigate to the next device and tell me what parameters are available"

Verify: (1) LLM selects correct tools, (2) params have correct types and values, (3) `session_snapshot` is used for perception before/after actions, (4) system prompt mental model is understood.

Fix any schema errors, confusing descriptions, or missing context discovered during testing.

**Test criteria:**
- LLM selects correct tools for all 4 test tasks
- Parameter types and values are correct (no "80" for volume, uses 0.0–1.0)
- LLM calls `session_snapshot` before acting and after acting to verify
- No schema parse errors or unknown tool references

**Acceptance:** Claude correctly interprets and uses the tool schemas with the system prompt for multi-step DAW control tasks.

### Batch 3.3 — CLI project setup + core commands

**Delegation:** in-session
**Decisions:** D-3.4, D-3.6
**Files:**
- `gradle/libs.versions.toml` (modify — add Picocli)
- `build.gradle.kts` (modify — add `cli` source set, shadow JAR task)
- `src/cli/java/dev/gregross/gig/cli/GigCli.java` (create)
- `src/cli/java/dev/gregross/gig/cli/RpcCommand.java` (create)
- `src/cli/java/dev/gregross/gig/cli/SnapshotCommand.java` (create)
- `src/cli/java/dev/gregross/gig/cli/TransportCommand.java` (create)
- `src/cli/java/dev/gregross/gig/cli/TrackCommand.java` (create)

**Work:**
Add Picocli dependency to version catalog. Add a `cli` source set in `build.gradle.kts` with its own `shadowJar` task that outputs `gig-cli.jar` (separate from the `.bwextension`). The CLI source set depends on Gson (for JSON) and Picocli but NOT on the Bitwig API or extension source.

Create the CLI entry point `GigCli.java` with Picocli `@Command` annotation and subcommands. Commands mirror the RPC surface:
- `gig transport play|stop|record|tempo <bpm>|position <beats>|loop on|off|metronome on|off`
- `gig track set-volume|set-pan|set-mute|set-solo|set-arm --index N --value V`
- `gig snapshot` — calls `session/snapshot`, prints result
- `gig rpc '{"method":"...","params":{}}'` — raw JSON-RPC escape hatch

All commands: construct a JSON-RPC request, POST to `http://localhost:8787`, print the result. Output is JSON by default, `--pretty` for formatted.

**Test criteria:**
- `./gradlew cliShadowJar` builds without errors
- `java -jar build/libs/gig-cli.jar --help` shows usage
- `java -jar build/libs/gig-cli.jar snapshot` returns JSON (with extension running)
- `java -jar build/libs/gig-cli.jar rpc '{"jsonrpc":"2.0","method":"api/list","params":{},"id":1}'` returns method list
- `java -jar build/libs/gig-cli.jar transport play` returns `"ok"`
- Extension `.bwextension` build is unaffected (no Picocli in it)

**Acceptance:** CLI JAR builds independently, all subcommands work against running extension, extension build unchanged.

### Batch 3.4 — WebSocket push notifications

**Delegation:** in-session
**Decisions:** D-3.5, D-3.6
**Files:**
- `src/main/java/dev/gregross/gig/extension/StateCache.java` (modify)
- `src/main/java/dev/gregross/gig/extension/GigMaestroExtension.java` (modify)

**Work:**
Add delta detection to `StateCache`:
- After `getSnapshot()` is called (or in a new method), hash each section's serialized JSON (transport, tracks, master, clips, scenes, device, app) using `String.hashCode()` and compare against previous hashes.
- Expose a method like `getChangedSections()` that returns the list of section names that changed since last check.

In `GigMaestroExtension.flush()`, after `commandQueue.drainAndExecute(dispatcher)`:
- If WebSocket clients are connected (`serverManager.getWsClientCount() > 0`), check for changed sections.
- If any sections changed, broadcast a JSON-RPC notification: `{"jsonrpc":"2.0","method":"state/changed","params":{"changed":["transport","device"]}}`.

**Test criteria:**
- Connect a WebSocket client (`wscat -c ws://localhost:8788`)
- Change tempo in Bitwig → WebSocket receives `state/changed` with `"transport"` in changed array
- Mute a track → receives `"tracks"` in changed array
- No notification when nothing changes (idle state)
- `session/snapshot` still works normally after delta detection is added

**Acceptance:** WebSocket clients receive real-time section-level change notifications. No notifications on idle. Snapshot unaffected.

### Batch 3.5 — Smoke test extension

**Delegation:** in-session
**Decisions:** D-3.3a
**Depends on:** Batches 3.1, 3.3, 3.4
**Files:**
- `scripts/smoke-test.sh` (modify)

**Work:**
Extend smoke test suite to cover Phase 3 additions:
- Tool schema validation: `jq length tools/claude-tools.json` = 36, validate each tool has `name`, `description`, `input_schema`
- CLI: `java -jar build/libs/gig-cli.jar snapshot` returns valid JSON
- CLI: `java -jar build/libs/gig-cli.jar rpc` with `api/list` returns all 36 methods
- WebSocket push: connect, trigger a state change, verify notification received
- API list includes no new methods (36 total unchanged)

**Test criteria:**
- All new smoke tests pass
- All existing 76 smoke tests still pass
- Total test count documented

**Acceptance:** Full smoke test suite passes covering all three Phase 3 deliverables.

---

**Phase Acceptance Criteria:**
- [ ] `tools/claude-tools.json` contains 36 valid tool definitions matching all RPC methods
- [ ] `tools/system-prompt.md` covers viewport model, perception-action loop, value ranges, cursor model, indices
- [ ] Claude interactively selects correct tools with correct params for multi-step tasks
- [ ] CLI builds as separate JAR, all subcommands work, extension build unaffected
- [ ] WebSocket push broadcasts section-level change notifications on state change
- [ ] No notifications broadcast when state is idle
- [ ] All smoke tests pass (existing 76 + new Phase 3 tests)
- [ ] Extension loads and runs in Bitwig with no regressions

**Completion triggers Phase 4 → version `0.4.0`**

---

## Plan Amendments

<!-- Log any changes to the plan after creation -->

| Date | Version | Amendment | Reason |
|------|---------|-----------|--------|
