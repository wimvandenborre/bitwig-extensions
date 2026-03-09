# Plan

> Living document — reflects what was done, not just what was intended.
> This file tracks the ACTIVE phase only. Completed phases are archived to `phases/`.

---

## Active Phase

### Phase 6 — Track Management (v0.6.x)

> Enable the LLM to create, select, rename, delete, and duplicate tracks programmatically. Includes explicit creation methods per track type (audio/instrument/effect), index-based selection with error guardrails, a TrackBankManager utility for index validation, richer responses with cursor context, tool schemas, CLI commands, and smoke tests.

**Decisions:** D-6.1a, D-6.2a, D-6.3a, D-6.4a, D-6.5

| Batch | Version | Title | Delegation | Status |
|-------|---------|-------|------------|--------|
| 6.1 | `0.6.1` | TrackBankManager utility + pure tests (index/offset/errors) | team | done |
| 6.2 | `0.6.2` | Track management RPC handlers | team | done |
| 6.3 | `0.6.3` | Tool schemas + system prompt update | team | done |
| 6.4 | `0.6.4` | CLI TrackManageCommand + smoke tests | team | done |

### Batch 6.1 — TrackBankManager Utility + Pure Tests (Index/Offset/Errors)

**Delegation:** team
**Decisions:** D-6.2a, D-6.3a
**Files:** `src/main/java/dev/gregross/gig/handlers/TrackBankManager.java`, `src/test/java/dev/gregross/gig/handlers/TrackBankManagerTest.java`
**Work:** Create `TrackBankManager` utility class:
- Constructor takes `TrackBank` and bank size (64)
- `validateIndex(int index)` — throws `IllegalArgumentException` with message including bank width and invalid index if out of 0–63 range
- `selectByIndex(int index)` — validates, then calls `trackBank.getItemAt(index).selectInEditor()`
- Unit tests: valid index (boundary 0, 63), out-of-range (-1, 64, 999), error message includes bank width
**Test criteria:** `./gradlew test --tests "*.TrackBankManagerTest"` — all tests pass.
**Acceptance:** TrackBankManager validates indices and delegates selection. Error messages include bank width context.

### Batch 6.2 — Track Management RPC Handlers

**Delegation:** team
**Decisions:** D-6.1a, D-6.2a, D-6.3a, D-6.4a
**Files:** `src/main/java/dev/gregross/gig/handlers/TrackHandler.java`, `src/main/java/dev/gregross/gig/extension/GigMaestroExtension.java`
**Work:**
- Add `Application`, `CursorTrack`, and `TrackBankManager` as new constructor parameters to `TrackHandler`
- Update `GigMaestroExtension.init()` to create `TrackBankManager` and pass new deps
- Register 7 new RPC methods:
  - `track/createAudio` — `application.createAudioTrack(position)`, return `{ok, cursorTrackName}`
  - `track/createInstrument` — `application.createInstrumentTrack(position)`, return `{ok, cursorTrackName}`
  - `track/createEffect` — `application.createEffectTrack(position)`, return `{ok, cursorTrackName}`
  - `track/select` — delegate to `trackBankManager.selectByIndex(index)`, return `{ok, cursorTrackName}`
  - `track/rename` — `cursorTrack.name().set(name)`, return `{ok, cursorTrackName}`
  - `track/deleteSelected` — `cursorTrack.deleteObject()`, return `{ok: true}`
  - `track/duplicate` — `cursorTrack.duplicate()`, return `{ok, cursorTrackName}`
- All richer responses return `JsonObject` with `ok: true` and `cursorTrackName` per D-6.4a
**Test criteria:** `./gradlew shadowJar` builds. Manual test: create audio track, select it, rename, duplicate, delete.
**Acceptance:**
- All 7 methods registered and callable
- All mutation methods return `{ok: true, data: {cursorTrackName, ...}}` (richer responses per D-6.4a)
- `track/deleteSelected` returns `{ok: true}` (no cursor context)
- Out-of-range index on `track/select` returns JSON-RPC error code -32602 with bank width in message
- Invalid operations return structured JSON-RPC errors (TRACK_OUT_OF_RANGE, etc.)

Depends on Batch 6.1.

### Batch 6.3 — Tool Schemas + System Prompt Update

**Delegation:** team
**Decisions:** D-6.1a
**Files:** `tools/claude-tools.json`, `tools/system-prompt.md`
**Work:**
- Add 7 new tool definitions: `track_createAudio`, `track_createInstrument`, `track_createEffect`, `track_select`, `track_rename`, `track_deleteSelected`, `track_duplicate`
- Update `tools/system-prompt.md`: add track management section covering creation (audio/instrument/effect), selection by index, rename, delete, duplicate workflow
- Tool count: >= 55 (48 existing + 7 new)
**Test criteria:** JSON validates. All 7 new tool names present in schema. No existing tools removed or broken.
**Acceptance:** Schema compiles and validates. All 7 new tools have correct schemas with proper param types. No breaking changes to existing tools. System prompt documents the track management workflow.

Depends on Batch 6.2 (needs final method signatures and response shapes).

### Batch 6.4 — CLI TrackManageCommand + Smoke Tests

**Delegation:** team
**Decisions:** D-6.1a
**Files:** `src/cli/java/dev/gregross/gig/cli/TrackCommand.java`, `src/cli/java/dev/gregross/gig/cli/GigCli.java`, `scripts/smoke-test.sh`
**Work:**
- Add new subcommands to existing `TrackCommand`: `create-audio`, `create-instrument`, `create-effect`, `select`, `rename`, `delete-selected`, `duplicate`
- Smoke tests (offline): tool schema checks for 7 new tools, param types, system prompt track management section, CLI help for new subcommands
- Smoke tests (online): create audio track, select by index, rename, verify name in snapshot, duplicate, delete, create instrument track, out-of-range select error with bank width
**Test criteria:** `./scripts/smoke-test.sh --offline` and `./scripts/smoke-test.sh` all pass. `./gradlew test` all pass.
**Acceptance:** CLI track management commands work. Smoke tests cover happy path, error cases, and full create→select→rename→delete workflow.

Depends on Batch 6.2 and Batch 6.3.

**Phase Acceptance Criteria:**
- [ ] 7 new RPC methods registered and callable (>= 55 total)
- [ ] track/createAudio, createInstrument, createEffect create correct track types
- [ ] track/select by index works for empty/named tracks
- [ ] track/select out-of-range returns error with bank width
- [ ] track/rename sets cursor track name
- [ ] track/deleteSelected removes cursor track
- [ ] track/duplicate clones cursor track
- [ ] Richer responses include cursorTrackName
- [ ] TrackBankManager validates indices with helpful errors
- [ ] Tool schemas valid JSON (>= 55 tools, no breaking changes)
- [ ] System prompt documents track management
- [ ] CLI track subcommands functional
- [ ] All existing tests still pass (243+)
- [ ] New unit tests for TrackBankManager pass
- [ ] New smoke tests pass

**Completion triggers Phase 7 → version `0.7.0`**

---

## Plan Amendments

<!-- Log any changes to the plan after creation -->

| Date | Version | Amendment | Reason |
|------|---------|-----------|--------|
| 2026-02-28 | 0.6.1 | Renamed batch 6.1 to "pure tests (index/offset/errors)" | Push Bitwig-integration behavior to 6.2 |
| 2026-02-28 | 0.6.2 | Added rich response + structured error code acceptance criteria | Explicit criteria for D-6.4a responses |
| 2026-02-28 | 0.6.3 | Tool count criterion changed to ">= 55" + no breaking changes | Avoid brittle fixed-count assertions |
