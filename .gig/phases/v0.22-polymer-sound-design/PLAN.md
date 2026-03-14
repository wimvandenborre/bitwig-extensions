# Plan

> Living document — reflects what was done, not just what was intended.
> This file tracks the ACTIVE phase only. Completed phases are archived to `phases/`.

---

## Active Phase

### Phase 22 — Polymer Sound Design (v0.22.x)

> Add a `device/discoverAll` RPC method that dumps every parameter page of the current device in one call, use it to generate a Polymer reference file, then create 5 sound design presets as JSON files directly compatible with `macro/createSound`. Update system prompt and smoke tests.

**Decisions:** D-22.1, D-22.2, D-22.3, D-22.4, D-22.5

| Batch | Version | Title | Delegation | Status |
|-------|---------|-------|------------|--------|
| 22.1 | `0.22.1` | `device/discoverAll` RPC implementation | in-session | done |
| 22.2 | `0.22.2` | Unit tests for discoverAll | in-session | done |
| 22.3 | `0.22.3` | Fix discovery getName() → pageNames() | in-session | done |
| 22.4 | `0.22.4` | Polymer parameter map + 5 sound presets | in-session | done |
| 22.5 | `0.22.5` | Tool definition + system prompt + smoke tests | in-session | done |
| 22.6 | `0.22.6` | Build verification | in-session | done |
| 22.7 | `0.22.7` | Fix setParameters page-switch timing bug | in-session | done |
| 22.8 | `0.22.8` | Preset refinement from live testing | in-session | done |

---

### Batch 22.1 — `device/discoverAll` RPC implementation

**Delegation:** in-session
**Decisions:** D-22.1, D-22.4
**Files:**
- `gig-maestro/src/main/java/dev/gregross/gig/handlers/DeviceHandler.java`

**Work:**
Add `device/discoverAll` RPC method to DeviceHandler. Takes no parameters. Implementation:
1. Read `pageCount` from `remoteControlsPage.pageCount().get()`
2. Save current `pageIndex` for restoration
3. For each page (0 to pageCount-1):
   - Set `remoteControlsPage.selectedPageIndex().set(i)`
   - Schedule a 100ms-delayed task that reads all 8 parameter names, values, and displayed values from `remoteControlsPage.getParameter(j)`
   - Collect into a JsonArray
4. After all pages collected, restore original page index
5. Return JSON: `{ deviceName, pageCount, pages: [{ index, name, parameters: [{ index, name, value, displayedValue }] }] }`

Since this is async (scheduled tasks), the method needs to collect results via a callback or use the existing `TaskScheduler` pattern. The simplest approach: register each page read as a scheduled task at `i * FLUSH_DELAY_MS`, with the final task building and returning the complete JSON response via a CompletableFuture or similar mechanism.

**Alternative simpler approach:** Since the StateCache already observes parameter state, we can read the StateCache's `getDeviceState()` after each page switch. But StateCache is in a different class. The cleaner path is to read directly from the `remoteControlsPage` API objects in the handler, using the same `scheduler.schedule()` pattern.

**Test criteria:** Method registered in dispatcher, returns valid JSON structure.
**Acceptance:** `device/discoverAll` callable via RPC, returns all pages of current device.

---

### Batch 22.2 — Unit tests for discoverAll

**Delegation:** in-session
**Decisions:** D-22.1
**Files:**
- `gig-maestro/src/test/java/dev/gregross/gig/handlers/DeviceHandlerTest.java`

**Work:**
Add tests:
1. `discoverAll_registeredInDispatcher` — method count check
2. `discoverAll_returnsAllPages` — mock a 3-page device, verify response contains 3 pages with 8 parameters each
3. `discoverAll_restoresOriginalPage` — verify page index restored after scan
4. `discoverAll_emptyDevice` — device with 0 pages returns empty pages array

**Test criteria:** `./gradlew :gig-maestro:test` passes.
**Acceptance:** All 4 tests green.

---

### Batch 22.3 — Polymer parameter map + 5 sound presets

**Delegation:** in-session (requires live Bitwig for discovery)
**Decisions:** D-22.2, D-22.3
**Files:**
- `gig-maestro/data/devices/polymer.json` (new)
- `gig-maestro/data/presets/polymer/pluck.json` (new)
- `gig-maestro/data/presets/polymer/bass.json` (new)
- `gig-maestro/data/presets/polymer/lead.json` (new)
- `gig-maestro/data/presets/polymer/pad.json` (new)
- `gig-maestro/data/presets/polymer/keys.json` (new)

**Work:**
1. Build and deploy extension with `device/discoverAll`
2. Load Polymer in Bitwig, call `device/discoverAll` via curl
3. Save raw output as `data/devices/polymer.json` (with cleanup/formatting)
4. Using the parameter map + synthesis knowledge, create 5 preset files:
   - **pluck.json** — Short amp decay, bright filter with fast envelope, slight detune
   - **bass.json** — Low octave, sub-heavy, low-pass filter, minimal release
   - **lead.json** — Saw-based, moderate filter, some resonance, mono-style
   - **pad.json** — Slow attack/release, detuned oscillators, warm filter
   - **keys.json** — Medium attack, moderate decay, piano-like envelope
5. Each preset follows: `{ name, description, device: "Polymer", pages: [...] }` — directly passable to `macro/createSound`

**Test criteria:** JSON files are valid, preset pages arrays match Polymer's actual page structure.
**Acceptance:** Each preset loadable via `macro/createSound` producing a distinct sound in Bitwig.

---

### Batch 22.4 — Tool definition + system prompt + smoke tests

**Delegation:** in-session
**Decisions:** D-22.5
**Files:**
- `gig-maestro/tools/claude-tools.json`
- `gig-maestro/tools/system-prompt.md`
- `gig-maestro/scripts/tests/offline-schemas.sh`

**Work:**
1. Add `device_discoverAll` tool definition to claude-tools.json (no parameters, returns full device map)
2. Update system prompt:
   - Document `device/discoverAll` in the device control section
   - Add "Device Reference Files" section pointing to `data/devices/`
   - Add "Sound Presets" section pointing to `data/presets/`
   - Update the discover-design-apply workflow to mention discoverAll as Step 1
3. Update offline-schemas.sh:
   - Add tool existence check for `device_discoverAll`
   - Add system prompt keyword checks for discoverAll, reference files, presets

**Test criteria:** `./scripts/smoke-test.sh --offline` passes with new assertions.
**Acceptance:** Tool defined, prompt updated, smoke tests green.

---

### Batch 22.5 — Build verification

**Delegation:** in-session
**Decisions:** all
**Files:** none (verification only)

**Work:**
1. `./gradlew clean build` — full build passes
2. `./gradlew :gig-maestro:test` — all tests pass
3. `./scripts/smoke-test.sh --offline` — offline smoke tests pass

**Test criteria:** All three commands exit 0.
**Acceptance:** Clean build with no warnings or failures.

---

**Phase Acceptance Criteria:**
- [ ] `device/discoverAll` returns complete parameter map for any device
- [ ] `data/devices/polymer.json` contains accurate Polymer parameter layout
- [ ] 5 preset files loadable via `macro/createSound` producing distinct sounds
- [ ] Tool definition and system prompt document the new capabilities
- [ ] All tests and smoke tests pass
- [ ] Build clean

**Completion triggers Phase 23 → version `0.23.0`**
