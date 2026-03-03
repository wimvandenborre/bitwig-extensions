# Plan

> Living document — reflects what was done, not just what was intended.
> This file tracks the ACTIVE phase only. Completed phases are archived to `phases/`.

---

## Active Phase

### Phase 19 — Clip Launcher Automation (v0.19.x)

> Expose clip launcher automation controls at all three levels: per-clip settings (launch quantization, launch mode, shuffle, accent), global transport defaults (default launch quantization, post-recording action, overdub, fill mode), and enhanced launch methods with per-launch overrides via launchWithOptions on clips and scenes.

**Decisions:** D-19.1, D-19.2, D-19.3, D-19.4, D-19.5, D-19.6

| Batch | Version | Title | Delegation | Status |
|-------|---------|-------|------------|--------|
| 19.1 | `0.19.1` | Clip cursor + transport snapshot observers | in-session | done |
| 19.2 | `0.19.2` | ClipHandler — 6 per-clip RPC methods | in-session | done |
| 19.3 | `0.19.3` | TransportHandler — 6 transport RPC methods + enhanced launch | in-session | done |
| 19.4 | `0.19.4` | Unit tests | in-session | done |
| 19.5 | `0.19.5` | Tool schemas + system prompt update | in-session | done |
| 19.6 | `0.19.6` | Smoke tests | in-session | done |

### Batch 19.1 — Clip cursor + transport snapshot observers

**Delegation:** in-session
**Decisions:** D-19.2, D-19.3
**Files:** `src/main/java/dev/gregross/gig/extension/StateCache.java`
**Work:**
- Add 5 clip cursor observers in `registerClipCursorObservers()`: launchQuantization (enum), launchMode (enum), shuffle (boolean), accent (double), useLoopStartAsQuantizationReference (boolean)
- Add 5 transport observers in `registerObservers()`: defaultLaunchQuantization (enum), clipLauncherPostRecordingAction (enum), clipLauncherPostRecordingTimeOffset (double), isClipLauncherOverdubEnabled (boolean), isFillModeActive (boolean)
- Extend `getClipState()` with the 5 new clip fields
- Extend `getTransportState()` with the 5 new transport fields
- Wire CursorClip ref for new methods (launchQuantization(), launchMode(), getShuffle(), getAccent(), useLoopStartAsQuantizationReference())
**Test criteria:** `./gradlew test` passes; StateCacheDeltaTest still valid; snapshot includes new fields
**Acceptance:** 10 new observer fields visible in transport and clip snapshot sections

### Batch 19.2 — ClipHandler — 6 per-clip RPC methods

**Delegation:** in-session
**Decisions:** D-19.4
**Files:** `src/main/java/dev/gregross/gig/handlers/ClipHandler.java`
**Work:**
- Add `clip/setLaunchQuantization` — params: `quantization` (enum: default, none, 8, 4, 2, 1, 1/2, 1/4, 1/8, 1/16)
- Add `clip/setLaunchMode` — params: `launchMode` (enum: default, from_start, continue_or_from_start, continue_or_synced, synced)
- Add `clip/setShuffle` — params: `enabled` (boolean)
- Add `clip/setAccent` — params: `value` (double 0.0–1.0)
- Add `clip/setUseLoopStartAsQuantizationReference` — params: `enabled` (boolean)
- Add `clip/getLaunchSettings` — returns all 5 values from stateCache
- All setters operate on cursorClip with enum/value validation
**Test criteria:** `./gradlew test` passes; 6 new methods registered
**Acceptance:** All 6 methods respond correctly via RPC

### Batch 19.3 — TransportHandler — 6 transport RPC methods + enhanced launch

**Delegation:** in-session
**Decisions:** D-19.5, D-19.6
**Files:** `src/main/java/dev/gregross/gig/handlers/TransportHandler.java`, `src/main/java/dev/gregross/gig/handlers/ClipHandler.java`, `src/main/java/dev/gregross/gig/handlers/SceneHandler.java`
**Work:**
- Add `transport/setDefaultLaunchQuantization` — params: `quantization` (enum: none, 8, 4, 2, 1, 1/2, 1/4, 1/8, 1/16) — note: no "default" for global
- Add `transport/setPostRecordingAction` — params: `action` (enum: off, play_recorded, record_next_free_slot, stop, return_to_arrangement, return_to_previous_clip, play_random)
- Add `transport/setPostRecordingTimeOffset` — params: `beats` (double)
- Add `transport/setClipLauncherOverdub` — params: `enabled` (boolean)
- Add `transport/setFillMode` — params: `enabled` (boolean)
- Add `transport/getClipLauncherSettings` — returns all 5 transport clip launcher values
- Enhance `clip/launch` with optional `quantization` + `launchMode` params → calls launchWithOptions when present
- Enhance `scene/launch` with optional `quantization` + `launchMode` params → calls launchWithOptions when present
**Test criteria:** `./gradlew test` passes; 6 new transport methods + 2 enhanced methods
**Acceptance:** All 6 transport methods work; clip/launch and scene/launch accept optional params

### Batch 19.4 — Unit tests

**Delegation:** in-session
**Decisions:** D-19.1 through D-19.6
**Files:** `src/test/java/dev/gregross/gig/handlers/ClipHandlerTest.java`, `src/test/java/dev/gregross/gig/handlers/TransportHandlerTest.java`, `src/test/java/dev/gregross/gig/handlers/SceneHandlerTest.java`
**Work:**
- Update ClipHandlerTest: verify new method count, test enum validation for launchQuantization and launchMode, test accent range validation
- Update TransportHandlerTest: verify new method count, test enum validation for quantization and postRecordingAction
- Update SceneHandlerTest: verify method count unchanged (scene/launch enhanced, not new)
- Test parameter validation rejects invalid enum values
**Test criteria:** `./gradlew test` passes with all new tests
**Acceptance:** All handler tests pass with updated method counts and validation tests

### Batch 19.5 — Tool schemas + system prompt update

**Delegation:** in-session
**Decisions:** D-19.1 through D-19.6
**Files:** `tools/claude-tools.json`, `tools/system-prompt.md`
**Work:**
- Add 12 new tool schemas: 6 clip methods + 6 transport methods
- Update 2 existing schemas: clip_launch + scene_launch with optional quantization/launchMode params
- Add "Clip Launcher Automation" section to system prompt with enum values reference, launch levels explanation
- Document the 3-level launch control model (global → per-clip → per-launch)
**Test criteria:** `./gradlew test` passes; JSON is valid; tool count = previous + 12
**Acceptance:** 12 new tools + 2 updated tools in schemas; system prompt documents clip launcher automation

### Batch 19.6 — Smoke tests

**Delegation:** in-session
**Decisions:** D-19.1 through D-19.6
**Files:** `scripts/smoke-test.sh`
**Work:**
- Offline: verify 12 new methods in api/list, new snapshot fields in transport + clip sections
- Online: test clip/setLaunchQuantization, clip/getLaunchSettings, transport/setDefaultLaunchQuantization, transport/getClipLauncherSettings, transport/setFillMode, clip/launch with options
**Test criteria:** `./scripts/smoke-test.sh --offline` passes
**Acceptance:** All offline smoke tests pass; online tests cover key methods

**Phase Acceptance Criteria:**
- [ ] 12 new RPC methods registered and functional
- [ ] 2 existing methods (clip/launch, scene/launch) enhanced with optional params
- [ ] 10 new snapshot fields (5 clip + 5 transport) in state cache
- [ ] All unit tests pass (`./gradlew test`)
- [ ] All offline smoke tests pass
- [ ] Tool schemas and system prompt updated
- [ ] Conventional commits per batch

**Completion triggers Phase 20 → version `0.20.0`**
