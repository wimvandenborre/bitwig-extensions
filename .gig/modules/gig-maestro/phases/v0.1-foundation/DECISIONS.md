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

## 2026-02-27 — Build: What build system and project structure?

**Decision:** Gradle with Kotlin DSL. Single-module project targeting Bitwig Controller API v18 (minimum), built against v20 (current). JDK 21. Output is a `.bwextension` JAR installed to `~/Documents/Bitwig Studio/Extensions/`.
**Rationale:** The official bitwig-extensions repo uses Gradle. Gradle handles fat-JAR packaging (shadow plugin) needed to bundle dependencies into the `.bwextension`. Kotlin DSL provides type-safe build scripts. API v18 minimum ensures compatibility with Bitwig 5.0+.
**Alternatives considered:** Maven (used by DrivenByMoss, viable but Gradle is more idiomatic for the Bitwig ecosystem and has better fat-JAR tooling).
**Status:** AMENDED
**ID:** D-1.1

## 2026-02-27 — Build: What build system and project structure? (amended)

**Decision:** Gradle with Kotlin DSL. Single-module project targeting Bitwig Controller API **v25**. JDK 21. Output is a `.bwextension` JAR installed to `~/Documents/Bitwig Studio/Extensions/`. Targets Bitwig 6.0 (confirmed: installed Bitwig 6.0 Beta 13, existing Maestro.bwextension uses `getRequiredAPIVersion() = 25`).
**Rationale:** User's installed Bitwig is 6.0 Beta 13. The existing Maestro extension already targets API 25. No reason to target older API versions — this project is built for the user's environment.
**Alternatives considered:** API v18-20 (original proposal, unnecessarily conservative given the installed version).
**Status:** ACTIVE
**ID:** D-1.1a
**Note:** Overridden by user — original: API v18 minimum, built against v20.

## 2026-02-27 — Transport: What protocol and transport layer?

**Decision:** JSON-RPC 2.0 over **dual transport** — HTTP (`http://localhost:8787/rpc`) for stateless CLI requests (fire-and-forget commands, one-shot queries) and WebSocket (`ws://localhost:8787/ws`) for persistent LLM agent sessions (bidirectional, real-time state push via notifications). Both listeners on a single configurable port (default `8787`). HTTP health check at `/health`.
**Rationale:** CLI tools benefit from simple HTTP POST (no connection management, easy `curl` testing). LLM agents need persistent WebSocket for real-time DAW state notifications without polling. Both share the same JSON-RPC dispatcher — transport is just plumbing. The JDK built-in `com.sun.net.httpserver` handles both HTTP paths, with WebSocket upgrade handled by Java-WebSocket on the same port or an adjacent port.
**Alternatives considered:** WebSocket-only (forces CLI to maintain connections), HTTP-only with SSE (no true bidirectional), separate ports per transport (unnecessary complexity).
**Status:** ACTIVE
**ID:** D-1.2

## 2026-02-27 — Networking: What libraries for WebSocket and HTTP?

**Decision:** Java-WebSocket (TooTallNate) for WebSocket server (~120KB, zero dependencies). `com.sun.net.httpserver` (JDK built-in) for the HTTP health endpoint. Gson for JSON serialization (~300KB).
**Rationale:** Minimal dependency footprint is critical inside a DAW extension. Java-WebSocket is proven in the Bitwig ecosystem (used by `bitwig-websocket-rpc`). The JDK HTTP server adds zero bytes. Gson is smaller than Jackson and sufficient for JSON-RPC payloads. Total added footprint: ~420KB.
**Alternatives considered:** NanoHTTPD (dormant maintenance), Jackson (larger, ~1.5MB), Javalin/Undertow (far too heavy for an embedded DAW plugin).
**Status:** ACTIVE
**ID:** D-1.3

## 2026-02-27 — RPC: How to implement JSON-RPC 2.0 dispatch?

**Decision:** Manual implementation (~400-500 lines). A `JsonRpcDispatcher` class with a method registry, supporting requests, notifications, batch requests, and standard error codes. No annotation processing or reflection.
**Rationale:** JSON-RPC 2.0 is simple enough that a library adds more weight than value. Manual implementation gives full control over threading (critical in the DAW context), avoids reflection/classpath scanning issues in Bitwig's classloader, and keeps dependencies at zero beyond Gson.
**Alternatives considered:** jsonrpc4j (servlet dependency baggage, reflection-based), Nimbus jsonrpc2-server (less ergonomic, adds json-smart dependency).
**Status:** ACTIVE
**ID:** D-1.4

## 2026-02-27 — Threading: How to handle thread safety between network and Bitwig API?

**Decision:** Producer-consumer pattern with a thread-safe command queue. Network threads (WebSocket/HTTP) enqueue incoming RPC requests. The Bitwig flush cycle dequeues and executes them on the Control Surface Session thread. State snapshots are built on the session thread and published to a concurrent read-only cache that network threads can access for responses.
**Rationale:** Bitwig's Controller API is single-threaded — all API calls must happen on the Control Surface Session thread. Network callbacks arrive on separate threads. A command queue bridges the gap safely. `host.requestFlush()` triggers the dequeue cycle. This pattern is used by `bitwig-websocket-rpc` and DrivenByMoss.
**Alternatives considered:** Direct API calls from network threads (dangerous, documented as unsafe), locking around API calls (risk of deadlock with the audio engine).
**Status:** ACTIVE
**ID:** D-1.5

## 2026-02-27 — API Surface: What Bitwig state to expose in v0.1.0?

**Decision:** Start with a focused core: **Transport** (play/stop/record/tempo/position/time signature), **TrackBank** (name/volume/pan/mute/solo/arm for up to 64 tracks), **MasterTrack** (volume/pan), and **Application** (undo/redo only — `undo()`, `redo()`, `canUndo()`, `canRedo()`, `projectName()`). View toggling, zoom, clipboard, and navigation methods are excluded from v0.1.0. Add clip launcher and device control in later milestones.
**Rationale:** Confirmed via `javap` on the installed Bitwig 6.0 `Application` interface: it exposes undo/redo, project name, engine activation, track creation, panel/view toggling, clipboard ops, zoom, and navigation. For LLM agents, undo/redo and project name are high-value (error recovery, session identification). View toggling and zoom are UI-specific and low-value for programmatic control. Transport and track state remain the primary surface.
**Alternatives considered:** Full Application surface (most methods are UI-focused, not useful for LLM agents), dropping Application entirely (undo/redo is too valuable to skip).
**Status:** ACTIVE
**ID:** D-1.6

## 2026-02-27 — Architecture: What package structure and module boundaries?

**Decision:** Four packages under `com.gigmaestro`: `extension` (Bitwig extension entry points — Definition + Extension classes), `rpc` (JSON-RPC dispatcher, request/response models), `server` (WebSocket + HTTP server lifecycle), `handlers` (RPC method handlers organized by domain — transport, tracks, master, application).
**Rationale:** Clean separation between Bitwig API integration, protocol handling, and network transport. Method handlers are the primary extension point — adding new API surface means adding new handler classes without touching RPC or server code. Keeps the extension entry point thin.
**Alternatives considered:** Flat single-package (doesn't scale), domain-driven with deeper nesting (over-engineered for the current scope).
**Status:** AMENDED
**ID:** D-1.7

## 2026-02-27 — Architecture: What package structure and module boundaries? (amended)

**Decision:** Four packages under `dev.gregross.gig`: `extension` (Bitwig extension entry points — Definition + Extension classes), `rpc` (JSON-RPC dispatcher, request/response models), `server` (WebSocket + HTTP server lifecycle), `handlers` (RPC method handlers organized by domain — transport, tracks, master, application).
**Rationale:** Matches user's existing convention (`dev.gregross.bitwig.maestro` in the existing Maestro extension). Consistent personal domain namespace.
**Alternatives considered:** `com.gigmaestro` (original proposal, not aligned with user's established package convention).
**Status:** ACTIVE
**ID:** D-1.7a
**Note:** Overridden by user — original: `com.gigmaestro`.

## 2026-02-27 — Git: What branching and versioning strategy?

**Decision:** Trunk-based development with short-lived batch branches (`batch/1.x-name`). Squash merge to `main` at each checkpoint. Tag each version (`v0.1.x`). Conventional commits within branches.
**Rationale:** User-specified strategy. Keeps `main` clean with one commit per batch. Tags provide granular rollback points matching the gig versioning scheme. Short-lived branches prevent drift.
**Alternatives considered:** Feature branches per phase (too long-lived), direct commits to main (no review gate).
**Status:** ACTIVE
**ID:** D-1.8
