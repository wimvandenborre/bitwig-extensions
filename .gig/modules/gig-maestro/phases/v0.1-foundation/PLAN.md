# Plan

> Living document — reflects what was done, not just what was intended.
> This file tracks the ACTIVE phase only. Completed phases are archived to `phases/`.

---

## Active Phase

### Phase 1 — Foundation (v0.1.x)

> Scaffold the Gradle project, implement the JSON-RPC 2.0 dispatcher, stand up dual HTTP+WebSocket servers, wire the thread-safe command queue, and expose Transport, TrackBank, MasterTrack, and Application handlers — producing a working `.bwextension` that can be loaded in Bitwig 6.0 and queried from `curl` or a WebSocket client.

**Decisions:** D-1.1a, D-1.2, D-1.3, D-1.4, D-1.5, D-1.6, D-1.7a

| Batch | Version | Title | Delegation | Dependencies | Status |
|-------|---------|-------|------------|--------------|--------|
| 1.1 | `0.1.1` | Gradle project scaffold | in-session | — | done |
| 1.2 | `0.1.2` | JSON-RPC 2.0 dispatcher | in-session | — | done |
| 1.3 | `0.1.3` | HTTP + WebSocket servers | in-session | 1.2 | done |
| 1.4 | `0.1.4` | Command queue + thread bridge | in-session | 1.3 | done |
| 1.5 | `0.1.5` | Extension entry point + observers + snapshot + api.list + Application handler | in-session | 1.4 | done |
| 1.6 | `0.1.6` | Transport action handlers | in-session | 1.5 | done |
| 1.7 | `0.1.7` | Track + Master action handlers | in-session | 1.5 | done |
| 1.8 | `0.1.8` | Integration test + smoke suite | in-session | 1.6, 1.7 | done |

---

### Batch 1.1 — Gradle project scaffold

**Delegation:** in-session
**Decisions:** D-1.1a, D-1.7a
**Files:**
- `build.gradle.kts`
- `settings.gradle.kts`
- `gradle.properties`
- `src/main/java/dev/gregross/gig/` (package directories)
- `.gitignore`

**Work:**
- Initialize Gradle wrapper (Kotlin DSL)
- Configure `com.bitwig:extension-api:25` dependency
- Add `org.java-websocket:Java-WebSocket` and `com.google.code.gson:gson` dependencies
- Configure Shadow plugin to produce `.bwextension` fat JAR
- Add a deploy task that copies the `.bwextension` to `~/Documents/Bitwig Studio/Extensions/`
- Create package directory structure: `extension/`, `rpc/`, `server/`, `handlers/`
- Set up `.gitignore` for Gradle, IDE, and build artifacts

**Test criteria:**
- `./gradlew build` compiles successfully with zero errors
- `./gradlew shadowJar` produces a `.bwextension` file
- Package directories exist under `src/main/java/dev/gregross/gig/`

**Acceptance:** Clean Gradle build producing a `.bwextension` artifact.

---

### Batch 1.2 — JSON-RPC 2.0 dispatcher

**Delegation:** in-session
**Decisions:** D-1.4
**Files:**
- `src/main/java/dev/gregross/gig/rpc/JsonRpcRequest.java`
- `src/main/java/dev/gregross/gig/rpc/JsonRpcResponse.java`
- `src/main/java/dev/gregross/gig/rpc/JsonRpcError.java`
- `src/main/java/dev/gregross/gig/rpc/JsonRpcDispatcher.java`
- `src/main/java/dev/gregross/gig/rpc/MethodHandler.java`

**Work:**
- `JsonRpcRequest` — parse incoming JSON into method, params, id fields
- `JsonRpcResponse` — build success/error response JSON
- `JsonRpcError` — standard error codes (-32700, -32600, -32601, -32602, -32603) plus application error range
- `MethodHandler` — functional interface: `JsonElement handle(JsonObject params)`
- `JsonRpcDispatcher` — method registry (`Map<String, MethodHandler>`), `register()`, `handle(String json)` returning response JSON. Supports single requests, notifications (no response), and batch requests (JSON array). Uses Gson for parse/serialize.

**Test criteria:**
- Unit test: dispatcher returns correct response for registered method
- Unit test: dispatcher returns `-32601` for unknown method
- Unit test: batch request returns array of responses
- Unit test: notification (no `id`) returns null (no response)
- `./gradlew build` passes

**Acceptance:** Dispatcher correctly routes requests, handles errors per spec, supports batch mode.

---

### Batch 1.3 — HTTP + WebSocket servers

**Delegation:** in-session
**Decisions:** D-1.2, D-1.3
**Depends on:** Batch 1.2
**Files:**
- `src/main/java/dev/gregross/gig/server/HttpRpcServer.java`
- `src/main/java/dev/gregross/gig/server/WsRpcServer.java`
- `src/main/java/dev/gregross/gig/server/ServerManager.java`

**Work:**
- `HttpRpcServer` — wraps `com.sun.net.httpserver.HttpServer`. Routes:
  - `POST /rpc` — passes body to `JsonRpcDispatcher`, returns response
  - `GET /health` — returns `{"status":"ok","version":"0.1.0"}`
  - CORS headers for local development
- `WsRpcServer` — extends `WebSocketServer` (Java-WebSocket). `onMessage` passes to `JsonRpcDispatcher`, sends response. Tracks connected clients for notification broadcast.
- `ServerManager` — lifecycle wrapper: `start(port)` starts both servers (HTTP on `port`, WebSocket on `port+1`), `stop()` shuts both down, `broadcast(json)` sends to all WebSocket clients.

**Test criteria:**
- Unit test: `HttpRpcServer` processes a POST to `/rpc` and returns valid JSON-RPC response
- Unit test: `/health` returns status OK
- Unit test: `WsRpcServer` echoes correct response on message
- `./gradlew build` passes

**Acceptance:** Both servers start, accept connections, and route to dispatcher.

---

### Batch 1.4 — Command queue + thread bridge

**Delegation:** in-session
**Decisions:** D-1.5
**Depends on:** Batch 1.3
**Files:**
- `src/main/java/dev/gregross/gig/rpc/CommandQueue.java`
- `src/main/java/dev/gregross/gig/rpc/RpcCommand.java`

**Work:**
- `RpcCommand` — wraps a JSON-RPC request string + a `CompletableFuture<String>` for the response
- `CommandQueue` — thread-safe queue (`ConcurrentLinkedQueue<RpcCommand>`)
  - `enqueue(request)` → returns `CompletableFuture<String>` (called from network threads)
  - `drainAndExecute(dispatcher)` → dequeues all pending commands, executes on current thread (Bitwig session thread), completes futures with responses
- Update `HttpRpcServer` and `WsRpcServer` to enqueue commands instead of calling dispatcher directly
- Network threads call `host.requestFlush()` after enqueue to trigger the Bitwig flush cycle

**Test criteria:**
- Unit test: enqueue + drain produces correct response via future
- Unit test: multiple commands drain in FIFO order
- `./gradlew build` passes

**Acceptance:** Commands flow from network thread → queue → session thread → response back to caller.

---

### Batch 1.5 — Extension entry point + observers + snapshot + api.list + Application handler

**Delegation:** in-session
**Decisions:** D-1.1a, D-1.6, D-1.7a
**Depends on:** Batch 1.4
**Files:**
- `src/main/java/dev/gregross/gig/extension/GigMaestroDefinition.java`
- `src/main/java/dev/gregross/gig/extension/GigMaestroExtension.java`
- `src/main/java/dev/gregross/gig/extension/StateCache.java`
- `src/main/java/dev/gregross/gig/handlers/ApplicationHandler.java`

**Work:**
- `GigMaestroDefinition` — extends `ControllerExtensionDefinition`. Name: "Gig Maestro", vendor: "gregross", API version: 25, UUID generated. Zero MIDI ports (pure network extension).
- `GigMaestroExtension` — extends `ControllerExtension`
  - `init()`: create Bitwig API objects (Transport, TrackBank 64 tracks, MasterTrack, Application), create `JsonRpcDispatcher`, create `CommandQueue`, register all observers, build `StateCache`, register `session/snapshot` + `api/list` + Application handlers, start `ServerManager`
  - `flush()`: call `commandQueue.drainAndExecute(dispatcher)`, update `StateCache` snapshot, push state notifications to WebSocket clients
  - `exit()`: stop `ServerManager`, clean up
- `StateCache` — thread-safe snapshot of all observed DAW state. Updated on session thread during `flush()`. Observers for:
  - **Transport:** `isPlaying`, `isArrangerRecordEnabled`, `tempo`, `playPosition`, `timeSignature`, `isArrangerLoopEnabled`, `isMetronomeEnabled`
  - **TrackBank (64):** `name`, `volume`, `pan`, `mute`, `solo`, `arm`, `color` per track
  - **MasterTrack:** `volume`, `pan`
  - **Application:** `projectName`, `canUndo`, `canRedo`, `hasActiveEngine`
- Register RPC methods:
  - `session/snapshot` → returns full state cache as JSON (transport, tracks, master, application — all in one response). Pure read off the cache, no Bitwig API calls at request time.
  - `api/list` → returns array of all registered method names with parameter schemas. Introspection endpoint for LLM agents and CLI tooling.
  - `app/undo` → `application.undo()`
  - `app/redo` → `application.redo()`
  - `app/getState` → returns `{ projectName, canUndo, canRedo, hasActiveEngine }`
- Port configurable via extension preferences (`host.getPreferences()`)

**Test criteria:**
- `./gradlew shadowJar` produces valid `.bwextension`
- Extension loads in Bitwig without errors (manual verification)
- `curl` to `/health` returns OK
- `curl` to `session/snapshot` returns JSON with transport, tracks, master, and app sections (validates full pipeline: HTTP → queue → session thread → cache read → response)
- `curl` to `api/list` returns registered method names
- `app/undo` triggers undo in Bitwig (manual verification)
- `./gradlew build` passes

**Acceptance:** Extension loads, full observer pipeline wired, `session/snapshot` returns complete DAW state, `api/list` provides method introspection, Application handler works.

---

### Batch 1.6 — Transport action handlers

**Delegation:** in-session
**Decisions:** D-1.6
**Depends on:** Batch 1.5
**Files:**
- `src/main/java/dev/gregross/gig/handlers/TransportHandler.java`

**Work:**
Register JSON-RPC action methods on the dispatcher (observers and state reads already wired in 1.5 via `StateCache` and `session/snapshot`):
- `transport/play` → `transport.play()`
- `transport/stop` → `transport.stop()`
- `transport/record` → `transport.record()`
- `transport/togglePlay` → `transport.togglePlay()`
- `transport/rewind` → `transport.rewind()`
- `transport/fastForward` → `transport.fastForward()`
- `transport/tapTempo` → `transport.tapTempo()`
- `transport/setTempo` → `transport.tempo().set(value)` (params: `{ tempo: number }`)
- `transport/setPosition` → `transport.getPosition().set(value)` (params: `{ beats: number }`)
- `transport/setLoop` → `transport.isArrangerLoopEnabled().set(value)` (params: `{ enabled: boolean }`)
- `transport/setMetronome` → `transport.isMetronomeEnabled().set(value)` (params: `{ enabled: boolean }`)

**Test criteria:**
- `curl` to `transport/play` starts playback, `transport/stop` stops it (manual verification in Bitwig)
- `curl` to `transport/setTempo` changes tempo visible in Bitwig
- `session/snapshot` reflects state changes after actions
- `./gradlew build` passes

**Acceptance:** All transport actions callable via HTTP and WebSocket, state cache updates reflected in `session/snapshot`.

---

### Batch 1.7 — Track + Master action handlers

**Delegation:** in-session
**Decisions:** D-1.6
**Depends on:** Batch 1.5
**Files:**
- `src/main/java/dev/gregross/gig/handlers/TrackHandler.java`
- `src/main/java/dev/gregross/gig/handlers/MasterHandler.java`

**Work:**
Register JSON-RPC action methods (observers and state reads already wired in 1.5 via `StateCache` and `session/snapshot`):

**TrackHandler:**
- `track/setVolume` → `track.volume().set(value)` (params: `{ index, value }`)
- `track/setPan` → `track.pan().set(value)` (params: `{ index, value }`)
- `track/setMute` → `track.mute().set(value)` (params: `{ index, muted }`)
- `track/setSolo` → `track.solo().set(value)` (params: `{ index, soloed }`)
- `track/setArm` → `track.arm().set(value)` (params: `{ index, armed }`)

**MasterHandler:**
- `master/setVolume` → `masterTrack.volume().set(value)` (params: `{ value }`)
- `master/setPan` → `masterTrack.pan().set(value)` (params: `{ value }`)

**Test criteria:**
- `curl` to `track/setVolume` changes a track's volume in Bitwig (manual verification)
- `curl` to `master/setVolume` changes master volume in Bitwig (manual verification)
- `session/snapshot` reflects updated values after mutations
- `./gradlew build` passes

**Acceptance:** Track and master state writable via RPC, mutations reflected in snapshot.

---

### Batch 1.8 — Integration test + smoke suite

**Delegation:** in-session
**Decisions:** D-1.2, D-1.6
**Depends on:** Batch 1.6, 1.7
**Files:**
- `scripts/smoke-test.sh` (or equivalent)

**Work:**
End-to-end validation of the complete v0.1.0 API surface:

1. **Health check:** `GET /health` returns `{"status":"ok","version":"0.1.0"}`
2. **Pipeline validation:** `session/snapshot` returns full state (transport, tracks, master, app)
3. **Introspection:** `api/list` returns all registered methods
4. **Transport actions:** `transport/play`, `transport/stop`, `transport/setTempo`, verify state changes via `session/snapshot`
5. **Track actions:** `track/setVolume`, `track/setMute` on a populated project, verify via snapshot
6. **Master actions:** `master/setVolume`, verify via snapshot
7. **Application actions:** `app/undo`, `app/redo`, verify `canUndo`/`canRedo` toggle
8. **WebSocket:** connect via `wscat` or similar, send request, verify response + receive state notifications on transport state change
9. **Batch request:** send JSON array of multiple requests, verify array response
10. **Error handling:** send malformed JSON, unknown method, invalid params — verify correct error codes

Create a `scripts/smoke-test.sh` that runs the HTTP tests via `curl` and reports pass/fail.

**Test criteria:**
- All HTTP methods respond with correct JSON-RPC responses
- WebSocket client receives bidirectional communication (request/response + notifications)
- Error codes match JSON-RPC 2.0 spec (-32700, -32601, -32602)
- No errors in Bitwig controller console
- Smoke test script exits 0

**Acceptance:** Complete v0.1.0 API surface validated end-to-end over both transports. Smoke test passes cleanly.

---

**Phase Acceptance Criteria:**
- [ ] `./gradlew build` compiles with zero errors
- [ ] `.bwextension` loads in Bitwig 6.0 without errors
- [ ] HTTP `/health` returns OK
- [ ] All transport methods (play, stop, record, tempo, position, loop, metronome) work via `curl`
- [ ] All track methods (getAll, get, setVolume, setPan, setMute, setSolo, setArm) work via `curl`
- [ ] Master track methods (get, setVolume, setPan) work via `curl`
- [ ] Application methods (undo, redo, getState) work via `curl`
- [ ] WebSocket client receives JSON-RPC notifications on state changes
- [ ] Thread safety: no crashes or deadlocks under concurrent access

**Completion triggers Phase 2 → version `0.2.0`**

---

## Plan Amendments

<!-- Log any changes to the plan after creation -->

| Date | Version | Amendment | Reason |
|------|---------|-----------|--------|
| 2026-02-27 | 0.1.0 | Expanded 1.5 to include observers, StateCache, session/snapshot, api/list, and Application handler. Simplified 1.6/1.7 to action-only handlers. Reworked 1.8 from "Application handler + integration test" to "Integration test + smoke suite". | User feedback: validate full pipeline at 1.5 before action handlers; separate concerns in 1.8. |
