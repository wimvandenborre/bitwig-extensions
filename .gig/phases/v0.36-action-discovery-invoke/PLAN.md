# Plan

> Living document — reflects what was done, not just what was intended.
> This file tracks the ACTIVE phase only. Completed phases are archived to `phases/`.

---

## Active Phase

### Phase 36 — Action Discovery & Invoke (v0.36.x)

> Expose Bitwig's generic Action system via RPC, enabling Claude to discover and invoke any Bitwig menu command or keyboard shortcut by ID. Also completes Application API coverage with track group navigation.

**Decisions:** D-1.1, D-1.2, D-1.3, D-1.4, D-1.5

| Batch | Version | Title | Delegation | Status |
|-------|---------|-------|------------|--------|
| 36.1 | `0.36.1` | ApplicationHandler: action RPCs + track group nav | in-session | done |
| 36.2 | `0.36.2` | Unit tests | in-session | done |
| 36.3 | `0.36.3` | Tool definitions + system prompt + smoke tests | in-session | done |
| 36.4 | `0.36.4` | Build verification | in-session | done |

### Batch 36.1 — ApplicationHandler: action RPCs + track group nav

**Delegation:** in-session
**Decisions:** D-1.1, D-1.2, D-1.3, D-1.4
**Files:** `gig-maestro/src/main/java/dev/gregross/gig/handlers/ApplicationHandler.java`
**Work:**
- Add `action/list` — calls `application.getActions()`, returns flat JSON array of `{id, name, category, menuItemText}`. Optional `category` param filters by category name.
- Add `action/listCategories` — calls `application.getActionCategories()`, returns array of `{id, name}`.
- Add `action/invoke` — takes `{id}`, calls `application.getAction(id)`, null check → RPC error -32001, otherwise `action.invoke()`.
- Add `app/navigateIntoTrackGroup` — takes `{trackIndex}`, calls `application.navigateIntoTrackGroup(track)`.
- Add `app/navigateToParentTrackGroup` — no params, calls `application.navigateToParentTrackGroup()`.
- Add necessary imports: `Action`, `ActionCategory`, `JsonArray`, `RpcException`, `TrackBank` (or pass track reference).
**Test criteria:** `./gradlew :gig-maestro:test` passes
**Acceptance:** 5 new RPC methods registered in ApplicationHandler

### Batch 36.2 — Unit tests

**Delegation:** in-session
**Decisions:** D-1.1, D-1.2, D-1.3
**Files:** `gig-maestro/src/test/java/dev/gregross/gig/handlers/ApplicationHandlerTest.java`, `gig-maestro/src/test/java/dev/gregross/gig/handlers/HandlerRegistrationIntegrationTest.java`
**Work:**
- Add tests for action/list (with and without category filter)
- Add tests for action/listCategories
- Add tests for action/invoke (valid ID, null/invalid ID → error)
- Add tests for app/navigateIntoTrackGroup and app/navigateToParentTrackGroup
- Update HandlerRegistrationIntegrationTest method count
**Test criteria:** `./gradlew :gig-maestro:test` — all tests pass
**Acceptance:** 6+ new test methods covering all new RPCs

### Batch 36.3 — Tool definitions + system prompt + smoke tests

**Delegation:** in-session
**Decisions:** D-1.1, D-1.2, D-1.3, D-1.4
**Files:** `gig-maestro/tools/claude-tools.json`, `gig-maestro/tools/system-prompt.md`, `gig-maestro/scripts/tests/offline-schemas.sh`
**Work:**
- Add tool definitions for all 5 new methods in claude-tools.json
- Add "Action Discovery & Invoke" + "Track Group Navigation" sections to system prompt
- Add schema checks in offline-schemas.sh (tool existence, param checks, prompt section checks)
- Update tool count threshold
**Test criteria:** `./scripts/smoke-test.sh --offline` passes
**Acceptance:** All new tools documented, prompt updated, smoke tests green

### Batch 36.4 — Build verification

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
- [ ] 5 new RPC methods: action/list, action/listCategories, action/invoke, app/navigateIntoTrackGroup, app/navigateToParentTrackGroup
- [ ] Unit tests cover all new methods including error cases
- [ ] Tool definitions and system prompt documented
- [ ] Offline smoke tests updated and passing
- [ ] Clean build with shadowJar

**Completion triggers Phase 37 → version `0.37.0`**
