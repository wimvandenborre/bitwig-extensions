# State

## Current State

| Field | Value |
|-------|-------|
| **Version** | `0.7.0` |
| **Phase** | 6 — Track Management |
| **Status** | `VERIFIED` |
| **Last Batch** | Phase 6 archived |
| **Last Updated** | 2026-02-28 |

---

## Batch History

<!-- Newest first. Type: PLANNED or UNPLANNED -->

| Version | Phase | Batch Title | Type | Status | Timestamp |
|---------|-------|-------------|------|--------|-----------|
| 0.6.4 | 6 | CLI TrackManageCommand + smoke tests | PLANNED | done | 2026-02-28 |
| 0.6.3 | 6 | Tool schemas + system prompt update | PLANNED | done | 2026-02-28 |
| 0.6.2 | 6 | Track management RPC handlers | PLANNED | done | 2026-02-28 |
| 0.6.1 | 6 | TrackBankManager utility + pure tests | PLANNED | done | 2026-02-28 |
| 0.5.5 | 5 | CLI DeviceCommand + smoke tests | PLANNED | done | 2026-02-28 |
| 0.5.4 | 5 | Tool schemas + system prompt update | PLANNED | done | 2026-02-28 |
| 0.5.3 | 5 | Device insertion + removal RPC handlers | PLANNED | done | 2026-02-28 |
| 0.5.2 | 5 | DeviceLibrary utility + unit tests | PLANNED | done | 2026-02-28 |
| 0.5.1 | 5 | Spike: Validate insertFile() mechanism | PLANNED | done | 2026-02-28 |
| 0.4.6 | 4 | Add clip/delete RPC method | UNPLANNED | done | 2026-02-28 |
| 0.4.5 | 4 | CLI NoteCommand + smoke tests | PLANNED | done | 2026-02-28 |
| 0.4.4 | 4 | Tool schemas + system prompt update | PLANNED | done | 2026-02-28 |
| 0.4.3 | 4 | NoteHandler + clip/select + batch note operations | PLANNED | done | 2026-02-28 |
| 0.4.2 | 4 | Fix deprecated BeatTimeValue.addRawValueObserver | UNPLANNED | done | 2026-02-28 |
| 0.4.1 | 4 | CursorClip creation + clip observers + snapshot | PLANNED | done | 2026-02-28 |
| 0.3.5 | 3 | Smoke test extension + LLM validation | PLANNED | done | 2026-02-28 |
| 0.3.4 | 3 | WebSocket push notifications | PLANNED | done | 2026-02-28 |
| 0.3.3 | 3 | CLI project setup + core commands | PLANNED | done | 2026-02-28 |
| 0.3.1 | 3 | Tool schemas + system prompt | PLANNED | done | 2026-02-28 |
| 0.2.5 | 2 | Smoke test extension | PLANNED | done | 2026-02-27 |
| 0.2.4 | 2 | Device + Cursor action handlers | PLANNED | done | 2026-02-27 |
| 0.2.3 | 2 | Device observers + snapshot | PLANNED | done | 2026-02-27 |
| 0.2.2 | 2 | Clip + Scene action handlers | PLANNED | done | 2026-02-27 |
| 0.2.1 | 2 | Clip launcher observers + snapshot | PLANNED | done | 2026-02-27 |
| 0.1.8 | 1 | Integration test + smoke suite | PLANNED | done | 2026-02-27 |
| 0.1.7 | 1 | Track + Master action handlers | PLANNED | done | 2026-02-27 |
| 0.1.6 | 1 | Transport action handlers | PLANNED | done | 2026-02-27 |
| 0.1.5 | 1 | Extension entry point + observers + snapshot + api.list + app handler | PLANNED | done | 2026-02-27 |
| 0.1.4 | 1 | Command queue + thread bridge | PLANNED | done | 2026-02-27 |
| 0.1.3 | 1 | HTTP + WebSocket servers | PLANNED | done | 2026-02-27 |
| 0.1.2 | 1 | JSON-RPC 2.0 dispatcher | PLANNED | done | 2026-02-27 |
| 0.1.1 | 1 | Gradle project scaffold | PLANNED | done | 2026-02-27 |
| 0.0.1 | 0 | Project discovery & scaffold | PLANNED | done | 2026-02-27 |

---

## Active Decisions

<!-- Decisions that affect current/upcoming work -->

- **D-6.1a** — 7 explicit RPC methods: createAudio, createInstrument, createEffect, deleteSelected, rename, select, duplicate
- **D-6.2a** — track/select via selectInEditor() with TRACK_OUT_OF_RANGE error + bank width
- **D-6.3a** — TrackHandler + TrackBankManager utility class
- **D-6.4a** — Richer responses: {ok, cursorTrackName, cursorTrackIndex}
- **D-6.5** — Defer Action system to later phase

---

## Open Flags

<!-- Items that need human attention -->

_None._

---

## Working Memory

- Package root: `dev.gregross.gig` → `src/main/java/dev/gregross/gig/`
- Packages: `extension/`, `rpc/`, `server/`, `handlers/`
- API reference: `docs/bitwig-api-reference.txt` (536KB flat file)
- Extension install: `~/Documents/Bitwig Studio/Extensions/`
- Bitwig version: 6.0 Beta 13, API v25, JDK 21
- Bitwig API JAR: `/Applications/Bitwig Studio.app/Contents/Java/bitwig.jar` (not on Maven Central for v25)
- Shadow plugin: `com.gradleup.shadow:9.0.0-beta12` — outputs directly to Extensions dir
- HTTP port: 8787 (configurable), WebSocket: 8788 (port+1)
- Existing extensions at install path: Maestro.bwextension (predecessor)
- Key Bitwig API: `ControllerHost.createTransport()`, `createMainTrackBank(64,0,0)`, `createMasterTrack(0)`, `createApplication()`
- Track hierarchy: Track extends Channel extends DeviceChain; `name()` from DeviceChain, `volume()/pan()/mute()/solo()` from Channel, `arm()` from Track
- Transport: `play()`, `stop()`, `record()`, `tempo()`, `getPosition()`, `playPosition()`, `timeSignature()`, `isPlaying()`, `isArrangerRecordEnabled()`
- Threading: network threads → CommandQueue → `host.requestFlush()` → `flush()` drains queue on session thread
- MasterTrack extends Track with no additional methods
- Bitwig API quirks: JAR strips generic type signatures — need explicit callback casts; `Parameter.addRawValueObserver()` deprecated — use `Parameter.value().addRawValueObserver()`; `Parameter.value().set()` has take-over — use `setImmediately()` for RPC; `SettableBeatTimeValue.addRawValueObserver()` deprecated since API v2 — use `addValueObserver()` instead; `NoteStep.State` inner enum not resolvable via `NoteStep.State.NoteOn` — use `step.state().name().equals("NoteOn")` string comparison
- Service loader: `META-INF/services/com.bitwig.extension.ExtensionDefinition` required for Bitwig to discover the extension
- ClipLauncherSlotBank: bank-level indexed callbacks (`IndexedBooleanValueChangedCallback`, `IndexedStringValueChangedCallback`) more efficient than per-slot observers
- `ClipLauncherSlotBankPlaybackStateChangedCallback`: `(slotIndex, playbackState, isQueued)` — playbackState: 0=stopped, 1=playing, 2=recording
- `StringArrayValue.addValueObserver()` requires explicit `(StringArrayValueChangedCallback)` cast + `(String[])` value cast
- CursorTrack: `host.createCursorTrack(id, name, sends, scenes, followSelection)` — follows UI track selection
- CursorDevice: `cursorTrack.createCursorDevice(id, name, sends, CursorDeviceFollowMode.FOLLOW_SELECTION)`
- CursorRemoteControlsPage: `cursorDevice.createCursorRemoteControlsPage(8)` — 8 params per page
- `RangedValue.displayedValue()` returns `StringValue` with formatted param value ("1.2 kHz")
- SceneBank: `trackBank.sceneBank()`, `sceneBank.getScene(i)` — Scene has `name()`, `clipCount()`
- CursorClip: `cursorTrack.createLauncherCursorClip(id, name, 64, 128)` — 64-step viewport, full MIDI range, follows selected clip
- NoteStep: `getStep(channel, x, y)` → state, velocity, duration; `setStep(channel, x, y, vel, dur)`; `clearStep(channel, x, y)`
- Batch note ops: `clip/setNotes` accepts array, loops internally; `clip/getNotes` returns sparse array
- NoteHandler: `handlers/NoteHandler.java` — note-level operations on CursorClip
- Total RPC methods: 55 (Phase 1: 23, Phase 2: 13, Phase 4: 8, Phase 5: 4, Phase 6: 7)
- Tool schemas: `tools/claude-tools.json` — 48 Claude tool_use definitions, 1:1 with RPC methods (underscore naming)
- System prompt: `tools/system-prompt.md` — viewport model, perception-action loop, value ranges, cursor model, indices
- CLI source: `src/cli/java/dev/gregross/gig/cli/` — GigCli, RpcClient, RpcCommand, SnapshotCommand, TransportCommand, TrackCommand
- CLI JAR: `build/libs/gig-cli.jar` via `./gradlew cliShadowJar` — Picocli 4.7.6
- CLI posts to `http://{host}:{port}/rpc` (not root URL)
- `StateCache.getChangedSections()` — per-section hash delta detection (transport, tracks, scenes, device, master, application)
- WebSocket push: `state/changed` notification with `changed` array, broadcast only when `getWsClientCount() > 0`
- Smoke test `--offline` flag for testing without Bitwig
- Total tests: 51 unit + 101 offline smoke = 152 (online smoke tests require Bitwig)
- Bitwig device library: `/Applications/Bitwig Studio.app/Contents/Resources/Library/devices/` — 151 `.bwdevice` files
- InsertionPoint API: `endOfDeviceChainInsertionPoint()`, `beforeDeviceInsertionPoint()`, `afterDeviceInsertionPoint()` — all have `insertFile(String)`, `insertVST2Device(int)`, `insertVST3Device(String)`, `insertCLAPDevice(String)`
- DeviceLibrary: new utility class in `handlers/` — name→path map, case-insensitive, close-match errors
- Phase 5 adds 4 RPC methods: device/insertBitwigDevice, device/insertPluginDevice, device/listBitwigDevices, device/remove
- **Spike validated:** `insertFile()` works from Controller API — Polymer.bwdevice inserted successfully via `cursorTrack.endOfDeviceChainInsertionPoint().insertFile(path)`
- **Known limitation:** CursorDevice loses selection after `deleteObject()` — `selectNext`/`selectPrevious` don't re-acquire. Future: `device/removeAll` with re-selection loop
- Bitwig API track creation: `Application.createAudioTrack(int)`, `createInstrumentTrack(int)`, `createEffectTrack(int)` — position -1 = append
- Track deletion: `cursorTrack.deleteObject()` (Track extends DeleteableObject since API v10)
- Track renaming: `cursorTrack.name().set(String)` (SettableStringValue from DeviceChain)
- Track selection by index: `trackBank.getItemAt(i).selectInEditor()` — CursorTrack follows via `followSelection=true`
- Phase 6 adds 7 RPC methods: track/createAudio, track/createInstrument, track/createEffect, track/select, track/rename, track/deleteSelected, track/duplicate
- TrackBankManager: utility for index validation + selection delegation

---

## Session Recovery

1. Read this file — current state
2. Read `PLAN.md` — what's next
3. Read `DECISIONS.md` — what's been decided
4. Resume from next batch
