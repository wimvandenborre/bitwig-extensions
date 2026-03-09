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

## 2026-03-02 — Scope: What should Phase 17 cover?

**Decision:** Start with a **spike batch** to test PopupBrowser behavior, then build infrastructure + observation + navigation methods. The API has no explicit `open()` method — `selectNextFile()`/`selectPreviousFile()` may or may not cycle presets without the browser UI being visible. The spike will determine: (a) does `selectNextFile()` work as headless preset cycling, (b) what `exists()` returns at rest, (c) whether content type switching triggers the browser. After the spike, build BrowserHandler with result navigation, content type switching, filter observation, and commit/cancel. Target: ~12 RPC methods + browser snapshot section.
**Rationale:** The deprecated preset cycling methods (ISS-007) were the only runtime-broken API methods we've encountered. PopupBrowser is the official replacement but its programmatic behavior is undocumented. A spike prevents building on false assumptions.
**Alternatives considered:** (1) Skip spike, build everything — risky, could waste an entire phase on methods that don't work headlessly. (2) Only add observation — too conservative, won't replace preset cycling.
**Status:** ACTIVE
**ID:** D-17.1

## 2026-03-02 — Architecture: Where does browser code live?

**Decision:** Create a new **BrowserHandler** class. `PopupBrowser` is created via `host.createPopupBrowser()` in `GigMaestroExtension.init()` and passed to BrowserHandler. Add a `"browser"` section to the StateCache snapshot. Register observers for: `exists`, `title`, `selectedContentTypeName`, `contentTypeNames`, `canAudition`, `shouldAudition`, plus a results cursor item (`name`, `isSelected`).
**Rationale:** PopupBrowser is a distinct API object with its own lifecycle — separate handler follows the existing one-handler-per-API-object pattern. Snapshot observation lets the agent know browser state without polling.
**Alternatives considered:** (1) Put in DeviceHandler — browser is not device-specific, it handles presets, samples, devices, etc. (2) Put in ApplicationHandler — browser is not an application concept.
**Status:** ACTIVE
**ID:** D-17.2

## 2026-03-02 — Navigation: How to expose result browsing?

**Decision:** Expose 6 result navigation methods: `browser/selectNextFile`, `browser/selectPreviousFile`, `browser/selectFirstFile`, `browser/selectLastFile`, `browser/commit`, `browser/cancel`. These map directly to PopupBrowser methods. Also expose `browser/setContentType` to switch between Device/Preset/Sample/etc. tabs. Total: 7 core methods.
**Rationale:** `selectNextFile`/`selectPreviousFile` are the direct replacements for the deprecated `switchToNextPreset()`/`switchToPreviousPreset()`. `commit()` applies the selection, `cancel()` closes the browser. Content type switching enables targeting specific browser tabs programmatically.
**Alternatives considered:** (1) Expose full cursor-based navigation with filter column cursors — too complex for first iteration, add in a future phase if needed. (2) Only selectNext/Previous — need commit/cancel for complete workflows.
**Status:** ACTIVE
**ID:** D-17.3

## 2026-03-02 — Filters: How deep should filter access go?

**Decision:** For Phase 17, expose filter columns as **read-only observation** in the browser snapshot section — show the name of each filter column and its current entry count. Add 2 filter navigation methods: `browser/selectFilterItem` (set a filter column's cursor to next/previous) and `browser/getFilters` (return current filter state). Defer deep filter manipulation (hierarchical parent/child navigation, bank-based browsing) to a future phase.
**Rationale:** Filter observation gives the agent enough context to understand what's available. Deep filter manipulation requires creating cursor items and banks for 8 filter columns — significant complexity that should wait until basic browser navigation is validated by the spike.
**Alternatives considered:** (1) No filter access at all — agent can't understand what's in the browser. (2) Full filter banks with hierarchical navigation — too much for first iteration.
**Status:** ACTIVE
**ID:** D-17.4

## 2026-03-02 — Observation: What goes in the browser snapshot section?

**Decision:** Add a `"browser"` section to `session/snapshot` with: `exists` (boolean — browser is active), `title` (string), `selectedContentTypeName` (string), `contentTypeNames` (string[] — available tabs), `canAudition` (boolean), `shouldAudition` (boolean), `resultName` (string — current result cursor item name), `resultIsSelected` (boolean). Create a `CursorBrowserResultItem` via `resultsColumn().createCursorItem()` for tracking the current result.
**Rationale:** These fields let the agent know: is the browser open, what tab is active, what's currently selected, and what content types are available. This is the minimum needed for headless preset cycling workflows.
**Alternatives considered:** (1) Include filter column entry counts — adds 8 more fields, defer unless spike shows it's needed. (2) No browser snapshot — agent has no visibility into browser state.
**Status:** ACTIVE
**ID:** D-17.5
