# Plan

> Living document — reflects what was done, not just what was intended.
> This file tracks the ACTIVE phase only. Completed phases are archived to `phases/`.

---

## Active Phase

### Phase 17 — PopupBrowser API Integration (v0.17.x)

> Add programmatic browser access to Bitwig via the PopupBrowser API. Starts with a spike to validate headless preset cycling (the replacement for deprecated `switchToNextPreset*()` methods from ISS-007), then builds BrowserHandler with result navigation, content type switching, filter observation, and commit/cancel. Adds a "browser" snapshot section for state visibility.

**Decisions:** D-17.1, D-17.2, D-17.3, D-17.4, D-17.5

| Batch | Version | Title | Delegation | Status |
|-------|---------|-------|------------|--------|
| 17.1 | `0.17.1` | Spike: validate PopupBrowser headless behavior | in-session | pending |
| 17.2 | `0.17.2` | Browser snapshot observers | in-session | pending |
| 17.3 | `0.17.3` | BrowserHandler — RPC methods | in-session | pending |
| 17.4 | `0.17.4` | Unit tests | in-session | pending |
| 17.5 | `0.17.5` | Tool schemas + system prompt update | in-session | pending |
| 17.6 | `0.17.6` | Smoke tests | in-session | pending |

### Batch 17.1 — Spike: validate PopupBrowser headless behavior

**Delegation:** in-session
**Decisions:** D-17.1
**Files:** `GigMaestroExtension.java` (temporary spike code)
**Work:**
- Create `PopupBrowser` via `host.createPopupBrowser()` in `init()`
- Register minimal observers: `exists()`, `title()`, `selectedContentTypeName()`, `contentTypeNames()`
- Create a temporary RPC method `browser/spike` that:
  1. Returns current browser state (exists, title, contentTypes)
  2. Calls `selectNextFile()` and returns what changed
  3. Reports whether `exists()` is true/false at rest
- Build, deploy to Bitwig, test manually via curl
- **Key questions to answer:**
  - Does `exists()` return true at rest (browser not visible)?
  - Does `selectNextFile()` cycle presets without opening the browser UI?
  - What do `contentTypeNames` contain?
  - Does `commit()` do anything when browser is not visually open?
- Document findings in STATE.md working memory
- Remove spike code after testing (or keep if useful)
**Test criteria:** Extension builds and loads in Bitwig; spike method responds
**Acceptance:** Clear answers to all 4 key questions documented

### Batch 17.2 — Browser snapshot observers

**Delegation:** in-session (depends on 17.1 findings)
**Decisions:** D-17.2, D-17.5
**Files:** `StateCache.java`, `GigMaestroExtension.java`
**Work:**
- Add browser state fields to StateCache:
  - `browserExists` (boolean), `browserTitle` (String), `browserSelectedContentType` (String)
  - `browserContentTypeNames` (String[]), `browserCanAudition` (boolean), `browserShouldAudition` (boolean)
  - `browserResultName` (String), `browserResultIsSelected` (boolean)
- Create `registerBrowserObservers(PopupBrowser)` method in StateCache
- Create `CursorBrowserResultItem` via `resultsColumn().createCursorItem()` for result tracking
- Add `getBrowserState()` private method for snapshot building
- Add `"browser"` section to `getSnapshot()` and `getChangedSections()`
- Wire PopupBrowser creation + observer registration in GigMaestroExtension
**Test criteria:** `./gradlew test` passes — existing tests still pass
**Acceptance:** Browser section appears in snapshot

### Batch 17.3 — BrowserHandler — RPC methods

**Delegation:** in-session (depends on 17.2)
**Decisions:** D-17.2, D-17.3, D-17.4
**Files:** `BrowserHandler.java` (new), `GigMaestroExtension.java`
**Work:**
- Create BrowserHandler with PopupBrowser + StateCache dependencies
- Register ~9 RPC methods:
  - `browser/selectNextFile` — `popupBrowser.selectNextFile()`
  - `browser/selectPreviousFile` — `popupBrowser.selectPreviousFile()`
  - `browser/selectFirstFile` — `popupBrowser.selectFirstFile()`
  - `browser/selectLastFile` — `popupBrowser.selectLastFile()`
  - `browser/commit` — `popupBrowser.commit()`
  - `browser/cancel` — `popupBrowser.cancel()`
  - `browser/setContentType` — `popupBrowser.selectedContentTypeIndex().set(index)`
  - `browser/setShouldAudition` — `popupBrowser.shouldAudition().set(enabled)`
  - `browser/getState` — return browser state from stateCache
- Wire BrowserHandler in GigMaestroExtension
- Adjust method count based on spike findings (17.1)
**Test criteria:** `./gradlew shadowJar` compiles
**Acceptance:** All methods registered, build succeeds

### Batch 17.4 — Unit tests

**Delegation:** in-session (depends on 17.3)
**Decisions:** D-17.2, D-17.3
**Files:** `BrowserHandlerTest.java` (new)
**Work:**
- Create BrowserHandlerTest: registration count, method name assertions
- Validate parameter requirements: `browser/setContentType` requires `index`, `browser/setShouldAudition` requires `enabled`
**Test criteria:** `./gradlew test` — all tests pass
**Acceptance:** All new methods have test coverage

### Batch 17.5 — Tool schemas + system prompt update

**Delegation:** in-session (depends on 17.3)
**Decisions:** D-17.3, D-17.5
**Files:** `tools/claude-tools.json`, `tools/system-prompt.md`
**Work:**
- Add ~9 tool schemas to claude-tools.json (143→~152 tools)
- Update `session_snapshot` description to mention browser section
- Add "Browser & Preset Navigation" section to system-prompt.md covering:
  - Browser state observation (exists, title, contentType)
  - Preset cycling workflow (selectNextFile → commit)
  - Content type switching for device/preset/sample browsing
  - Audition toggle
**Test criteria:** JSON validates
**Acceptance:** Tool schemas match RPC methods, system prompt updated

### Batch 17.6 — Smoke tests

**Delegation:** in-session (depends on 17.4, 17.5)
**Decisions:** D-17.1
**Files:** `scripts/smoke-test.sh`
**Work:**
- Add Phase 17 offline tests:
  - Tool schema presence for all browser methods
  - Field type assertions on tool schemas
  - System prompt content assertions
- Add Phase 17 online tests:
  - `api/list` includes all browser methods
  - `session/snapshot` returns browser section with expected fields
  - `browser/getState` returns state object
  - `browser/setShouldAudition` with true/false
  - `browser/setContentType` with valid index
  - `browser/selectNextFile` / `browser/selectPreviousFile` execute without error
**Test criteria:** `./scripts/smoke-test.sh --offline` passes
**Acceptance:** All offline smoke tests pass

**Phase Acceptance Criteria:**
- [ ] Spike documents PopupBrowser headless behavior
- [ ] ~9 new RPC methods registered and callable
- [ ] Browser snapshot section with 8 fields
- [ ] BrowserHandler created
- [ ] All unit tests pass
- [ ] All offline smoke tests pass
- [ ] Tool schemas updated
- [ ] System prompt documents browser navigation

**Completion triggers Phase 18 -> version `0.18.0`**
