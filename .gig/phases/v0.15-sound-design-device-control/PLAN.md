# Plan

> Living document — reflects what was done, not just what was intended.
> This file tracks the ACTIVE phase only. Completed phases are archived to `phases/`.

---

## Active Phase — Phase 15: gig-maestro Sound Design Device Control (v0.15.x)

> Add preset navigation RPC methods, modulated parameter value observation, and comprehensive system prompt sound design recipes. Enables Claude to guide users through sound design workflows for bass, leads, pads, ambient, and drums using Bitwig's built-in instruments.

**Decisions:** D-15.1, D-15.2, D-15.3, D-15.4, D-15.5, D-15.6

| Batch | Version | Title | Delegation | Status |
|-------|---------|-------|------------|--------|
| 15.1 | `0.15.1` | Preset navigation RPC methods | team | done |
| 15.2 | `0.15.2` | Modulated parameter values | team | done |
| 15.3 | `0.15.3` | Tool definitions + system prompt | in-session | done |
| 15.4 | `0.15.4` | Handler + StateCache tests | in-session | done |
| 15.5 | `0.15.5` | Verify full build | in-session | done |

### Batch 15.1 — Preset navigation RPC methods

**Delegation:** team
**Decisions:** D-15.2
**Files:**
- `gig-maestro/src/main/java/dev/gregross/gig/handlers/DeviceHandler.java` (modify)
- `gig-maestro/src/main/java/dev/gregross/gig/handlers/MasterDeviceHandler.java` (modify)
**Work:**
- Add 6 preset navigation methods to DeviceHandler's `register()`:
  - `device/nextPreset` → `cursorDevice.switchToNextPreset()`
  - `device/previousPreset` → `cursorDevice.switchToPreviousPreset()`
  - `device/nextPresetCategory` → `cursorDevice.switchToNextPresetCategory()`
  - `device/previousPresetCategory` → `cursorDevice.switchToPreviousPresetCategory()`
  - `device/nextPresetCreator` → `cursorDevice.switchToNextPresetCreator()`
  - `device/previousPresetCreator` → `cursorDevice.switchToPreviousPresetCreator()`
- Mirror all 6 on MasterDeviceHandler with `masterDevice/` prefix
- All return `new JsonPrimitive("ok")` (same pattern as selectNext/selectPrevious)
**Test criteria:** Compiles without error
**Acceptance:** 12 new RPC methods registered

### Batch 15.2 — Modulated parameter values

**Delegation:** team
**Decisions:** D-15.3
**Files:**
- `gig-maestro/src/main/java/dev/gregross/gig/extension/StateCache.java` (modify)
**Work:**
- Add fields: `private final double[] paramModulatedValues = new double[PARAM_COUNT]` and `private final double[] masterParamModulatedValues = new double[PARAM_COUNT]`
- In `registerDeviceObservers()`, after existing `param.value()` observer (line ~578), add:
  ```java
  param.modulatedValue().markInterested();
  param.modulatedValue().addValueObserver((DoubleValueChangedCallback) v -> paramModulatedValues[idx] = v);
  ```
- In `registerMasterDeviceObservers()`, add same pattern for master params
- In `getSnapshot()` device section, add `modulatedValue` to each parameter JSON object
- In `getSnapshot()` masterDevice section, add same
**Test criteria:** Compiles without error
**Acceptance:** Snapshot device parameters include `modulatedValue` field

### Batch 15.3 — Tool definitions + system prompt

**Delegation:** in-session
**Decisions:** D-15.4, D-15.5
**Depends on:** Batches 15.1, 15.2
**Files:**
- `gig-maestro/tools/claude-tools.json` (modify)
- `gig-maestro/tools/system-prompt.md` (modify)
**Work:**
- Add 12 tool definitions to claude-tools.json (6 device + 6 masterDevice preset methods)
- Add "Sound Design Recipes" section to system-prompt.md with:
  - **Sound Design Workflow** overview (preset→tweak→layer→FX pipeline)
  - **Bass** recipes: sub bass (Polymer/Phase-4, low filter, long decay), pluck bass (short envelope, resonant filter), growl bass (FM/wavetable, distortion)
  - **Lead** recipes: mono lead (Polymer, saw/square, filter mod), poly lead (Polysynth, detuned), pluck lead (short amp env, reverb)
  - **Pad** recipes: warm pad (Polymer, low-pass, slow attack, chorus), evolving pad (wavetable, LFO mod), ambient pad (long reverb, delay)
  - **Ambient/Texture** recipes: drone (pad + long reverb + delay), riser (filter sweep, pitch bend), atmosphere (granular, reverb)
  - **Drums** recipes: using Drum Machine, per-pad device chains, layering
  - Each recipe: device name, tag sequence, key parameter ranges, FX chain
- Document new `modulatedValue` field in snapshot description
**Test criteria:** JSON is valid, system prompt is coherent
**Acceptance:** Claude can follow recipes to create specific sound types

### Batch 15.4 — Handler + StateCache tests

**Delegation:** in-session
**Decisions:** D-15.6
**Depends on:** Batches 15.1, 15.2
**Files:**
- `gig-maestro/src/test/java/dev/gregross/gig/handlers/DeviceHandlerTest.java` (modify)
- `gig-maestro/src/test/java/dev/gregross/gig/handlers/MasterDeviceHandlerTest.java` (modify)
- `gig-maestro/src/test/java/dev/gregross/gig/extension/StateCacheSnapshotTest.java` (modify)
- `gig-maestro/src/test/java/dev/gregross/gig/extension/StateCacheTestHelper.java` (modify)
**Work:**
- Add 6 tests to DeviceHandlerTest: one per preset method, verify `cursorDevice.switchTo*()` called
- Add 6 tests to MasterDeviceHandlerTest: same pattern for master device
- Update StateCacheTestHelper.populateDevice() to set `paramModulatedValues[0]`
- Add assertion in StateCacheSnapshotTest.snapshot_device_containsAllFields for `modulatedValue`
- Update populateMasterDevice() and snapshot_masterDevice test similarly
**Test criteria:** `./gradlew :gig-maestro:test` passes
**Acceptance:** All new methods have behavioral test coverage

### Batch 15.5 — Verify full build

**Delegation:** in-session
**Decisions:** —
**Depends on:** Batches 15.1–15.4
**Files:** —
**Work:** Run `./gradlew clean build` to verify all modules compile and all tests pass.
**Test criteria:** Exit code 0, BUILD SUCCESSFUL
**Acceptance:** All tests green across gig-maestro and launchpad-mk2

**Phase Acceptance Criteria:**
- [ ] 12 preset navigation RPC methods registered (6 device + 6 master)
- [ ] Snapshot parameters include `modulatedValue` for both track and master devices
- [ ] claude-tools.json has 12 new tool definitions
- [ ] system-prompt.md has sound design recipes for 5 categories
- [ ] All new methods have behavioral test coverage
- [ ] `./gradlew clean build` passes with all tests green

**Completion triggers governance → version `0.15.0`**
