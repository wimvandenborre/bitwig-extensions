# Plan

> Living document — reflects what was done, not just what was intended.
> This file tracks the ACTIVE phase only. Completed phases are archived to `phases/`.

---

## Active Phase

### Phase 30 — gig-maestro: Global Groove & High-Value API Gaps (v0.30.x)

> Add three high-value API capabilities: Global Groove control (new GrooveHandler with shuffle/accent parameters), exclusive solo toggle, and application zoom methods. Includes StateCache integration for groove, tool definitions, system prompt updates, and smoke tests.

**Decisions:** D-1.1, D-1.2, D-1.3, D-1.4, D-1.5, D-1.6

| Batch | Version | Title | Delegation | Status |
|-------|---------|-------|------------|--------|
| 30.1 | `0.30.1` | GrooveHandler + StateCache + extension wiring | team | done |
| 30.2 | `0.30.2` | Exclusive solo + zoom methods | team | done |
| 30.3 | `0.30.3` | Unit tests | in-session | done |
| 30.4 | `0.30.4` | Tool definitions + system prompt + smoke tests | in-session | done |
| 30.5 | `0.30.5` | Build verification | in-session | done |

### Batch 30.1 — GrooveHandler + StateCache + extension wiring

**Delegation:** team
**Decisions:** D-1.2, D-1.3, D-1.6
**Files:**
- `gig-maestro/src/main/java/dev/gregross/gig/handlers/GrooveHandler.java` (NEW)
- `gig-maestro/src/main/java/dev/gregross/gig/extension/GigMaestroExtension.java` (MODIFY)
- `gig-maestro/src/main/java/dev/gregross/gig/extension/StateCache.java` (MODIFY)
**Work:**
1. Create `GrooveHandler` with 3 RPC methods:
   - `groove/getState` — returns `{enabled, shuffleAmount, shuffleRate, accentAmount, accentRate, accentPhase}` (all doubles 0.0–1.0 except enabled which is boolean)
   - `groove/setEnabled` — params: `{enabled: boolean}` → calls `groove.getEnabled().set(enabled ? 1.0 : 0.0)`
   - `groove/setParameter` — params: `{name: string, value: double}` → dispatches to the named Parameter's `set(value)` method. Valid names: "shuffleAmount", "shuffleRate", "accentAmount", "accentRate", "accentPhase"
2. Add `registerGrooveObservers(Groove groove)` to StateCache — mark all 6 params interested, add to snapshot under `groove` key, include in delta
3. Wire in GigMaestroExtension: `Groove groove = host.createGroove()`, register observers, create and register handler
**Test criteria:** Unit tests in batch 30.3
**Acceptance:** GrooveHandler registered with 3 methods, groove state in snapshot and delta

### Batch 30.2 — Exclusive solo + zoom methods

**Delegation:** team
**Decisions:** D-1.4, D-1.5
**Files:**
- `gig-maestro/src/main/java/dev/gregross/gig/handlers/TrackHandler.java` (MODIFY)
- `gig-maestro/src/main/java/dev/gregross/gig/handlers/ApplicationHandler.java` (MODIFY)
**Work:**
1. Add `track/toggleSolo` to TrackHandler:
   - params: `{index: int, exclusive?: boolean}`
   - If `exclusive` is true: `track.solo().toggle(true)`
   - If `exclusive` is false or absent: `track.solo().toggle(false)`
2. Add 4 zoom methods to ApplicationHandler:
   - `app/zoomIn` — no params, calls `application.zoomIn()`
   - `app/zoomOut` — no params, calls `application.zoomOut()`
   - `app/zoomToFit` — no params, calls `application.zoomToFit()`
   - `app/zoomToSelection` — no params, calls `application.zoomToSelection()`
**Test criteria:** Unit tests in batch 30.3
**Acceptance:** 5 new RPC methods registered and callable

### Batch 30.3 — Unit tests

**Delegation:** in-session
**Decisions:** D-1.2, D-1.3, D-1.4, D-1.5, D-1.6
**Depends on:** Batch 30.1, 30.2
**Files:**
- `gig-maestro/src/test/java/dev/gregross/gig/handlers/GrooveHandlerTest.java` (NEW)
- `gig-maestro/src/test/java/dev/gregross/gig/handlers/TrackHandlerTest.java` (MODIFY)
- `gig-maestro/src/test/java/dev/gregross/gig/handlers/ApplicationHandlerTest.java` (MODIFY)
**Work:**
1. GrooveHandlerTest: test getState returns all 6 fields, setEnabled true/false, setParameter for each valid name, setParameter with invalid name throws, setParameter with out-of-range value
2. TrackHandlerTest: add test for toggleSolo default (non-exclusive), toggleSolo exclusive=true
3. ApplicationHandlerTest: add tests for all 4 zoom methods
**Test criteria:** `./gradlew :gig-maestro:test` passes
**Acceptance:** All new methods have test coverage

### Batch 30.4 — Tool definitions + system prompt + smoke tests

**Delegation:** in-session
**Depends on:** Batch 30.3
**Files:**
- `gig-maestro/tools/claude-tools.json` (MODIFY)
- `gig-maestro/tools/system-prompt.md` (MODIFY)
- `gig-maestro/scripts/tests/offline-schemas.sh` (MODIFY)
**Work:**
1. Add tool definitions for `groove_getState`, `groove_setEnabled`, `groove_setParameter`, `track_toggleSolo`, `app_zoomIn`, `app_zoomOut`, `app_zoomToFit`, `app_zoomToSelection`
2. Update system prompt with groove control section and zoom methods in app section
3. Add smoke test assertions for new tool schemas and system prompt references
**Test criteria:** `gig-maestro/scripts/smoke-test.sh --offline` passes
**Acceptance:** All 8 new tools in schema, documented in prompt, validated by smoke tests

### Batch 30.5 — Build verification

**Delegation:** in-session
**Depends on:** Batch 30.4
**Files:** None
**Work:** Run `./gradlew :gig-maestro:shadowJar` and `./gradlew :gig-maestro:test` to confirm clean build
**Test criteria:** Both commands exit 0
**Acceptance:** Extension builds and all tests pass

**Phase Acceptance Criteria:**
- [ ] GrooveHandler registered with 3 RPC methods
- [ ] Groove state in snapshot and delta notifications
- [ ] track/toggleSolo with exclusive flag works
- [ ] 4 zoom methods in ApplicationHandler
- [ ] All unit tests pass
- [ ] Tool schemas and system prompt updated
- [ ] Offline smoke tests pass
- [ ] Clean build with shadowJar

**Completion triggers Phase 31 -> version `0.31.0`**
