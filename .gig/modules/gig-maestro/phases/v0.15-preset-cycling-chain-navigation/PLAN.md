# Plan

> Living document — reflects what was done, not just what was intended.
> This file tracks the ACTIVE phase only. Completed phases are archived to `phases/`.

---

## Active Phase

### Phase 15 — Preset Cycling & Device Chain Navigation (v0.15.x)

> Add preset browsing via deprecated cycling methods and nested device chain navigation via slot entry/exit. Extends both DeviceHandler and MasterDeviceHandler with 12 new RPC methods. Adds 5 nesting-related fields to device and masterDevice snapshot sections so agents can inspect device structure before navigating.

**Decisions:** D-15.1, D-15.2, D-15.3, D-15.4, D-15.5

| Batch | Version | Title | Delegation | Status |
|-------|---------|-------|------------|--------|
| 15.1 | `0.15.1` | Snapshot nesting observers | in-session | done |
| 15.2 | `0.15.2` | DeviceHandler + MasterDeviceHandler — 12 RPC methods | in-session | done |
| 15.3 | `0.15.3` | Unit tests | in-session | done |
| 15.4 | `0.15.4` | Tool schemas + system prompt update | in-session | done |
| 15.5 | `0.15.5` | Smoke tests | in-session | done |

### Batch 15.1 — Snapshot nesting observers

**Delegation:** in-session
**Decisions:** D-15.3, D-15.5
**Files:** `src/main/java/dev/gregross/gig/extension/StateCache.java`
**Work:**
- Add 5 nesting-related fields for track device: `deviceIsNested` (boolean), `deviceHasSlots` (boolean), `deviceSlotNames` (String[]), `deviceHasLayers` (boolean), `deviceHasDrumPads` (boolean)
- Add matching 5 fields for master device: `masterDeviceIsNested`, `masterDeviceHasSlots`, `masterDeviceSlotNames`, `masterDeviceHasLayers`, `masterDeviceHasDrumPads`
- Register observers in `registerDeviceObservers()` and `registerMasterDeviceObservers()`
- Include nesting fields in `getDeviceState()` and `getMasterDeviceState()` snapshot methods
**Test criteria:** `./gradlew test` — existing StateCacheDeltaTest still passes (snapshot shape expanded but hash logic unchanged)
**Acceptance:** Snapshot JSON includes `isNested`, `hasSlots`, `slotNames`, `hasLayers`, `hasDrumPads` in both `device` and `masterDevice` sections

### Batch 15.2 — DeviceHandler + MasterDeviceHandler — 12 RPC methods

**Delegation:** in-session
**Decisions:** D-15.2, D-15.3, D-15.4
**Files:** `src/main/java/dev/gregross/gig/handlers/DeviceHandler.java`, `src/main/java/dev/gregross/gig/handlers/MasterDeviceHandler.java`
**Work:**
- DeviceHandler: add 8 methods — `device/nextPreset`, `device/previousPreset`, `device/nextPresetCategory`, `device/previousPresetCategory`, `device/nextPresetCreator`, `device/previousPresetCreator`, `device/enterSlot` (param: `name`), `device/exitToParent`
- MasterDeviceHandler: add 4 methods — `masterDevice/nextPreset`, `masterDevice/previousPreset`, `masterDevice/enterSlot` (param: `name`), `masterDevice/exitToParent`
- `enterSlot` validates `name` param is present; `exitToParent` is parameterless
- Preset cycling methods are parameterless fire-and-forget calls
**Test criteria:** `./gradlew test` — all existing tests pass, new methods registered
**Acceptance:** `api.list` returns all 12 new methods, total count = 117 (105 + 12)

### Batch 15.3 — Unit tests

**Delegation:** in-session
**Decisions:** D-15.4
**Files:** `src/test/java/dev/gregross/gig/handlers/DeviceHandlerTest.java`, `src/test/java/dev/gregross/gig/handlers/MasterDeviceHandlerTest.java`
**Work:**
- DeviceHandlerTest: update registration count (17 → 25), add `enterSlot` missing name validation test
- MasterDeviceHandlerTest: update registration count (10 → 14), add `enterSlot` missing name validation test
**Test criteria:** `./gradlew test` — all tests pass
**Acceptance:** Registration tests verify all new methods, validation tests cover required params

### Batch 15.4 — Tool schemas + system prompt update

**Delegation:** in-session
**Decisions:** D-15.4, D-15.5
**Files:** `tools/claude-tools.json`, `tools/system-prompt.md`
**Work:**
- Add 12 new tool schemas (136 total): 8 `device_*` + 4 `master_device_*`
- Update `session_snapshot` tool description to mention nesting fields
- Add "Preset Cycling & Chain Navigation" section to system prompt
- Document slot navigation workflow: check `hasSlots` → `slotNames` → `enterSlot` → work → `exitToParent`
- Note deprecated status of preset methods in system prompt (functional but deprecated)
**Test criteria:** `jq` validates JSON, grep confirms all 12 method names present
**Acceptance:** 136 tool schemas, system prompt documents preset cycling + chain navigation

### Batch 15.5 — Smoke tests

**Delegation:** in-session
**Decisions:** D-15.1
**Files:** `scripts/smoke-test.sh`
**Work:**
- Offline: 12 tool schema presence checks, nesting field assertions in snapshot description, system prompt assertions
- Online: preset cycling (nextPreset/previousPreset on a device), enterSlot on container device (Instrument Layer), exitToParent, verify snapshot nesting fields
**Test criteria:** `./scripts/smoke-test.sh --offline` passes
**Acceptance:** All offline smoke tests pass; online tests documented for UAT

**Phase Acceptance Criteria:**
- [ ] 12 new RPC methods registered (8 device + 4 masterDevice)
- [ ] Nesting fields in device + masterDevice snapshot sections
- [ ] 136 tool schemas in claude-tools.json
- [ ] System prompt documents preset cycling + chain navigation workflows
- [ ] All unit tests pass (220+ existing + new)
- [ ] All offline smoke tests pass
- [ ] UAT: preset cycling changes preset name in snapshot
- [ ] UAT: enterSlot navigates into container device, exitToParent returns

**Completion triggers Phase 16 → version `0.16.0`**
