# Decisions

> Append-only log. Never delete entries — amend or revise instead.
> Active decisions for the current phase live here.
> When a phase completes, its decisions are archived to `phases/`.

<!-- Decision statuses:
  PROPOSED  — Claude's recommendation, awaiting user approval
  ACTIVE    — Approved and in effect
  AMENDED   — Overridden by user (original preserved, new entry appended)
  REVISED   — Claude revised based on new information (original preserved)
-->

<!-- Entry format:
## YYYY-MM-DD — Domain: Question

**Decision:** What was decided.
**Rationale:** Why this choice was made.
**Alternatives considered:** What else was evaluated.
**Status:** ACTIVE | ACTIVE | AMENDED | REVISED
**ID:** D-{batch}.{num}
-->

## 2026-03-02 — Scope: Which filter columns to expose?

**Decision:** Expose all 8 named filter columns via a **generic column parameter** pattern. RPC methods accept a `column` string param (`"category"`, `"tag"`, `"creator"`, `"device"`, `"deviceType"`, `"fileType"`, `"location"`, `"smartCollection"`). This avoids 8× method duplication — one `browser/selectFilterItem` method handles any column.
**Rationale:** PopupBrowser exposes 8 named column accessors (not indexed). Creating per-column methods (8×N) would bloat the API. A column parameter maps cleanly to the named accessors via a switch/map lookup. Column availability varies by content type — `column.exists()` handles unavailable columns gracefully with an error response.
**Alternatives considered:** (1) Only expose category + tag + creator (most common) — limits agent flexibility for device type/location filtering. (2) Per-column methods (e.g., `browser/selectCategoryNext`) — 40+ methods for 8 columns × 5 navigation ops = API bloat.
**Status:** ACTIVE
**ID:** D-18.1

## 2026-03-02 — Navigation: Cursor vs bank approach?

**Decision:** Use **cursor-based navigation** via `CursorBrowserFilterItem` per column, not bank-based. Create one cursor item per filter column at init time. Expose navigation methods: `selectNext`, `selectPrevious`, `selectFirst`, `selectLast`, `selectParent`, `selectFirstChild`. Bank-based scrolling deferred — cursor navigation is sufficient for programmatic agents that don't need to display paginated lists.
**Rationale:** Cursor navigation is simpler and matches the existing device/track cursor patterns in the codebase. An agent iterates filters sequentially — it doesn't need a scrollable window. Banks add complexity (bank size, scroll position, item arrays) without clear agent value. Cursor items support hierarchical navigation (parent/child) which is critical for nested filter categories.
**Alternatives considered:** (1) Bank-based only — more data at once but overkill for sequential agent navigation. (2) Both cursor + bank — complexity without clear need in this phase.
**Status:** ACTIVE
**ID:** D-18.2

## 2026-03-02 — Observation: What filter state goes in the snapshot?

**Decision:** Add a `"filters"` sub-object inside the existing `"browser"` snapshot section. For each of the 8 columns, include: `exists` (boolean), `name` (string — cursor item name), `hitCount` (int — results matching this filter), `entryCount` (int — total entries in column). This gives the agent visibility into what filters are available and what's currently selected, without overwhelming the snapshot.
**Rationale:** 4 fields × 8 columns = 32 fields, but structured as a nested object it's clean. `entryCount` tells the agent how many filter values exist. `hitCount` tells how many results match the current filter. `name` shows what's currently selected. `exists` handles columns that aren't available for the current content type.
**Alternatives considered:** (1) No filter state — agent has no visibility, must call getFilters explicitly. (2) Full item bank arrays — too much data, snapshot would be huge.
**Status:** ACTIVE
**ID:** D-18.3

## 2026-03-02 — Methods: What RPC methods to add?

**Decision:** Add 8 RPC methods to BrowserHandler: `browser/filterSelectNext`, `browser/filterSelectPrevious`, `browser/filterSelectFirst`, `browser/filterSelectLast`, `browser/filterSelectParent`, `browser/filterSelectFirstChild`, `browser/filterReset` (selects wildcard/all), `browser/getFilters` (returns all filter column states). All methods accept a `column` string parameter. Total: 8 new methods (Phase 17 had 11 → Phase 18 brings to 19 browser methods).
**Rationale:** The 6 navigation methods cover flat and hierarchical browsing. `filterReset` is essential — agents need to clear a filter back to "All" (wildcard). `getFilters` provides on-demand filter state without waiting for snapshot. These 8 methods give complete filter navigation capability.
**Alternatives considered:** (1) Fewer methods — skip parent/child, limits hierarchical browsing. (2) Add `filterSetByName` (search by name) — API doesn't support text search on filter items, only cursor navigation.
**Status:** ACTIVE
**ID:** D-18.4

## 2026-03-02 — Results: Add result bank for richer browsing?

**Decision:** Add a **result item bank** (size 8) to complement the existing result cursor. This gives the agent visibility into multiple results at once — not just the single cursor item. Add `browser/getResults` method returning the bank window (8 items with name + isSelected). Add `browser/scrollResults` method (direction: forward/backward/pageForward/pageBackward). Also add `resultsEntryCount` to the browser snapshot section.
**Rationale:** The Phase 17 result cursor only shows one item at a time. With filters, the agent needs to see what's available in the results. A bank of 8 items lets the agent scan results efficiently. `entryCount` tells the agent how many total results match current filters.
**Alternatives considered:** (1) No result bank — agent navigates blindly one item at a time. (2) Larger bank (16+) — diminishing returns, 8 is sufficient for agent scanning.
**Status:** ACTIVE
**ID:** D-18.5
