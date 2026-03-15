# Plan

> Living document — reflects what was done, not just what was intended.
> This file tracks the ACTIVE phase only. Completed phases are archived to `phases/`.

---

## Active Phase

### Phase 34 — gig-maestro: Cue Mix & Scene Creation (v0.34.x)

> Add headphone cue mix control (cueVolume, cueMix as cached parameters with get/set RPCs) and scene creation methods (create empty scene, create scene from playing clips). Cue values are cached in StateCache, included in project snapshot and delta, and exposed via project/setCueVolume and project/setCueMix. Scene creation is added to SceneHandler.

**Decisions:** D-1.1, D-1.2, D-1.3, D-1.4

| Batch | Version | Title | Delegation | Status |
|-------|---------|-------|------------|--------|
| 34.1 | `0.34.1` | StateCache cue mix + ProjectHandler set RPCs | team | done |
| 34.2 | `0.34.2` | SceneHandler scene creation methods | team | done (pre-existing) |
| 34.3 | `0.34.3` | Unit tests | in-session | done |
| 34.4 | `0.34.4` | Tool definitions + system prompt + smoke tests | in-session | done |
| 34.5 | `0.34.5` | Build verification | in-session | done |

### Batch 34.1 — StateCache cue mix + ProjectHandler set RPCs

**Delegation:** team
**Decisions:** D-1.2, D-1.4
**Files:**
- `gig-maestro/src/main/java/dev/gregross/gig/extension/StateCache.java` (MODIFY)
- `gig-maestro/src/main/java/dev/gregross/gig/handlers/ProjectHandler.java` (MODIFY)
**Work:**
1. In StateCache, add fields `double cueVolume`, `double cueMix`. In `registerObservers()` where Project is handled, add:
   - `project.cueVolume().markInterested()` + `project.cueVolume().value().addValueObserver(...)`
   - `project.cueMix().markInterested()` + `project.cueMix().value().addValueObserver(...)`
2. Include `cueVolume` and `cueMix` in the project snapshot section (getProjectState method)
3. In ProjectHandler, add 2 RPCs:
   - `project/setCueVolume` — params: `{value: double}`, calls `project.cueVolume().value().set(value)`
   - `project/setCueMix` — params: `{value: double}`, calls `project.cueMix().value().set(value)`
4. Extend `project/getState` response to include cueVolume and cueMix from StateCache
**Test criteria:** Unit tests in batch 34.3
**Acceptance:** Cue values in snapshot, 2 set RPCs, getState includes cue values

### Batch 34.2 — SceneHandler scene creation methods

**Delegation:** team
**Decisions:** D-1.3
**Files:**
- `gig-maestro/src/main/java/dev/gregross/gig/handlers/SceneHandler.java` (MODIFY)
- `gig-maestro/src/main/java/dev/gregross/gig/extension/GigMaestroExtension.java` (MODIFY — pass Project to SceneHandler)
**Work:**
1. Modify SceneHandler constructor to accept Project parameter
2. Add 2 RPCs:
   - `scene/create` — no params, calls `project.createScene()`, returns "ok"
   - `scene/createFromPlaying` — no params, calls `project.createSceneFromPlayingLauncherClips()`, returns "ok"
3. Update GigMaestroExtension to pass Project when constructing SceneHandler
**Test criteria:** Unit tests in batch 34.3
**Acceptance:** 2 new scene methods registered

### Batch 34.3 — Unit tests

**Delegation:** in-session
**Depends on:** Batch 34.1, 34.2
**Files:**
- `gig-maestro/src/test/java/dev/gregross/gig/handlers/ProjectHandlerTest.java` (MODIFY)
- `gig-maestro/src/test/java/dev/gregross/gig/handlers/SceneHandlerTest.java` (MODIFY)
- `gig-maestro/src/test/java/dev/gregross/gig/extension/StateCacheSnapshotTest.java` (MODIFY)
**Work:**
1. ProjectHandlerTest: add tests for setCueVolume, setCueMix, verify getState includes cue values
2. SceneHandlerTest: add tests for scene/create and scene/createFromPlaying, update method count
3. StateCacheSnapshotTest: verify cueVolume and cueMix in project snapshot
**Test criteria:** `./gradlew :gig-maestro:test` passes
**Acceptance:** All new methods have test coverage

### Batch 34.4 — Tool definitions + system prompt + smoke tests

**Delegation:** in-session
**Depends on:** Batch 34.3
**Files:**
- `gig-maestro/tools/claude-tools.json` (MODIFY)
- `gig-maestro/tools/system-prompt.md` (MODIFY)
- `gig-maestro/scripts/tests/offline-schemas.sh` (MODIFY)
**Work:**
1. Add tool definitions for `project_setCueVolume`, `project_setCueMix`, `scene_create`, `scene_createFromPlaying`
2. Update system prompt with Cue Mix Control and Scene Creation sections
3. Add smoke test assertions
**Test criteria:** `gig-maestro/scripts/smoke-test.sh --offline` passes
**Acceptance:** 4 new tools in schema, documented in prompt

### Batch 34.5 — Build verification

**Delegation:** in-session
**Depends on:** Batch 34.4
**Files:** None
**Work:** Run `./gradlew :gig-maestro:shadowJar` and `./gradlew :gig-maestro:test`
**Test criteria:** Both commands exit 0
**Acceptance:** Extension builds and all tests pass

**Phase Acceptance Criteria:**
- [ ] cueVolume and cueMix cached in StateCache
- [ ] Cue values in project snapshot and delta
- [ ] project/setCueVolume and project/setCueMix RPCs work
- [ ] project/getState includes cueVolume and cueMix
- [ ] scene/create creates an empty scene
- [ ] scene/createFromPlaying captures playing clips as scene
- [ ] All unit tests pass
- [ ] Tool schemas and system prompt updated
- [ ] Offline smoke tests pass
- [ ] Clean build with shadowJar

**Completion triggers Phase 35 -> version `0.35.0`**
