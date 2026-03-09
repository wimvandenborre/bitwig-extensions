# Plan

> Living document — reflects what was done, not just what was intended.
> This file tracks the ACTIVE phase only. Completed phases are archived to `phases/`.

---

## Active Phase

### Phase 4 — gig-maestro: Test Coverage Expansion (v0.4.x)

> Expand gig-maestro test coverage by adding parameter validation tests to 4 handlers with testable gaps, creating a dedicated JsonParamValidator test class, and completing the requireArray extraction from Phase 3.

**Decisions:** D-4.1, D-4.2, D-4.3, D-4.4

| Batch | Version | Title | Delegation | Status |
|-------|---------|-------|------------|--------|
| 4.1 | `0.4.1` | Extract requireArray + JsonParamValidatorTest | in-session | done |
| 4.2 | `0.4.2` | TrackHandler + NoteHandler validation tests | team | done |
| 4.3 | `0.4.3` | ClipHandler + ProjectHandler validation tests | team | done |
| 4.4 | `0.4.4` | Verify full build | in-session | done |

### Batch 4.1 — Extract requireArray + JsonParamValidatorTest

**Delegation:** in-session
**Decisions:** D-4.3, D-4.4
**Files:**
- Edit `gig-maestro/src/main/java/dev/gregross/gig/rpc/JsonParamValidator.java` — add `requireArray()`
- Edit `gig-maestro/src/main/java/dev/gregross/gig/handlers/NoteHandler.java` — remove private `requireArray()`, add static import
- Edit `gig-maestro/src/main/java/dev/gregross/gig/handlers/MacroHandler.java` — remove private `requireArray()`, use shared one
- Create `gig-maestro/src/test/java/dev/gregross/gig/rpc/JsonParamValidatorTest.java`

**Work:**
1. Add `requireArray(JsonObject, String)` to `JsonParamValidator`.
2. Remove private `requireArray()` from NoteHandler and MacroHandler; ensure static import covers it.
3. Create `JsonParamValidatorTest` with tests for all 7 methods: `requireInt`, `requireString`, `requireBoolean`, `requireDouble`, `optionalInt`, `optionalString`, `requireArray` — both happy-path (returns correct value) and error-path (throws with message containing key name).

**Test criteria:** `./gradlew :gig-maestro:test` passes; new test class has ≥14 test methods.
**Acceptance:** No private `requireArray()` in handlers; JsonParamValidator has dedicated test coverage.

### Batch 4.2 — TrackHandler + NoteHandler validation tests

**Delegation:** team (independent of 4.3)
**Decisions:** D-4.2
**Files:**
- Edit `gig-maestro/src/test/java/dev/gregross/gig/handlers/TrackHandlerTest.java`
- Edit `gig-maestro/src/test/java/dev/gregross/gig/handlers/NoteHandlerTest.java`

**Work:**
1. TrackHandler — add 3 tests:
   - `setCrossfade_invalidMode_returnsError` (mode not A/B/AB)
   - `setMonitor_invalidMode_returnsError` (mode not ON/OFF/AUTO)
   - `select_missingIndex_returnsError`
2. NoteHandler — add 8 tests:
   - `setNotes_missingNotes_returnsError`
   - `setNotes_xOutOfRange_returnsError` (x ≥ 256)
   - `setNotes_yOutOfRange_returnsError` (y ≥ 128)
   - `setChance_chanceOutOfRange_returnsError` (> 1.0)
   - `setNoteOccurrence_unknownCondition_returnsError`
   - `setNoteRepeat_countOutOfRange_returnsError` (> 127)
   - `setNoteRecurrence_lengthOutOfRange_returnsError` (> 8)
   - `setNoteExpressions_missingNotes_returnsError`

**Test criteria:** `./gradlew :gig-maestro:test` passes; TrackHandlerTest has 15 tests; NoteHandlerTest has 16 tests.
**Acceptance:** All new validation edge cases covered.

### Batch 4.3 — ClipHandler + ProjectHandler validation tests

**Delegation:** team (independent of 4.2)
**Decisions:** D-4.2
**Files:**
- Edit `gig-maestro/src/test/java/dev/gregross/gig/handlers/ClipHandlerTest.java`
- Edit `gig-maestro/src/test/java/dev/gregross/gig/handlers/ProjectHandlerTest.java`

**Work:**
1. ClipHandler — add 4 tests:
   - `sceneLaunch_indexOutOfRange_returnsError` (index ≥ 5)
   - `sceneLaunch_partialOptions_returnsError` (quantization without launchMode)
   - `clipLaunch_invalidQuantization_returnsError`
   - `clipLaunch_invalidLaunchMode_returnsError`
2. ProjectHandler — add 1 test:
   - `getState_returnsStateCacheData` (verify response has all 4 fields from StateCache defaults)

**Test criteria:** `./gradlew :gig-maestro:test` passes; ClipHandlerTest has 26 tests; ProjectHandlerTest has 2 tests.
**Acceptance:** Scene launch edge cases and project state response covered.

### Batch 4.4 — Verify full build

**Delegation:** in-session (depends on 4.1, 4.2, 4.3)
**Decisions:** D-4.1
**Files:** None (verification only)

**Work:**
1. Run `./gradlew clean build` — full build.
2. Count total test methods and confirm increase from 310.
3. Verify no private `requireArray()` methods remain in handlers via grep.

**Test criteria:** Clean build succeeds; all tests pass; test count ≥ 326.
**Acceptance:** Phase complete, no regressions.

**Phase Acceptance Criteria:**
- [ ] `JsonParamValidatorTest` exists with ≥14 tests
- [ ] `requireArray()` moved to `JsonParamValidator`, removed from NoteHandler and MacroHandler
- [ ] TrackHandlerTest expanded with crossfade/monitor/select validation tests
- [ ] NoteHandlerTest expanded with range and missing-param validation tests
- [ ] ClipHandlerTest expanded with scene launch and launch option tests
- [ ] ProjectHandlerTest expanded with getState response test
- [ ] `./gradlew clean build` succeeds
- [ ] Total test count ≥ 326

**Completion triggers Phase 5 → version `0.5.0`**

---

## Plan Amendments

| Date | Version | Amendment | Reason |
|------|---------|-----------|--------|
| — | — | — | — |
