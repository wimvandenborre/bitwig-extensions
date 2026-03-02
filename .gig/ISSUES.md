# Issues

> Discovered bugs, limitations, and workflow problems. Each entry tracks status and root cause.

---

## Open Issues

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
