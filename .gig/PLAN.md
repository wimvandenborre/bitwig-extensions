# Plan

> Living document — reflects what was done, not just what was intended.
> This file tracks the ACTIVE phase only. Completed phases are archived to `phases/`.

---

## Active Phase

### Phase 38 — Modulation Mapping & Clip Key Scrolling (v0.38.x)

> Add remote control mapping mode toggling (isBeingMapped) to DeviceHandler and MasterDeviceHandler with StateCache integration. Also add clip key scrolling methods to NoteHandler for vertical note editor navigation.

**Decisions:** D-1.1, D-1.2, D-1.3, D-1.4

| Batch | Version | Title | Delegation | Status |
|-------|---------|-------|------------|--------|
| 38.1 | `0.38.1` | StateCache isBeingMapped + device/master mapping RPCs + clip key scroll | in-session | pending |
| 38.2 | `0.38.2` | Unit tests | in-session | pending |
| 38.3 | `0.38.3` | Tool definitions + system prompt + smoke tests | in-session | pending |
| 38.4 | `0.38.4` | Build verification | in-session | pending |

### Batch 38.1 — StateCache isBeingMapped + device/master mapping RPCs + clip key scroll

**Delegation:** in-session
**Decisions:** D-1.1, D-1.2, D-1.3, D-1.4
**Files:** `StateCache.java`, `DeviceHandler.java`, `MasterDeviceHandler.java`, `NoteHandler.java`
**Work:**
- StateCache: Add `paramIsBeingMapped[8]` and `masterParamIsBeingMapped[8]` boolean arrays. Register observers via `remoteControlsPage.getParameter(i).isBeingMapped().markInterested()` + `addValueObserver()`. Include in snapshot under device/masterDevice params. Include in delta.
- DeviceHandler: Add `device/setParameterMapping` (index, enabled) and `device/getParameterMapping` (returns 8 booleans).
- MasterDeviceHandler: Add `masterDevice/setParameterMapping` (index, enabled) and `masterDevice/getParameterMapping`.
- NoteHandler: Add `note/scrollToKey` (key), `note/scrollKeysPageUp`, `note/scrollKeysPageDown`, `note/scrollKeysStepUp`, `note/scrollKeysStepDown`.
**Test criteria:** `./gradlew :gig-maestro:compileJava` passes
**Acceptance:** 9 new RPC methods, StateCache fields added

### Batch 38.2 — Unit tests

**Delegation:** in-session
**Decisions:** D-1.1, D-1.2, D-1.3, D-1.4
**Files:** `DeviceHandlerTest.java`, `MasterDeviceHandlerTest.java`, `NoteHandlerTest.java`, `StateCacheSnapshotTest.java`
**Work:**
- DeviceHandlerTest: test setParameterMapping/getParameterMapping
- MasterDeviceHandlerTest: test masterDevice equivalents
- NoteHandlerTest: test all 5 clip key scroll methods + scrollToKey validation
- StateCacheSnapshotTest: verify isBeingMapped appears in snapshot
**Test criteria:** `./gradlew :gig-maestro:test` — all tests pass
**Acceptance:** 10+ new test methods

### Batch 38.3 — Tool definitions + system prompt + smoke tests

**Delegation:** in-session
**Decisions:** all
**Files:** `claude-tools.json`, `system-prompt.md`, `offline-schemas.sh`
**Work:**
- Add 9 tool definitions
- Add "Remote Control Mapping" and "Clip Key Scrolling" sections to system prompt
- Add smoke test checks
- Update tool count threshold
**Test criteria:** `./scripts/smoke-test.sh --offline` passes
**Acceptance:** All tools documented, smoke tests green

### Batch 38.4 — Build verification

**Delegation:** in-session
**Decisions:** all
**Files:** none
**Work:** Full build + test + smoke verification
**Test criteria:** All three commands exit 0
**Acceptance:** Clean build, all tests green

**Phase Acceptance Criteria:**
- [ ] 9 new RPC methods across DeviceHandler, MasterDeviceHandler, NoteHandler
- [ ] isBeingMapped cached in StateCache snapshot and delta
- [ ] Unit tests cover all methods including error cases
- [ ] Tool definitions and system prompt documented
- [ ] Clean build with shadowJar

**Completion triggers Phase 39 → version `0.39.0`**
