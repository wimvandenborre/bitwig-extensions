# Plan

> Living document — reflects what was done, not just what was intended.
> This file tracks the ACTIVE phase only. Completed phases are archived to `phases/`.

---

## Active Phase

### Phase 6 — gig-maestro: Expand Mockito Coverage (v0.6.x)

> Add Mockito behavioral tests to the remaining 11 direct-API handlers, verifying every method that calls Bitwig API objects. Follows the same @ExtendWith + @Mock + arrange-act-verify pattern established in Phase 5. StateCache-only methods are skipped (already covered by validation tests).

**Decisions:** D-6.1, D-6.2, D-6.3, D-6.4

| Batch | Version | Title | Delegation | Status |
|-------|---------|-------|------------|--------|
| 6.1 | `0.6.1` | ApplicationHandler + SendHandler + NoteInputHandler | in-session | done |
| 6.2 | `0.6.2` | TransportHandler | in-session | done |
| 6.3 | `0.6.3` | ArrangerHandler + SceneHandler | in-session | done |
| 6.4 | `0.6.4` | ClipHandler + NoteHandler | in-session | done |
| 6.5 | `0.6.5` | DeviceHandler + MasterDeviceHandler | in-session | done |
| 6.6 | `0.6.6` | BrowserHandler | in-session | done |
| 6.7 | `0.6.7` | Verify full build | in-session | done |

### Batch 6.1 — ApplicationHandler + SendHandler + NoteInputHandler

**Delegation:** team (independent of 6.2–6.6)
**Decisions:** D-6.1, D-6.2, D-6.3
**Files:**
- Edit `gig-maestro/src/test/java/dev/gregross/gig/handlers/ApplicationHandlerTest.java`
- Edit `gig-maestro/src/test/java/dev/gregross/gig/handlers/SendHandlerTest.java`
- Edit `gig-maestro/src/test/java/dev/gregross/gig/handlers/NoteInputHandlerTest.java`

**Work:**
1. **ApplicationHandlerTest** — convert to `@ExtendWith(MockitoExtension.class)`, add `@Mock Application mockApplication` + `@Mock ControllerHost mockHost`. Add 6 behavioral tests:
   - `undo_callsApplicationUndo` — verify `mockApplication.undo()`
   - `redo_callsApplicationRedo` — verify `mockApplication.redo()`
   - `activateEngine_callsApplicationActivateEngine`
   - `deactivateEngine_callsApplicationDeactivateEngine`
   - `showNotification_callsHostShowPopupNotification` — verify `mockHost.showPopupNotification("text")`
   - `setPanelLayout_callsApplicationSetPanelLayout` — verify `mockApplication.setPanelLayout("ARRANGE")`
   - Skip `app/getState` (reads from Application mock properties — low value read-only aggregation).

2. **SendHandlerTest** — convert to `@ExtendWith(MockitoExtension.class)`, add `@Mock TrackBank mockTrackBank` + chain mocks for Track → SendBank → Send → value/sendMode/isEnabled. Add 3 behavioral tests:
   - `setLevel_callsSendValueSetImmediately` — 4-level chain: `trackBank.getItemAt(0).sendBank().getItemAt(0).value().setImmediately(0.8)`
   - `setMode_callsSendModeSet` — 3-level chain: `trackBank.getItemAt(0).sendBank().getItemAt(0).sendMode().set("PRE")`
   - `setEnabled_callsSendIsEnabledSet` — 3-level chain: `trackBank.getItemAt(0).sendBank().getItemAt(0).isEnabled().set(true)`

3. **NoteInputHandlerTest** — convert to `@ExtendWith(MockitoExtension.class)`, add `@Mock NoteInput mockNoteInput` + `@Mock Arpeggiator mockArpeggiator` + `@Mock NoteLatch mockNoteLatch` + chain mocks. Add 8 behavioral tests:
   - `sendNote_callsNoteInputSendRawMidiEvent` — verify `mockNoteInput.sendRawMidiEvent(0x90, 60, 100)`
   - `sendMidi_callsNoteInputSendRawMidiEvent`
   - `arpeggiatorConfigure_callsArpeggiatorSetters` — verify mode/rate/etc. `.set()` calls
   - `arpeggiatorSetEnabled_callsArpeggiatorIsEnabledSet`
   - `arpeggiatorReleaseNotes_callsArpeggiatorReleaseNotes`
   - `noteLatchConfigure_callsNoteLatchSetters` — verify mode/mono/velocityThreshold `.set()` calls
   - `noteLatchSetEnabled_callsNoteLatchIsEnabledSet`
   - `noteLatchReleaseNotes_callsNoteLatchReleaseNotes`

**Test criteria:** `./gradlew :gig-maestro:test` passes; 17 new behavioral tests added.
**Acceptance:** All 3 handler test files have Mockito behavioral coverage.

### Batch 6.2 — TransportHandler

**Delegation:** team (independent of 6.1, 6.3–6.6)
**Decisions:** D-6.1, D-6.2, D-6.3
**Files:**
- Edit `gig-maestro/src/test/java/dev/gregross/gig/handlers/TransportHandlerTest.java`

**Work:**
1. Convert to `@ExtendWith(MockitoExtension.class)`, add `@Mock Transport mockTransport` + chain mocks for tempo, position, loop, metronome, automation, etc.
2. Add 30 behavioral tests:
   - **1-level calls (12):** play, stop, record, togglePlay, rewind, fastForward, tapTempo, resetAutomationOverrides, continuePlayback, restart, returnToArrangement, jumpToPreviousCueMarker, jumpToNextCueMarker
   - **2-level chains (14):** setPosition, setLoop, setMetronome, setPunchIn (2 verifies), setPunchOut (2 verifies), setAutomationWriteMode, setArrangerAutomationWrite, setClipLauncherAutomationWrite, setPreRoll, setMetronomeVolume, setDefaultLaunchQuantization, setPostRecordingAction, setPostRecordingTimeOffset, setClipLauncherOverdub, setFillMode
   - **3-level chains (2):** setTempo (`transport.tempo().value().setRaw(120.0)`), setLoopRange (multiple 2-level calls)
   - Skip `getLoopRange` and `getClipLauncherSettings` (StateCache-only reads).

**Test criteria:** `./gradlew :gig-maestro:test` passes; 30 new behavioral tests added.
**Acceptance:** TransportHandler has complete Bitwig API call verification.

### Batch 6.3 — ArrangerHandler + SceneHandler

**Delegation:** team (independent of 6.1–6.2, 6.4–6.6)
**Decisions:** D-6.1, D-6.2, D-6.3
**Files:**
- Edit `gig-maestro/src/test/java/dev/gregross/gig/handlers/ArrangerHandlerTest.java`
- Edit `gig-maestro/src/test/java/dev/gregross/gig/handlers/SceneHandlerTest.java`

**Work:**
1. **ArrangerHandlerTest** — convert to `@ExtendWith(MockitoExtension.class)`, add `@Mock Arranger mockArranger` + `@Mock Transport mockTransport` + `@Mock CueMarkerBank mockCueMarkerBank` + chain mocks. Add 15 behavioral tests:
   - **Arranger setters (7):** setPlaybackFollow, setClipLauncherVisible, setTimelineVisible, setCueMarkersVisible, setEffectTracksVisible, setIoSectionVisible, setDoubleRowTrackHeight — all 2-level chains (`arranger.isX().set()`)
   - **CueMarker operations (6):** addAtPlayhead (1-level via transport), launch, rename, setPosition, duplicate, delete — 2-level chains via `cueMarkerBank.getItemAt(index)`
   - **CueMarkerBank scroll (2):** scrollTo (2-level), scrollBy (1-level)
   - Skip `cueMarker/list` and `cueMarkerBank/getScrollInfo` (StateCache-only reads).

2. **SceneHandlerTest** — convert to `@ExtendWith(MockitoExtension.class)`, add `@Mock SceneBank mockSceneBank` + `@Mock Project mockProject` + chain mocks. Add 11 behavioral tests:
   - **Project calls (2):** create, createFromPlaying — 1-level
   - **Scene operations (7):** duplicate, rename, delete, setColor, launchAlt, launchRelease, launchReleaseAlt — 2-level via `sceneBank.getScene(index)`
   - **SceneBank scroll (2):** scrollTo (2-level), scrollBy (1-level)
   - Skip `sceneBank/getScrollInfo` (StateCache-only read).

**Test criteria:** `./gradlew :gig-maestro:test` passes; 26 new behavioral tests added.
**Acceptance:** ArrangerHandler and SceneHandler have complete Bitwig API call verification.

### Batch 6.4 — ClipHandler + NoteHandler

**Delegation:** team (independent of 6.1–6.3, 6.5–6.6)
**Decisions:** D-6.1, D-6.2, D-6.3
**Files:**
- Edit `gig-maestro/src/test/java/dev/gregross/gig/handlers/ClipHandlerTest.java`
- Edit `gig-maestro/src/test/java/dev/gregross/gig/handlers/NoteHandlerTest.java`

**Work:**
1. **ClipHandlerTest** — convert to `@ExtendWith(MockitoExtension.class)`, add `@Mock TrackBank mockTrackBank` + `@Mock SceneBank mockSceneBank` + `@Mock Clip mockCursorClip` + chain mocks for Track → ClipLauncherSlotBank → ClipLauncherSlot. Add 28 behavioral tests:
   - **ClipLauncherSlotBank operations (5):** launch (simple), stop, record, create, duplicate — via `track.clipLauncherSlotBank()`
   - **Slot operations (7):** launch (with options), select, delete, setColor, showInEditor, launchAlt, launchRelease, launchReleaseAlt — via `slotBank.getItemAt()`
   - **Scene launch (1):** scene/launch (simple) — via `sceneBank.launchScene(index)`
   - **CursorClip operations (6):** rename, quantize, transpose, duplicateContent — 1-level
   - **CursorClip 2-level chains (8):** setLaunchQuantization, setLaunchMode, setShuffle, setAccent, setPlayStart, setPlayStop, setLoopStart, setLoopLength, setLoopEnabled, setUseLoopStartAsQuantizationReference
   - **Complex (1):** duplicateToSlot — 3-level chain via replaceInsertionPoint().copySlotsOrScenes()
   - Skip `getLaunchSettings` and `getPlaybackSettings` (StateCache-only reads).

2. **NoteHandlerTest** — convert to `@ExtendWith(MockitoExtension.class)`, add `@Mock Clip mockCursorClip` + chain mocks for NoteStep. Add 10 behavioral tests:
   - **1-level calls (3):** setNotes (verify setStep), clearNote (verify clearStep), setStepSize, scrollSteps
   - **clearAllNotes (1):** verify setStepSize + scrollToStep + clearSteps sequence
   - **NoteStep operations (5):** setChance, setNoteExpressions, setNoteRepeat, setNoteOccurrence, setNoteRecurrence — 2-level via `cursorClip.getStep()`
   - Skip `getNotes` (read-only grid iteration).

**Test criteria:** `./gradlew :gig-maestro:test` passes; 38 new behavioral tests added.
**Acceptance:** ClipHandler and NoteHandler have complete Bitwig API call verification.

### Batch 6.5 — DeviceHandler + MasterDeviceHandler

**Delegation:** team (independent of 6.1–6.4, 6.6)
**Decisions:** D-6.1, D-6.2, D-6.3
**Files:**
- Edit `gig-maestro/src/test/java/dev/gregross/gig/handlers/DeviceHandlerTest.java`
- Edit `gig-maestro/src/test/java/dev/gregross/gig/handlers/MasterDeviceHandlerTest.java`

**Work:**
1. **DeviceHandlerTest** — convert to `@ExtendWith(MockitoExtension.class)`, add `@Mock CursorTrack mockCursorTrack` + `@Mock CursorDevice mockCursorDevice` + `@Mock CursorRemoteControlsPage mockRemoteControlsPage` + `@Mock DrumPadBank mockDrumPadBank` + `@Mock DeviceLibrary mockDeviceLibrary` + `@Mock Transport mockTransport` + `@Mock ControllerHost mockHost` + chain mocks. Add 21 behavioral tests:
   - **CursorDevice 1-level (6):** selectNext, selectPrevious, remove, enterSlot, exitToParent, enterKeyPad
   - **CursorDevice 2-level (2):** setEnabled, enterLayer
   - **RemoteControlsPage 1-level (3):** nextPage, previousPage, selectPageByTag
   - **RemoteControlsPage 2-level (4):** selectPage, deleteAllAutomation, restoreAutomationControl, touch
   - **RemoteControlsPage 3-level (1):** setParameterValue
   - **Device insertion (2):** insertBitwigDevice, insertPluginDevice
   - **CursorTrack (2):** cursor/selectTrack (next + previous)
   - **Complex (1):** writeEnvelope (simplified verification of key API interactions)
   - Skip `listBitwigDevices`, `hasAutomation`, `getDrumPads` (read-only).

2. **MasterDeviceHandlerTest** — convert to `@ExtendWith(MockitoExtension.class)`, add `@Mock MasterTrack mockMasterTrack` + `@Mock CursorDevice mockCursorDevice` + `@Mock CursorRemoteControlsPage mockRemoteControlsPage` + `@Mock DeviceLibrary mockDeviceLibrary` + chain mocks. Add 15 behavioral tests:
   - **CursorDevice 1-level (6):** selectNext, selectPrevious, remove, enterSlot, exitToParent, enterKeyPad
   - **CursorDevice 2-level (2):** setEnabled, enterLayer
   - **RemoteControlsPage 1-level (3):** nextPage, previousPage, selectPageByTag
   - **RemoteControlsPage 2-level (1):** selectPage
   - **RemoteControlsPage 3-level (1):** setParameterValue
   - **Device insertion (2):** insertBitwigDevice, insertPluginDevice

**Test criteria:** `./gradlew :gig-maestro:test` passes; 36 new behavioral tests added.
**Acceptance:** DeviceHandler and MasterDeviceHandler have complete Bitwig API call verification.

### Batch 6.6 — BrowserHandler

**Delegation:** team (independent of 6.1–6.5)
**Decisions:** D-6.1, D-6.2, D-6.3
**Files:**
- Edit `gig-maestro/src/test/java/dev/gregross/gig/handlers/BrowserHandlerTest.java`

**Work:**
1. Convert to `@ExtendWith(MockitoExtension.class)`, add `@Mock PopupBrowser mockPopupBrowser` + `@Mock CursorDevice mockCursorDevice` + chain mocks for InsertionPoint, BrowserFilterCursor, BrowserFilterColumn, etc.
2. Add 17 behavioral tests:
   - **Browser file navigation (6):** selectNextFile, selectPreviousFile, selectFirstFile, selectLastFile — 1-level
   - **Browser actions (2):** commit, cancel — 1-level
   - **Browser config (2):** setContentType, setShouldAudition — 2-level
   - **Browse initiation (2):** browsePresets, browseInsertDevice — 2-level via cursorDevice insertion points
   - **Filter navigation (6):** filterSelectNext, filterSelectPrevious, filterSelectFirst, filterSelectLast, filterSelectParent, filterSelectFirstChild — via StateCache filter cursors
   - **Filter reset (1):** filterReset — 3-level via StateCache filter columns
   - **Scroll results (1):** scrollResults — via StateCache result bank
   - Skip `getState`, `getFilters`, `getResults` (StateCache-only reads).
   - Note: filter/scroll tests need StateCache mock or real StateCache with pre-configured cursors — determine approach during implementation.

**Test criteria:** `./gradlew :gig-maestro:test` passes; 17 new behavioral tests added.
**Acceptance:** BrowserHandler has complete Bitwig API call verification.

### Batch 6.7 — Verify full build

**Delegation:** in-session (depends on 6.1–6.6)
**Decisions:** D-6.1
**Files:** None (verification only)

**Work:**
1. Run `./gradlew clean build` — full build.
2. Count total test methods and confirm increase from 367.
3. Verify all 11 handler test files have Mockito behavioral tests.

**Test criteria:** Clean build succeeds; all tests pass; test count ≥ 531 (367 + 164).
**Acceptance:** Full Mockito behavioral coverage for all direct-API handlers.

**Phase Acceptance Criteria:**
- [ ] ApplicationHandlerTest has 6 new behavioral tests
- [ ] TransportHandlerTest has 30 new behavioral tests
- [ ] ArrangerHandlerTest has 15 new behavioral tests
- [ ] SceneHandlerTest has 11 new behavioral tests
- [ ] ClipHandlerTest has 28 new behavioral tests
- [ ] NoteHandlerTest has 10 new behavioral tests
- [ ] DeviceHandlerTest has 21 new behavioral tests
- [ ] MasterDeviceHandlerTest has 15 new behavioral tests
- [ ] BrowserHandlerTest has 17 new behavioral tests
- [ ] NoteInputHandlerTest has 8 new behavioral tests
- [ ] SendHandlerTest has 3 new behavioral tests
- [ ] All existing validation tests still pass unchanged
- [ ] `./gradlew clean build` succeeds
- [ ] Total test count ≥ 531

**Completion triggers Phase 7 → version `0.7.0`**

---

## Plan Amendments

| Date | Version | Amendment | Reason |
|------|---------|-----------|--------|
| — | — | — | — |
