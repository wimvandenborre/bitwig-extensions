# Plan

> Living document — reflects what was done, not just what was intended.
> This file tracks the ACTIVE phase only. Completed phases are archived to `phases/`.

---

## Active Phase

### Phase 16 — Project & Session Management (v0.16.x)

> Add project-wide state operations (unsolo/unmute/unarm all, createScene), audio engine control, popup notifications, panel layout, and transport navigation (cue marker jumping, continue, restart, return to arrangement, pre-roll, metronome volume). Extends ApplicationHandler, TransportHandler, and adds new ProjectHandler. Adds 7 snapshot fields and 17 new RPC methods.

**Decisions:** D-16.1, D-16.2, D-16.3, D-16.4, D-16.5

| Batch | Version | Title | Delegation | Status |
|-------|---------|-------|------------|--------|
| 16.1 | `0.16.1` | Snapshot observers — project + transport fields | in-session | done |
| 16.2 | `0.16.2` | 15 RPC methods — ApplicationHandler + ProjectHandler + TransportHandler | in-session | done |
| 16.3 | `0.16.3` | Unit tests | in-session | done |
| 16.4 | `0.16.4` | Tool schemas + system prompt update | in-session | done |
| 16.5 | `0.16.5` | Smoke tests | in-session | done |

### Batch 16.1 — Snapshot observers — project + transport fields

**Delegation:** in-session
**Decisions:** D-16.3
**Files:** `StateCache.java`, `GigMaestroExtension.java`
**Work:**
- Add 7 new fields to StateCache:
  - `hasSoloedTracks` (boolean) — `project.hasSoloedTracks().addValueObserver()`
  - `hasMutedTracks` (boolean) — `project.hasMutedTracks().addValueObserver()`
  - `hasArmedTracks` (boolean) — `project.hasArmedTracks().addValueObserver()`
  - `isModified` (boolean) — `project.isModified().addValueObserver()`
  - `panelLayout` (String) — `application.panelLayout().addValueObserver()`
  - `metronomeVolume` (double) — `transport.metronomeVolume().addRawValueObserver()`
  - `preRoll` (String) — `transport.preRoll().addValueObserver()`
- Add project fields to `getApplicationState()` snapshot (hasSoloedTracks, hasMutedTracks, hasArmedTracks, isModified, panelLayout)
- Add transport fields to `getTransportState()` snapshot (metronomeVolume, preRoll)
- Pass `Project` object to StateCache constructor (currently only used for SceneHandler)
**Test criteria:** `./gradlew test` passes — existing tests still pass with new fields
**Acceptance:** Snapshot sections include new fields

### Batch 16.2 — 17 RPC methods — ApplicationHandler + ProjectHandler + TransportHandler

**Delegation:** in-session
**Decisions:** D-16.2, D-16.4, D-16.5
**Files:** `ApplicationHandler.java`, `ProjectHandler.java` (new), `TransportHandler.java`, `GigMaestroExtension.java`
**Work:**
- **ApplicationHandler** (+4 methods):
  - `app/activateEngine` — `application.activateEngine()`
  - `app/deactivateEngine` — `application.deactivateEngine()`
  - `app/showNotification` — `host.showPopupNotification(text)` (requires ControllerHost param)
  - `app/setPanelLayout` — `application.setPanelLayout(layout)` (validate: ARRANGE/MIX/EDIT)
- **ProjectHandler** (new, 6 methods):
  - `project/unsoloAll` — `project.unsoloAll()`
  - `project/unmuteAll` — `project.unmuteAll()`
  - `project/unarmAll` — `project.unarmAll()`
  - `project/createScene` — `project.createScene()`
  - `project/createSceneFromPlaying` — `project.createSceneFromPlayingLauncherClips()`
  - `project/getState` — return hasSoloedTracks, hasMutedTracks, hasArmedTracks, isModified from stateCache
- **TransportHandler** (+7 methods):
  - `transport/continuePlayback` — `transport.continuePlayback()`
  - `transport/restart` — `transport.restart()`
  - `transport/returnToArrangement` — `transport.returnToArrangement()`
  - `transport/jumpToPreviousCueMarker` — `transport.jumpToPreviousCueMarker()`
  - `transport/jumpToNextCueMarker` — `transport.jumpToNextCueMarker()`
  - `transport/setPreRoll` — `transport.preRoll().set(value)` (validate: none/one_bar/two_bars/four_bars)
  - `transport/setMetronomeVolume` — `transport.metronomeVolume().setImmediately(value)` (0.0–1.0)
- Wire ProjectHandler in GigMaestroExtension.java
**Test criteria:** `./gradlew shadowJar` compiles; `./gradlew test` — existing tests pass (new tests in 16.3)
**Acceptance:** All 17 methods registered, build succeeds

### Batch 16.3 — Unit tests

**Delegation:** in-session
**Decisions:** D-16.2
**Files:** `ApplicationHandlerTest.java`, `ProjectHandlerTest.java` (new), `TransportHandlerTest.java`
**Work:**
- Update ApplicationHandlerTest: registration count 3→7, add engine/notification/panelLayout method assertions, validate `app/showNotification` requires `text` param, validate `app/setPanelLayout` requires `layout` param
- Create ProjectHandlerTest: registration count 6, validate all method names, `project/getState` returns expected fields
- Update TransportHandlerTest: registration count 19→26, add new method assertions, validate `transport/setPreRoll` requires `value` param, validate `transport/setMetronomeVolume` requires `value` param
**Test criteria:** `./gradlew test` — all tests pass (target: ~230+)
**Acceptance:** All new methods have test coverage

### Batch 16.4 — Tool schemas + system prompt update

**Delegation:** in-session
**Decisions:** D-16.1, D-16.4
**Files:** `tools/claude-tools.json`, `tools/system-prompt.md`
**Work:**
- Add 17 tool schemas to claude-tools.json (128→145 tools)
- Update `session_snapshot` description to mention project state fields + transport additions
- Add "Project & Session Management" section to system-prompt.md covering:
  - Engine control workflow (activate/deactivate)
  - Project state queries (project/getState) and bulk reset (unsolo/unmute/unarm all)
  - Transport navigation (cue marker jumping, continue vs play, return to arrangement)
  - Notifications for user feedback
  - Pre-roll and metronome volume for recording workflows
**Test criteria:** `python3 -c "import json; json.load(open('tools/claude-tools.json'))"` — valid JSON
**Acceptance:** 145 tool schemas, system prompt updated

### Batch 16.5 — Smoke tests

**Delegation:** in-session
**Decisions:** D-16.1
**Files:** `scripts/smoke-test.sh`
**Work:**
- Add Phase 16 offline tests:
  - Tool schema presence for all 17 new tools
  - Snapshot field type assertions
  - System prompt content assertions
- Add Phase 16 online tests:
  - `api/list` includes all 17 new methods
  - `session/snapshot` returns new project/transport fields
  - `app/activateEngine` + `app/deactivateEngine` round-trip
  - `app/showNotification` with text
  - `app/setPanelLayout` with valid/invalid layout
  - `project/unsoloAll`, `project/unmuteAll`, `project/unarmAll` execute without error
  - `project/createScene` creates a scene
  - `transport/continuePlayback`, `transport/restart` execute
  - `transport/setPreRoll` with valid/invalid values
  - `transport/setMetronomeVolume` with 0.5
**Test criteria:** `./scripts/smoke-test.sh --offline` passes
**Acceptance:** All offline smoke tests pass

**Phase Acceptance Criteria:**
- [ ] 17 new RPC methods registered and callable
- [ ] 7 new snapshot fields present in session/snapshot
- [ ] ProjectHandler created with 6 methods
- [ ] ApplicationHandler extended to 7 methods
- [ ] TransportHandler extended to 26 methods
- [ ] All unit tests pass
- [ ] All offline smoke tests pass
- [ ] Tool schemas: 145 total
- [ ] System prompt documents project & session management

**Completion triggers Phase 17 -> version `0.17.0`**
