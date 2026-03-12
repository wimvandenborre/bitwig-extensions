# Decisions

> Append-only log. Never delete entries — amend or revise instead.
> Active decisions for the current phase live here.
> When a phase completes, its decisions are archived to `phases/`.

## 2026-03-12 — Audit Result: Which handlers need additional tests

**Decision:** Only BrowserHandler needs additional tests. All other 15 handlers (plus 2 utilities) have GOOD or better coverage ratios (1.25x+). BrowserHandler has 0.86x (18 tests for 21 endpoints) — missing behavioral tests for 11 Phase-18 endpoints (6 filter cursor, filterReset, 4 scrollResults directions).
**Rationale:** Systematic audit of all 18 handler files: ApplicationHandler 1.43x, ArrangerHandler 2.76x, ClipHandler 1.87x, DeviceHandler 2.52x, MasterHandler 1.80x, MasterDeviceHandler 2.27x, MacroHandler 5.80x, NoteHandler 2.36x, NoteInputHandler 1.60x, ProjectHandler 1.25x, SceneHandler 2.75x, SendHandler 2.33x, TrackHandler 1.29x, TransactionHandler 3.67x, TransportHandler 1.84x. BrowserHandler 0.86x is the sole outlier.
**Alternatives considered:** (1) Also expand TrackHandler (1.29x) and ProjectHandler (1.25x) — their ratios are acceptable given their method simplicity. (2) Do nothing — BrowserHandler's filter/scroll tests are important because they involve StateCache indirection that could hide bugs.
**Status:** ACTIVE
**ID:** D-12.1

## 2026-03-12 — StateCache Mocking: How to test filter cursor and result bank endpoints

**Decision:** Replace `new StateCache()` in BrowserHandlerTest with a mock StateCache. Stub `getFilterCursors()` to return an array of mock `CursorBrowserFilterItem` objects, `getFilterColumns()` to return mock `BrowserFilterColumn` array with wildcard items, and `getResultBank()` to return a mock `BrowserResultsItemBank`. This lets behavioral tests verify that the correct cursor/column/bank method is called.
**Rationale:** The existing test uses a real StateCache which has null cursors/columns/bank — calling filter endpoints would NPE. Switching to mock StateCache allows injecting mock objects at each level. The real StateCache only populates these during `init()` which requires a live Bitwig host.
**Alternatives considered:** (1) Use reflection to set StateCache fields — fragile, couples to internal field names. (2) Create StateCache with a test constructor — invasive production change for test-only benefit.
**Status:** ACTIVE
**ID:** D-12.2

## 2026-03-12 — Test Structure: How to organize the new tests

**Decision:** Add the new behavioral tests directly to the existing `BrowserHandlerTest.java`. Add mock StateCache and required chain mocks as new fields. Update `@BeforeEach` to use mock StateCache. Keep existing tests intact — they work with the dispatcher regardless of StateCache implementation.
**Rationale:** Single test class per handler is the established pattern. The existing 18 tests all pass through the dispatcher, which doesn't depend on StateCache for the Phase-17 endpoints. Phase-18 endpoints (filter/scroll) are the ones that need StateCache — adding them to the same file keeps the class cohesive.
**Alternatives considered:** (1) Create a separate BrowserHandlerPhase18Test — breaks the one-class-per-handler convention unnecessarily.
**Status:** ACTIVE
**ID:** D-12.3
