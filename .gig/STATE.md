# State

## Current State

| Field | Value |
|-------|-------|
| **Version** | `0.2.5` |
| **Phase** | 2 — Clip Grid + Device Params |
| **Status** | `VERIFIED` |
| **Last Batch** | Smoke test extension |
| **Last Updated** | 2026-02-27 |

---

## Batch History

<!-- Newest first. Type: PLANNED or UNPLANNED -->

| Version | Phase | Batch Title | Type | Status | Timestamp |
|---------|-------|-------------|------|--------|-----------|
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

_Phase 2 decisions archived to `.gig/phases/v0.2-clip-grid-device-params/DECISIONS.md`. Run `/gig:decide` to start Phase 3._

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
- Bitwig API quirks: JAR strips generic type signatures — need explicit callback casts; `Parameter.addRawValueObserver()` deprecated — use `Parameter.value().addRawValueObserver()`; `Parameter.value().set()` has take-over — use `setImmediately()` for RPC
- Service loader: `META-INF/services/com.bitwig.extension.ExtensionDefinition` required for Bitwig to discover the extension
- ClipLauncherSlotBank: bank-level indexed callbacks (`IndexedBooleanValueChangedCallback`, `IndexedStringValueChangedCallback`) more efficient than per-slot observers
- `ClipLauncherSlotBankPlaybackStateChangedCallback`: `(slotIndex, playbackState, isQueued)` — playbackState: 0=stopped, 1=playing, 2=recording
- `StringArrayValue.addValueObserver()` requires explicit `(StringArrayValueChangedCallback)` cast + `(String[])` value cast
- CursorTrack: `host.createCursorTrack(id, name, sends, scenes, followSelection)` — follows UI track selection
- CursorDevice: `cursorTrack.createCursorDevice(id, name, sends, CursorDeviceFollowMode.FOLLOW_SELECTION)`
- CursorRemoteControlsPage: `cursorDevice.createCursorRemoteControlsPage(8)` — 8 params per page
- `RangedValue.displayedValue()` returns `StringValue` with formatted param value ("1.2 kHz")
- SceneBank: `trackBank.sceneBank()`, `sceneBank.getScene(i)` — Scene has `name()`, `clipCount()`
- Total RPC methods: 36 (Phase 1: 23, Phase 2: 13)

---

## Session Recovery

1. Read this file — current state
2. Read `PLAN.md` — what's next
3. Read `DECISIONS.md` — what's been decided
4. Resume from next batch
