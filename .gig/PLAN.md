# Plan

> Living document — reflects what was done, not just what was intended.
> This file tracks the ACTIVE phase only. Completed phases are archived to `phases/`.

---

## Active Phase

### Phase 37 — DetailEditor Panel Control (v0.37.x)

> Expose the Bitwig DetailEditor (note editor/detail panel) zoom and navigation via RPC, independently from the arranger. Enables Claude to control the note editor view for precise editing workflows.

**Decisions:** D-1.1, D-1.2, D-1.3, D-1.4

| Batch | Version | Title | Delegation | Status |
|-------|---------|-------|------------|--------|
| 37.1 | `0.37.1` | DetailEditorHandler + extension wiring | in-session | done |
| 37.2 | `0.37.2` | Unit tests | in-session | done |
| 37.3 | `0.37.3` | Tool definitions + system prompt + smoke tests | in-session | done |
| 37.4 | `0.37.4` | Build verification | in-session | done |

### Batch 37.1 — DetailEditorHandler + extension wiring

**Delegation:** in-session
**Decisions:** D-1.1, D-1.2, D-1.4
**Files:** `gig-maestro/src/main/java/dev/gregross/gig/handlers/DetailEditorHandler.java` (NEW), `gig-maestro/src/main/java/dev/gregross/gig/extension/GigMaestroExtension.java`
**Work:**
- Create `DetailEditorHandler` with `DetailEditor` and its `ScrollbarModel` as constructor params.
- Register 8 RPC methods: detailEditor/zoomIn, detailEditor/zoomOut, detailEditor/zoomToFit, detailEditor/zoomToSelection, detailEditor/zoomToFitSelectionOrAll, detailEditor/zoomInLanes, detailEditor/zoomOutLanes, detailEditor/zoomToRegion (from/to params).
- In GigMaestroExtension: create DetailEditor via `host.createDetailEditor()`, wire handler.
**Test criteria:** `./gradlew :gig-maestro:compileJava` passes
**Acceptance:** 8 new RPC methods registered

### Batch 37.2 — Unit tests

**Delegation:** in-session
**Decisions:** D-1.1, D-1.2
**Files:** `gig-maestro/src/test/java/dev/gregross/gig/handlers/DetailEditorHandlerTest.java` (NEW), `gig-maestro/src/test/java/dev/gregross/gig/extension/HandlerRegistrationIntegrationTest.java`
**Work:**
- Create DetailEditorHandlerTest with mock DetailEditor + ScrollbarModel
- Test all 8 methods: verify correct Bitwig API calls
- Test zoomToRegion param validation (missing from/to)
- Update HandlerRegistrationIntegrationTest to include DetailEditorHandler
**Test criteria:** `./gradlew :gig-maestro:test` — all tests pass
**Acceptance:** 10+ test methods covering all RPCs + error cases

### Batch 37.3 — Tool definitions + system prompt + smoke tests

**Delegation:** in-session
**Decisions:** D-1.1, D-1.2
**Files:** `gig-maestro/tools/claude-tools.json`, `gig-maestro/tools/system-prompt.md`, `gig-maestro/scripts/tests/offline-schemas.sh`
**Work:**
- Add 8 tool definitions in claude-tools.json
- Add "DetailEditor Panel Control" section to system prompt
- Add smoke test checks (tool existence, param checks, prompt checks)
- Update tool count threshold
**Test criteria:** `./scripts/smoke-test.sh --offline` passes
**Acceptance:** All tools documented, prompt updated, smoke tests green

### Batch 37.4 — Build verification

**Delegation:** in-session
**Decisions:** all
**Files:** none
**Work:**
- Run `./gradlew :gig-maestro:shadowJar` — clean build
- Run `./gradlew :gig-maestro:test` — all tests pass
- Run `./scripts/smoke-test.sh --offline` — offline smoke tests pass
**Test criteria:** All three commands exit 0
**Acceptance:** Clean build, all tests green

**Phase Acceptance Criteria:**
- [ ] New DetailEditorHandler with 8 RPC methods
- [ ] Unit tests cover all methods including error cases
- [ ] Tool definitions and system prompt documented
- [ ] Offline smoke tests updated and passing
- [ ] Clean build with shadowJar

**Completion triggers Phase 38 → version `0.38.0`**
