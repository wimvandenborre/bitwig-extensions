# Plan

> Living document — reflects what was done, not just what was intended.
> This file tracks the ACTIVE phase only. Completed phases are archived to `phases/`.

---

## Active Phase

### Phase 20 — Clip Launcher Grid Enhancements (v0.20.x)

> Complete the clip launcher grid toolkit with color control (clip + scene), clip boundary/loop adjustment, note quantize and transpose, content duplication, alternative launch methods for pad-style performance, and editor integration.

**Decisions:** D-20.1, D-20.2, D-20.3, D-20.4, D-20.5, D-20.6

| Batch | Version | Title | Delegation | Status |
|-------|---------|-------|------------|--------|
| 20.1 | `0.20.1` | Snapshot observers — scene color + cursor clip fields | in-session | done |
| 20.2 | `0.20.2` | ClipHandler — color, boundaries, quantize, transpose, extras | in-session | done |
| 20.3 | `0.20.3` | SceneHandler — scene color + alt launch methods | in-session | done |
| 20.4 | `0.20.4` | Unit tests | in-session | done |
| 20.5 | `0.20.5` | Tool schemas + system prompt update | in-session | done |
| 20.6 | `0.20.6` | Smoke tests | in-session | done |

### Batch 20.1 — Snapshot observers — scene color + cursor clip fields

**Delegation:** in-session
**Decisions:** D-20.6
**Files:** `src/main/java/dev/gregross/gig/extension/StateCache.java`
**Work:**
- Add scene color observers: `sceneColors` float[SCENE_COUNT][3] in scene observer registration, add to scenes snapshot output
- Add cursor clip `isLoopEnabled` (boolean) observer in registerClipCursorObservers
- Add cursor clip `loopStart` (double) observer in registerClipCursorObservers
- Add cursor clip color observer (r,g,b floats) in registerClipCursorObservers
- Extend getClipState() with isLoopEnabled, loopStart, color
- Extend getScenesState() with per-scene color
- Add public getter for clip playback settings (boundaries + loop)
**Test criteria:** `./gradlew test` passes; snapshot includes new fields
**Acceptance:** Scene colors in snapshot; cursor clip has isLoopEnabled, loopStart, color

### Batch 20.2 — ClipHandler — color, boundaries, quantize, transpose, extras

**Delegation:** in-session
**Decisions:** D-20.2, D-20.3, D-20.4, D-20.5
**Files:** `src/main/java/dev/gregross/gig/handlers/ClipHandler.java`
**Work:**
- Add `clip/setColor` — params: trackIndex, slotIndex, r, g, b (floats 0.0–1.0) — operates on slot
- Add `clip/setPlayStart` — params: beats (double) — cursor clip
- Add `clip/setPlayStop` — params: beats (double) — cursor clip
- Add `clip/setLoopStart` — params: beats (double) — cursor clip
- Add `clip/setLoopLength` — params: beats (double) — cursor clip
- Add `clip/setLoopEnabled` — params: enabled (boolean) — cursor clip
- Add `clip/getPlaybackSettings` — returns all boundary + loop values from stateCache
- Add `clip/quantize` — params: amount (double 0.0–1.0) — cursor clip
- Add `clip/transpose` — params: semitones (integer) — cursor clip
- Add `clip/duplicateContent` — no params — cursor clip
- Add `clip/showInEditor` — no params — cursor clip (via slot, not cursor)
- Add `clip/launchAlt` — params: trackIndex, slotIndex
- Add `clip/launchRelease` — params: trackIndex, slotIndex
- Add `clip/launchReleaseAlt` — params: trackIndex, slotIndex
**Test criteria:** `./gradlew test` passes; 14 new methods registered
**Acceptance:** All 14 methods respond correctly

### Batch 20.3 — SceneHandler — scene color + alt launch methods

**Delegation:** in-session
**Decisions:** D-20.2, D-20.5
**Files:** `src/main/java/dev/gregross/gig/handlers/SceneHandler.java`
**Work:**
- Add `scene/setColor` — params: index, r, g, b (floats 0.0–1.0)
- Add `scene/launchAlt` — params: index
- Add `scene/launchRelease` — params: index
- Add `scene/launchReleaseAlt` — params: index
**Test criteria:** `./gradlew test` passes; 4 new methods registered
**Acceptance:** All 4 methods respond correctly

### Batch 20.4 — Unit tests

**Delegation:** in-session
**Decisions:** D-20.1 through D-20.6
**Files:** `src/test/java/dev/gregross/gig/handlers/ClipHandlerTest.java`, `src/test/java/dev/gregross/gig/handlers/SceneHandlerTest.java`
**Work:**
- Update ClipHandlerTest: verify new method count (16→30), test color RGB validation, quantize amount range, transpose param validation, boundary param validation
- Update SceneHandlerTest: verify new method count, test color RGB validation, scene index validation
**Test criteria:** `./gradlew test` passes with all new tests
**Acceptance:** All handler tests pass with updated counts and validation

### Batch 20.5 — Tool schemas + system prompt update

**Delegation:** in-session
**Decisions:** D-20.1 through D-20.6
**Files:** `tools/claude-tools.json`, `tools/system-prompt.md`
**Work:**
- Add 18 new tool schemas (14 clip + 4 scene)
- Add "Clip Grid Enhancements" section to system prompt covering color, boundaries, quantize/transpose, alt launch
- Document the morph factor for quantize (0.0–1.0, not a grid value)
**Test criteria:** JSON valid; tool count = 176 + 18 = 194
**Acceptance:** 18 new tools in schemas; system prompt documents all new capabilities

### Batch 20.6 — Smoke tests

**Delegation:** in-session
**Decisions:** D-20.1 through D-20.6
**Files:** `scripts/smoke-test.sh`
**Work:**
- Offline: verify 18 new methods in tool schemas, new snapshot fields (scene color, clip isLoopEnabled/loopStart)
- Online: test clip/setColor, clip/quantize validation, clip/transpose validation, scene/setColor, clip/getPlaybackSettings, api/list for new methods
**Test criteria:** `./scripts/smoke-test.sh --offline` passes
**Acceptance:** All offline smoke tests pass

**Phase Acceptance Criteria:**
- [ ] 18 new RPC methods registered and functional
- [ ] Scene colors in snapshot (per-scene r,g,b)
- [ ] Cursor clip isLoopEnabled, loopStart, color in snapshot
- [ ] All unit tests pass (`./gradlew test`)
- [ ] All offline smoke tests pass
- [ ] Tool schemas and system prompt updated
- [ ] Conventional commits per batch

**Completion triggers Phase 21 → version `0.21.0`**
