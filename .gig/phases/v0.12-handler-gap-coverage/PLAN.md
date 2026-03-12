# Plan

> Living document — reflects what was done, not just what was intended.
> This file tracks the ACTIVE phase only. Completed phases are archived to `phases/`.

---

## Active Phase — Phase 12: gig-maestro Handler Gap Coverage (v0.12.x)

> Close the BrowserHandler test gap by adding 11 behavioral tests for Phase-18 endpoints (6 filter cursor, filterReset, getFilters, getResults, 4 scrollResults directions) using mock StateCache.

**Decisions:** D-12.1, D-12.2, D-12.3

| Batch | Version | Title | Delegation | Status |
|-------|---------|-------|------------|--------|
| 12.1 | `0.12.1` | Mock StateCache + filter cursor behavioral tests | in-session | done |
| 12.2 | `0.12.2` | filterReset + getFilters + getResults + scrollResults tests | in-session | done |
| 12.3 | `0.12.3` | Verify full build | in-session | done |

### Batch 12.1 — Mock StateCache + filter cursor behavioral tests

**Delegation:** in-session
**Decisions:** D-12.2, D-12.3
**Files:**
- `gig-maestro/src/test/java/dev/gregross/gig/handlers/BrowserHandlerTest.java` (modify)
**Work:**
- Add `@Mock private StateCache mockStateCache` field
- Add `@Mock private CursorBrowserFilterItem mockFilterCursor` field
- Update `@BeforeEach` to pass `mockStateCache` instead of `new StateCache()`
- Stub `mockStateCache.getFilterCursors()` to return array with `mockFilterCursor` at index 0 (category)
- Add 6 behavioral tests:
  - `filterSelectNext_callsCursorSelectNext` — column "category" → `cursors[0].selectNext()`
  - `filterSelectPrevious_callsCursorSelectPrevious`
  - `filterSelectFirst_callsCursorSelectFirst`
  - `filterSelectLast_callsCursorSelectLast`
  - `filterSelectParent_callsCursorSelectParent`
  - `filterSelectFirstChild_callsCursorSelectFirstChild`
- Verify existing 18 tests still pass (they don't depend on StateCache for Phase-17 endpoints)
**Test criteria:** `./gradlew :gig-maestro:test` passes
**Acceptance:** All 6 filter cursor endpoints verified to call correct CursorBrowserFilterItem method

### Batch 12.2 — filterReset + getFilters + getResults + scrollResults tests

**Delegation:** in-session
**Decisions:** D-12.2, D-12.3
**Depends on:** Batch 12.1
**Files:**
- `gig-maestro/src/test/java/dev/gregross/gig/handlers/BrowserHandlerTest.java` (modify)
**Work:**
- Add chain mocks: `@Mock BrowserFilterColumn mockFilterColumn`, `@Mock CursorBrowserFilterItem mockWildcard`, `@Mock SettableBooleanValue mockWildcardSelected`, `@Mock BrowserResultsItemBank mockResultBank`
- Stub `mockStateCache.getFilterColumns()` → array with `mockFilterColumn` at index 0
- Stub `mockFilterColumn.getWildcardItem()` → `mockWildcard`, `mockWildcard.isSelected()` → `mockWildcardSelected`
- Stub `mockStateCache.getResultBank()` → `mockResultBank`
- Stub `mockStateCache.getBrowserState()` → JsonObject with "filters" key
- Stub `mockStateCache.getResultBankState()` → JsonObject
- Add 5 behavioral tests:
  - `filterReset_setsWildcardSelectedTrue` — column "category" → `columns[0].getWildcardItem().isSelected().set(true)`
  - `getFilters_returnsBrowserStateFilters` — returns the "filters" sub-object from `getBrowserState()`
  - `getResults_returnsResultBankState` — returns `getResultBankState()` result
  - `scrollResults_forward_callsBankScrollForwards` — direction "forward" → `bank.scrollForwards()`
  - `scrollResults_pageBackward_callsBankScrollPageBackwards` — direction "pageBackward" → `bank.scrollPageBackwards()`
**Test criteria:** `./gradlew :gig-maestro:test` passes
**Acceptance:** filterReset wildcard chain verified; getFilters/getResults return correct data; scrollResults dispatches to correct bank method

### Batch 12.3 — Verify full build

**Delegation:** in-session
**Decisions:** —
**Depends on:** Batches 12.1–12.2
**Files:** —
**Work:** Run `./gradlew clean build` to verify all modules compile and all tests pass.
**Test criteria:** Exit code 0, BUILD SUCCESSFUL
**Acceptance:** All tests green across gig-maestro and launchpad-mk2

**Phase Acceptance Criteria:**
- [ ] BrowserHandler coverage ratio increases from 0.86x to 1.38x (29 tests for 21 endpoints)
- [ ] All 6 filter cursor endpoints have behavioral tests
- [ ] filterReset wildcard chain fully verified
- [ ] scrollResults dispatches correctly for all 4 directions (2 tested + 2 validated by symmetry)
- [ ] `./gradlew clean build` passes with all tests green

**Completion triggers governance → version `0.12.0`**

<!-- ARCHIVED: Phase 11 — gig-maestro: CLI Test Coverage (v0.11.x)

**Decisions:** D-11.1, D-11.2, D-11.3, D-11.4, D-11.5

| Batch | Version | Title | Delegation | Status |
|-------|---------|-------|------------|--------|
| 11.1 | `0.11.1` | FakeRpcClient + TestableGigCli + RpcClientFormatTest | in-session | done |
| 11.2 | `0.11.2` | Rename SongCommandTest → CliCommandStructureTest + expand | in-session | done |
| 11.3 | `0.11.3` | TransportCommandTest + TrackCommandTest | in-session | done |
| 11.4 | `0.11.4` | DeviceNoteCommandTest | in-session | done |
| 11.5 | `0.11.5` | Verify full build | in-session | done |

### Batch 11.1 — FakeRpcClient + TestableGigCli + RpcClientFormatTest

**Delegation:** in-session
**Decisions:** D-11.1, D-11.3
**Files:**
- `gig-maestro/src/test/java/dev/gregross/gig/cli/FakeRpcClient.java` (create)
- `gig-maestro/src/test/java/dev/gregross/gig/cli/TestableGigCli.java` (create)
- `gig-maestro/src/test/java/dev/gregross/gig/cli/RpcClientFormatTest.java` (create)
**Work:**
- Create `FakeRpcClient` extending `RpcClient` — overrides `call()` to record (method, params) and return configurable result, overrides `callRaw()` similarly. Provides `getLastMethod()`, `getLastParams()`, `getCalls()`, `reset()`.
- Create `TestableGigCli` extending `GigCli` — overrides `createClient()` to return a shared `FakeRpcClient` instance. Provides `getFakeClient()`.
- Create `RpcClientFormatTest` — ~6 tests:
  - `format_nonPretty_compactJson`
  - `format_pretty_indentedJson`
  - `formatRaw_nonPretty_returnsUnchanged`
  - `formatRaw_pretty_indentsJson`
  - `format_nullElement`
  - `format_nestedObject`
**Test criteria:** `./gradlew :gig-maestro:test` passes
**Acceptance:** FakeRpcClient compiles and records calls; format tests pass

### Batch 11.2 — Rename SongCommandTest → CliCommandStructureTest + expand

**Delegation:** in-session
**Decisions:** D-11.5
**Depends on:** Batch 11.1
**Files:**
- `gig-maestro/src/test/java/dev/gregross/gig/cli/SongCommandTest.java` (rename → CliCommandStructureTest.java)
- `gig-maestro/src/test/java/dev/gregross/gig/cli/CliCommandStructureTest.java` (modify)
**Work:**
- Rename file and class from `SongCommandTest` to `CliCommandStructureTest`
- Keep all 7 existing tests
- Add tests for each top-level command's subcommand count:
  - `transportHas11Subcommands`
  - `trackHas12Subcommands`
  - `deviceHas4Subcommands`
  - `noteHas8Subcommands`
  - `snapshotIsRunnable` (no subcommands, implements Runnable)
  - `rpcRequiresRequestArgument`
- Add help text tests for key parameterized commands:
  - `transportTempoHelpShowsBpmParam`
  - `trackSetVolumeHelpShowsIndexAndValue`
**Test criteria:** `./gradlew :gig-maestro:test` passes
**Acceptance:** All structure tests pass; existing tests preserved; new subcommand counts validated

### Batch 11.3 — TransportCommandTest + TrackCommandTest

**Delegation:** in-session
**Decisions:** D-11.1, D-11.2, D-11.3
**Depends on:** Batch 11.1
**Files:**
- `gig-maestro/src/test/java/dev/gregross/gig/cli/TransportCommandTest.java` (create)
- `gig-maestro/src/test/java/dev/gregross/gig/cli/TrackCommandTest.java` (create)
**Work:**
- Create `TransportCommandTest` using TestableGigCli — ~11 tests verifying RPC method+params for each subcommand:
  - `play_callsTransportPlay`, `stop_callsTransportStop`, `record_callsTransportRecord`
  - `toggle_callsTransportTogglePlay`, `rewind_callsTransportRewind`, `ff_callsTransportFastForward`
  - `tapTempo_callsTransportTapTempo`
  - `tempo_callsSetTempoWithBpm` — verify params contain `{"tempo": 120.0}`
  - `position_callsSetPositionWithBeats` — verify params contain `{"beats": 8.0}`
  - `loop_on_callsSetLoopEnabled` — verify params contain `{"enabled": true}`
  - `metronome_off_callsSetMetronomeDisabled` — verify params contain `{"enabled": false}`
- Create `TrackCommandTest` using TestableGigCli — ~12 tests:
  - `setVolume_callsWithIndexAndValue`, `setPan_callsWithIndexAndValue`
  - `setMute_on_callsWithEnabled`, `setSolo_off_callsWithDisabled`
  - `setArm_on_callsWithEnabled`
  - `createAudio_callsWithPosition`, `createInstrument_callsWithPosition`, `createEffect_callsWithPosition`
  - `select_callsWithIndex`, `rename_callsWithName`
  - `deleteSelected_callsCorrectMethod`, `duplicate_callsCorrectMethod`
**Test criteria:** `./gradlew :gig-maestro:test` passes
**Acceptance:** Every transport/track subcommand verified to produce correct RPC method + params

### Batch 11.4 — DeviceNoteCommandTest

**Delegation:** in-session
**Decisions:** D-11.1, D-11.2, D-11.3
**Depends on:** Batch 11.1
**Files:**
- `gig-maestro/src/test/java/dev/gregross/gig/cli/DeviceNoteCommandTest.java` (create)
**Work:**
- Create `DeviceNoteCommandTest` — ~12 tests:
  - Device: `insertBitwig_callsWithNameAndPosition`, `insertPlugin_callsWithTypeAndId`, `listBitwig_callsCorrectMethod`, `remove_callsCorrectMethod`
  - Note: `select_callsWithTrackAndSlot`, `delete_callsWithTrackAndSlot`, `setNotes_callsWithNotesJson`, `clearNote_callsWithStepAndNote`, `clearAll_callsCorrectMethod`, `getNotes_callsCorrectMethod`, `setStepSize_callsWithSize`, `scrollSteps_callsWithOffset`
**Test criteria:** `./gradlew :gig-maestro:test` passes
**Acceptance:** All device and note subcommands verified

### Batch 11.5 — Verify full build

**Delegation:** in-session
**Decisions:** —
**Depends on:** Batches 11.1–11.4
**Files:** —
**Work:** Run `./gradlew clean build` to verify all modules compile and all tests pass.
**Test criteria:** Exit code 0, BUILD SUCCESSFUL
**Acceptance:** All tests green across gig-maestro and launchpad-mk2

**Phase Acceptance Criteria:**
- [ ] FakeRpcClient + TestableGigCli provide testable CLI execution without HTTP
- [ ] RpcClient.format/formatRaw covered by pure-function tests
- [ ] CliCommandStructureTest validates all 7 top-level commands and subcommand counts
- [ ] TransportCommandTest covers all 11 transport subcommands
- [ ] TrackCommandTest covers all 12 track subcommands
- [ ] DeviceNoteCommandTest covers all 4 device + 8 note subcommands
- [ ] `./gradlew clean build` passes with all tests green

**Completion triggers Phase 12 → version `0.12.0`**
-->

<!-- ARCHIVED: Phase 10 — launchpad-mk2: Extension Behavioral Tests (v0.10.x)

> Add comprehensive behavioral tests for `LaunchpadMk2Extension` covering LED flush rendering (grid, scene launch, top row), MIDI input handling (pad press, CC navigation, modal utility actions), and init wiring verification. Uses real constructor with mock ControllerHost and a shared 8×8 mock grid setup.

**Decisions:** D-10.1, D-10.2, D-10.3, D-10.4, D-10.5

| Batch | Version | Title | Delegation | Status |
|-------|---------|-------|------------|--------|
| 10.1 | `0.10.1` | Shared mock setup + LaunchpadMk2FlushTest | in-session | done |
| 10.2 | `0.10.2` | LaunchpadMk2MidiInputTest | in-session | done |
| 10.3 | `0.10.3` | LaunchpadMk2InitTest | in-session | done |
| 10.4 | `0.10.4` | Verify full build | in-session | done |

### Batch 10.1 — Shared mock setup + LaunchpadMk2FlushTest

**Delegation:** in-session
**Decisions:** D-10.1, D-10.2, D-10.4, D-10.5
**Files:**
- `launchpad-mk2/src/test/java/com/gregross/bitwig/launchpadmk2/LaunchpadMk2ExtensionTestBase.java` (create)
- `launchpad-mk2/src/test/java/com/gregross/bitwig/launchpadmk2/LaunchpadMk2FlushTest.java` (create)
**Work:**
- Create `LaunchpadMk2ExtensionTestBase` — abstract base with `@BeforeEach` that:
  - Mocks ControllerHost, MidiIn, MidiOut, TrackBank, 8 Tracks (each with ClipLauncherSlotBank + 8 ClipLauncherSlots), SceneBank + 8 Scenes, CursorTrack, Transport, Application
  - Each slot gets mock BooleanValue for hasContent/isPlaying/isRecording/isPlaybackQueued/isStopQueued/isRecordingQueued + mock ColorValue
  - Each track gets mock BooleanValue for arm + mock ColorValue
  - Scenes get mock ColorValue
  - Transport gets mock BooleanValue for isPlaying/isArrangerRecordEnabled
  - CursorTrack gets mock BooleanValue for hasPrevious/hasNext/arm/solo/mute
  - SceneBank gets mock BooleanValue for canScrollBackwards/canScrollForwards
  - Stubs markInterested() and addValueObserver() as no-ops
  - Calls `extension.init()` after all stubs configured
  - Provides helper: `setSlotState(track, scene, hasContent, isPlaying, isRecording, isQueued)` etc.
- Create `LaunchpadMk2FlushTest` extending base — ~25 tests:
  - `flushGrid_emptySlot_sendsOff` — velocity 0 via sendMidi(0x90, note, 0)
  - `flushGrid_armedEmptySlot_sendsTrackArmed` — velocity 11
  - `flushGrid_hasContent_sendsRgbSysEx` — packRgb of slot color
  - `flushGrid_playing_sendsPulseRed` — sendMidi(0x92, note, 5)
  - `flushGrid_recording_sendsVelocityRed` — sendMidi(0x90, note, 5)
  - `flushGrid_queued_sendsPulseAmber` — sendMidi(0x92, note, 61)
  - `flushGrid_stopQueued_sendsPulseAmber`
  - `flushGrid_recordingQueued_sendsRecording`
  - `flushGrid_noteMapping_invertedRowCol` — verify correct note for track 0, scene 0
  - `flushGrid_cachedState_noResend` — call flush twice with same state, second sends nothing
  - `flushSceneLaunch_idle_sendsRgbSysEx`
  - `flushSceneLaunch_allPlaying_sendsPulseRed`
  - `flushSceneLaunch_anyPlaying_sendsPulseOrange`
  - `flushSceneLaunch_anyQueued_sendsPulseAmber`
  - `flushSceneLaunch_noContent_allPlayingFalse`
  - `flushTopRow_globalMode_playingGreenPulse`
  - `flushTopRow_globalMode_stoppedDimGreen`
  - `flushTopRow_globalMode_recordingRedPulse`
  - `flushTopRow_globalMode_notRecordingDimRed`
  - `flushTopRow_trackMode_armedRedPulse`
  - `flushTopRow_trackMode_soloYellowPulse`
  - `flushTopRow_trackMode_mutedOrangePulse`
  - `flushTopRow_utilityMode_staticColors`
  - `flushTopRow_navSceneActive`
  - `flushTopRow_navInactive`
  - `flush_notDirty_skipsAll`
**Test criteria:** `./gradlew :launchpad-mk2:test` passes, all flush tests green
**Acceptance:** All 6 clip states produce correct MIDI output; scene launch logic covers all 4 branches; top row covers all 3 modes

### Batch 10.2 — LaunchpadMk2MidiInputTest

**Delegation:** in-session
**Decisions:** D-10.1, D-10.3
**Depends on:** Batch 10.1 (uses test base)
**Files:**
- `launchpad-mk2/src/test/java/com/gregross/bitwig/launchpadmk2/LaunchpadMk2MidiInputTest.java` (create)
**Work:**
- Create `LaunchpadMk2MidiInputTest` extending base — ~15 tests:
  - `gridPad_stoppedClip_launches` — note on triggers slot.launch()
  - `gridPad_playingClip_stops` — note on triggers track.stop()
  - `gridPad_invertedMapping_correctTrackScene` — verify CCW rotation math
  - `gridPad_outOfBounds_ignored` — invalid note does nothing
  - `gridPad_noteOff_ignored` — data2 == 0 does nothing
  - `sceneLaunch_notAllPlaying_launchesScene` — col 8 press launches scene
  - `sceneLaunch_allPlaying_stopsAllTracks` — col 8 press stops all
  - `sceneLaunch_noContent_launchesScene` — empty scene still launches
  - `cc_up_scrollsBackwards`
  - `cc_down_scrollsForwards`
  - `cc_left_selectsNext` — CCW rotation
  - `cc_right_selectsPrevious` — CCW rotation
  - `cc_session_cyclesUtilityMode` — cycles 0→1→2→0
  - `cc_user1_globalMode_togglesRecord`
  - `cc_user1_trackMode_togglesArm`
  - `cc_user1_utilityMode_capturesScene`
  - `cc_user2_globalMode_stopsTransport`
  - `cc_user2_trackMode_togglesSolo`
  - `cc_user2_utilityMode_undoes`
  - `cc_mixer_globalMode_playsTransport`
  - `cc_mixer_trackMode_togglesMute`
  - `cc_mixer_utilityMode_redoes`
**Test criteria:** `./gradlew :launchpad-mk2:test` passes, all input tests green
**Acceptance:** Grid pad launch/stop toggle verified; scene launch all-playing logic correct; all 9 modal actions (3×3) verified; navigation CCs map correctly

### Batch 10.3 — LaunchpadMk2InitTest

**Delegation:** in-session
**Decisions:** D-10.1, D-10.2, D-10.3
**Depends on:** Batch 10.1 (uses test base)
**Files:**
- `launchpad-mk2/src/test/java/com/gregross/bitwig/launchpadmk2/LaunchpadMk2InitTest.java` (create)
**Work:**
- Create `LaunchpadMk2InitTest` extending base — ~5 tests:
  - `init_sendsSessionLayoutSysEx` — verify midiOut.sendSysex called with session layout
  - `init_sendsResetLedsSysEx` — verify reset LEDs SysEx sent
  - `init_setsMidiCallback` — verify midiIn.setMidiCallback called
  - `init_createsTrackBankAndSceneBank` — verify host factory method calls
  - `init_marksDirty` — flush after init should send LED data (not skip)
**Test criteria:** `./gradlew :launchpad-mk2:test` passes, all init tests green
**Acceptance:** Init SysEx messages verified; MIDI callback registered; track bank created with correct params

### Batch 10.4 — Verify full build

**Delegation:** in-session
**Decisions:** —
**Depends on:** Batches 10.1, 10.2, 10.3
**Files:** —
**Work:** Run `./gradlew clean build` to verify all modules compile and all tests pass across the entire project.
**Test criteria:** Exit code 0, BUILD SUCCESSFUL
**Acceptance:** All tests green across gig-maestro and launchpad-mk2

**Phase Acceptance Criteria:**
- [ ] LaunchpadMk2ExtensionTestBase provides reusable 8×8 mock grid
- [ ] FlushTest covers all 6 clip LED states, 4 scene launch branches, 3 utility modes
- [ ] MidiInputTest covers grid launch/stop, scene launch, all CC actions
- [ ] InitTest verifies SysEx startup and wiring
- [ ] `./gradlew clean build` passes with all tests green

**Completion triggers Phase 11 → version `0.11.0`**
-->

---

## Plan Amendments

| Date | Version | Amendment | Reason |
|------|---------|-----------|--------|
| — | — | — | — |
