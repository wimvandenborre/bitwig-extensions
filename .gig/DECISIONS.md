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

## 2026-03-01 — Scope: What does Phase 12 cover?

**Decision:** Phase 12 adds two layers: (1) a **transaction method** `session/transaction` that executes an arbitrary sequence of RPC calls atomically with stop-on-error semantics, and (2) a **macro layer** of predefined compound operations that collapse common multi-call workflows into single RPC methods. Together these reduce agent round-trips from 5-10 calls to 1 for common workflows.
**Rationale:** The LLM agent currently chains calls manually — creating a track with a device takes 3 calls, writing a clip takes 3 calls, building a full song section takes 10+ calls. Each round-trip costs latency and context window tokens. Transactions let the agent batch arbitrary calls. Macros go further by encapsulating the most common patterns server-side with validation and proper error handling.
**Alternatives considered:** Only transactions (no macros) — misses the opportunity to optimize the most common workflows. Only macros (no transactions) — forces us to anticipate every workflow the agent might need.
**Status:** ACTIVE
**ID:** D-12.1

## 2026-03-01 — API: Transaction method design

**Decision:** New RPC method `session/transaction` accepts `{ operations: [{ method: "clip/create", params: {...} }, ...] }`. Executes each operation sequentially within a single handler call (same flush cycle). Returns `{ results: [...], completedCount: N }`. On first error, stops execution and returns `{ results: [...partial...], completedCount: N, error: { step: N, code, message } }`. Each result entry is the raw return value from the handler.
**Rationale:** JSON-RPC batch already exists but doesn't stop on first error — it executes ALL requests and returns individual responses. `session/transaction` adds stop-on-error semantics, which is critical for dependent sequences (e.g., create clip then select it — if create fails, don't try to select). Executing within a single handler call means all operations run in one flush cycle, which is the same atomicity guarantee as `clip/setNotes` loops. The dispatcher already has an internal `handle` method we can call per-operation.
**Alternatives considered:** Modifying JSON-RPC batch behavior to add stop-on-error — breaks JSON-RPC 2.0 spec compliance. A `session/execute` with a script DSL — over-engineered for the use case.
**Status:** AMENDED — User added `preSnapshot` and `postSnapshot` booleans. See D-12.2a.
**ID:** D-12.2

## 2026-03-01 — API: Transaction error semantics

**Decision:** Stop on first error. No rollback — Bitwig API has no transactional undo (app/undo is user-level, not granular). Return partial results so the agent knows what succeeded and what failed. The agent can decide to call `app/undo` if needed. Transaction errors use a new error code `-32010` (`TRANSACTION_STEP_FAILED`) with data `{ step, method, error: { code, message } }`.
**Rationale:** True rollback is impossible — Bitwig operations like `track.createInstrumentTrack()` have side effects that can't be undone programmatically. The best we can do is stop-on-error to prevent cascading failures (the original trigger for bug fixes in Phase 11). Partial results give the agent enough information to recover.
**Alternatives considered:** Attempt `app/undo` N times for N completed steps — fragile and could undo unrelated user actions. Ignore errors and continue — defeats the purpose of transactions.
**Status:** AMENDED — User added optional `rollback: "undoAll"` flag. See D-12.3a.
**ID:** D-12.3

## 2026-03-01 — API: Which macros to include?

**Decision:** 4 macros targeting the highest-impact workflows:
1. `macro/createTrack` — create instrument/audio/effect track + rename + optionally insert a Bitwig device. Params: `{ type, name, device? }`. Returns `{ trackIndex }`.
2. `macro/createClip` — create empty clip + select it (cursor clip ready for note writing). Params: `{ trackIndex, slotIndex, lengthInBeats, name? }`. Returns `{ ok }`.
3. `macro/writeClip` — create clip + select + write notes + optionally rename. All-in-one note writing. Params: `{ trackIndex, slotIndex, lengthInBeats, notes, name? }`. Returns `{ count }`.
4. `macro/buildSection` — create a scene + rename + write clips across multiple tracks in one call. Params: `{ sceneName, clips: [{ trackIndex, lengthInBeats, notes, name? }, ...] }`. Returns `{ sceneIndex, clipCount }`.
**Rationale:** These collapse the 4 most common multi-step workflows from the system prompt: track setup (3 calls → 1), clip creation (3 calls → 1), clip writing (3-4 calls → 1), and section building (10+ calls → 1). `buildSection` is the highest-impact macro — building a 4-track song section currently requires 12+ individual calls. All macros are deterministic (no async scheduling needed) and execute within a single flush cycle.
**Alternatives considered:** More macros (e.g., `macro/mixTracks`, `macro/setupCueMarkers`) — diminishing returns, can be done via transactions. Fewer macros (only transaction, no macros) — misses the 10x reduction in calls for `buildSection`.
**Status:** AMENDED — User redlined macro params to be explicit/deterministic. See D-12.4a.
**ID:** D-12.4

## 2026-03-01 — Architecture: Where does the code live?

**Decision:** Transaction logic goes in `JsonRpcDispatcher` as a new registered method `session/transaction` (registered in `GigMaestroExtension.init()` alongside `session/snapshot`). Macros go in a new `MacroHandler` class that takes a `JsonRpcDispatcher` reference to call existing methods internally. MacroHandler calls `dispatcher.handleInternal(method, params)` — a new package-private method that executes a handler and returns the result (or throws) without JSON serialization overhead.
**Rationale:** Transaction is a dispatcher-level concern (it orchestrates arbitrary methods). Macros are a handler-level concern (they compose specific methods). Giving MacroHandler a dispatcher reference lets macros reuse all existing validation and logic without duplicating code. The `handleInternal` method avoids serializing/deserializing JSON for internal calls.
**Alternatives considered:** Macros calling Bitwig API directly (bypassing handlers) — duplicates validation logic, maintenance burden. Macros as middleware wrapping the dispatcher — over-complicated for 4 methods.
**Status:** ACTIVE
**ID:** D-12.5

## 2026-03-01 — Tools: How many new tool schemas?

**Decision:** 6 new tool schemas: `session_transaction`, `macro_createTrack`, `macro_createClip`, `macro_writeClip`, `macro_buildSection`, plus update to system prompt with a "Transactions & Macros" section. Total RPC methods: 105 (99 + 6). Update system prompt to recommend macros over manual call sequences.
**Rationale:** Each new RPC method gets a tool schema following the existing 1:1 pattern. The system prompt update is critical — it should actively recommend `macro/buildSection` over the manual 10-step workflow, otherwise the agent won't use it.
**Alternatives considered:** Not creating tool schemas for transactions (let agent use raw JSON-RPC batch) — the agent won't discover or use it without a tool definition.
**Status:** ACTIVE
**ID:** D-12.6

## 2026-03-01 — API (amended): Transaction method design with snapshot options

**Decision:** New RPC method `session/transaction` accepts `{ operations: [{ method: "clip/create", params: {...} }, ...], preSnapshot?: boolean, postSnapshot?: boolean }`. `preSnapshot` (default false) captures and returns a full state snapshot before executing any operations. `postSnapshot` (default false) captures and returns a snapshot after all operations complete (or after the last successful step on error). Executes each operation sequentially within a single handler call (same flush cycle). Returns `{ results: [...], completedCount: N, preSnapshot?: {...}, postSnapshot?: {...} }`. On first error, stops execution and returns `{ results: [...partial...], completedCount: N, error: { step: N, code, message }, preSnapshot?: {...}, postSnapshot?: {...} }`. Each result entry is the raw return value from the handler. **Controller-thread safety:** The transaction executor must ensure each step runs on the Bitwig controller thread (within `flush()` context).
**Rationale:** The LLM agent often needs one snapshot before and one after a transaction instead of N intermediate ones. Adding optional booleans avoids extra round-trips for `session/snapshot` before and after the transaction call. Controller-thread safety ensures all Bitwig API calls happen on the correct thread, matching the existing `flush()` → `CommandQueue.drain()` pattern.
**Alternatives considered:** Original D-12.2 without snapshot options — requires 2 extra round-trips for the common before/after pattern.
**Status:** ACTIVE
**ID:** D-12.2a

## 2026-03-01 — API (amended): Transaction error semantics with optional rollback

**Decision:** Default behavior: stop on first error, return partial results + failed step index. No automatic rollback. Optional flag `rollback: "undoAll"` — when set, after stopping on error, call `app/undo` N times for N successfully completed steps (best-effort). The rollback is best-effort because `app/undo` operates on Bitwig's user-level undo stack and may undo unrelated user actions if the transaction steps didn't each create their own undo entry. Transaction errors use error code `-32010` (`TRANSACTION_STEP_FAILED`) with data `{ step, method, error: { code, message } }`. When rollback is used, add `{ rolledBack: true, undoCount: N }` to the response.
**Rationale:** The agent sometimes needs atomic semantics where partial completion is worse than full failure (e.g., creating a track + inserting a device — a track with no device is useless). `undoAll` gives the agent the option without forcing it. The "best-effort" qualifier is important — Bitwig's undo is user-level and not all operations create distinct undo entries.
**Alternatives considered:** Original D-12.3 without rollback option — agent must manually call `app/undo` in a loop, which wastes round-trips and requires knowing the count. Automatic rollback always — too aggressive for operations where partial success is fine.
**Status:** ACTIVE
**ID:** D-12.3a

## 2026-03-01 — API (amended): Macro definitions with explicit, deterministic params

**Decision:** 4 macros with explicit verb naming and deterministic params:
1. `macro/createTrack` — create + rename + optionally insert device. Params: `{ type: "audio"|"instrument"|"effect", name?: string, position?: int, device?: string }`. Returns `{ trackIndex }`.
2. `macro/createClip` — create empty clip + select (cursor clip ready). Params: `{ trackIndex: int, sceneIndex: int, lengthBeats: number }`. Returns `{ ok }`.
3. `macro/writeClip` — create clip + select + write notes. Params: `{ trackIndex: int, sceneIndex: int, stepSize: number, notes: [{x, y, velocity, duration}, ...], name?: string }`. Returns `{ count }`.
4. `macro/buildSection` — create scene + write clips across tracks. Params: `{ sceneName: string, clips: [{ trackIndex: int, lengthBeats: number, stepSize: number, notes: [{x, y, velocity, duration}, ...], name?: string }, ...] }`. Returns `{ sceneIndex, clipCount }`.
**Rationale:** User redline: params must be concrete and deterministic — no abstract `templateId`, `rootNote`, `scale`, `instrumentationPreset` fields that would require a template registry or music theory engine. The macros should compose existing RPC operations, not introduce new abstractions. `stepSize` is required for `writeClip` and `buildSection` because `clip/setNotes` requires knowing the step resolution. `sceneIndex` replaces `slotIndex` for consistency with scene-oriented workflows.
**Alternatives considered:** Original D-12.4 with `slotIndex` naming — `sceneIndex` is more consistent since macros operate at the scene level. Abstract `buildSection` with `templateId/rootNote/scale` — requires a template system that doesn't exist and introduces non-determinism.
**Status:** ACTIVE
**ID:** D-12.4a
