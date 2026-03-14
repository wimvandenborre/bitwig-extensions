# Plan

> Living document — reflects what was done, not just what was intended.
> This file tracks the ACTIVE phase only. Completed phases are archived to `phases/`.

---

## Active Phase

### Phase 32 — gig-maestro: Track Queries & Cursor Navigation (v0.32.x)

> Add track capability flags (canHoldNoteData, canHoldAudioData) to StateCache and snapshot for all track bank tracks. Add CursorTrack navigation methods: selectParent, selectFirstChild, setPinned, and getInfo. Enables Claude to make conditional decisions based on track capabilities and navigate group hierarchies.

**Decisions:** D-1.1, D-1.2, D-1.3, D-1.4

| Batch | Version | Title | Delegation | Status |
|-------|---------|-------|------------|--------|
| 32.1 | `0.32.1` | StateCache track capabilities + cursor navigation RPCs | team | done |
| 32.2 | `0.32.2` | Fix groove parameter observer deprecation | in-session | done |
| 32.3 | `0.32.3` | Unit tests | in-session | done |
| 32.4 | `0.32.4` | Tool definitions + system prompt + smoke tests | in-session | done |
| 32.5 | `0.32.5` | Build verification | in-session | done |

### Batch 32.1 — StateCache track capabilities + cursor navigation RPCs

**Delegation:** team
**Decisions:** D-1.1, D-1.2, D-1.3, D-1.4
**Files:**
- `gig-maestro/src/main/java/dev/gregross/gig/extension/StateCache.java` (MODIFY)
- `gig-maestro/src/main/java/dev/gregross/gig/handlers/TrackHandler.java` (MODIFY)
**Work:**
1. In StateCache, add two new boolean arrays:
   - `trackCanHoldNoteData[TRACK_COUNT]` and `trackCanHoldAudioData[TRACK_COUNT]`
   - Register observers in `registerGroupObservers()` alongside trackType/isGroup:
     ```java
     track.canHoldNoteData().markInterested();
     track.canHoldNoteData().addValueObserver(v -> trackCanHoldNoteData[idx] = v);
     track.canHoldAudioData().markInterested();
     track.canHoldAudioData().addValueObserver(v -> trackCanHoldAudioData[idx] = v);
     ```
   - Add to snapshot in the track object: `track.addProperty("canHoldNoteData", trackCanHoldNoteData[i])` and same for audio
2. In TrackHandler, add 4 cursor methods:
   - `cursor/selectParent` — no params, calls `cursorTrack.selectParent()`, returns "ok"
   - `cursor/selectFirstChild` — no params, calls `cursorTrack.selectFirstChild()`, returns "ok"
   - `cursor/setPinned` — params: `{pinned: boolean}`, calls `cursorTrack.isPinned().set(pinned)`, returns "ok"
   - `cursor/getInfo` — no params, returns `{name, trackType, isPinned}` by reading cursorTrack properties
**Test criteria:** Unit tests in batch 32.2
**Acceptance:** Track capabilities in snapshot, 4 cursor methods registered

### Batch 32.2 — Unit tests

**Delegation:** in-session
**Decisions:** D-1.1, D-1.2, D-1.3, D-1.4
**Depends on:** Batch 32.1
**Files:**
- `gig-maestro/src/test/java/dev/gregross/gig/handlers/TrackHandlerTest.java` (MODIFY)
- `gig-maestro/src/test/java/dev/gregross/gig/extension/StateCacheSnapshotTest.java` (MODIFY — if snapshot tests exist)
**Work:**
1. TrackHandlerTest: add tests for selectParent, selectFirstChild, setPinned true/false, getInfo returns expected fields
2. StateCacheSnapshotTest: verify canHoldNoteData and canHoldAudioData appear in snapshot
3. Update method count assertion in TrackHandlerTest
**Test criteria:** `./gradlew :gig-maestro:test` passes
**Acceptance:** All new methods have test coverage

### Batch 32.3 — Tool definitions + system prompt + smoke tests

**Delegation:** in-session
**Depends on:** Batch 32.2
**Files:**
- `gig-maestro/tools/claude-tools.json` (MODIFY)
- `gig-maestro/tools/system-prompt.md` (MODIFY)
- `gig-maestro/scripts/tests/offline-schemas.sh` (MODIFY)
**Work:**
1. Add tool definitions for `cursor_selectParent`, `cursor_selectFirstChild`, `cursor_setPinned`, `cursor_getInfo`
2. Update session_snapshot description to mention canHoldNoteData/canHoldAudioData
3. Update system prompt with Track Capabilities and Cursor Navigation sections
4. Add smoke test assertions for new tools and prompt references
**Test criteria:** `gig-maestro/scripts/smoke-test.sh --offline` passes
**Acceptance:** All 4 new tools in schema, documented in prompt, validated by smoke tests

### Batch 32.4 — Build verification

**Delegation:** in-session
**Depends on:** Batch 32.3
**Files:** None
**Work:** Run `./gradlew :gig-maestro:shadowJar` and `./gradlew :gig-maestro:test` to confirm clean build
**Test criteria:** Both commands exit 0
**Acceptance:** Extension builds and all tests pass

**Phase Acceptance Criteria:**
- [ ] canHoldNoteData and canHoldAudioData in snapshot for all tracks
- [ ] Track capabilities in delta notifications
- [ ] cursor/selectParent navigates to parent group
- [ ] cursor/selectFirstChild navigates into group
- [ ] cursor/setPinned controls cursor pinning
- [ ] cursor/getInfo returns cursor track state
- [ ] All unit tests pass
- [ ] Tool schemas and system prompt updated
- [ ] Offline smoke tests pass
- [ ] Clean build with shadowJar

**Completion triggers Phase 33 -> version `0.33.0`**
