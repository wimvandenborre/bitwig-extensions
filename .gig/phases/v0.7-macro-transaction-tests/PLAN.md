# Plan

> Living document — reflects what was done, not just what was intended.
> This file tracks the ACTIVE phase only. Completed phases are archived to `phases/`.

---

## Active Phase

### Phase 7 — gig-maestro: MacroHandler + TransactionHandler Tests (v0.7.x)

> Add targeted validation edge-case tests to MacroHandlerTest and TransactionHandlerTest, closing gaps in missing-param validation and error-type × rollback × snapshot interactions. Existing 46 tests use the correct orchestration testing pattern and are preserved unchanged.

**Decisions:** D-7.1, D-7.2, D-7.3, D-7.4

| Batch | Version | Title | Delegation | Status |
|-------|---------|-------|------------|--------|
| 7.1 | `0.7.1` | MacroHandler + TransactionHandler edge-case tests | in-session | pending |
| 7.2 | `0.7.2` | Verify full build | in-session | pending |

### Batch 7.1 — MacroHandler + TransactionHandler edge-case tests

**Delegation:** in-session
**Decisions:** D-7.1, D-7.2, D-7.3
**Files:**
- Edit `gig-maestro/src/test/java/dev/gregross/gig/handlers/MacroHandlerTest.java`
- Edit `gig-maestro/src/test/java/dev/gregross/gig/handlers/TransactionHandlerTest.java`

**Work:**
1. **MacroHandlerTest** — add ~8 validation edge-case tests:
   - `createTrack_withPosition_forwardsPosition` — verify position param forwarded via callLog
   - `createClip_missingSceneIndex_returnsError`
   - `createClip_missingLengthBeats_returnsError`
   - `writeClip_missingStepSize_returnsError`
   - `writeClip_missingTrackIndex_returnsError`
   - `buildSection_clipMissingTrackIndex_returnsError`
   - `buildSection_clipMissingLengthBeats_returnsError`
   - `setupScenes_sceneMissingName_returnsError`

2. **TransactionHandlerTest** — add ~4 edge-case tests:
   - `transactionRollbackOnIllegalArgumentError` — verify rollback triggers for IllegalArgException too
   - `transactionInvalidParamsType_returnsError` — operation with params as array
   - `transactionRollbackWithPostSnapshot` — verify both rollback and postSnapshot present on error
   - `transactionRollbackOnIllegalArgCountsCorrectly` — verify undoCallCount matches completedCount

**Test criteria:** `./gradlew :gig-maestro:test` passes; ~12 new tests added.
**Acceptance:** All validation gaps identified in D-7.2 and D-7.3 are covered.

### Batch 7.2 — Verify full build

**Delegation:** in-session (depends on 7.1)
**Decisions:** D-7.1
**Files:** None (verification only)

**Work:**
1. Run `./gradlew clean :gig-maestro:test` — full clean build.
2. Count total tests and confirm increase from 530.
3. Verify all handler test files pass.

**Test criteria:** Clean build succeeds; all tests pass; test count ≥ 542 (530 + 12).
**Acceptance:** Full test coverage for all 13 handler classes.

**Phase Acceptance Criteria:**
- [ ] MacroHandlerTest has ~8 new validation edge-case tests
- [ ] TransactionHandlerTest has ~4 new edge-case tests
- [ ] All existing 530 tests still pass
- [ ] `./gradlew clean :gig-maestro:test` succeeds
- [ ] Total test count ≥ 542

**Completion triggers Phase 8 → version `0.8.0`**

---

## Plan Amendments

| Date | Version | Amendment | Reason |
|------|---------|-----------|--------|
| — | — | — | — |
