# Issues

> Discovered bugs, limitations, and workflow problems. Each entry tracks status and root cause.

---

## Open Issues

### ISS-008: browser/getState resultName lags behind file selection

**Severity:** Major
**Source:** UAT — song creation test
**Phase:** Discovered post-Phase 20
**Status:** OPEN
**Description:** After calling `browser/selectNextFile` or `browser/selectFirstFile`, the `resultName` field in `browser/getState` does not update immediately. It continues reporting the previously committed preset name (or empty string) for multiple calls. This makes it impossible to reliably confirm which preset is selected before committing. During testing, navigating to "Mellow Keys" (8 steps from first) and committing actually loaded "Cosy Sofa Keys" or a different preset because the cursor position and the reported name were out of sync.
**Evidence:** Called `selectFirstFile` + 7x `selectNextFile` to reach index 7, `getState` still reported previous preset name. Committed and got wrong preset. Repeated attempts with different step counts yielded inconsistent results.
**Root cause (suspected):** StateCache observer for `resultName` fires asynchronously — the `selectNextFile` RPC returns before the observer callback updates the cached name. Need a flush cycle or delay between navigation and state read.
**Batch:** —

---

### ISS-009: Preset browsing replaces device and clears clip content

**Severity:** Minor
**Source:** UAT — song creation test
**Phase:** Discovered post-Phase 20
**Status:** OPEN
**Description:** When `browser/browsePresets` is used and a preset is committed, it replaces the device configuration entirely. For Drum Machine, this wipes the clip content (all notes lost) because the new preset reconfigures the drum pads. The user must re-write notes after loading a preset. This is expected Bitwig behavior (preset = full device state replacement), but the tool schemas don't warn about this side effect.
**Evidence:** Created clips with notes on all 4 tracks, then loaded presets via browser/commit. After loading "Soulful - Soul Crushed Kit" on Drum Machine, snapshot showed `hasContent: false` on slot 0. Had to re-create clips and re-write all notes.
**Batch:** —

---

### ISS-010: track/setVolume param name inconsistency with tool schema

**Severity:** Minor
**Source:** UAT — song creation test
**Phase:** Discovered post-Phase 20
**Status:** OPEN
**Description:** The `track/setVolume` RPC handler expects `{"index": N, "value": X}` but the tool schema name `track_setVolume` may lead LLM agents to guess the param is named `"volume"` instead of `"value"`. During testing, `{"index":0,"volume":0.75}` returned a null pointer error. This is not a bug per se — the schema is correct — but the param name `"value"` is inconsistent with the method name `setVolume`. Other methods like `track/setPan` likely use the same pattern.
**Evidence:** `{"index":0,"volume":0.75}` → `-32603 Internal error: Cannot invoke ... because the return value of "JsonObject.get(String)" is null`. Correct call: `{"index":0,"value":0.75}`.
**Batch:** —

---

### ISS-007: Deprecated preset cycling methods throw runtime error in API v25

**Severity:** Blocker
**Source:** UAT
**Phase:** 15
**Status:** RESOLVED
**Description:** All 8 `switchToNext/PreviousPreset*()` methods on CursorDevice throw `"This has been deprecated since API version 2: Use the new browser API instead"` at runtime in Bitwig 6.0 / API v25. They compile fine but fail when called. Affects device/nextPreset, device/previousPreset, device/nextPresetCategory, device/previousPresetCategory, device/nextPresetCreator, device/previousPresetCreator, masterDevice/nextPreset, masterDevice/previousPreset.
**Evidence:** UAT testing returned `-32603` with deprecation message for all 8 methods.
**Batch:** v0.15.6 [UNPLANNED]

### ~~ISS-001: Notes truncated beyond 64-step grid viewport~~ → RESOLVED

Moved to [Resolved Issues](#resolved-issues).

---

### ~~ISS-002: `macro/buildSection` fails after bulk scene deletion~~ → RESOLVED

Moved to [Resolved Issues](#resolved-issues).

---

### ~~ISS-003: `clip/getNotes` requires manual cursor settle delay~~ → RESOLVED

Moved to [Resolved Issues](#resolved-issues).

---

### ~~ISS-004: `track/select` cursor mapping doesn't match track bank indices~~ → RESOLVED

Moved to [Resolved Issues](#resolved-issues).

---

### ~~ISS-005: Deferred macro writes are fire-and-forget (no error reporting)~~ → RESOLVED

Moved to [Resolved Issues](#resolved-issues).

---

### ~~ISS-006: Multiple scene renames in same flush cycle get shifted~~ → RESOLVED

Moved to [Resolved Issues](#resolved-issues).

---

## Resolved Issues

### ISS-001: Notes truncated beyond 64-step grid viewport

**Severity:** Critical → **Resolved:** 2026-03-01
**Fix:** Increased `CLIP_GRID_WIDTH` from 64 to 256 in `GigMaestroExtension.java` and `GRID_WIDTH` in `NoteHandler.java`. Grid now covers 256 steps — at `stepSize=0.25` that's 64 beats (16 bars). Also added x/y bounds validation in `clip/setNotes` so out-of-range notes throw an error instead of being silently dropped.
**Files:** `GigMaestroExtension.java`, `NoteHandler.java`

### ISS-002: `macro/buildSection` fails after bulk scene deletion

**Severity:** Medium → **Resolved:** 2026-03-01
**Fix:** Added optional `sceneIndex` parameter to `macro/buildSection`. When provided, skips scene creation and scroll logic entirely — uses the given index as the slot index directly. This bypasses the stale `stateCache.getSceneItemCount()` issue. The auto-create path (no `sceneIndex`) is preserved but documented as unreliable after bulk deletion. Updated tool schema to recommend `sceneIndex` after manual scene creation.
**Files:** `MacroHandler.java`, `MacroHandlerTest.java`, `claude-tools.json`

### ISS-003: `clip/getNotes` requires manual cursor settle delay

**Severity:** Low → **Resolved:** 2026-03-01
**Fix:** Documented the async cursor settle delay in tool schemas for both `clip/select` and `clip/getNotes`. `clip/select` now warns that cursor movement is async and a 1-2 second wait is needed before `getNotes`. `clip/getNotes` schema warns about stale/empty results if called too soon after `clip/select`. This is a Bitwig API limitation — writing is not affected (uses deferred scheduling).
**Files:** `claude-tools.json`

### ISS-004: `track/select` cursor mapping doesn't match track bank indices

**Severity:** Low → **Resolved:** 2026-03-01
**Fix:** Changed `track/select` response from `cursorTrackName` (stale — cursor hasn't moved yet) to `trackName` + `trackIndex` sourced from the track bank's cached state (accurate immediately). Added `StateCache.getTrackName(index)` method. Updated tool schema to document async cursor movement.
**Files:** `TrackHandler.java`, `StateCache.java`, `claude-tools.json`

### ISS-005: Deferred macro writes are fire-and-forget (no error reporting)

**Severity:** Medium → **Resolved:** 2026-03-01
**Fix:** Documented optimistic return values in tool schemas for `macro/writeClip` and `macro/buildSection`. Both descriptions now explicitly state that `count`/`clipCount` are optimistic (writes deferred to future flush cycles) and recommend `clip/getNotes` for verification after a delay. This is an inherent limitation of Bitwig's `scheduleTask()` — errors in scheduled callbacks can't propagate back to already-sent RPC responses.
**Files:** `claude-tools.json`

### ISS-006: Multiple scene renames in same flush cycle get shifted

**Severity:** Medium → **Resolved:** 2026-03-01
**Fix:** Added `macro/setupScenes` method that creates scenes in phase 1 (same flush) then defers each rename to a separate flush cycle via `scheduleTask`. This avoids Bitwig's scene bank re-indexing within a single flush. Each rename runs in its own flush cycle at `FLUSH_DELAY_MS * (i+1)` spacing.
**Files:** `MacroHandler.java`, `MacroHandlerTest.java`, `claude-tools.json`
