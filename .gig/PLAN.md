# Plan

> Living document — reflects what was done, not just what was intended.
> This file tracks the ACTIVE phase only. Completed phases are archived to `phases/`.

---

## Active Phase

### Phase 18 — Macro createSound (v0.18.x)

> Add a high-level `macro/createSound` RPC method that handles device insertion + flush-cycle wait + multi-page parameter setting in a single call, bridging the timing gap the LLM can't manage on its own.

**Decisions:** D-18.1, D-18.2, D-18.3, D-18.4

| Batch | Version | Title | Delegation | Status |
|-------|---------|-------|------------|--------|
| 18.1 | `0.18.1` | macro/createSound implementation | in-session | pending |
| 18.2 | `0.18.2` | Tool definition + system prompt update | in-session | pending |
| 18.3 | `0.18.3` | MacroHandler tests for createSound | in-session | pending |
| 18.4 | `0.18.4` | Build verification | in-session | pending |

### Batch 18.1 — macro/createSound implementation

**Delegation:** in-session
**Decisions:** D-18.1, D-18.2, D-18.3
**Files:**
- `gig-maestro/src/main/java/dev/gregross/gig/handlers/MacroHandler.java`

**Work:**
1. Add `macro/createSound` registration in `register()` method
2. Handler method `handleCreateSound`:
   - Parse params: optional `device` (string), optional `position` (string, default "end"), required `pages` array
   - Validate pages array (same validation as device/setParameters: non-empty, indices 0-7, values 0.0-1.0)
   - If `device` provided: call `dispatcher.handleInternal("device/insertBitwigDevice", deviceParams)` immediately
   - Schedule parameter setting at FLUSH_DELAY_MS:
     - `scheduler.schedule(() -> dispatcher.handleInternal("device/setParameters", pagesParams), FLUSH_DELAY_MS)`
   - If no `device`: call `dispatcher.handleInternal("device/setParameters", pagesParams)` immediately (no flush wait needed)
   - Return `{ok: true, device: name|"current", pageCount: N, paramCount: N, inserted: bool}`

**Test criteria:** Compiles, existing tests pass

---

### Batch 18.2 — Tool definition + system prompt update

**Delegation:** in-session
**Decisions:** D-18.4
**Files:**
- `gig-maestro/tools/claude-tools.json`
- `gig-maestro/tools/system-prompt.md`

**Work:**
1. Add `macro_createSound` tool definition with nested schema
2. Update system prompt "Apply" step in "Creating Sounds From Scratch" to reference `macro_createSound` as the preferred single-call method
3. Add a brief note that `macro_createSound` without `device` reshapes the current device

**Depends on:** Batch 18.1
**Test criteria:** JSON validates, system prompt references correct tool names

---

### Batch 18.3 — MacroHandler tests for createSound

**Delegation:** in-session
**Decisions:** D-18.4
**Files:**
- `gig-maestro/src/test/java/dev/gregross/gig/handlers/MacroHandlerTest.java`

**Work:**
1. Add stub for `device/setParameters` in existing call-log setup
2. Test: createSound with device insertion — verify call sequence: insertBitwigDevice then setParameters
3. Test: createSound without device — verify only setParameters called (no insertion)
4. Test: missing pages — returns error
5. Test: param index out of range — returns error
6. Update method count assertion if needed

**Depends on:** Batch 18.1
**Test criteria:** All new + existing tests pass

---

### Batch 18.4 — Build verification

**Work:**
1. `./gradlew :gig-maestro:test` — all tests pass
2. `./gradlew :gig-maestro:shadowJar` — builds cleanly
3. Validate claude-tools.json

**Depends on:** All prior batches
**Test criteria:** Clean build, all tests green

---

**Phase Acceptance Criteria:**
- [ ] `macro/createSound` with device inserts then sets params after flush delay
- [ ] `macro/createSound` without device sets params immediately
- [ ] Tool definition in claude-tools.json
- [ ] System prompt references macro in apply workflow
- [ ] Tests verify choreography and validation

**Completion triggers Phase 19 → version `0.19.0`**
