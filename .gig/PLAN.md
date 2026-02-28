# Plan

> Living document — reflects what was done, not just what was intended.
> This file tracks the ACTIVE phase only. Completed phases are archived to `phases/`.

---

## Active Phase

### Phase 5 — Device Insertion (v0.5.x)

> Enable the LLM to add and remove devices on tracks — Bitwig built-in instruments/effects via name resolution from the `.bwdevice` library, and third-party VST2/VST3/CLAP plugins via ID. Includes a spike to validate that `insertFile()` works from the Controller API, a reusable DeviceLibrary utility for name→path resolution, 4 new RPC methods, tool schemas, CLI commands, and smoke tests.

**Decisions:** D-5.1, D-5.2, D-5.3a, D-5.4, D-5.5, D-5.6

| Batch | Version | Title | Delegation | Status |
|-------|---------|-------|------------|--------|
| 5.1 | `0.5.1` | Spike: Validate insertFile() mechanism | in-session | pending |
| 5.2 | `0.5.2` | DeviceLibrary utility + unit tests | team | pending |
| 5.3 | `0.5.3` | Device insertion + removal RPC handlers | team | pending |
| 5.4 | `0.5.4` | Tool schemas + system prompt update | team | pending |
| 5.5 | `0.5.5` | CLI DeviceCommand + smoke tests | team | pending |

### Batch 5.1 — Spike: Validate insertFile() Mechanism

**Delegation:** in-session
**Decisions:** D-5.1
**Files:** `src/main/java/dev/gregross/gig/handlers/DeviceHandler.java`
**Work:** Add a temporary `device/insertTest` RPC method that calls `cursorTrack.endOfDeviceChainInsertionPoint().insertFile(path)` with a hardcoded `.bwdevice` path (e.g., `/Applications/Bitwig Studio.app/Contents/Resources/Library/devices/Polymer.bwdevice`). Build, install, and invoke via CLI/curl with a track selected in Bitwig. If it works, the spike succeeds and we proceed. If it fails, investigate the fallback from D-5.1 (UUID-based insertion). Remove the test method after validation.
**Test criteria:** Call `device/insertTest` via `curl -X POST http://localhost:8787/rpc` with Bitwig open and a track selected. A Polymer device should appear on the track's device chain.
**Acceptance:** insertFile() confirmed working (or fallback path identified and decisions revised).

### Batch 5.2 — DeviceLibrary Utility + Unit Tests

**Delegation:** team
**Decisions:** D-5.4, D-5.5
**Files:** `src/main/java/dev/gregross/gig/handlers/DeviceLibrary.java`, `src/test/java/dev/gregross/gig/handlers/DeviceLibraryTest.java`
**Work:** Create `DeviceLibrary` utility class:
- Constructor takes the device library directory path (default: `/Applications/Bitwig Studio.app/Contents/Resources/Library/devices/`)
- On construction, scan directory for `*.bwdevice` files, build `Map<String, Path>` (lowercase name → full path)
- `resolve(String name)` — case-insensitive lookup, returns `Path` or throws with close matches (Levenshtein or prefix match)
- `listDevices()` — returns sorted list of available device names
- Unit tests with a temp directory containing fake `.bwdevice` files: test exact match, case-insensitive match, missing device with suggestions, list all
**Test criteria:** `./gradlew test --tests "*.DeviceLibraryTest"` — all tests pass. Tests cover: exact match, case mismatch, not-found with suggestions, empty directory, listDevices ordering.
**Acceptance:** DeviceLibrary resolves names to paths correctly, returns helpful errors on miss.

Depends on Batch 5.1 (spike must confirm insertFile() works).

### Batch 5.3 — Device Insertion + Removal RPC Handlers

**Delegation:** team
**Decisions:** D-5.1, D-5.2, D-5.3a, D-5.5, D-5.6
**Files:** `src/main/java/dev/gregross/gig/handlers/DeviceHandler.java`, `src/main/java/dev/gregross/gig/extension/GigExtension.java`
**Work:**
- Initialize `DeviceLibrary` in `GigExtension` at startup, pass to `DeviceHandler`
- `device/insertBitwigDevice`: resolve name via DeviceLibrary, get insertion point based on `position` param ("end"→`cursorTrack.endOfDeviceChainInsertionPoint()`, "before"→`cursorDevice.beforeDeviceInsertionPoint()`, "after"→`cursorDevice.afterDeviceInsertionPoint()`), call `insertFile(path)`
- `device/insertPluginDevice`: dispatch on `type` param to `insertVST2Device(int)` / `insertVST3Device(String)` / `insertCLAPDevice(String)` on the appropriate insertion point
- `device/listBitwigDevices`: delegate to `DeviceLibrary.listDevices()`
- `device/remove`: call `cursorDevice.deleteObject()`
- Register all 4 methods in the RPC dispatcher
**Test criteria:** Build succeeds (`./gradlew shadowJar`). Manual test: insert a Bitwig device by name, list devices, remove a device.
**Acceptance:** All 4 RPC methods registered and callable. insertBitwigDevice resolves names and inserts. insertPluginDevice dispatches by type. listBitwigDevices returns device names. device/remove deletes cursor device.

Depends on Batch 5.2.

### Batch 5.4 — Tool Schemas + System Prompt Update

**Delegation:** team
**Decisions:** D-5.3a
**Files:** `tools/claude-tools.json`, `tools/system-prompt.md`
**Work:**
- Add 4 new tool definitions to `claude-tools.json`: `device_insert_bitwig_device`, `device_insert_plugin_device`, `device_list_bitwig_devices`, `device_remove`
- Update `tools/system-prompt.md`: add device insertion section covering available devices, insertion positions, plugin types, and the insert→verify workflow
- Increment tool count in system prompt
**Test criteria:** `cat tools/claude-tools.json | python3 -m json.tool` validates. Tool count in system prompt matches actual count (48 = 44 existing + 4 new).
**Acceptance:** All 4 tools have correct schemas matching RPC params. System prompt documents the device insertion workflow.

Depends on Batch 5.3 (needs final method signatures).

### Batch 5.5 — CLI DeviceCommand + Smoke Tests

**Delegation:** team
**Decisions:** D-5.3a
**Files:** `src/cli/java/dev/gregross/gig/cli/DeviceCommand.java`, `src/cli/java/dev/gregross/gig/cli/GigCli.java`, `src/test/java/dev/gregross/gig/smoke/*.java`
**Work:**
- Add `DeviceCommand` subcommand with: `insert-bitwig`, `insert-plugin`, `list-bitwig`, `remove`
- Register in `GigCli`
- Smoke tests: list-bitwig-devices returns non-empty array, insert-bitwig-device with valid name succeeds, insert-plugin-device with type+id succeeds, device-remove succeeds, insert-bitwig-device with invalid name returns error with suggestions
**Test criteria:** `./gradlew test --tests "*.smoke.*"` — all smoke tests pass (including new ones). `./gradlew cliShadowJar` builds successfully.
**Acceptance:** CLI device commands work end-to-end. Smoke tests cover happy path and error cases for all 4 methods.

Depends on Batch 5.3 and Batch 5.4.

**Phase Acceptance Criteria:**
- [ ] insertFile() validated via spike (or fallback approach decided)
- [ ] DeviceLibrary scans and resolves all 151 `.bwdevice` files
- [ ] 4 new RPC methods registered and callable
- [ ] Tool schemas valid JSON with correct parameter types
- [ ] System prompt documents device insertion workflow
- [ ] CLI device commands functional
- [ ] All existing tests still pass (200+)
- [ ] New unit tests for DeviceLibrary pass
- [ ] New smoke tests for device RPC methods pass

**Completion triggers Phase 6 → version `0.6.0`**

---

## Plan Amendments

<!-- Log any changes to the plan after creation -->

| Date | Version | Amendment | Reason |
|------|---------|-----------|--------|
