# Decisions

> Append-only log. Never delete entries — amend or revise instead.
> Active decisions for the current phase live here.
> When a phase completes, its decisions are archived to `phases/`.

## 2026-03-12 — Strategy: How to test StateCache without Bitwig

**Decision:** Use Java reflection to inject field values into StateCache, then test snapshot serialization and getter methods. This tests the most bug-prone code (JSON structure, field mapping, bounds checking) without requiring Bitwig API mocking or observer registration. Complement with ArgumentCaptor tests for one representative `register*Observers` method to prove callback wiring works.
**Rationale:** StateCache has 179 private volatile fields with no setters. The observer callbacks are trivial lambdas (`v -> field = v`) unlikely to have bugs. The serialization logic in `getSnapshot()` and 10+ `get*State()` methods is where bugs hide — field name typos, missing properties, wrong types, incorrect array indexing. Reflection lets us test this directly with zero Bitwig dependency.
**Alternatives considered:** (1) Full ArgumentCaptor mocking of all 13 register methods — too verbose (100+ callbacks), low ROI given callback simplicity. (2) Add package-private setters to production code — invasive change for test-only benefit. (3) Skip StateCache testing entirely — leaves the largest untested class (1,609 lines) uncovered.
**Status:** ACTIVE
**ID:** D-14.1

## 2026-03-12 — Snapshot Tests: What JSON structure to validate

**Decision:** Create `StateCacheSnapshotTest` that uses a reflection helper to set known values, then validates all 13 snapshot sections. For each section: verify all expected JSON keys exist, values match injected data, nested objects have correct structure (e.g., color → {r, g, b}), arrays have correct lengths (8 tracks, 5 scenes, 8 params, 16 cue markers). Test one section per test method for clear failure isolation.
**Rationale:** getSnapshot() is 100+ lines of JSON assembly with manual field-to-property mapping. A single typo (e.g., `addProperty("isPlying", isPlaying)`) would silently produce wrong data. Validating the complete schema catches these errors. One test per section keeps failures localized.
**Alternatives considered:** (1) One big test for all 13 sections — harder to diagnose failures. (2) JSON schema validation library — over-engineering for known structure.
**Status:** ACTIVE
**ID:** D-14.2

## 2026-03-12 — Getter Tests: What individual methods to test

**Decision:** Create `StateCacheGetterTest` testing all public getter methods: `getTrackName` (valid index, out-of-bounds), `clipHasContent` (valid indices, out-of-bounds), `getClipStepSize`/`setClipStepSize`, `hasSoloedTracks`/`hasMutedTracks`/`hasArmedTracks`, `isModified`, `getSceneBankScrollInfo`/`getTrackBankScrollInfo`/`getCueMarkerBankScrollInfo` (JSON structure), `getSceneItemCount`/`getTrackItemCount`/`getCueMarkerItemCount`, plus all specialized state methods (`getBrowserState`, `getResultBankState`, `getClipLaunchSettings`, `getClipPlaybackSettings`, `getClipLauncherSettings`, `getArpeggiatorState`, `getNoteLatchState`). Use reflection to set fields before each test.
**Rationale:** These getters include bounds checking (returning "" or false for invalid indices), type conversion, and JSON assembly. Each is a potential bug surface. Testing with known injected state verifies correct behavior.
**Alternatives considered:** (1) Only test getSnapshot — misses bounds checking logic in getTrackName/clipHasContent. (2) Test via observer callbacks — requires Bitwig API mocking, doesn't test the getter logic itself.
**Status:** ACTIVE
**ID:** D-14.3

## 2026-03-12 — Callback Tests: Selective observer verification

**Decision:** Create `StateCacheObserverTest` testing `registerObservers()` with transport mocks only. Mock Transport and capture callbacks via ArgumentCaptor for `isPlaying`, `tempo`, and `playPosition`. Invoke captured callbacks with known values, then verify fields updated via `getSnapshot()`. This proves the callback→field→snapshot pipeline works end-to-end for the most critical section. Do NOT test all 13 register methods — the pattern is identical across all.
**Rationale:** Testing one register method proves the pattern. Transport is the most frequently queried section and has the simplest mock chain (no nested banks). If transport callbacks work, the same lambda pattern works everywhere. Testing all 13 would add 50+ mock objects for diminishing returns.
**Alternatives considered:** (1) Test all 13 register methods — 100+ mocks, 100+ captors, extreme verbosity. (2) Skip callback testing entirely — misses the end-to-end callback→snapshot path. (3) Test registerClipObservers instead — more complex due to nested TrackBank→Track→ClipLauncherSlotBank chain.
**Status:** ACTIVE
**ID:** D-14.4

## 2026-03-12 — Test Organization: File structure

**Decision:** Create 3 new test files alongside existing `StateCacheDeltaTest`: `StateCacheSnapshotTest` (13 tests, one per section), `StateCacheGetterTest` (~20 tests for all getters), `StateCacheObserverTest` (~5 tests for transport callback pipeline). Share a `StateCacheTestHelper` utility with static methods for reflection-based field setting.
**Rationale:** Splitting by concern (snapshot structure, getter behavior, observer wiring) keeps each file focused and makes failures easy to diagnose. The helper class avoids duplicating reflection boilerplate across test files.
**Alternatives considered:** (1) One giant StateCacheTest — would exceed 500 lines, harder to navigate. (2) Add to existing StateCacheDeltaTest — mixes delta-specific tests with unrelated concerns.
**Status:** ACTIVE
**ID:** D-14.5
