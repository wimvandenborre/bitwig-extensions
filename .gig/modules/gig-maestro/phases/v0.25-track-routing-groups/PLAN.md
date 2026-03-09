# Plan

> Living document — reflects what was done, not just what was intended.
> This file tracks the ACTIVE phase only. Completed phases are archived to `phases/`.

---

## Active Phase

**Phase 25 — Track Routing & Groups**

---

## Batches

### Batch 25.1 — Snapshot observers (trackType, isGroup, isGroupExpanded)

**Version:** `0.25.1`
**Delegation:** in-session
**Files:** `StateCache.java`

**Tasks:**
1. Add 3 new volatile field arrays: `trackTypes[TRACK_COUNT]`, `trackIsGroup[TRACK_COUNT]`,
   `trackIsGroupExpanded[TRACK_COUNT]`
2. Add `registerGroupObservers(TrackBank)` method — loops tracks, subscribes to
   `track.trackType()`, `track.isGroup()`, `track.isGroupExpanded()` with markInterested +
   addValueObserver
3. Add the 3 fields to each track object in `getTrackState()` snapshot section
4. Wire `registerGroupObservers(trackBank)` call in `GigMaestroExtension.init()`

**Test criteria:**
- `./gradlew test` passes
- StateCacheDeltaTest still passes (no section count change — fields are within existing
  `tracks` section)

---

### Batch 25.2 — TrackHandler — 6 new RPC methods

**Version:** `0.25.2`
**Delegation:** in-session
**Files:** `TrackHandler.java`, `GigMaestroExtension.java`

**Tasks:**
1. Add `NoteInput noteInput` field to TrackHandler constructor
2. Update TrackHandler instantiation in GigMaestroExtension to pass `noteInput`
3. Add `track/setGroupExpanded` — params: `{expanded: boolean}` or `{toggle: true}`.
   Uses `cursorTrack.isGroupExpanded().set(bool)` or `.toggle()`
4. Add `track/navigateInto` — no required params. Uses
   `application.navigateIntoTrackGroup(cursorTrack)`
5. Add `track/navigateToParent` — no required params. Uses
   `application.navigateToParentTrackGroup()`
6. Add `track/createGroup` — no required params. Uses
   `cursorTrack.createParentTrack(SEND_COUNT, SCENE_COUNT)` (pass constants or hardcode 4, 5)
7. Add `track/addNoteSource` — no required params. Uses
   `cursorTrack.addNoteSource(noteInput)`
8. Add `track/removeNoteSource` — no required params. Uses
   `cursorTrack.removeNoteSource(noteInput)`

**Test criteria:**
- `./gradlew shadowJar` compiles
- `./gradlew test` passes

---

### Batch 25.3 — Unit tests

**Version:** `0.25.3`
**Delegation:** in-session
**Files:** `TrackHandlerTest.java`

**Tasks:**
1. Update TrackHandler constructor call in test setUp to include null NoteInput param
2. Add registration test for 6 new methods
3. Update total method count assertion (18 → 24)
4. Add validation test: `track/setGroupExpanded` missing both `expanded` and `toggle`
5. Add validation test: `track/setGroupExpanded` with both `expanded` and `toggle`

**Test criteria:**
- `./gradlew test` passes
- All new tests green

---

### Batch 25.4 — Tool schemas + system prompt update

**Version:** `0.25.4`
**Delegation:** in-session
**Files:** `tools/claude-tools.json`, `tools/system-prompt.md`

**Tasks:**
1. Add 6 tool schemas to claude-tools.json:
   - `track_setGroupExpanded` — expanded (boolean, optional), toggle (boolean, optional)
   - `track_navigateInto` — no required params
   - `track_navigateToParent` — no required params
   - `track_createGroup` — no required params
   - `track_addNoteSource` — no required params
   - `track_removeNoteSource` — no required params
2. Update `session_snapshot` description to mention trackType, isGroup, isGroupExpanded
3. Add "Track Routing & Groups" section to system-prompt.md covering:
   - Group creation (createGroup wraps selected track)
   - Group navigation (navigateInto/navigateToParent for drilling into groups)
   - Group fold/unfold (setGroupExpanded)
   - Note routing workflow (addNoteSource + sendNote for targeted playback)
   - Track type reference table

**Test criteria:**
- JSON valid
- Tool count = 222 (216 + 6)

---

### Batch 25.5 — Smoke tests

**Version:** `0.25.5`
**Delegation:** in-session
**Files:** `scripts/smoke-test.sh`

**Tasks:**
1. Add tool presence assertions for 6 new tools
2. Add parameter assertions (setGroupExpanded has expanded/toggle properties)
3. Add system prompt mention assertions (Track Routing, navigateInto, noteSource)
4. Add snapshot field assertions (trackType, isGroup, isGroupExpanded in session_snapshot)

**Test criteria:**
- `./scripts/smoke-test.sh --offline` passes
- All new assertions green

---

## Acceptance Criteria

- [ ] Snapshot shows trackType, isGroup, isGroupExpanded for each track
- [ ] 6 new RPC methods registered and callable
- [ ] Unit tests cover registration + parameter validation
- [ ] Tool schemas valid, count = 222
- [ ] System prompt documents group workflows
- [ ] Smoke tests pass offline
- [ ] `./gradlew test` all green
