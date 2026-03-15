# Plan

> Living document — reflects what was done, not just what was intended.
> This file tracks the ACTIVE phase only. Completed phases are archived to `phases/`.

---

## Active Phase

### Phase 35 — gig-maestro: Arranger Lane Zoom & Timeline Navigation (v0.35.x)

> Add arranger lane height zoom (4 methods for zooming track lanes in/out, all or selected) and precision timeline navigation via ScrollbarModel (zoomToContentRegion for framing specific bar ranges, zoomToFitSelectionOrAll for toggling between selection and full view). Total: 6 new RPC methods on ArrangerHandler.

**Decisions:** D-1.1, D-1.2, D-1.3

| Batch | Version | Title | Delegation | Status |
|-------|---------|-------|------------|--------|
| 35.1 | `0.35.1` | ArrangerHandler lane zoom + timeline navigation | in-session | done |
| 35.2 | `0.35.2` | Unit tests | in-session | done |
| 35.3 | `0.35.3` | Tool definitions + system prompt + smoke tests | in-session | done |
| 35.4 | `0.35.4` | Build verification | in-session | done |

### Batch 35.1 — ArrangerHandler lane zoom + timeline navigation

**Delegation:** in-session
**Decisions:** D-1.2, D-1.3
**Files:**
- `gig-maestro/src/main/java/dev/gregross/gig/handlers/ArrangerHandler.java` (MODIFY)
- `gig-maestro/src/main/java/dev/gregross/gig/extension/GigMaestroExtension.java` (MODIFY)
**Work:**
1. In GigMaestroExtension, create the ScrollbarModel: `var scrollbar = arranger.getHorizontalScrollbarModel()` and pass it to ArrangerHandler (modify constructor).
2. In ArrangerHandler, add 6 RPC methods:
   - `arranger/zoomInLanes` — no params, calls `arranger.zoomInLaneHeightsAll()`
   - `arranger/zoomOutLanes` — no params, calls `arranger.zoomOutLaneHeightsAll()`
   - `arranger/zoomInSelectedLanes` — no params, calls `arranger.zoomInLaneHeightsSelected()`
   - `arranger/zoomOutSelectedLanes` — no params, calls `arranger.zoomOutLaneHeightsSelected()`
   - `arranger/zoomToRegion` — params: `{from: double, to: double}` (beat positions), calls `scrollbar.zoomToContentRegion(from, to)`
   - `arranger/zoomToFitSelectionOrAll` — no params, calls `scrollbar.zoomToFitSelectionOrAll()`
**Test criteria:** Unit tests in batch 35.2
**Acceptance:** 6 new RPC methods registered

### Batch 35.2 — Unit tests

**Delegation:** in-session
**Depends on:** Batch 35.1
**Files:**
- `gig-maestro/src/test/java/dev/gregross/gig/handlers/ArrangerHandlerTest.java` (MODIFY)
**Work:**
1. Add mock for ScrollbarModel
2. Update method count assertion
3. Add registration test for 6 new methods
4. Add behavioral tests: zoomInLanes, zoomOutLanes, zoomInSelectedLanes, zoomOutSelectedLanes call arranger methods; zoomToRegion calls scrollbar.zoomToContentRegion; zoomToFitSelectionOrAll calls scrollbar.zoomToFitSelectionOrAll
**Test criteria:** `./gradlew :gig-maestro:test` passes
**Acceptance:** All new methods have test coverage

### Batch 35.3 — Tool definitions + system prompt + smoke tests

**Delegation:** in-session
**Depends on:** Batch 35.2
**Files:**
- `gig-maestro/tools/claude-tools.json` (MODIFY)
- `gig-maestro/tools/system-prompt.md` (MODIFY)
- `gig-maestro/scripts/tests/offline-schemas.sh` (MODIFY)
**Work:**
1. Add tool definitions for 6 new methods
2. Update system prompt with Arranger Lane Zoom and Timeline Navigation sections
3. Add smoke test assertions
**Test criteria:** `gig-maestro/scripts/smoke-test.sh --offline` passes
**Acceptance:** 6 new tools in schema, documented in prompt

### Batch 35.4 — Build verification

**Delegation:** in-session
**Depends on:** Batch 35.3
**Files:** None
**Work:** Run `./gradlew :gig-maestro:shadowJar` and `./gradlew :gig-maestro:test`
**Test criteria:** Both commands exit 0
**Acceptance:** Extension builds and all tests pass

**Phase Acceptance Criteria:**
- [ ] 4 lane zoom methods registered and callable
- [ ] arranger/zoomToRegion navigates to beat range
- [ ] arranger/zoomToFitSelectionOrAll toggles fit mode
- [ ] All unit tests pass
- [ ] Tool schemas and system prompt updated
- [ ] Offline smoke tests pass
- [ ] Clean build with shadowJar

**Completion triggers Phase 36 -> version `0.36.0`**
