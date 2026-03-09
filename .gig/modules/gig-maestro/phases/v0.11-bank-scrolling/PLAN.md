# Plan

> Living document — reflects what was done, not just what was intended.
> This file tracks the ACTIVE phase only. Completed phases are archived to `phases/`.

---

## Active Phase

### Phase 11 — Bank Scrolling (v0.11.x)

> Add viewport navigation to all three banks (SceneBank, CueMarkerBank, TrackBank). Each bank gets `scrollTo`, `scrollBy`, and `getScrollInfo` RPC methods under a `{domain}Bank/` namespace. Snapshot is enhanced with uniform bank-window metadata (`scrollPosition`, `itemCount`, `bankSize`, `canScrollForwards`, `canScrollBackwards`) for all three banks. CueMarker snapshot restructured from flat array to bank-window object. 11 new observers in StateCache, 9 new RPC methods, bringing total to 99.

**Decisions:** D-11.1, D-11.2a, D-11.3, D-11.4, D-11.5, D-11.6

| Batch | Version | Title | Delegation | Status |
|-------|---------|-------|------------|--------|
| 11.1 | `0.11.1` | StateCache observers + snapshot restructuring | in-session | done |
| 11.2 | `0.11.2` | SceneHandler scroll methods | in-session | done |
| 11.3 | `0.11.3` | ArrangerHandler cueMarkerBank scroll methods | in-session | done |
| 11.4 | `0.11.4` | TrackHandler trackBank scroll methods | in-session | done |
| 11.5 | `0.11.5` | Tool schemas + system prompt update | in-session | done |
| 11.6a | `0.11.6` | Unit tests | in-session | done |
| 11.6b | `0.11.7` | Smoke tests | in-session | done |
| 11.8 | `0.11.8` | Fix clip operation reliability | in-session | done |
| 11.9 | `0.11.9` | 8×5 APC40-style grid + clip launcher feedback | in-session | done |

### Batch 11.1 — StateCache observers + snapshot restructuring

**Delegation:** in-session
**Decisions:** D-11.4, D-11.5
**Files:**
- `src/main/java/dev/gregross/gig/extension/StateCache.java` (MODIFY)

**Work:**
1. Add 11 new observer fields + registrations:
   - `registerClipObservers`: add `sceneBank.itemCount()`, `sceneBank.canScrollForwards()`, `sceneBank.canScrollBackwards()` observers (scrollPosition already exists)
   - `registerArrangementObservers`: add `cueMarkerBank.scrollPosition()`, `cueMarkerBank.itemCount()`, `cueMarkerBank.canScrollForwards()`, `cueMarkerBank.canScrollBackwards()` observers
   - `registerObservers`: add `trackBank.scrollPosition()`, `trackBank.itemCount()`, `trackBank.canScrollForwards()`, `trackBank.canScrollBackwards()` observers
2. Restructure `getScenesState()`:
   - Rename `bankOffset` → `scrollPosition` for consistency with RPC naming
   - Add `itemCount`, `canScrollForwards`, `canScrollBackwards` fields
   - Keep `bankSize` and `scenes` array as-is
3. Restructure `getArrangementState()` cueMarkers section:
   - Wrap in bank-window object: `{ bankSize: 16, scrollPosition, itemCount, canScrollBackwards, canScrollForwards, items: [...] }`
   - Filter cueMarkers to only include those that exist (currently emits all 16 slots)
4. Add track bank metadata to `getTracksState()`:
   - Wrap in bank-window object: `{ bankSize: 64, scrollPosition, itemCount, canScrollBackwards, canScrollForwards, tracks: [...] }`
5. Update `getChangedSections()` hash computation to include new bank metadata fields

**Test criteria:**
- Extension compiles with `./gradlew shadowJar`
- `session/snapshot` scenes section has `scrollPosition`, `itemCount`, `canScrollForwards`, `canScrollBackwards`
- `session/snapshot` cueMarkers section is a bank-window object with `items` array
- `session/snapshot` tracks section is a bank-window object with `tracks` array

**Acceptance:** All 3 bank sections in snapshot follow uniform `{ bankSize, scrollPosition, itemCount, canScroll*, items/scenes/tracks }` pattern. 11 new observers registered.

### Batch 11.2 — SceneHandler scroll methods

**Delegation:** team
**Depends on:** Batch 11.1 (snapshot fields used by getScrollInfo)
**Decisions:** D-11.2a, D-11.3
**Files:**
- `src/main/java/dev/gregross/gig/handlers/SceneHandler.java` (MODIFY)

**Work:**
1. Add `StateCache` as constructor dependency (needed for `getScrollInfo` to return cached values)
2. Update `GigMaestroExtension.java` to pass `stateCache` to `SceneHandler`
3. Register 3 new RPC methods:
   - `sceneBank/scrollTo` — param: `position` (int, absolute global index). Validate `position >= 0 && position < itemCount`. Call `sceneBank.scrollPosition().set(position)`. Return `{ ok: true }`. On invalid position: return JSON-RPC error with `{ code: -32001, message: "POSITION_OUT_OF_RANGE", data: { itemCount, requestedPosition } }`.
   - `sceneBank/scrollBy` — param: `amount` (int, positive=forward, negative=backward). Call `sceneBank.scrollBy(amount)`. Return `{ ok: true }`.
   - `sceneBank/getScrollInfo` — no params. **Snapshot-backed:** return `{ scrollPosition, itemCount, bankSize: 8, canScrollForwards, canScrollBackwards }` from cached StateCache observer values (same data as snapshot, no separate live pull).

**Test criteria:**
- Extension compiles with `./gradlew shadowJar`
- `api/list` includes `sceneBank/scrollTo`, `sceneBank/scrollBy`, `sceneBank/getScrollInfo`

**Acceptance:** All 3 scene bank scroll methods registered with position validation.

### Batch 11.3 — ArrangerHandler cueMarkerBank scroll methods

**Delegation:** team (parallel with 11.2 and 11.4)
**Depends on:** Batch 11.1
**Decisions:** D-11.2a, D-11.3
**Files:**
- `src/main/java/dev/gregross/gig/handlers/ArrangerHandler.java` (MODIFY)

**Work:**
1. Add `StateCache` as constructor dependency
2. Update `GigMaestroExtension.java` to pass `stateCache` to `ArrangerHandler`
3. Register 3 new RPC methods:
   - `cueMarkerBank/scrollTo` — param: `position` (int). Validate bounds. Call `cueMarkerBank.scrollPosition().set(position)`. Return `{ ok: true }` or `POSITION_OUT_OF_RANGE` error.
   - `cueMarkerBank/scrollBy` — param: `amount` (int). Call `cueMarkerBank.scrollBy(amount)`. Return `{ ok: true }`.
   - `cueMarkerBank/getScrollInfo` — no params. **Snapshot-backed:** return `{ scrollPosition, itemCount, bankSize: 16, canScrollForwards, canScrollBackwards }` from cached StateCache observer values.

**Test criteria:**
- Extension compiles with `./gradlew shadowJar`
- `api/list` includes `cueMarkerBank/scrollTo`, `cueMarkerBank/scrollBy`, `cueMarkerBank/getScrollInfo`

**Acceptance:** All 3 cue marker bank scroll methods registered with position validation.

### Batch 11.4 — TrackHandler trackBank scroll methods

**Delegation:** team (parallel with 11.2 and 11.3)
**Depends on:** Batch 11.1
**Decisions:** D-11.2a, D-11.3
**Files:**
- `src/main/java/dev/gregross/gig/handlers/TrackHandler.java` (MODIFY)

**Work:**
1. Add `StateCache` as constructor dependency
2. Update `GigMaestroExtension.java` to pass `stateCache` to `TrackHandler`
3. Register 3 new RPC methods:
   - `trackBank/scrollTo` — param: `position` (int). Validate bounds. Call `trackBank.scrollPosition().set(position)`. Return `{ ok: true }` or `POSITION_OUT_OF_RANGE` error.
   - `trackBank/scrollBy` — param: `amount` (int). Call `trackBank.scrollBy(amount)`. Return `{ ok: true }`.
   - `trackBank/getScrollInfo` — no params. **Snapshot-backed:** return `{ scrollPosition, itemCount, bankSize: 64, canScrollForwards, canScrollBackwards }` from cached StateCache observer values.

**Test criteria:**
- Extension compiles with `./gradlew shadowJar`
- `api/list` includes `trackBank/scrollTo`, `trackBank/scrollBy`, `trackBank/getScrollInfo`

**Acceptance:** All 3 track bank scroll methods registered with position validation.

### Batch 11.5 — Tool schemas + system prompt update

**Delegation:** in-session
**Depends on:** Batch 11.2, 11.3, 11.4
**Decisions:** D-11.1, D-11.2a
**Files:**
- `tools/claude-tools.json` (MODIFY — add 9 tool schemas)
- `tools/system-prompt.md` (MODIFY — add bank navigation section)

**Work:**
1. Add 9 tool schemas matching new RPC methods (underscore naming):
   - `scene_bank_scroll_to`, `scene_bank_scroll_by`, `scene_bank_get_scroll_info`
   - `cue_marker_bank_scroll_to`, `cue_marker_bank_scroll_by`, `cue_marker_bank_get_scroll_info`
   - `track_bank_scroll_to`, `track_bank_scroll_by`, `track_bank_get_scroll_info`
2. Update system prompt:
   - Add "Bank Navigation" section explaining viewport model, scroll semantics, absolute vs relative
   - Document updated snapshot structure (all 3 banks now bank-window objects)
   - Add guidance: "Check `canScrollForwards` before scrolling" pattern
   - Add "Snapshot v0.11" migration note with old → new field mapping:
     - `scenes.bankOffset` → `scenes.scrollPosition`
     - `tracks` (flat array) → `tracks.tracks` (inside bank-window object)
     - `arrangement.cueMarkers` (flat array) → `cueMarkers.items` (inside bank-window object)
     - New fields on all banks: `scrollPosition`, `itemCount`, `canScrollForwards`, `canScrollBackwards`, `bankSize`
3. Document `getScrollInfo` as snapshot-backed: returns cached observer values, same data as snapshot bank metadata (no separate live pull)

**Test criteria:**
- All 9 tool schemas valid JSON
- Tool names follow underscore convention matching RPC method names
- System prompt describes bank navigation and snapshot v0.11 changes

**Acceptance:** Tool schemas and system prompt updated with all 9 scroll methods. Snapshot migration documented.

### Batch 11.6a — Unit tests

**Delegation:** in-session
**Depends on:** Batch 11.2, 11.3, 11.4
**Files:**
- `src/test/java/dev/gregross/gig/handlers/SceneHandlerTest.java` (MODIFY — add scroll tests)
- `src/test/java/dev/gregross/gig/handlers/ArrangerHandlerTest.java` (MODIFY — add scroll tests)
- `src/test/java/dev/gregross/gig/handlers/TrackHandlerTest.java` (MODIFY — add scroll tests)

**Work:**
1. Unit tests for each handler's scroll methods:
   - `scrollTo` — validate `position` param required, bounds validation
   - `scrollBy` — validate `amount` param required
   - `getScrollInfo` — verify return shape has all 5 fields
   - POSITION_OUT_OF_RANGE error shape validation
2. Method registration tests for all 9 new methods

**Test criteria:**
- `./gradlew test` passes all new and existing tests

**Acceptance:** All handler scroll unit tests green, no regressions.

### Batch 11.6b — Smoke tests

**Delegation:** in-session
**Depends on:** Batch 11.5
**Files:**
- `scripts/smoke-test.sh` (MODIFY — add offline schema tests for new methods)

**Work:**
1. Offline smoke tests: schema validation for all 9 new tool schemas
2. Update method count check (99 total: 90 + 9)
3. Validate updated snapshot structure documentation

**Test criteria:**
- `./scripts/smoke-test.sh --offline` passes with updated method count

**Acceptance:** All offline smoke tests green.

### Batch 11.8 — Fix clip operation reliability [UNPLANNED]

**Delegation:** in-session
**Trigger:** Discovered during live testing — `clearAllNotes` only cleared visible step window, `clip/select` on empty slot silently failed causing note writes to wrong clip.
**Files:**
- `src/main/java/dev/gregross/gig/handlers/NoteHandler.java` (MODIFY — add StateCache dep, fix clearAllNotes)
- `src/main/java/dev/gregross/gig/handlers/ClipHandler.java` (MODIFY — add StateCache dep, guard clip/select)
- `src/main/java/dev/gregross/gig/extension/StateCache.java` (MODIFY — add getClipStepSize, clipHasContent getters)
- `src/main/java/dev/gregross/gig/extension/GigMaestroExtension.java` (MODIFY — pass stateCache to ClipHandler, NoteHandler)

**Work:**
1. **Bug fix: `clip/clearAllNotes` viewport limitation** — `clearSteps()` only clears the 64-step grid viewport, not the full clip. Fix: temporarily set step size to 4.0 (64 steps × 4.0 = 256 beats = 64 bars), scroll to step 0, clear, then restore original step size.
2. **Bug fix: `clip/select` on empty slot** — selecting an empty slot doesn't move the cursor clip, so subsequent `setNotes`/`clearAllNotes` silently operates on the wrong clip. Fix: check `stateCache.clipHasContent(trackIndex, slotIndex)` and return `-32602` error if slot is empty.
3. **StateCache getters:** Added `getClipStepSize()` and `clipHasContent(trackIndex, slotIndex)` public methods.
4. **Constructor updates:** NoteHandler and ClipHandler now take StateCache as dependency.

**Test criteria:**
- `./gradlew test` passes
- `./gradlew shadowJar` builds
- `clip/select` on empty slot returns error with "slot is empty" message

**Acceptance:** All three bugs fixed, tests pass, extension builds.

### Batch 11.9 — 8×5 APC40-style grid + clip launcher feedback [UNPLANNED]

**Delegation:** in-session
**Trigger:** User requested smaller bank window matching APC40 form factor for visual feedback in Bitwig UI.
**Files:**
- `src/main/java/dev/gregross/gig/extension/GigMaestroExtension.java` (MODIFY — TRACK_COUNT 64→8, SCENE_COUNT 8→5, add feedback calls)
- `src/main/java/dev/gregross/gig/extension/StateCache.java` (MODIFY — TRACK_COUNT 64→8, SCENE_COUNT 8→5)
- `src/main/java/dev/gregross/gig/handlers/ClipHandler.java` (MODIFY — SCENE_COUNT 8→5)
- `src/main/java/dev/gregross/gig/handlers/SceneHandler.java` (MODIFY — SCENE_COUNT 8→5)
- `tools/claude-tools.json` (MODIFY — bankSize refs updated)
- `tools/system-prompt.md` (MODIFY — bank window descriptions updated)
- `scripts/smoke-test.sh` (MODIFY — online assertions updated)

**Work:**
1. Change `TRACK_COUNT` from 64 to 8 and `SCENE_COUNT` from 8 to 5 across all files.
2. Add `trackBank.setShouldShowClipLauncherFeedback(true)` and `sceneBank.setIndication(true)` after bank creation.
3. Update tool schema descriptions and system prompt with new bank sizes.
4. Update online smoke test assertions for new counts.

**Test criteria:**
- `./gradlew test` passes
- `./gradlew shadowJar` builds
- `./scripts/smoke-test.sh --offline` passes
- Clip launcher feedback rectangle visible in Bitwig UI

**Acceptance:** 8×5 grid active, feedback rectangle visible, all tests pass.

**Phase Acceptance Criteria:**
- [x] 9 new RPC methods registered (3 per bank × 3 banks)
- [x] All 3 snapshot bank sections follow uniform `{ bankSize, scrollPosition, itemCount, canScrollBackwards, canScrollForwards, items }` pattern
- [x] CueMarker snapshot restructured from flat array to bank-window object
- [x] 11 new observers in StateCache (12 total per-bank scroll observers, 1 pre-existing)
- [x] `scrollTo` validates bounds and returns `POSITION_OUT_OF_RANGE` with `{ itemCount, requestedPosition }`
- [x] Tool schemas and system prompt updated for all 9 methods
- [x] All unit tests pass (`./gradlew test`) — 154 tests
- [x] All offline smoke tests pass (`./scripts/smoke-test.sh --offline`) — 216 tests
- [x] Total RPC methods: 99 (90 + 9)

**Completion triggers Phase 12 → version `0.12.0`**

---

## Plan Amendments

<!-- Log any changes to the plan after creation -->

| Date | Version | Amendment | Reason |
|------|---------|-----------|--------|
| 2026-02-28 | 0.11.0 | Batch 11.5: added snapshot v0.11 migration note with old→new field mapping table | Prevent downstream breakage in CLI/smoke tests/LLM prompts |
| 2026-02-28 | 0.11.0 | Batches 11.2–11.4: `getScrollInfo` explicitly documented as snapshot-backed (cached observer values, no separate live pull) | Align with Scrollable's value model and perception loop |
| 2026-03-01 | 0.11.8 | Added Batch 11.8 [UNPLANNED]: Fix clip operation reliability (clearAllNotes viewport, clip/select empty slot guard) | Discovered during live testing |
| 2026-03-01 | 0.11.9 | Added Batch 11.9 [UNPLANNED]: 8×5 APC40-style grid with clip launcher feedback rectangle | User requested smaller bank window for visual feedback |
| 2026-02-28 | 0.11.0 | Batches 11.2–11.4: `getScrollInfo` explicitly documented as snapshot-backed (cached observer values, no separate live pull) | Align with Scrollable's value model and perception loop |
