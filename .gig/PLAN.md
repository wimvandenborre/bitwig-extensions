# Plan

> Living document — reflects what was done, not just what was intended.
> This file tracks the ACTIVE phase only. Completed phases are archived to `phases/`.

---

## Active Phase

### Phase 12 — Transaction + Macro Layer (v0.12.x)

> Reduce agent round-trips from 5-10 calls to 1 for common workflows by adding a transaction executor (`session/transaction`) with stop-on-error semantics, optional rollback, and pre/post snapshot capture, plus 4 macro methods (`macro/createTrack`, `macro/createClip`, `macro/writeClip`, `macro/buildSection`) that compose existing handlers server-side via a new `handleInternal()` dispatcher method.

**Decisions:** D-12.1, D-12.2a, D-12.3a, D-12.4a, D-12.5, D-12.6

| Batch | Version | Title | Delegation | Status |
|-------|---------|-------|------------|--------|
| 12.1 | `0.12.1` | Dispatcher `handleInternal()` + `session/transaction` | in-session | pending |
| 12.2 | `0.12.2` | MacroHandler + `macro/createTrack` + `macro/createClip` | in-session | pending |
| 12.3 | `0.12.3` | `macro/writeClip` + `macro/buildSection` | in-session | pending |
| 12.4 | `0.12.4` | Unit tests | in-session | pending |
| 12.5 | `0.12.5` | Tool schemas + system prompt update | in-session | pending |
| 12.6 | `0.12.6` | Smoke tests | in-session | pending |

### Batch 12.1 — Dispatcher `handleInternal()` + `session/transaction`

**Delegation:** in-session
**Decisions:** D-12.2a, D-12.3a, D-12.5
**Files:**
- `src/main/java/dev/gregross/gig/rpc/JsonRpcDispatcher.java` (modify)
- `src/main/java/dev/gregross/gig/extension/GigMaestroExtension.java` (modify)
- `src/main/java/dev/gregross/gig/extension/StateCache.java` (modify — if snapshot capture method needed)

**Work:**
1. Add `handleInternal(String method, JsonObject params)` package-private method to `JsonRpcDispatcher`. Looks up handler, calls it, returns `JsonElement` result directly (no JSON serialization round-trip). Throws `RpcException` on handler error, `IllegalArgumentException` on missing method.
2. Register `session/transaction` as a handler in the dispatcher. Accepts `{ operations: [{method, params}, ...], preSnapshot?: boolean, postSnapshot?: boolean, rollback?: "undoAll" }`.
3. Transaction executor: iterate `operations` sequentially, call `handleInternal()` per step, collect results. On first `RpcException` or `Exception`: stop, record failed step index/method/error.
4. Pre/post snapshot: if `preSnapshot` is true, call `stateCache.captureSnapshot()` (or equivalent) before executing. If `postSnapshot` is true, capture after all operations (or after last successful step on error).
5. Rollback: if `rollback: "undoAll"` and error occurred, call `handleInternal("app/undo", {})` N times for N completed steps. Add `{ rolledBack: true, undoCount: N }` to response.
6. Return format: `{ results: [...], completedCount: N, preSnapshot?: {...}, postSnapshot?: {...} }` on success. On error: add `error: { step, method, error: { code, message } }`.
7. Error code: `-32010` (`TRANSACTION_STEP_FAILED`).
8. Wire `session/transaction` registration in `GigMaestroExtension.init()` — needs dispatcher + stateCache references.

**Test criteria:**
- `handleInternal("app/getInfo", {})` returns a valid `JsonElement`
- `handleInternal("nonexistent/method", {})` throws `RpcException`
- Transaction with 3 valid operations returns `completedCount: 3` and 3 results
- Transaction with error on step 2 returns `completedCount: 1`, partial results, and error with `step: 1`
- `preSnapshot: true` includes snapshot in response
- `rollback: "undoAll"` calls undo for completed steps on error

**Acceptance:** `session/transaction` executes arbitrary operation sequences with stop-on-error, optional snapshot, and optional rollback.

---

### Batch 12.2 — MacroHandler + `macro/createTrack` + `macro/createClip`

**Delegation:** in-session
**Decisions:** D-12.4a, D-12.5
**Depends on:** Batch 12.1 (`handleInternal()`)
**Files:**
- `src/main/java/dev/gregross/gig/handlers/MacroHandler.java` (create)
- `src/main/java/dev/gregross/gig/extension/GigMaestroExtension.java` (modify — wire MacroHandler)

**Work:**
1. Create `MacroHandler` class. Constructor takes `JsonRpcDispatcher` reference for calling `handleInternal()`.
2. `macro/createTrack` — Params: `{ type: "audio"|"instrument"|"effect", name?: string, position?: int, device?: string }`.
   - Call `handleInternal("track/create{Type}", { position })` (where Type = Audio/Instrument/Effect).
   - If `name` provided: call `handleInternal("track/rename", { name })`.
   - If `device` provided: call `handleInternal("device/insertBitwigDevice", { name: device })`.
   - Return `{ trackIndex }` (from track creation result or snapshot).
3. `macro/createClip` — Params: `{ trackIndex: int, sceneIndex: int, lengthBeats: number }`.
   - Call `handleInternal("clip/create", { trackIndex, slotIndex: sceneIndex, lengthInBeats: lengthBeats })`.
   - Call `handleInternal("clip/select", { trackIndex, slotIndex: sceneIndex })`.
   - Return `{ ok: true }`.
4. Register both methods in `MacroHandler.register(dispatcher)`.
5. Wire `MacroHandler` in `GigMaestroExtension.init()` after dispatcher is set up.

**Test criteria:**
- `macro/createTrack` with `type: "instrument", name: "Synth"` calls `track/createInstrument` then `track/rename`
- `macro/createTrack` with `device` param also calls `device/insertBitwigDevice`
- `macro/createClip` calls `clip/create` then `clip/select` with correct param mapping
- Error in any sub-step propagates as RpcException

**Acceptance:** Both macros compose existing handlers via `handleInternal()` and reduce multi-call workflows to single calls.

---

### Batch 12.3 — `macro/writeClip` + `macro/buildSection`

**Delegation:** in-session
**Decisions:** D-12.4a, D-12.5
**Depends on:** Batch 12.2 (MacroHandler exists)
**Files:**
- `src/main/java/dev/gregross/gig/handlers/MacroHandler.java` (modify)

**Work:**
1. `macro/writeClip` — Params: `{ trackIndex: int, sceneIndex: int, stepSize: number, notes: [{x, y, velocity, duration}, ...], name?: string }`.
   - Call `handleInternal("clip/create", { trackIndex, slotIndex: sceneIndex, lengthInBeats: stepSize * maxX })` — compute length from notes or require explicit `lengthBeats`? **Decision: require `lengthBeats` param** alongside `stepSize` to avoid scanning notes. Updated params: `{ trackIndex, sceneIndex, lengthBeats, stepSize, notes, name? }`.
   - Call `handleInternal("clip/select", { trackIndex, slotIndex: sceneIndex })`.
   - Call `handleInternal("clip/setStepSize", { stepSize })` if clip step size needs setting.
   - Call `handleInternal("clip/setNotes", { notes })`.
   - If `name` provided: call `handleInternal("clip/rename", { name })`.
   - Return `{ count: notes.length }`.
2. `macro/buildSection` — Params: `{ sceneName: string, clips: [{ trackIndex, lengthBeats, stepSize, notes, name? }, ...] }`.
   - Call `handleInternal("scene/create", {})` to create a new scene.
   - Call `handleInternal("scene/rename", { sceneIndex: <new scene index>, name: sceneName })`.
   - For each clip in `clips`: execute the `writeClip` sequence (create + select + setNotes) using `handleInternal()` calls, with `sceneIndex` = newly created scene's index.
   - Return `{ sceneIndex, clipCount }`.
3. Scene index discovery: after `scene/create`, need to determine the new scene's index. Use `stateCache.getSceneItemCount() - 1` or capture from snapshot. May need to pass `StateCache` to `MacroHandler` constructor.

**Test criteria:**
- `macro/writeClip` creates clip, selects it, writes notes, optionally renames
- `macro/buildSection` creates scene, renames it, writes N clips across tracks
- Error mid-section (e.g., invalid trackIndex) stops execution and reports which clip failed
- Note count in response matches input notes array length

**Acceptance:** `macro/buildSection` collapses 10+ individual calls into 1 for multi-track scene building.

---

### Batch 12.4 — Unit tests

**Delegation:** in-session
**Decisions:** D-12.2a, D-12.3a, D-12.4a
**Depends on:** Batches 12.1–12.3
**Files:**
- `src/test/java/dev/gregross/gig/rpc/JsonRpcDispatcherTest.java` (modify — add transaction tests)
- `src/test/java/dev/gregross/gig/handlers/MacroHandlerTest.java` (create)

**Work:**
1. Dispatcher tests: `handleInternal()` success/failure, transaction with valid ops, transaction with error at step N, `preSnapshot`/`postSnapshot`, `rollback: "undoAll"`.
2. MacroHandler tests: each macro method with mocked dispatcher, param validation, error propagation from sub-steps.
3. Use existing test patterns (mock handlers registered in dispatcher for unit testing).

**Test criteria:**
- All new unit tests pass via `./gradlew test`
- Transaction edge cases: empty operations array, single op, error on first step, error on last step
- Macro edge cases: missing required params, empty clips array for buildSection

**Acceptance:** Full unit test coverage for transaction executor and all 4 macros.

---

### Batch 12.5 — Tool schemas + system prompt update

**Delegation:** in-session
**Decisions:** D-12.6
**Depends on:** Batches 12.1–12.3 (need final method signatures)
**Files:**
- `tools/claude-tools.json` (modify — add 6 tool schemas)
- `tools/system-prompt.md` (modify — add Transactions & Macros section)

**Work:**
1. Add 6 tool schemas to `claude-tools.json`: `session_transaction`, `macro_createTrack`, `macro_createClip`, `macro_writeClip`, `macro_buildSection`, plus a `macro_buildSection` schema.
2. Each schema follows the existing pattern: `name`, `description`, `input_schema` with required/optional params.
3. Update `system-prompt.md`:
   - Add "Transactions & Macros" section recommending macros over manual call sequences.
   - Document `session/transaction` with examples.
   - Document each macro with params and when to use.
   - Update method count: 99 → 105.

**Test criteria:**
- `claude-tools.json` is valid JSON with 105 tool schemas
- Each new tool has proper `input_schema` with required fields
- System prompt references all 6 new methods
- Offline smoke tests pass (schema validation)

**Acceptance:** Agent can discover and use all 6 new methods via tool schemas.

---

### Batch 12.6 — Smoke tests

**Delegation:** in-session
**Decisions:** D-12.6
**Depends on:** Batches 12.4–12.5
**Files:**
- `scripts/smoke-test.sh` (modify — add offline assertions)

**Work:**
1. Add offline smoke tests:
   - Tool count assertion: 105 tools
   - Schema validation for each new tool
   - System prompt mentions "transaction", "macro"
2. Add online smoke test stubs (if Bitwig is running):
   - `session/transaction` with a simple 2-op sequence
   - `macro/createTrack` basic invocation

**Test criteria:**
- `./scripts/smoke-test.sh --offline` passes with updated assertions
- New tool schemas validated by existing schema checker

**Acceptance:** Smoke suite validates all Phase 12 additions.

---

**Phase Acceptance Criteria:**
- [ ] `session/transaction` executes operation sequences with stop-on-error semantics
- [ ] `preSnapshot` and `postSnapshot` capture state before/after transaction
- [ ] `rollback: "undoAll"` calls undo N times on error (best-effort)
- [ ] `macro/createTrack` creates track + optional rename + optional device insertion in 1 call
- [ ] `macro/createClip` creates + selects clip in 1 call
- [ ] `macro/writeClip` creates + selects + writes notes in 1 call
- [ ] `macro/buildSection` creates scene + writes clips across multiple tracks in 1 call
- [ ] All unit tests pass (`./gradlew test`)
- [ ] All smoke tests pass (`./scripts/smoke-test.sh --offline`)
- [ ] 105 tool schemas in `claude-tools.json`
- [ ] System prompt documents transactions and macros with usage recommendations

**Completion triggers Phase 13 → version `0.13.0`**
