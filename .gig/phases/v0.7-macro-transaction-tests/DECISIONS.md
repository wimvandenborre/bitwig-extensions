# Decisions

> Append-only log. Never delete entries — amend or revise instead.
> Active decisions for the current phase live here.
> When a phase completes, its decisions are archived to `phases/`.

## 2026-03-11 — Scope: What gaps remain in MacroHandler + TransactionHandler tests

**Decision:** Add targeted validation edge-case tests to MacroHandlerTest and minor gap-fill tests to TransactionHandlerTest. Do NOT rewrite or restructure existing tests — they already use the correct orchestration testing pattern (dispatcher stubs + call-log verification for MacroHandler, stub-handlers for TransactionHandler). The 46 existing tests are well-designed and comprehensive for the behavioral happy paths.
**Rationale:** Research shows MacroHandler has 21 tests and TransactionHandler has 25 tests already covering registration, behavioral sequences, error handling, snapshots, and rollback. The existing call-log pattern is the correct approach for orchestration handlers — it verifies the exact sequence of `handleInternal()` calls. The only gaps are untested validation paths (missing required params for sub-methods).
**Alternatives considered:** (a) Convert to Mockito — rejected, the dispatcher-stub pattern is superior for orchestration testing because it verifies call ordering and parameter forwarding. (b) Skip this phase entirely — rejected, there are real validation gaps.
**Status:** ACTIVE
**ID:** D-7.1

## 2026-03-11 — MacroHandler gaps: Validation edge cases

**Decision:** Add ~8 validation tests for MacroHandler covering: (1) createTrack with position parameter forwarding, (2) createClip missing sceneIndex, (3) createClip missing lengthBeats, (4) writeClip missing stepSize, (5) writeClip missing trackIndex, (6) buildSection with clip missing trackIndex, (7) buildSection with clip missing lengthBeats, (8) setupScenes with scene missing name. These test error paths that currently have no coverage.
**Rationale:** The existing 21 MacroHandler tests cover all happy paths and some error paths (invalid type, missing type, empty clips array, missing sceneName, missing notes). But several required-param validation paths are untested — if a future refactor breaks param forwarding, these gaps would go undetected.
**Alternatives considered:** (a) Test every possible missing-param combination — rejected, diminishing returns. (b) Only test behavioral additions — rejected, the behavioral paths are already well-covered.
**Status:** ACTIVE
**ID:** D-7.2

## 2026-03-11 — TransactionHandler gaps: Minor edge cases

**Decision:** Add ~4 tests for TransactionHandler: (1) rollback triggered by IllegalArgumentException (currently only tested for RpcException), (2) transaction with operation having invalid params type (array), (3) rollback with postSnapshot on error, (4) verify undoCallCount=0 when error on step 0 with rollback requested (already tested but worth explicit assertion). These are minor but close real gaps.
**Rationale:** The existing 25 tests are comprehensive. These 4 additions close the remaining edge-case gaps around error type × rollback × snapshot interaction.
**Alternatives considered:** (a) More extensive negative testing — rejected, existing coverage is already thorough. (b) No changes to TransactionHandler — considered viable but a few real gaps exist.
**Status:** ACTIVE
**ID:** D-7.3

## 2026-03-11 — Batch structure: Single batch + verify

**Decision:** 2 batches: (1) Add all new tests to both handlers, (2) verify full build. This is a small phase — no need for team parallelism or complex batching.
**Rationale:** Total new tests is ~12, across 2 files. A single implementation batch keeps overhead proportional to the work.
**Alternatives considered:** (a) One batch per handler — rejected, unnecessary overhead for ~12 tests.
**Status:** ACTIVE
**ID:** D-7.4
