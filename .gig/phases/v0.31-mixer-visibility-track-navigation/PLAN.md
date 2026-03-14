# Plan

> Living document — reflects what was done, not just what was intended.
> This file tracks the ACTIVE phase only. Completed phases are archived to `phases/`.

---

## Active Phase

### Phase 31 — gig-maestro: Mixer Visibility & Track Navigation (v0.31.x)

> Add full Mixer panel control via a new MixerHandler: 6 section visibility toggles (meter, I/O, sends, clip launcher, devices, crossfade), 4 track width zoom methods, plus 2 mixer navigation methods on TrackHandler (selectInMixer, makeVisibleInMixer). Total: 12 new RPC methods.

**Decisions:** D-1.1, D-1.2, D-1.3, D-1.4, D-1.5, D-1.6

| Batch | Version | Title | Delegation | Status |
|-------|---------|-------|------------|--------|
| 31.1 | `0.31.1` | MixerHandler + extension wiring | team | done |
| 31.2 | `0.31.2` | Track mixer navigation methods | team | done |
| 31.3 | `0.31.3` | Unit tests | in-session | done |
| 31.4 | `0.31.4` | Tool definitions + system prompt + smoke tests | in-session | done |
| 31.5 | `0.31.5` | Build verification | in-session | done |

### Batch 31.1 — MixerHandler + extension wiring

**Delegation:** team
**Decisions:** D-1.2, D-1.3, D-1.4, D-1.6
**Files:**
- `gig-maestro/src/main/java/dev/gregross/gig/handlers/MixerHandler.java` (NEW)
- `gig-maestro/src/main/java/dev/gregross/gig/extension/GigMaestroExtension.java` (MODIFY)
**Work:**
1. Create `MixerHandler` with 7 RPC methods:
   - `mixer/getState` — returns `{meter, io, sends, clipLauncher, devices, crossFade}` (all booleans)
   - `mixer/setSection` — params: `{section: string, visible: boolean}`. Valid sections: "meter", "io", "sends", "clipLauncher", "devices", "crossFade". Dispatches to the named SettableBooleanValue's `set()`.
   - `mixer/zoomInAll` — no params, calls `mixer.zoomInTrackWidthsAll()`
   - `mixer/zoomOutAll` — no params, calls `mixer.zoomOutTrackWidthsAll()`
   - `mixer/zoomInSelected` — no params, calls `mixer.zoomInTrackWidthsSelected()`
   - `mixer/zoomOutSelected` — no params, calls `mixer.zoomOutTrackWidthsSelected()`
2. Wire in GigMaestroExtension: `Mixer mixer = host.createMixer()`, create and register handler
**Test criteria:** Unit tests in batch 31.3
**Acceptance:** MixerHandler registered with 7 methods, all callable

### Batch 31.2 — Track mixer navigation methods

**Delegation:** team
**Decisions:** D-1.5
**Files:**
- `gig-maestro/src/main/java/dev/gregross/gig/handlers/TrackHandler.java` (MODIFY)
**Work:**
1. Add `track/selectInMixer` — params: `{index: int}`. Gets track at index, calls `track.selectInMixer()`.
2. Add `track/makeVisibleInMixer` — params: `{index: int}`. Gets track at index, calls `track.makeVisibleInMixer()`.
**Test criteria:** Unit tests in batch 31.3
**Acceptance:** 2 new RPC methods registered and callable

### Batch 31.3 — Unit tests

**Delegation:** in-session
**Decisions:** D-1.2, D-1.3, D-1.4, D-1.5
**Depends on:** Batch 31.1, 31.2
**Files:**
- `gig-maestro/src/test/java/dev/gregross/gig/handlers/MixerHandlerTest.java` (NEW)
- `gig-maestro/src/test/java/dev/gregross/gig/handlers/TrackHandlerTest.java` (MODIFY)
**Work:**
1. MixerHandlerTest: test getState returns all 6 fields, setSection for each valid name, setSection with invalid name throws, setSection missing params throws, all 4 zoom methods call correct Mixer methods
2. TrackHandlerTest: add tests for selectInMixer and makeVisibleInMixer
**Test criteria:** `./gradlew :gig-maestro:test` passes
**Acceptance:** All new methods have test coverage

### Batch 31.4 — Tool definitions + system prompt + smoke tests

**Delegation:** in-session
**Depends on:** Batch 31.3
**Files:**
- `gig-maestro/tools/claude-tools.json` (MODIFY)
- `gig-maestro/tools/system-prompt.md` (MODIFY)
- `gig-maestro/scripts/tests/offline-schemas.sh` (MODIFY)
**Work:**
1. Add tool definitions for `mixer_getState`, `mixer_setSection`, `mixer_zoomInAll`, `mixer_zoomOutAll`, `mixer_zoomInSelected`, `mixer_zoomOutSelected`, `track_selectInMixer`, `track_makeVisibleInMixer`
2. Update system prompt with Mixer Panel Control section
3. Add smoke test assertions for new tool schemas and system prompt references
**Test criteria:** `gig-maestro/scripts/smoke-test.sh --offline` passes
**Acceptance:** All 8 new tools in schema, documented in prompt, validated by smoke tests

### Batch 31.5 — Build verification

**Delegation:** in-session
**Depends on:** Batch 31.4
**Files:** None
**Work:** Run `./gradlew :gig-maestro:shadowJar` and `./gradlew :gig-maestro:test` to confirm clean build
**Test criteria:** Both commands exit 0
**Acceptance:** Extension builds and all tests pass

**Phase Acceptance Criteria:**
- [ ] MixerHandler registered with 7 RPC methods
- [ ] mixer/getState returns all 6 section visibility booleans
- [ ] mixer/setSection sets any of 6 sections
- [ ] 4 mixer zoom methods work
- [ ] track/selectInMixer and track/makeVisibleInMixer work
- [ ] All unit tests pass
- [ ] Tool schemas and system prompt updated
- [ ] Offline smoke tests pass
- [ ] Clean build with shadowJar

**Completion triggers Phase 32 -> version `0.32.0`**
