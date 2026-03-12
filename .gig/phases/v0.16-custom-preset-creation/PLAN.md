# Plan

> Living document — reflects what was done, not just what was intended.
> This file tracks the ACTIVE phase only. Completed phases are archived to `phases/`.

---

## Phase 16 — gig-maestro: Custom Preset Creation

**Goal:** Enable the LLM to create sounds from scratch by setting device parameters directly, instead of browsing presets.

**Decisions:** D-16.1 (Scope), D-16.2 (Batch Setter), D-16.3 (System Prompt), D-16.4 (Testing)

---

### Batch 16.1 — Batch parameter setter methods

**Files:**
- `gig-maestro/src/main/java/dev/gregross/gig/handlers/DeviceHandler.java`
- `gig-maestro/src/main/java/dev/gregross/gig/handlers/MasterDeviceHandler.java`

**Work:**
1. Add `device/setParameters` to DeviceHandler:
   - Accepts `{pages: [{pageIndex: int, params: [{index: int, value: double}]}]}`
   - Validates all inputs (page indices, param indices 0-7, values 0.0-1.0)
   - First page: navigate + set params immediately in handler
   - Subsequent pages: schedule via TaskScheduler with staggered delays (100ms per page)
   - Returns `{ok: true, pageCount: N, paramCount: N}`
2. Mirror to `masterDevice/setParameters` in MasterDeviceHandler
3. DeviceHandler needs TaskScheduler injected (follow MacroHandler pattern)
4. MasterDeviceHandler needs TaskScheduler injected

**Dependencies:** TaskScheduler (already exists), DeviceHandler/MasterDeviceHandler constructors need update

**Test criteria:** Compiles, existing tests still pass

**Status:** done

---

### Batch 16.2 — Tool definitions

**Files:**
- `gig-maestro/tools/claude-tools.json`

**Work:**
1. Add `device_setParameters` tool definition with nested schema for pages/params
2. Add `masterDevice_setParameters` tool definition (same schema)

**Dependencies:** Batch 16.1 (needs to know exact param shape)

**Test criteria:** JSON validates cleanly

**Status:** done

---

### Batch 16.3 — Handler tests

**Files:**
- `gig-maestro/src/test/java/dev/gregross/gig/handlers/DeviceHandlerTest.java`
- `gig-maestro/src/test/java/dev/gregross/gig/handlers/MasterDeviceHandlerTest.java`

**Work:**
1. DeviceHandlerTest:
   - Update method count assertion
   - Add validation tests: missing pages, empty pages, index out of range, value out of range
   - Add behavioral test: single-page set (verify param.value().setImmediately() called)
   - Add behavioral test: multi-page set (verify page navigation + TaskScheduler.schedule() called)
2. MasterDeviceHandlerTest: mirror same tests

**Dependencies:** Batch 16.1 (code must exist to test)

**Test criteria:** All new + existing tests pass

**Status:** done

---

### Batch 16.4 — System prompt rewrite

**Files:**
- `gig-maestro/tools/system-prompt.md`

**Work:**
1. Replace the "Sound Design Recipes" section with from-scratch methodology:
   - **Discover** — scan device pages via selectPage + snapshot to learn available parameters
   - **Design** — apply synthesis knowledge to choose parameter values
   - **Apply** — use device/setParameters to write all values at once
2. Add synthesis principles reference:
   - Oscillator types and their sonic character
   - Filter types and use cases
   - Envelope shaping for different articulations
   - Modulation strategies for movement/interest
3. Add from-scratch recipes by category:
   - Bass (sub, pluck, growl)
   - Leads (mono, poly, pluck)
   - Pads (warm, evolving, ambient)
   - Ambient/Texture (drone, riser, atmosphere)
4. Each recipe specifies the WORKFLOW (which page tags to visit, what synthesis concepts to apply) not specific parameter names

**Dependencies:** None (can run parallel with 16.1-16.3)

**Test criteria:** System prompt reads coherently, workflow references correct RPC methods

**Status:** done

---

### Batch 16.5 — Build verification

**Work:**
1. `./gradlew :gig-maestro:test` — all tests pass
2. `./gradlew :gig-maestro:shadowJar` — builds cleanly
3. Validate claude-tools.json

**Dependencies:** All prior batches

**Test criteria:** Clean build, all tests green

**Status:** done
