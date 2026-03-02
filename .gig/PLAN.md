# Plan

> Living document — reflects what was done, not just what was intended.
> This file tracks the ACTIVE phase only. Completed phases are archived to `phases/`.

---

## Active Phase

### Phase 13 — Mixer & Routing Intelligence (v0.13.x)

> Add send/return routing, track color control, crossfade mode, input monitoring, and expanded master track controls. Brings the mixer from basic level/pan to a full mixing surface — the agent can set up complete bus architectures (instrument → send → effect return), color-code tracks, and control crossfade/monitor modes.

**Decisions:** D-13.1, D-13.2a, D-13.3, D-13.4, D-13.5, D-13.6, D-13.7

| Batch | Version | Title | Delegation | Status |
|-------|---------|-------|------------|--------|
| 13.1 | `0.13.1` | Send infrastructure — StateCache observers + snapshot | in-session | pending |
| 13.2 | `0.13.2` | SendHandler — send/setLevel, send/setMode, send/setEnabled | in-session | pending |
| 13.3 | `0.13.3` | TrackHandler — track/setColor, track/setCrossfade, track/setMonitor + observers | in-session | pending |
| 13.4 | `0.13.4` | MasterHandler — master/setMute, master/setSolo, master/setColor | in-session | pending |
| 13.5 | `0.13.5` | Unit tests | in-session | pending |
| 13.6 | `0.13.6` | Tool schemas + system prompt update | in-session | pending |
| 13.7 | `0.13.7` | Smoke tests | in-session | pending |

### Batch 13.1 — Send infrastructure — StateCache observers + snapshot

**Delegation:** in-session
**Decisions:** D-13.2a
**Files:** `GigMaestroExtension.java`, `StateCache.java`
**Work:**
- Change `createMainTrackBank(TRACK_COUNT, 0, SCENE_COUNT)` to `createMainTrackBank(TRACK_COUNT, SEND_COUNT, SCENE_COUNT)` where `SEND_COUNT = 4`
- In `StateCache`, add per-track send observation: for each track's `sendBank()`, observe send name, value (level), and subscribe to `isPreFader()` and `sendChannelColor()` for snapshot readouts
- Add `trackCrossfadeModes` (String[8]) and `trackMonitorModes` (String[8]) observer arrays
- Observe `track.crossFadeMode()` and `track.monitorMode()` per track
- Extend snapshot builder: add `sends` array per track entry with `{ name, level, isPreFader, color: {r,g,b}, enabled }`, plus `crossfadeMode` and `monitorMode` fields per track
- Wire new `registerSendObservers(trackBank)` call in extension init
**Test criteria:** `./gradlew shadowJar` builds. Snapshot includes `sends`, `crossfadeMode`, `monitorMode` per track.
**Acceptance:** Snapshot returns sends array with 4 entries per track, crossfade/monitor modes visible.

### Batch 13.2 — SendHandler — RPC methods

**Delegation:** in-session
**Decisions:** D-13.2a, D-13.6
**Files:** `SendHandler.java` (new), `GigMaestroExtension.java`
**Work:**
- Create `SendHandler` class taking `TrackBank` reference
- Register 3 methods: `send/setLevel`, `send/setMode`, `send/setEnabled`
- `send/setLevel`: validate trackIndex (0–7), sendIndex (0–3), value (0.0–1.0), call `send.value().set(value)`
- `send/setMode`: validate mode enum ("AUTO"/"PRE"/"POST"), call `send.sendMode().set(mode)`
- `send/setEnabled`: call `send.isEnabled().set(enabled)`
- Wire into extension init
**Test criteria:** `./gradlew shadowJar` builds. RPC calls modify send state.
**Acceptance:** `send/setLevel`, `send/setMode`, `send/setEnabled` all return `{ ok: true }`.

### Batch 13.3 — TrackHandler — color, crossfade, monitor

**Delegation:** in-session
**Decisions:** D-13.3, D-13.5, D-13.6
**Files:** `TrackHandler.java`
**Work:**
- Add `track/setColor`: params `{ index, r, g, b }` — `track.color().set(r, g, b)`
- Add `track/setCrossfade`: params `{ index, mode }` — `track.crossFadeMode().set(mode)`
- Add `track/setMonitor`: params `{ index, mode }` — `track.monitorMode().set(mode)`
- Pass `trackBank` reference to existing constructor (already available)
**Test criteria:** `./gradlew shadowJar` builds. RPC calls change track color/crossfade/monitor.
**Acceptance:** All 3 methods return `{ ok: true }`, snapshot reflects changes.

### Batch 13.4 — MasterHandler — mute, solo, color

**Delegation:** in-session
**Decisions:** D-13.4, D-13.3, D-13.6
**Files:** `MasterHandler.java`
**Work:**
- Add `master/setMute`: params `{ value: boolean }` — `masterTrack.mute().set(value)`
- Add `master/setSolo`: params `{ value: boolean }` — `masterTrack.solo().set(value)`
- Add `master/setColor`: params `{ r, g, b }` — `masterTrack.color().set(r, g, b)`
**Test criteria:** `./gradlew shadowJar` builds. Master mute/solo/color controllable via RPC.
**Acceptance:** All 3 methods return `{ ok: true }`, snapshot reflects master mute/solo/color changes.

### Batch 13.5 — Unit tests

**Delegation:** in-session
**Decisions:** D-13.2a, D-13.3, D-13.4, D-13.5
**Files:** `SendHandlerTest.java` (new), `TrackHandlerTest.java`, `MasterHandlerTest.java`, `StateCacheTest.java`
**Work:**
- SendHandlerTest: test all 3 send methods + validation (out-of-range index, invalid mode)
- TrackHandlerTest: test setColor, setCrossfade, setMonitor + validation
- MasterHandlerTest: test setMute, setSolo, setColor
- StateCacheTest: test send observers populate snapshot, crossfade/monitor modes in snapshot
**Test criteria:** `./gradlew test` passes. All new tests green.
**Acceptance:** Full test suite passes with no regressions.

### Batch 13.6 — Tool schemas + system prompt update

**Delegation:** in-session
**Decisions:** D-13.7
**Files:** `tools/claude-tools.json`, `tools/system-prompt.md`
**Work:**
- Add 9 tool schemas: `send_setLevel`, `send_setMode`, `send_setEnabled`, `track_setColor`, `track_setCrossfade`, `track_setMonitor`, `master_setMute`, `master_setSolo`, `master_setColor`
- Update system prompt with "Mixer & Routing" section:
  - Send routing workflow (create effect track → use send/setLevel to route)
  - Color coding conventions
  - Crossfade modes (A/B/AB) for live performance
  - Monitor modes (ON/OFF/AUTO) for recording
  - Send snapshot structure
**Test criteria:** `./scripts/smoke-test.sh --offline` passes schema validation.
**Acceptance:** 114 tool schemas, system prompt documents all mixer features.

### Batch 13.7 — Smoke tests

**Delegation:** in-session
**Decisions:** D-13.7
**Files:** `scripts/smoke-test.sh`
**Work:**
- Add offline smoke tests for 9 new tool schemas (structure validation)
- Add online smoke tests for send/setLevel, track/setColor, master/setMute, track/setCrossfade, track/setMonitor
- Verify snapshot includes sends array and new fields
**Test criteria:** `./scripts/smoke-test.sh` passes (full suite including new tests).
**Acceptance:** All smoke tests pass. Total test count updated.

**Phase Acceptance Criteria:**
- [ ] 9 new RPC methods registered and callable
- [ ] Send levels route audio to effect tracks (manual verification)
- [ ] Track colors visible in Bitwig UI after `track/setColor`
- [ ] Crossfade mode changes reflected in Bitwig mixer
- [ ] Monitor mode changes reflected in Bitwig mixer
- [ ] Master mute/solo controllable via RPC
- [ ] Snapshot includes sends (name, level, isPreFader, color, enabled) per track
- [ ] Snapshot includes crossfadeMode and monitorMode per track
- [ ] All unit tests pass (`./gradlew test`)
- [ ] All smoke tests pass (`./scripts/smoke-test.sh`)
- [ ] System prompt documents mixer & routing workflows

**Completion triggers Phase 14 → version `0.14.0`**
