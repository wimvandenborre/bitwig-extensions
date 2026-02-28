# Plan

> Living document — reflects what was done, not just what was intended.
> This file tracks the ACTIVE phase only. Completed phases are archived to `phases/`.

---

## Active Phase

### Phase 9 — Envelope Writing (v0.9.x)

> Add programmatic automation envelope writing via simulated touch recording, plus per-parameter automation management (hasAutomation, deleteAllAutomation, restoreAutomationControl, touch). The core `writeEnvelope` method takes a parameter index and array of {position, value} points, internally manipulating transport position + parameter touch to create automation data. Full state save/restore ensures no side effects on transport position, play/stop state, or automation write enables. Snapshot extended with `hasAutomation` per parameter.

**Decisions:** D-9.1a, D-9.2, D-9.3, D-9.4a, D-9.5a, D-9.6

| Batch | Version | Title | Delegation | Status |
|-------|---------|-------|------------|--------|
| 9.1 | `0.9.1` | Spike: validate position-jump envelope writing | in-session | done |
| 9.2 | `0.9.2` | Per-parameter automation methods + snapshot | in-session | done |
| 9.3 | `0.9.3` | writeEnvelope with state save/restore | in-session | done |
| 9.4 | `0.9.4` | Tool schemas + system prompt update | in-session | done |
| 9.5 | `0.9.5` | Unit tests + smoke tests | in-session | done |

### Batch 9.1 — Spike: Validate Position-Jump Envelope Writing

**Delegation:** in-session
**Decisions:** D-9.2
**Files:** none (manual test via CLI/curl against running Bitwig)
**API25 contract — methods under test:**
- `Parameter.touch(boolean)` — enters/exits touch automation recording
- `Parameter.value().setImmediately(double)` — sets parameter value bypassing take-over
- `Transport.getPosition()` / `SettableBeatTimeValue.set(double)` — read/write transport position
- `Transport.automationWriteMode()` — must be in "touch" or "write" mode
- `Transport.isArrangerAutomationWriteEnabled()` — must be true
- `Parameter.hasAutomation()` — verify automation data was created
**Work:**
- With Bitwig running and a project with an instrument track + device:
  1. Enable arranger automation write: `transport/setArrangerAutomationWrite {enabled: true}`
  2. Set write mode to "touch": `transport/setAutomationWriteMode {mode: "touch"}`
  3. Set transport position to beat 0: `transport/setPosition {position: 0}`
  4. Touch parameter 0: call `param.touch(true)` via temporary test RPC or direct curl
  5. Set value: `device/setParameterValue {index: 0, value: 0.2}`
  6. Untouch: `param.touch(false)`
  7. Move transport to beat 4, repeat with value 0.8
  8. Check `param.hasAutomation()` — should return true
  9. Check Bitwig arranger — automation lane should show 2 points
- **If position-jump works:** Approach validated. Proceed with full implementation.
- **If position-jump fails:** Try alternative — start playback, use a timed delay between sets, stop. Document which approach works.
- Record findings in `.gig/DECISIONS.md` as revision to D-9.2.
**Test criteria:** A 3-point test (beats 0, 4, 8 with values 0.2, 0.8, 0.5) produces `hasAutomation() == true` afterward. Visual confirmation of automation points in Bitwig arranger.
**Acceptance:** Deterministic proof that position-jump + touch + setImmediately creates observable automation presence (`hasAutomation == true`). Approach for writeEnvelope confirmed.

### Batch 9.2 — Per-Parameter Automation Methods + Snapshot

**Delegation:** in-session
**Decisions:** D-9.3, D-9.5a, D-9.6
**Files:** `handlers/DeviceHandler.java`, `extension/StateCache.java`
**API25 contract — methods used:**
- `Parameter.hasAutomation()` → `BooleanValue` (observable)
- `Parameter.deleteAllAutomation()` → void
- `Parameter.restoreAutomationControl()` → void
- `Parameter.touch(boolean)` → void
**Work:**
- Add 4 RPC methods to `DeviceHandler`:
  - `device/hasAutomation` — `{index: int}` → `{hasAutomation: boolean}` — calls `param.hasAutomation().get()`
  - `device/deleteAllAutomation` — `{index: int}` → `{ok: true}` — calls `param.deleteAllAutomation()`
  - `device/restoreAutomationControl` — `{index: int}` → `{ok: true}` — calls `param.restoreAutomationControl()`
  - `device/touch` — `{index: int, touched: boolean}` → `{ok: true}` — calls `param.touch(touched)`
- All validate index 0-7 (reuse existing `PARAM_COUNT` check pattern).
- In `StateCache.registerDeviceObservers()`: add `param.hasAutomation().markInterested()` + observer to a new `boolean[] paramHasAutomation` array.
- In `StateCache.getDeviceState()`: add `hasAutomation` field to each parameter object in the snapshot.
**Test criteria:** `api/list` includes all 4 new methods. `device/hasAutomation` returns boolean. Snapshot parameter objects include `hasAutomation` field. Build succeeds.
**Acceptance:** All 4 per-parameter automation methods work. Snapshot reflects `hasAutomation` per parameter.

No dependencies — these are straight API wrappers, independent of the spike.

### Batch 9.3 — writeEnvelope with State Save/Restore

**Delegation:** in-session
**Decisions:** D-9.3, D-9.4a
**Files:** `handlers/DeviceHandler.java`, `extension/GigMaestroExtension.java`
**API25 contract — methods used:**
- `Transport.getPosition()` → `SettableBeatTimeValue` (save/restore position)
- `Transport.isPlaying()` → `SettableBooleanValue` (save/restore play state)
- `Transport.stop()` → void
- `Transport.play()` → void
- `Parameter.touch(boolean)` → void
- `Parameter.value().setImmediately(double)` → void
- `Transport.isArrangerAutomationWriteEnabled()` → `SettableBooleanValue` (precondition check)
**Work:**
- Update `DeviceHandler` constructor to also accept `Transport`.
- Update wiring in `GigMaestroExtension.init()` to pass `transport` to `DeviceHandler`.
- Add `device/writeEnvelope` RPC method:
  - Params: `{index: int, points: [{position: number, value: number}]}`
  - **Preconditions:**
    - Validate index 0-7
    - Check `transport.isArrangerAutomationWriteEnabled()` is true — error if not: "Arranger automation write must be enabled"
  - **Input validation:**
    - Sort points ascending by position
    - Validate all positions >= 0.0 — error if any negative
    - Clamp all values to [0.0, 1.0]
    - Duplicate positions: last-wins (filter after sort)
  - **State save:** Capture transport position, isPlaying state
  - **Execution:** Stop transport if playing. For each point: set transport position → touch → setImmediately → untouch
  - **State restore (finally block):** Restore transport position, restore play/stop state, ensure parameter is untouched
  - Returns `{ok: true, pointsWritten: N}`
**Test criteria:** `api/list` includes `device/writeEnvelope`. Method validates index, rejects negative positions, clamps values, errors if automation write disabled. Build succeeds.
**Acceptance:** `writeEnvelope` creates automation data for N points with full state save/restore. Transport position unchanged after call. Parameter untouched after call.

Depends on Batch 9.1 (spike confirms approach) and Batch 9.2 (touch method exists).

### Batch 9.4 — Tool Schemas + System Prompt Update

**Delegation:** in-session
**Decisions:** D-9.1a, D-9.4a, D-9.5a
**Files:** `tools/claude-tools.json`, `tools/system-prompt.md`
**Work:**
- Add 5 tool schemas to `claude-tools.json`:
  - `device_hasAutomation` — `{index: int}` → checks if parameter has automation
  - `device_deleteAllAutomation` — `{index: int}` → deletes all automation for parameter
  - `device_restoreAutomationControl` — `{index: int}` → restores parameter to automation curve
  - `device_touch` — `{index: int, touched: boolean}` → touches/untouches parameter for recording
  - `device_writeEnvelope` — `{index: int, points: array}` → writes automation curve
- Add "Envelope Writing" section to system prompt covering:
  - How `writeEnvelope` works (position-jump simulation, not real-time)
  - Prerequisites: arranger automation write must be enabled, automation mode set
  - Point format: `{position: beats, value: 0.0-1.0 normalized}`
  - State restoration guarantee (transport position, play state preserved)
  - Per-parameter automation lifecycle: write → check (hasAutomation) → delete → restore
  - Touch method for manual recording workflows
  - Warning: no automation curve reading (write-only)
- Update `session_snapshot` tool description to mention `hasAutomation` in device parameters.
**Test criteria:** Tool JSON validates with >= 79 tools. System prompt contains "Envelope Writing" section. All tool names use underscore convention.
**Acceptance:** Agent has complete guidance for envelope writing workflow. All 5 new tools documented.

Depends on Batches 9.2 and 9.3 (all methods must be finalized).

### Batch 9.5 — Unit Tests + Smoke Tests

**Delegation:** in-session
**Decisions:** D-9.1a
**Files:** `src/test/java/dev/gregross/gig/handlers/DeviceHandlerTest.java`, `scripts/smoke-test.sh`
**Work:**
- Unit tests for new DeviceHandler methods:
  - Registration tests: all 5 new methods registered
  - `device/hasAutomation`: missing index → error, index out of range → error
  - `device/deleteAllAutomation`: missing index → error, index out of range → error
  - `device/restoreAutomationControl`: missing index → error, index out of range → error
  - `device/touch`: missing index → error, missing touched → error, index out of range → error
  - `device/writeEnvelope`: missing index → error, missing points → error, index out of range → error, negative position → error, automation write disabled → error
- Offline smoke tests:
  - Verify all 5 new tools exist in `claude-tools.json`
  - Verify system prompt has "Envelope Writing" section
  - Verify tool count >= 79
  - Spot-check parameter types for `device_writeEnvelope` (index: integer, points: array)
- Online smoke tests:
  - `device/hasAutomation` returns `{hasAutomation: boolean}`
  - `device/touch` with valid index returns `{ok: true}`
  - `device/writeEnvelope` with automation write disabled returns error
**Test criteria:** `./gradlew test` all pass. `./scripts/smoke-test.sh --offline` all pass. New test count covers all 5 methods.
**Acceptance:** Full test coverage for Phase 9. No existing tests broken.

Depends on Batches 9.2, 9.3 (unit tests for handlers), and 9.4 (offline smoke tests for schemas/prompt).

**Phase Acceptance Criteria:**
- [ ] Spike validates position-jump + touch + setValue creates automation data
- [ ] 4 per-parameter automation methods work: hasAutomation, deleteAllAutomation, restoreAutomationControl, touch
- [ ] `device/writeEnvelope` creates automation curve from array of {position, value} points
- [ ] writeEnvelope preconditions: rejects if arranger automation write not enabled
- [ ] writeEnvelope input validation: sorts points, rejects negative positions, clamps values to [0,1]
- [ ] writeEnvelope state restoration: transport position + play/stop state preserved, parameter untouched in finally
- [ ] Snapshot `device.remoteControls.parameters[].hasAutomation` reflects automation state
- [ ] 5 new RPC methods (total >= 79)
- [ ] Tool schemas for all new methods (total >= 79 tools)
- [ ] System prompt has "Envelope Writing" section
- [ ] All existing tests pass (83 unit + 152 offline smoke)
- [ ] New unit tests + smoke tests for all Phase 9 methods

**Completion triggers Phase 10 → version `0.10.0`**

---

## Plan Amendments

<!-- Log any changes to the plan after creation -->

| Date | Version | Amendment | Reason |
|------|---------|-----------|--------|
| 2026-02-28 | 0.9.0 | 9.2 no longer depends on 9.1; 9.1 acceptance strengthened to require 3-point test with `hasAutomation == true` | User review: decouple wrappers from spike to reduce schedule risk, anchor spike to measurable success condition |
