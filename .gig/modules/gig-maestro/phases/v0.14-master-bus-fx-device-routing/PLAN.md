# Plan

> Living document — reflects what was done, not just what was intended.
> This file tracks the ACTIVE phase only. Completed phases are archived to `phases/`.

---

## Active Phase

### Phase 14 — Master Bus FX & Device Routing (v0.14.x)

> Add a dedicated CursorDevice on the master track for inserting, removing, navigating, and controlling master bus effects. Closes the "No master track CursorDevice" limitation. The agent gains the ability to build complete master signal chains (EQ → compressor → limiter) with full parameter control.

**Decisions:** D-14.1, D-14.2, D-14.3, D-14.4, D-14.5, D-14.6, D-14.7

| Batch | Version | Title | Delegation | Status |
|-------|---------|-------|------------|--------|
| 14.1 | `0.14.1` | Master cursor infrastructure — CursorDevice + observers + snapshot | in-session | done |
| 14.2 | `0.14.2` | MasterDeviceHandler — 10 RPC methods | in-session | done |
| 14.3 | `0.14.3` | Unit tests | in-session | done |
| 14.4 | `0.14.4` | Tool schemas + system prompt update | in-session | done |
| 14.5 | `0.14.5` | Smoke tests | in-session | done |

### Batch 14.1 — Master cursor infrastructure — CursorDevice + observers + snapshot

**Delegation:** in-session
**Decisions:** D-14.2, D-14.6
**Files:** `GigMaestroExtension.java`, `StateCache.java`
**Work:**
- In `GigMaestroExtension.init()`, create master cursor device: `masterTrack.createCursorDevice("gig-master-device", "Gig Master Device", 0, CursorDeviceFollowMode.FOLLOW_SELECTION)`
- Create master remote controls page: `masterCursorDevice.createCursorRemoteControlsPage(8)`
- In `StateCache`, add master device state fields mirroring existing device fields: `masterDeviceName`, `masterDeviceEnabled`, `masterDeviceIsPlugin`, `masterDevicePosition`, `masterPresetName`, `masterPresetCategory`, `masterPresetCreator`, `masterPageIndex`, `masterPageCount`, `masterDevicePageNames`, `masterParamNames[8]`, `masterParamValues[8]`, `masterParamDisplayedValues[8]`
- Add `registerMasterDeviceObservers(CursorDevice, CursorRemoteControlsPage)` method in StateCache
- Add `getMasterDeviceState()` private method to build snapshot section
- Include `masterDevice` key in snapshot output
- Wire observers in extension init
**Test criteria:** `./gradlew shadowJar` builds. Snapshot includes `masterDevice` section with device name, parameters array.
**Acceptance:** Snapshot returns `masterDevice` with all fields populated when a device exists on master.

### Batch 14.2 — MasterDeviceHandler — 10 RPC methods

**Delegation:** in-session
**Decisions:** D-14.3, D-14.4, D-14.5
**Files:** `MasterDeviceHandler.java` (new), `GigMaestroExtension.java`
**Work:**
- Create `MasterDeviceHandler` class with constructor `(MasterTrack, CursorDevice, CursorRemoteControlsPage, DeviceLibrary)`
- Register 10 methods:
  - `masterDevice/selectNext`: `masterCursorDevice.selectNext()`
  - `masterDevice/selectPrevious`: `masterCursorDevice.selectPrevious()`
  - `masterDevice/setEnabled`: validate `enabled` param, `masterCursorDevice.isEnabled().set(enabled)`
  - `masterDevice/insertBitwigDevice`: validate `name` + `position` params, resolve via DeviceLibrary, insert at insertion point
  - `masterDevice/insertPluginDevice`: validate `type` + `id` + `position` params, insert VST2/VST3/CLAP
  - `masterDevice/remove`: `masterCursorDevice.deleteObject()`
  - `masterDevice/selectPage`: validate `index` param, `masterRemoteControlsPage.selectedPageIndex().set(index)`
  - `masterDevice/nextPage`: `masterRemoteControlsPage.selectNext()`
  - `masterDevice/previousPage`: `masterRemoteControlsPage.selectPrevious()`
  - `masterDevice/setParameterValue`: validate `index` (0-7) + `value` (0.0-1.0), `masterRemoteControlsPage.getParameter(index).value().setImmediately(value)`
- Wire into extension init
**Test criteria:** `./gradlew shadowJar` builds. RPC calls modify master device state.
**Acceptance:** All 10 methods return `{ ok: true }`.

### Batch 14.3 — Unit tests

**Delegation:** in-session
**Decisions:** D-14.4, D-14.5
**Files:** `MasterDeviceHandlerTest.java` (new)
**Work:**
- Test method registration (10 methods)
- Test validation: missing `enabled` param for setEnabled, invalid page index, missing device name, invalid plugin type, parameter index out of range, parameter value out of range
**Test criteria:** `./gradlew test` passes. All new tests green.
**Acceptance:** Full test suite passes with no regressions.

### Batch 14.4 — Tool schemas + system prompt update

**Delegation:** in-session
**Decisions:** D-14.7
**Files:** `tools/claude-tools.json`, `tools/system-prompt.md`
**Work:**
- Add 10 tool schemas: `masterDevice_selectNext`, `masterDevice_selectPrevious`, `masterDevice_setEnabled`, `masterDevice_insertBitwigDevice`, `masterDevice_insertPluginDevice`, `masterDevice_remove`, `masterDevice_selectPage`, `masterDevice_nextPage`, `masterDevice_previousPage`, `masterDevice_setParameterValue`
- Update system prompt: add "Master Bus FX" subsection under Mixer & Routing explaining when to use `masterDevice/` vs `device/`, master chain workflow (insert EQ → compressor → limiter), snapshot `masterDevice` structure
**Test criteria:** `./scripts/smoke-test.sh --offline` passes schema validation.
**Acceptance:** 124 tool schemas, system prompt documents master device workflow.

### Batch 14.5 — Smoke tests

**Delegation:** in-session
**Decisions:** D-14.7
**Files:** `scripts/smoke-test.sh`
**Work:**
- Add offline smoke tests for 10 new tool schemas (structure validation)
- Add offline tests for system prompt master device documentation
- Add online smoke tests: masterDevice/insertBitwigDevice, masterDevice/selectNext, masterDevice/setParameterValue, masterDevice/remove, snapshot masterDevice section
**Test criteria:** `./scripts/smoke-test.sh` passes (full suite including new tests).
**Acceptance:** All smoke tests pass.

**Phase Acceptance Criteria:**
- [ ] 10 new RPC methods registered and callable
- [ ] Master device insertion works (Bitwig native + plugin)
- [ ] Master device navigation (selectNext/selectPrevious) works
- [ ] Master device parameter control works
- [ ] Master device removal works
- [ ] Snapshot includes `masterDevice` section with device state + parameters
- [ ] All unit tests pass (`./gradlew test`)
- [ ] All smoke tests pass (`./scripts/smoke-test.sh`)
- [ ] System prompt documents master bus FX workflow

**Completion triggers Phase 15 → version `0.15.0`**
