# Plan

> Living document — reflects what was done, not just what was intended.
> This file tracks the ACTIVE phase only. Completed phases are archived to `phases/`.

---

## Active Phase

### Phase 24 — Device Sound Design Navigation (v0.24.x)

> Expose layer navigation, drum pad device chain navigation, and parameter page tag filtering on both CursorDevice handlers. These three capabilities enable programmatic sound design by letting the LLM navigate inside complex devices (Instrument Layer sub-chains, Drum Machine per-pad chains) and jump directly to relevant parameter pages (osc, filter, envelope, lfo). 6 new RPC methods total.

**Decisions:** D-24.1, D-24.2, D-24.3, D-24.4, D-24.5

| Batch | Version | Title | Delegation | Status |
|-------|---------|-------|------------|--------|
| 24.1 | `0.24.1` | DeviceHandler — layer, keypad, page tag methods | in-session | done |
| 24.2 | `0.24.2` | MasterDeviceHandler — layer, keypad, page tag methods | in-session | done |
| 24.3 | `0.24.3` | Unit tests | in-session | pending |
| 24.4 | `0.24.4` | Tool schemas + system prompt update | in-session | pending |
| 24.5 | `0.24.5` | Smoke tests | in-session | pending |

---

### Batch 24.1 — DeviceHandler — layer, keypad, page tag methods

**Delegation:** in-session
**Decisions:** D-24.2, D-24.3, D-24.5
**Files:** `src/main/java/dev/gregross/gig/handlers/DeviceHandler.java`
**Work:**
1. Add `device/enterLayer` — accepts `{index?: int, name?: string}` (one required, mutually exclusive). Calls `cursorDevice.selectFirstInLayer(index)` or `cursorDevice.selectFirstInLayer(name)`. Validates that exactly one of index/name is provided.
2. Add `device/enterKeyPad` — accepts `{key: int}` (MIDI note 0-127). Calls `cursorDevice.selectFirstInKeyPad(key)`. Validates key range 0-127.
3. Add `device/selectPageByTag` — accepts `{tag: string, direction?: "next"|"previous", cycle?: boolean}`. Validates tag against known set: env, eq, filter, fx, lfo, mixer, osc, perf. Defaults: direction="next", cycle=true. Calls `remoteControlsPage.selectNextPageMatching(tag, cycle)` or `selectPreviousPageMatching(tag, cycle)`.

**Test criteria:** `./gradlew shadowJar` compiles. DeviceHandler registers 23 methods (was 20).
**Acceptance:** 3 new device methods registered and functional.

---

### Batch 24.2 — MasterDeviceHandler — layer, keypad, page tag methods

**Delegation:** in-session (depends on 24.1 — same pattern)
**Decisions:** D-24.2, D-24.3, D-24.5
**Files:** `src/main/java/dev/gregross/gig/handlers/MasterDeviceHandler.java`
**Work:**
1. Add `masterDevice/enterLayer` — same params as device/enterLayer. Calls `cursorDevice.selectFirstInLayer()`.
2. Add `masterDevice/enterKeyPad` — same params as device/enterKeyPad. Calls `cursorDevice.selectFirstInKeyPad()`.
3. Add `masterDevice/selectPageByTag` — same params as device/selectPageByTag. Calls `remoteControlsPage.selectNextPageMatching()` / `selectPreviousPageMatching()`.

**Test criteria:** `./gradlew shadowJar` compiles. MasterDeviceHandler registers 15 methods (was 12).
**Acceptance:** 3 new masterDevice methods registered and functional.

---

### Batch 24.3 — Unit tests

**Delegation:** in-session (depends on 24.1, 24.2)
**Decisions:** D-24.2, D-24.3
**Files:** `src/test/java/dev/gregross/gig/handlers/DeviceHandlerTest.java`, `src/test/java/dev/gregross/gig/handlers/MasterDeviceHandlerTest.java`
**Work:**
1. DeviceHandler tests:
   - Registration: `device/enterLayer`, `device/enterKeyPad`, `device/selectPageByTag` present.
   - Total method count: 23.
   - `device/enterLayer` missing both index and name → error.
   - `device/enterLayer` both index and name → error.
   - `device/enterKeyPad` missing key → error.
   - `device/enterKeyPad` key out of range → error.
   - `device/selectPageByTag` missing tag → error.
   - `device/selectPageByTag` invalid tag → error.
2. MasterDeviceHandler tests:
   - Registration: 3 new methods present.
   - Total method count: 15.

**Test criteria:** `./gradlew test` passes all new tests.
**Acceptance:** 10+ new unit tests passing.

---

### Batch 24.4 — Tool schemas + system prompt update

**Delegation:** in-session (depends on 24.1, 24.2)
**Decisions:** D-24.3, D-24.5
**Files:** `tools/claude-tools.json`, `tools/system-prompt.md`
**Work:**
1. Add 6 tool schemas: `device_enterLayer`, `device_enterKeyPad`, `device_selectPageByTag`, `masterDevice_enterLayer`, `masterDevice_enterKeyPad`, `masterDevice_selectPageByTag`.
2. Add "Device Sound Design Navigation" section to system prompt with layer/keypad entry, page tag filtering, known tags table, and sound design workflow guidance.

**Test criteria:** `jq . tools/claude-tools.json` validates. System prompt mentions layer navigation and page tags.
**Acceptance:** 6 new schemas + system prompt section.

---

### Batch 24.5 — Smoke tests

**Delegation:** in-session (depends on 24.4)
**Decisions:** D-24.1
**Files:** `scripts/smoke-test.sh`
**Work:**
1. Schema validation: 6 new tool schemas present.
2. Schema validation: `device_selectPageByTag` has tag enum with 8 values.
3. Schema validation: `device_enterLayer` has index and name properties.
4. Schema validation: `device_enterKeyPad` has key property.
5. System prompt mentions "Device Sound Design Navigation" section.
6. System prompt mentions page tag filtering.

**Test criteria:** `./scripts/smoke-test.sh --offline` passes all new assertions.
**Acceptance:** 12+ new smoke assertions passing.

---

**Phase Acceptance Criteria:**
- [ ] `device/enterLayer` navigates into layers by index or name
- [ ] `device/enterKeyPad` navigates into drum pad device chains by MIDI key
- [ ] `device/selectPageByTag` jumps to parameter pages by tag (osc, filter, env, etc.)
- [ ] All 3 methods mirrored on masterDevice
- [ ] Tag validation against 8 known tags (env, eq, filter, fx, lfo, mixer, osc, perf)
- [ ] enterLayer validates mutually exclusive index/name params
- [ ] enterKeyPad validates key range 0-127
- [ ] All unit tests pass (`./gradlew test`)
- [ ] All smoke tests pass (`./scripts/smoke-test.sh --offline`)
- [ ] 6 new tool schemas in claude-tools.json
- [ ] System prompt documents Device Sound Design Navigation

**Completion triggers Phase 25 → version `0.25.0`**
