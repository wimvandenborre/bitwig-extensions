# Plan

> Living document — reflects what was done, not just what was intended.
> This file tracks the ACTIVE phase only. Completed phases are archived to `phases/`.

---

## Active Phase

### Phase 2 — Clip Grid + Device Params (v0.2.x)

> Extend the Bitwig bridge with clip launcher state and actions (8 slots per track, scene launching) and cursor-based device parameter access (CursorTrack → CursorDevice → 8-param RemoteControlsPage), completing the core action surface needed for LLM agents to control a full session — tracks, transport, clips, and devices — before building CLI or agent clients on top.

**Decisions:** D-2.1, D-2.2, D-2.3, D-2.4, D-2.5, D-2.6a

| Batch | Version | Title | Delegation | Dependencies | Status |
|-------|---------|-------|------------|--------------|--------|
| 2.1 | `0.2.1` | Clip launcher observers + snapshot | in-session | — | done |
| 2.2 | `0.2.2` | Clip + Scene action handlers | in-session | 2.1 | done |
| 2.3 | `0.2.3` | Device observers + snapshot | in-session | — | done |
| 2.4 | `0.2.4` | Device + Cursor action handlers | in-session | 2.3 | done |
| 2.5 | `0.2.5` | Smoke test extension | in-session | 2.2, 2.4 | done |

---

### Batch 2.1 — Clip launcher observers + snapshot

**Delegation:** in-session
**Decisions:** D-2.1
**Files:**
- `src/main/java/dev/gregross/gig/extension/GigMaestroExtension.java` (modify TrackBank creation)
- `src/main/java/dev/gregross/gig/extension/StateCache.java` (add clip + scene observers + snapshot sections)

**Work:**
- Change `host.createMainTrackBank(64, 0, 0)` to `host.createMainTrackBank(64, 0, 8)` — third arg sets scene slot count to 8
- Add clip state arrays to StateCache: per-track × 8 slots for `hasContent`, `isPlaying`, `isRecording`, `isPlaybackQueued`, `isRecordingQueued`, `isStopQueued`, `name`, `color`
- Add scene state arrays: 8 scenes with `name`, `clipCount`
- Register clip observers via `ClipLauncherSlotBank` indexed callbacks (`addHasContentObserver`, `addIsPlayingObserver`, `addIsRecordingObserver`, `addNameObserver`, etc.)
- Register scene observers via `SceneBank` from the TrackBank (`trackBank.sceneBank()`)
- Extend `getSnapshot()` to include `clips` array nested under each track and a top-level `scenes` array
- Wire `registerClipObservers(trackBank)` call in GigMaestroExtension.init()

**Test criteria:**
- `./gradlew build` compiles with zero errors
- `./gradlew shadowJar` produces `.bwextension`
- Extension loads in Bitwig without errors
- `session/snapshot` returns tracks with `clips` arrays (8 slots each) and top-level `scenes` array
- Clip slots with content show `hasContent: true` and populated names

**Acceptance:** Clip launcher state observable in snapshot — slots show playback state, content, names, and colors.

---

### Batch 2.2 — Clip + Scene action handlers

**Delegation:** in-session
**Decisions:** D-2.2, D-2.6a
**Depends on:** Batch 2.1
**Files:**
- `src/main/java/dev/gregross/gig/handlers/ClipHandler.java` (new)
- `src/main/java/dev/gregross/gig/extension/GigMaestroExtension.java` (register handler)

**Work:**
Register JSON-RPC action methods:
- `clip/launch` → `clipSlotBank.launch(slotIndex)` (params: `{ trackIndex, slotIndex }`)
- `clip/stop` → `clipSlotBank.stop()` (params: `{ trackIndex }`)
- `clip/record` → `clipSlotBank.record(slotIndex)` (params: `{ trackIndex, slotIndex }`)
- `clip/create` → `clipSlotBank.createEmptyClip(slotIndex, lengthInBeats)` (params: `{ trackIndex, slotIndex, lengthInBeats }`)
- `scene/launch` → `sceneBank.launchScene(index)` (params: `{ index }`)
- `scene/getState` → returns scene names and clip counts from StateCache

ClipHandler constructor takes TrackBank + SceneBank references.

**Test criteria:**
- `./gradlew build` passes
- `curl` to `clip/launch` triggers clip playback in Bitwig (manual verification)
- `curl` to `clip/stop` stops track's clip launcher (manual verification)
- `curl` to `scene/launch` triggers scene (manual verification)
- `session/snapshot` reflects clip state changes after actions
- `api/list` includes all new clip/scene methods

**Acceptance:** All clip and scene actions callable via HTTP, state changes reflected in snapshot.

---

### Batch 2.3 — Device observers + snapshot

**Delegation:** in-session
**Decisions:** D-2.3, D-2.4
**Files:**
- `src/main/java/dev/gregross/gig/extension/GigMaestroExtension.java` (create CursorTrack, CursorDevice, RemoteControlsPage)
- `src/main/java/dev/gregross/gig/extension/StateCache.java` (add device + parameter observers + snapshot section)

**Work:**
- Create `CursorTrack` via `host.createCursorTrack("gig-cursor", "Gig Maestro", 0, 8, true)`
- Create `CursorDevice` via `cursorTrack.createCursorDevice("gig-device", "Gig Device", 0, CursorDeviceFollowMode.FOLLOW_SELECTION)`
- Create `CursorRemoteControlsPage` via `cursorDevice.createCursorRemoteControlsPage(8)`
- Add device state fields to StateCache: `deviceName`, `deviceEnabled`, `deviceIsPlugin`, `devicePosition`, `presetName`, `presetCategory`, `presetCreator`, `isWindowOpen`, `isExpanded`
- Add remote controls state: `pageIndex`, `pageCount`, `pageNames`, per-param (8): `paramName`, `paramValue`, `paramDisplayedValue`
- Add cursor track state: `cursorTrackName`
- Register observers with explicit callback casts (following Phase 1 patterns)
- Extend `getSnapshot()` with top-level `device` object containing device info + `remoteControls` with page info + `parameters` array
- Wire `registerDeviceObservers(cursorTrack, cursorDevice, remoteControlsPage)` in init()

**Test criteria:**
- `./gradlew build` compiles with zero errors
- Extension loads in Bitwig without errors or deprecation warnings
- `session/snapshot` returns `device` object with device name, enabled state, preset info
- `device.remoteControls` shows current page index, count, page names
- `device.remoteControls.parameters` shows 8 params with name, value, displayedValue
- Selecting a different device in Bitwig updates snapshot device info (FOLLOW_SELECTION)

**Acceptance:** Device and parameter state observable in snapshot — device info, page navigation state, and all 8 parameters readable.

---

### Batch 2.4 — Device + Cursor action handlers

**Delegation:** in-session
**Decisions:** D-2.5, D-2.6a
**Depends on:** Batch 2.3
**Files:**
- `src/main/java/dev/gregross/gig/handlers/DeviceHandler.java` (new)
- `src/main/java/dev/gregross/gig/extension/GigMaestroExtension.java` (register handler)

**Work:**
Register JSON-RPC action methods:
- `device/selectNext` → `cursorDevice.selectNext()` (no params)
- `device/selectPrevious` → `cursorDevice.selectPrevious()` (no params)
- `device/setEnabled` → `cursorDevice.isEnabled().set(enabled)` (params: `{ enabled }`)
- `device/selectPage` → `remoteControlsPage.selectedPageIndex().set(index)` (params: `{ index }`)
- `device/nextPage` → `remoteControlsPage.selectNextPage(false)` (no params)
- `device/previousPage` → `remoteControlsPage.selectPreviousPage(false)` (no params)
- `device/setParameterValue` → `remoteControlsPage.getParameter(index).value().setImmediately(value)` (params: `{ index, value }`)
- `cursor/selectTrack` → `cursorTrack.selectNext()` or `cursorTrack.selectPrevious()` (params: `{ direction }` — "next" or "previous")
- `cursor/getTrackState` → returns cursor track name and device name from StateCache

DeviceHandler constructor takes CursorTrack, CursorDevice, CursorRemoteControlsPage.

**Test criteria:**
- `./gradlew build` passes
- `curl` to `device/selectNext` moves to next device in chain (verified via snapshot)
- `curl` to `device/nextPage` changes parameter page (verified via snapshot)
- `curl` to `device/setParameterValue` changes a parameter value in Bitwig (manual verification)
- `curl` to `device/setEnabled` toggles device bypass (manual verification)
- `curl` to `cursor/selectTrack` moves to adjacent track (verified via snapshot)
- `api/list` includes all new device/cursor methods

**Acceptance:** All device and cursor actions callable via HTTP, mutations reflected in snapshot.

---

### Batch 2.5 — Smoke test extension

**Delegation:** in-session
**Decisions:** D-2.2, D-2.5
**Depends on:** Batch 2.2, 2.4
**Files:**
- `scripts/smoke-test.sh` (extend)

**Work:**
Extend the existing 40-test smoke suite with Phase 2 coverage:

1. **Clip snapshot:** `session/snapshot` returns `clips` array per track and `scenes` array
2. **Clip launch:** `clip/launch` on a slot with content, verify `isPlaying` in snapshot
3. **Clip stop:** `clip/stop` on a playing track, verify stopped
4. **Clip create:** `clip/create` on empty slot, verify `hasContent` in snapshot
5. **Scene launch:** `scene/launch`, verify clip playback state changes
6. **Scene getState:** `scene/getState` returns names and counts
7. **Device snapshot:** `session/snapshot` returns `device` object with remoteControls
8. **Device navigation:** `device/selectNext` + `device/selectPrevious`, verify device name changes
9. **Page navigation:** `device/nextPage` + `device/previousPage`, verify page index changes
10. **Parameter mutation:** `device/setParameterValue`, verify value update in snapshot
11. **Device enable:** `device/setEnabled`, verify enabled state toggle
12. **Cursor track:** `cursor/selectTrack`, verify cursor track name changes
13. **Error handling:** invalid trackIndex for clip actions, out-of-range param index

**Test criteria:**
- All new tests pass
- All original 40 tests still pass (regression)
- Smoke test script exits 0

**Acceptance:** Complete v0.2.x API surface validated end-to-end. Smoke test passes cleanly.

---

**Phase Acceptance Criteria:**
- [ ] `./gradlew build` compiles with zero errors
- [ ] `.bwextension` loads in Bitwig 6.0 without errors
- [ ] `session/snapshot` includes `clips` (8 per track), `scenes` (8), and `device` sections
- [ ] All clip methods (launch, stop, record, create) work via `curl`
- [ ] Scene methods (launch, getState) work via `curl`
- [ ] Device navigation (selectNext/Prev), enable, and parameter mutation work via `curl`
- [ ] Parameter page navigation (selectPage, next, prev) work via `curl`
- [ ] Cursor track navigation works via `curl`
- [ ] `device.remoteControls.parameters` shows names, values, and displayedValues
- [ ] Thread safety: no crashes or deadlocks under concurrent access
- [ ] All smoke tests pass (original 40 + new Phase 2 tests)

**Completion triggers Phase 3 → version `0.3.0`**

---

## Plan Amendments

<!-- Log any changes to the plan after creation -->

| Date | Version | Amendment | Reason |
|------|---------|-----------|--------|
