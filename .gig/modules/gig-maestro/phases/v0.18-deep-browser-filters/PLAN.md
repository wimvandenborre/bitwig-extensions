# Plan

> Living document — reflects what was done, not just what was intended.
> This file tracks the ACTIVE phase only. Completed phases are archived to `phases/`.

---

## Active Phase

### Phase 18 — Deep Browser Filters (v0.18.x)

> Add filter column navigation to the PopupBrowser API. Create cursor items for all 8 named filter columns (category, tag, creator, device, deviceType, fileType, location, smartCollection) with flat and hierarchical navigation. Add a result item bank for multi-result visibility. Extend the browser snapshot section with per-column filter state and result entry count.

**Decisions:** D-18.1, D-18.2, D-18.3, D-18.4, D-18.5

| Batch | Version | Title | Delegation | Status |
|-------|---------|-------|------------|--------|
| 18.1 | `0.18.1` | Filter cursor infrastructure + snapshot observers | in-session | pending |
| 18.2 | `0.18.2` | Result bank infrastructure + snapshot | in-session | pending |
| 18.3 | `0.18.3` | BrowserHandler — 10 new RPC methods | in-session | pending |
| 18.4 | `0.18.4` | Unit tests | in-session | pending |
| 18.5 | `0.18.5` | Tool schemas + system prompt update | in-session | pending |
| 18.6 | `0.18.6` | Smoke tests | in-session | pending |

### Batch 18.1 — Filter cursor infrastructure + snapshot observers

**Delegation:** in-session
**Decisions:** D-18.1, D-18.2, D-18.3
**Files:** `StateCache.java`, `GigMaestroExtension.java`
**Work:**
- In StateCache, add filter state fields for 8 columns:
  - `filterExists[8]` (boolean), `filterNames[8]` (String), `filterHitCounts[8]` (int), `filterEntryCounts[8]` (int)
- Define column name→index mapping: `FILTER_COLUMNS = {"category":0, "tag":1, "creator":2, "device":3, "deviceType":4, "fileType":5, "location":6, "smartCollection":7}`
- Create `registerFilterObservers(PopupBrowser)` method:
  - For each of the 8 named columns (`categoryColumn()`, `tagColumn()`, etc.):
    - Create cursor item via `column.createCursorItem()`
    - Observe: `exists()`, cursor `name()`, cursor `hitCount()`, column `entryCount()`
  - Store cursor items in an array for use by BrowserHandler
- Add `getFilterCursors()` method returning the cursor item array (for BrowserHandler)
- Extend `getBrowserState()` to include a `"filters"` sub-object with per-column state
- Add `"browser"` to changed sections detection (already exists — just verify filter changes propagate)
- Wire `registerFilterObservers(popupBrowser)` call in GigMaestroExtension
**Test criteria:** `./gradlew test` passes — existing tests still pass
**Acceptance:** Filter state appears in browser snapshot section

### Batch 18.2 — Result bank infrastructure + snapshot

**Delegation:** in-session (depends on 18.1)
**Decisions:** D-18.5
**Files:** `StateCache.java`, `GigMaestroExtension.java`
**Work:**
- Add result bank state fields: `resultBankNames[8]` (String), `resultBankSelected[8]` (boolean), `resultsEntryCount` (int)
- In `registerBrowserObservers()` (or new method), create result item bank via `popupBrowser.resultsColumn().createItemBank(8)`
- Observe bank items (8): `name()`, `isSelected()` per item
- Observe `resultsColumn().entryCount()`
- Store bank reference for BrowserHandler scroll methods
- Add `resultsEntryCount` to browser snapshot
- Add `getResultBank()` returning the bank reference
- Add `getResultBankState()` method for the getResults RPC
**Test criteria:** `./gradlew test` passes
**Acceptance:** `resultsEntryCount` appears in browser snapshot

### Batch 18.3 — BrowserHandler — 10 new RPC methods

**Delegation:** in-session (depends on 18.1, 18.2)
**Decisions:** D-18.1, D-18.4, D-18.5
**Files:** `BrowserHandler.java`, `GigMaestroExtension.java`
**Work:**
- Add column resolution helper: `resolveFilterColumn(String column)` → maps column name to cursor item from StateCache
- Register 8 filter navigation methods (all accept `column` param):
  - `browser/filterSelectNext` — cursor `selectNext()`
  - `browser/filterSelectPrevious` — cursor `selectPrevious()`
  - `browser/filterSelectFirst` — cursor `selectFirst()`
  - `browser/filterSelectLast` — cursor `selectLast()`
  - `browser/filterSelectParent` — cursor `selectParent()`
  - `browser/filterSelectFirstChild` — cursor `selectFirstChild()`
  - `browser/filterReset` — select wildcard item via `column.getWildcardItem().isSelected().set(true)`
  - `browser/getFilters` — return all filter column states from StateCache
- Register 2 result bank methods:
  - `browser/getResults` — return result bank state (8 items with name + isSelected)
  - `browser/scrollResults` — accept `direction` param (forward/backward/pageForward/pageBackward)
- Update BrowserHandler constructor to accept filter cursors array, wildcard items array, and result bank
- Update GigMaestroExtension wiring
**Test criteria:** `./gradlew shadowJar` compiles
**Acceptance:** All 10 methods registered, build succeeds (total: 21 browser methods)

### Batch 18.4 — Unit tests

**Delegation:** in-session (depends on 18.3)
**Decisions:** D-18.1, D-18.4
**Files:** `BrowserHandlerTest.java`
**Work:**
- Update registration count test: 11 → 21
- Add method name assertions for all 10 new methods
- Add validation tests: filter methods require `column` param, invalid column name returns error
- Add validation: `browser/scrollResults` requires `direction` param, invalid direction returns error
**Test criteria:** `./gradlew test` — all tests pass
**Acceptance:** All new methods have test coverage

### Batch 18.5 — Tool schemas + system prompt update

**Delegation:** in-session (depends on 18.3)
**Decisions:** D-18.1, D-18.3, D-18.5
**Files:** `tools/claude-tools.json`, `tools/system-prompt.md`
**Work:**
- Add 10 tool schemas to claude-tools.json (154→164 tools)
- Update `session_snapshot` description to mention filter columns and result bank
- Add "Browser Filter Navigation" subsection to system-prompt.md covering:
  - Column names and what they filter
  - Cursor navigation (flat + hierarchical)
  - Wildcard reset workflow
  - Result bank scanning
  - Filter + result combined workflow example
**Test criteria:** JSON validates
**Acceptance:** Tool schemas match RPC methods, system prompt updated

### Batch 18.6 — Smoke tests

**Delegation:** in-session (depends on 18.4, 18.5)
**Decisions:** D-18.1
**Files:** `scripts/smoke-test.sh`
**Work:**
- Add Phase 18 offline tests:
  - Tool schema presence for all 10 new methods
  - Field type assertions on tool schemas (column enum, direction enum)
  - System prompt content assertions
- Add Phase 18 online tests:
  - `api/list` includes all 10 new methods
  - `session/snapshot` browser section includes `filters` object
  - `browser/getFilters` returns filter state for 8 columns
  - `browser/getResults` returns result bank items
  - `browser/filterSelectNext` with valid column returns ok
  - `browser/filterSelectNext` with invalid column returns error
  - `browser/scrollResults` with valid direction returns ok
**Test criteria:** `./scripts/smoke-test.sh --offline` passes
**Acceptance:** All offline smoke tests pass

**Phase Acceptance Criteria:**
- [ ] 8 filter cursor items created and observed
- [ ] 8-item result bank created and observed
- [ ] 10 new RPC methods registered (21 total browser methods)
- [ ] Filter state in browser snapshot (4 fields × 8 columns)
- [ ] resultsEntryCount in browser snapshot
- [ ] All unit tests pass
- [ ] All offline smoke tests pass
- [ ] Tool schemas updated (154→164)
- [ ] System prompt documents filter navigation

**Completion triggers Phase 19 -> version `0.19.0`**
