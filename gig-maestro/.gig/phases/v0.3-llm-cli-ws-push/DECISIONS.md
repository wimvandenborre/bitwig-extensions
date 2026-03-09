# Decisions

> Append-only log. Never delete entries — amend or revise instead.
> Active decisions for the current phase live here.
> When a phase completes, its decisions are archived to `phases/`.

<!-- Decision statuses:
  PROPOSED  — Claude's recommendation, awaiting user approval
  ACTIVE    — Approved and in effect
  AMENDED   — Overridden by user (original preserved, new entry appended)
  REVISED   — Claude revised based on new information (original preserved)
-->

<!-- Entry format:
## YYYY-MM-DD — Domain: Question

**Decision:** What was decided.
**Rationale:** Why this choice was made.
**Alternatives considered:** What else was evaluated.
**Status:** PROPOSED | ACTIVE | AMENDED | REVISED
**ID:** D-{batch}.{num}
-->

## 2026-02-27 — LLM: What tool schema format and where to store definitions?

**Decision:** Generate Claude `tool_use` format schemas (JSON) for all 36 RPC methods. Store as `tools/claude-tools.json` (array of tool definitions). Each tool maps 1:1 to an RPC method — tool name is the method name with `/` replaced by `_` (e.g., `transport_play`, `track_setVolume`). Include `session_snapshot` as a tool so the LLM can explicitly request state. Parameters use JSON Schema with types, descriptions, required fields, and enums where applicable. Generate from the handler source code as the source of truth.
**Rationale:** Claude `tool_use` is the primary target (project purpose). 1:1 mapping keeps the mental model simple — every tool is one RPC call. Underscore naming matches Claude tool conventions. Storing as a static JSON file makes it easy to load into any LLM client, version-control, and diff. The handler source is the canonical parameter definition — generating from it ensures schemas stay accurate.
**Alternatives considered:** OpenAI function calling format (can be derived from Claude format trivially), dynamic generation at runtime via `api/list` (adds complexity, schemas need human-written descriptions that don't exist in the RPC layer), embedding schemas in Java annotations (over-engineered).
**Status:** ACTIVE
**ID:** D-3.1

## 2026-02-27 — LLM: What system prompt and mental model to provide?

**Decision:** Write a system prompt fragment (`tools/system-prompt.md`) that explains: (1) the viewport model — TrackBank is a fixed 64-track window, clip slots are 8 per track, device params are 8 per page, scenes are 8; (2) the perception-action loop — always call `session_snapshot` first to perceive state, then act, then snapshot again to verify; (3) value ranges — volume/pan are 0.0–1.0 normalized, tempo is raw BPM, position is beats; (4) cursor model — cursor track and device follow UI selection, use `cursor_selectTrack` and `device_selectNext/Previous` to navigate; (5) index conventions — all indices are 0-based. Include a few-shot example showing a typical workflow (snapshot → interpret → act → verify).
**Rationale:** LLMs need the mental model to use tools effectively. Without understanding that values are normalized 0-1, an agent will try to set volume to "80" instead of "0.8." The perception-action loop prevents blind actions. The viewport concept prevents confusion about why track 65 doesn't exist.
**Alternatives considered:** Embedding descriptions in each tool schema only (misses the holistic mental model), no system prompt (agents would guess at conventions and fail).
**Status:** ACTIVE
**ID:** D-3.2

## 2026-02-27 — LLM: How to test end-to-end with Claude?

**Decision:** Create a test script (`scripts/llm-test.sh` or Python) that loads the tool schemas and system prompt, sends a natural language instruction to Claude via the Anthropic API, executes the returned tool calls against the running Gig Maestro extension via HTTP, and feeds results back to Claude. Test with a concrete task: "Set the tempo to 120, mute track 2, and tell me the project name." This validates the full loop: prompt → tool selection → RPC execution → result interpretation.
**Rationale:** The tool schemas and system prompt are only valuable if they actually work with Claude. An end-to-end test catches schema errors (wrong types, missing descriptions) that unit tests can't find. A concrete multi-step task exercises tool selection, parameter formatting, and result parsing.
**Alternatives considered:** Manual testing via Claude.ai chat (not reproducible, can't automate), unit testing schemas against JSON Schema spec only (validates format but not LLM usability).
**Status:** AMENDED
**ID:** D-3.3

## 2026-02-28 — LLM: How to test end-to-end with Claude? (amended)

**Decision:** Test interactively by loading the system prompt and tool schemas into a Claude conversation (or Claude Code), giving it real tasks ("set the tempo to 128 and solo track 2"), and verifying manually that the LLM picks the right tools with correct params. No custom scripted test harness. Validation criteria: (1) LLM selects correct tools for natural language instructions, (2) params have correct types and values, (3) `session_snapshot` is used for perception before/after actions. Formalized automated testing deferred to post-Phase 3.
**Rationale:** Interactive testing reveals real LLM behavior — tool selection, parameter formatting, mental model comprehension — better than a scripted harness. A custom API integration script is maintenance overhead for a one-off validation. 15 minutes of interactive testing provides more signal.
**Alternatives considered:** Custom script calling Claude API (original proposal — one-off integration to maintain, less signal than interactive testing).
**Status:** ACTIVE
**ID:** D-3.3a
**Note:** Overridden by user — original: scripted test harness calling Claude API.

## 2026-02-27 — CLI: What framework and project structure for the CLI?

**Decision:** Picocli for the CLI framework. Separate `cli` source set or submodule (`src/cli/java/dev/gregross/gig/cli/`) with its own `main()` method and shadow JAR output. The CLI is a standalone JAR that talks to the running extension over HTTP — it does NOT embed the Bitwig extension. Commands mirror the RPC surface: `gig transport play`, `gig track set-volume --index 0 --value 0.5`, `gig snapshot`, `gig rpc '{"method":"...","params":{}}'` (escape hatch). Output is JSON by default, with `--pretty` for formatted output.
**Rationale:** Picocli is the standard Java CLI framework — annotation-driven, generates usage help, supports subcommands, ~400KB. A separate source set keeps the CLI out of the `.bwextension` (Bitwig doesn't need Picocli). HTTP-only keeps the CLI stateless — just fire `curl`-like requests. The `rpc` escape hatch means the CLI never blocks access to new methods.
**Alternatives considered:** Shell script wrapper around `curl` (fragile, no argument parsing), Go/Rust CLI (different language from the extension, maintenance burden), embedding CLI in the extension JAR (bloats the Bitwig plugin).
**Status:** ACTIVE
**ID:** D-3.4

## 2026-02-27 — WebSocket: How to implement state push notifications?

**Decision:** Add delta detection to `StateCache` — hash each section's serialized JSON (`getTransportState().toString().hashCode()` etc.) and compare against previous flush. When state changes and WebSocket clients are connected, broadcast a JSON-RPC notification listing only which sections changed: `{"jsonrpc":"2.0","method":"state/changed","params":{"changed":["transport","device"]}}`. Client calls `session/snapshot` if it wants the actual data. No inline state in the notification — keeps it lightweight. Integrate into `GigMaestroExtension.flush()` after command queue drain.
**Rationale:** `ServerManager.broadcast()` and `WsRpcServer` client tracking already exist and are tested. The missing piece is delta detection in the flush cycle. Per-section hash comparison is cheap and correct — no custom equals() needed. Notification-only (no inline data) avoids duplicate serialization and keeps the push payload tiny. Client can selectively fetch via snapshot.
**Alternatives considered:** Full snapshot on every flush (wasteful — 64 tracks × 8 clips = huge payload every ~10ms), field-level dirty flags (complex, error-prone with volatile fields), inline changed sections in notification (duplicate serialization, larger payloads).
**Status:** ACTIVE
**ID:** D-3.5

## 2026-02-27 — Architecture: How to organize Phase 3 deliverables?

**Decision:** New directories: `tools/` (tool schemas + system prompt), `src/cli/` (CLI source). Existing packages modified: `extension/StateCache.java` (delta detection), `extension/GigMaestroExtension.java` (broadcast in flush). Build configuration: add Picocli dependency and a separate `cli` shadow JAR task. Keep the `.bwextension` output unchanged — CLI is a separate artifact.
**Rationale:** Clean separation — `tools/` is language-agnostic (JSON + markdown, consumable by any LLM client), `src/cli/` is the Java CLI client, and the extension core stays in `src/main/`. Two build artifacts: `.bwextension` for Bitwig, CLI JAR for command line.
**Alternatives considered:** Putting tool schemas in `src/main/resources/` (they're not runtime resources for the extension), single JAR for both extension and CLI (Bitwig would load Picocli unnecessarily).
**Status:** ACTIVE
**ID:** D-3.6
