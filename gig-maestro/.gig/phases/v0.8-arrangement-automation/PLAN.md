# Plan

> Living document — reflects what was done, not just what was intended.
> This file tracks the ACTIVE phase only. Completed phases are archived to `phases/`.

---

## Active Phase

### Phase 8 — Arrangement & Automation Control (v0.8.x)

> Add arrangement production controls: arranger panel visibility/layout, arranger loop range + punch range, cue marker bank (create/list/launch/delete), and automation write mode toggles. Extends the existing TransportHandler for transport-owned state and introduces one new ArrangerHandler for arranger-owned state. Two new snapshot sections: `arranger` and `arrangement`.

**Decisions:** D-8.1, D-8.2a, D-8.3, D-8.4, D-8.5a, D-8.6, D-8.7

| Batch | Version | Title | Delegation | Status |
|-------|---------|-------|------------|--------|
| 8.1 | `0.8.1` | Arranger + CueMarkerBank creation + snapshot sections | in-session | done |
| 8.2 | `0.8.2` | ArrangerHandler — visibility toggles | in-session | done |
| 8.3 | `0.8.3` | TransportHandler — loop range + punch range + automation | in-session | done |
| 8.4 | `0.8.4` | ArrangerHandler — cue marker operations | in-session | done |
| 8.5 | `0.8.5` | Tool schemas + system prompt update | in-session | done |
| 8.6 | `0.8.6` | Unit tests + smoke tests | in-session | done |

### Batch 8.1 — Arranger + CueMarkerBank Creation + Snapshot Sections

**Delegation:** in-session
**Decisions:** D-8.2a, D-8.7
**Files:** `GigMaestroExtension.java`, `StateCache.java`
**API25 contract — objects anchored in this batch:**
- `host.createArranger()` → Arranger (7 SettableBooleanValue visibility toggles)
- `host.createCueMarkerBank(16)` → CueMarkerBank (16 CueMarker slots: name, position, color, launch, delete)
- Transport values (already created): `isArrangerLoopEnabled()`, `arrangerLoopStart()`, `arrangerLoopDuration()`, `isPunchInEnabled()`, `isPunchOutEnabled()`, `getInPosition()`, `getOutPosition()`, `automationWriteMode()`, `isArrangerAutomationWriteEnabled()`, `isClipLauncherAutomationWriteEnabled()`, `isAutomationOverrideActive()`
**Work:**
- Create `Arranger` via `host.createArranger()` in `init()`.
- Create `CueMarkerBank` via `host.createCueMarkerBank(16)` in `init()`.
- Register observers on all 7 arranger visibility booleans in StateCache.
- Register observers for loop range (`arrangerLoopStart`, `arrangerLoopDuration`, `isArrangerLoopEnabled`), punch (`isPunchInEnabled`, `isPunchOutEnabled`, `getInPosition`, `getOutPosition`), automation (`automationWriteMode`, `isArrangerAutomationWriteEnabled`, `isClipLauncherAutomationWriteEnabled`, `isAutomationOverrideActive`).
- Register observers for cue markers (16 slots: name, position, color).
- Add `arranger` section to snapshot: 7 boolean fields.
- Add `arrangement` section to snapshot: loop (start, duration, enabled), punch (inPosition, inEnabled, outPosition, outEnabled), automation (writeMode, arrangerWriteEnabled, clipLauncherWriteEnabled, overrideActive), cueMarkers (array of {index, name, position, color}).
- Add both sections to `getChangedSections()` delta detection.
**Test criteria:** `session/snapshot` returns `arranger` and `arrangement` sections. Arranger section has 7 boolean fields. Arrangement section has loop, punch, automation, and cueMarkers sub-objects. Build succeeds.
**Acceptance:** Snapshot reflects all new state. Delta detection works for both sections.

### Batch 8.2 — ArrangerHandler — Visibility Toggles

**Delegation:** in-session
**Decisions:** D-8.2a, D-8.3
**Files:** `handlers/ArrangerHandler.java`
**Work:**
- Create `ArrangerHandler` class following existing handler pattern.
- Constructor takes `Arranger` object.
- Register 7 RPC methods:
  - `arranger/setPlaybackFollow` — `{enabled: boolean}`
  - `arranger/setClipLauncherVisible` — `{enabled: boolean}`
  - `arranger/setTimelineVisible` — `{enabled: boolean}`
  - `arranger/setCueMarkersVisible` — `{enabled: boolean}`
  - `arranger/setEffectTracksVisible` — `{enabled: boolean}`
  - `arranger/setIoSectionVisible` — `{enabled: boolean}`
  - `arranger/setDoubleRowTrackHeight` — `{enabled: boolean}`
- All return `{ok: true}`.
- Wire up in `GigMaestroExtension.init()`.
**Test criteria:** `api/list` includes all 7 `arranger/*` methods. Each method returns `{ok: true}`. Snapshot reflects toggled state.
**Acceptance:** All 7 arranger visibility toggles work end-to-end.

Depends on Batch 8.1 (Arranger object + snapshot).

### Batch 8.3 — TransportHandler — Loop Range + Punch Range + Automation

**Delegation:** in-session
**Decisions:** D-8.2a, D-8.4, D-8.6
**Files:** `handlers/TransportHandler.java`
**Work:**
- Add 8 RPC methods to existing TransportHandler:
  - `transport/setLoopRange` — `{start: number, duration: number, enabled: boolean}` — sets arranger loop start, duration, and enabled state
  - `transport/setPunchIn` — `{position: number, enabled: boolean}`
  - `transport/setPunchOut` — `{position: number, enabled: boolean}`
  - `transport/getLoopRange` — returns `{loopStart, loopDuration, loopEnabled, punchInPosition, punchInEnabled, punchOutPosition, punchOutEnabled}`
  - `transport/setAutomationWriteMode` — `{mode: "latch"|"touch"|"write"}`
  - `transport/setArrangerAutomationWrite` — `{enabled: boolean}`
  - `transport/setClipLauncherAutomationWrite` — `{enabled: boolean}`
  - `transport/resetAutomationOverrides` — no params
- TransportHandler constructor already receives Transport — no wiring changes needed.
**Test criteria:** `api/list` includes all 8 new `transport/*` methods. `transport/setLoopRange` sets loop and snapshot reflects it. `transport/setAutomationWriteMode` changes mode. Build succeeds.
**Acceptance:** All 8 transport arrangement/automation methods work. Snapshot `arrangement` section reflects changes.

Depends on Batch 8.1 (snapshot sections must exist to verify).

### Batch 8.4 — ArrangerHandler — Cue Marker Operations

**Delegation:** in-session
**Decisions:** D-8.5a
**Files:** `handlers/ArrangerHandler.java`
**Work:**
- Add 4 RPC methods to ArrangerHandler (receives Transport + CueMarkerBank in constructor):
  - `cueMarker/addAtPlayhead` — no params, calls `transport.addCueMarkerAtPlaybackPosition()`, returns `{ok: true}`
  - `cueMarker/list` — returns array of `{index, name, position, color}` for all non-empty markers in the 16-slot bank
  - `cueMarker/launch` — `{index: integer, quantized: boolean}` — calls `bank.getItemAt(index).launch(quantized)`
  - `cueMarker/delete` — `{index: integer}` — calls `bank.getItemAt(index).delete()`
- Update ArrangerHandler constructor to also take `Transport` and `CueMarkerBank`.
- Update wiring in `GigMaestroExtension.init()`.
**Test criteria:** `api/list` includes all 4 `cueMarker/*` methods. `cueMarker/addAtPlayhead` creates a marker. `cueMarker/list` returns it. `cueMarker/delete` removes it. Build succeeds.
**Acceptance:** Full cue marker lifecycle works: add → list → launch → delete.

Depends on Batch 8.1 (Arranger + CueMarkerBank + Transport objects must exist).

### Batch 8.5 — Tool Schemas + System Prompt Update

**Delegation:** in-session
**Decisions:** D-8.1, D-8.3, D-8.4, D-8.5a, D-8.6
**Files:** `tools/claude-tools.json`, `tools/system-prompt.md`
**Work:**
- Add ~19 tool schemas to `claude-tools.json` (7 arranger + 8 transport + 4 cue marker).
- Add "Arrangement & Automation" section to system prompt covering:
  - Arranger visibility controls and what each toggle does
  - Loop range / punch range setup workflow
  - Cue marker workflow (set position → add at playhead → list → launch)
  - Automation write mode explanation (latch/touch/write) and when to use `resetOverrides`
  - Note that `cueMarker/addAtPlayhead` creates at current transport position — use `transport/setPosition` first to place at a specific beat
- Update snapshot documentation to include `arranger` and `arrangement` sections.
**Test criteria:** Tool JSON validates with >= 74 tools (55 existing + 19 new). System prompt contains "Arrangement & Automation" section. All tool names use underscore convention.
**Acceptance:** Agent has complete guidance for arrangement workflow. All new tools documented.

Depends on Batches 8.2, 8.3, 8.4 (all methods must be finalized).

### Batch 8.6 — Unit Tests + Smoke Tests

**Delegation:** in-session
**Decisions:** D-8.1
**Files:** `src/test/java/dev/gregross/gig/`, `scripts/smoke-test.sh`
**Work:**
- Unit tests for ArrangerHandler (visibility toggle validation, cue marker index validation).
- Unit tests for new TransportHandler methods (loop range param validation, automation mode enum validation, punch position validation).
- Offline smoke tests:
  - Verify all 19 new tools exist in `claude-tools.json`
  - Verify system prompt has "Arrangement & Automation" section
  - Verify tool count >= 74
  - Spot-check parameter types for key new tools
- Online smoke tests:
  - Arranger visibility toggles (set + snapshot verify)
  - Loop range set + get
  - Automation write mode set + verify in snapshot
  - Cue marker lifecycle: add → list → delete
**Test criteria:** `./gradlew test` all pass. `./scripts/smoke-test.sh --offline` all pass. New test count covers all 19 methods.
**Acceptance:** Full test coverage for Phase 8. No existing tests broken.

Depends on Batches 8.2, 8.3, 8.4 (unit tests for handlers). Offline smoke tests for schemas/prompt also depend on 8.5.

**Phase Acceptance Criteria:**
- [ ] Arranger object created in init(), 7 visibility toggles work via RPC
- [ ] Loop range (start, duration, enabled) settable and reflected in snapshot
- [ ] Punch in/out (position, enabled) settable and reflected in snapshot
- [ ] Automation write mode (latch/touch/write) settable and reflected in snapshot
- [ ] Automation override reset works
- [ ] Cue marker lifecycle: addAtPlayhead → list → launch → delete
- [ ] 16-slot cue marker bank with name, position, color in snapshot
- [ ] Two new snapshot sections: `arranger` (7 booleans) + `arrangement` (loop, punch, automation, cueMarkers)
- [ ] Delta detection works for both new sections
- [ ] ~19 new RPC methods (total >= 74)
- [ ] Tool schemas for all new methods (total >= 74 tools)
- [ ] System prompt has "Arrangement & Automation" section
- [ ] All existing tests pass (51 unit + 117 offline smoke)
- [ ] New unit tests + smoke tests for all Phase 8 methods

**Completion triggers Phase 9 → version `0.9.0`**

---

## Plan Amendments

<!-- Log any changes to the plan after creation -->

| Date | Version | Amendment | Reason |
|------|---------|-----------|--------|
| 2026-02-28 | 0.8.0 | 8.4 deps: 8.1 (not 8.2); 8.6 deps: 8.2+8.3+8.4 (not just 8.5); API25 contract added to 8.1 | User redline: correct dependency graph, explicit API surface |
