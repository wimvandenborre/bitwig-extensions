# Decisions

> Append-only log. Never delete entries — amend or revise instead.
> Active decisions for the current phase live here.
> When a phase completes, its decisions are archived to `phases/`.

## 2026-03-08 — Scope: What to test

**Decision:** Focus exclusively on parameter validation tests and infrastructure unit tests. Do not introduce a mocking framework. Do not attempt to test methods that call Bitwig API directly.
**Rationale:** The existing test pattern passes `null` for all Bitwig API objects, limiting testable surface to: method registration, parameter validation (missing/invalid params that throw before reaching null API), and StateCache interactions (real object). ~40 RPC methods call API immediately with no prior validation — these are untestable without mocking. Introducing Mockito would be a significant architectural change better suited to a dedicated phase.
**Alternatives considered:** (a) Introduce Mockito for full behavioral testing — rejected, scope creep and different concern. (b) Test only new JsonParamValidator — rejected, too narrow when many handlers have testable validation gaps.
**Status:** ACTIVE
**ID:** D-4.1

## 2026-03-08 — Priority: Which handlers to expand

**Decision:** Expand tests for 5 handlers in priority order: (1) TrackHandler — 3 new validation tests, (2) NoteHandler — 8 new validation tests, (3) ClipHandler — 4 new validation tests, (4) ProjectHandler — 1 new StateCache test, (5) BrowserHandler — no new tests (remaining gaps are untestable without mocking).
**Rationale:** TrackHandler has 2 enum validators (crossfade mode, monitor mode) and missing-param tests not yet covered. NoteHandler has the richest untested validation: x/y range, chance range, occurrence enum, repeat ranges, recurrence length, and missing-array-param tests. ClipHandler has scene/launch index range validation and additional color range tests. ProjectHandler's `getState` is testable via real StateCache. BrowserHandler's remaining 13 untested methods are parameterless API calls — nothing to validate without mocking.
**Alternatives considered:** Equal effort across all handlers — rejected, BrowserHandler and MasterHandler untested methods are untestable with current pattern.
**Status:** ACTIVE
**ID:** D-4.2

## 2026-03-08 — Infrastructure: Test JsonParamValidator

**Decision:** Add a dedicated `JsonParamValidatorTest` class covering all 6 public methods with happy-path and error-path tests.
**Rationale:** JsonParamValidator was extracted in Phase 3 with zero direct tests. It's indirectly tested through handler tests, but a dedicated test class ensures each method's error messages and edge cases (null values, missing keys, type coercion) are explicitly verified. Pure utility class — no API dependencies, easy to test.
**Alternatives considered:** Rely on indirect coverage from handler tests — rejected, newly extracted utility deserves its own test contract.
**Status:** ACTIVE
**ID:** D-4.3

## 2026-03-08 — NoteHandler: Deduplicate requireArray

**Decision:** While adding NoteHandler tests, migrate its private `requireArray()` method to `JsonParamValidator` — it duplicates the same method in MacroHandler and is the only remaining private validation method across handlers.
**Rationale:** During Phase 3 we extracted all `requireInt/String/Boolean/Double` and `optionalInt/String` methods. Both NoteHandler and MacroHandler still have private `requireArray()` methods doing the same thing. Moving to JsonParamValidator completes the extraction.
**Alternatives considered:** Leave it — rejected, it's the same pattern and only 2 occurrences to update.
**Status:** ACTIVE
**ID:** D-4.4
