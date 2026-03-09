# Plan

> Living document — reflects what was done, not just what was intended.
> This file tracks the ACTIVE phase only. Completed phases are archived to `phases/`.

---

## Active Phase

### Phase 5 — gig-maestro: Mockito Behavioral Testing (v0.5.x)

> Introduce Mockito as a test dependency and add behavioral tests for 3 handlers (ProjectHandler, MasterHandler, TrackHandler) that verify correct Bitwig API method calls. Establishes mocking patterns for all chain depths (1-level, 2-level, 3-level) as a template for future handler coverage.

**Decisions:** D-5.1, D-5.2, D-5.3, D-5.4

| Batch | Version | Title | Delegation | Status |
|-------|---------|-------|------------|--------|
| 5.1 | `0.5.1` | Add Mockito dependency | in-session | pending |
| 5.2 | `0.5.2` | ProjectHandler + MasterHandler mock tests | team | pending |
| 5.3 | `0.5.3` | TrackHandler mock tests | team | pending |
| 5.4 | `0.5.4` | Verify full build | in-session | pending |

### Batch 5.1 — Add Mockito dependency

**Delegation:** in-session
**Decisions:** D-5.1
**Files:**
- Edit `gradle/libs.versions.toml` — add mockito version + libraries
- Edit `gig-maestro/build.gradle.kts` — add testImplementation for mockito

**Work:**
1. Add `mockito = "5.15.2"` to `[versions]` in version catalog (or latest 5.x).
2. Add `mockito-core = { module = "org.mockito:mockito-core", version.ref = "mockito" }` and `mockito-junit-jupiter = { module = "org.mockito:mockito-junit-jupiter", version.ref = "mockito" }` to `[libraries]`.
3. Add `testImplementation(libs.mockito.core)` and `testImplementation(libs.mockito.junit.jupiter)` to gig-maestro's build.gradle.kts.
4. Run `./gradlew :gig-maestro:dependencies --configuration testCompileClasspath` to verify resolution.

**Test criteria:** `./gradlew :gig-maestro:test` passes; Mockito appears in dependency tree.
**Acceptance:** Mockito available for test compilation.

### Batch 5.2 — ProjectHandler + MasterHandler mock tests

**Delegation:** team (independent of 5.3)
**Decisions:** D-5.2, D-5.3, D-5.4
**Files:**
- Edit `gig-maestro/src/test/java/dev/gregross/gig/handlers/ProjectHandlerTest.java`
- Edit `gig-maestro/src/test/java/dev/gregross/gig/handlers/MasterHandlerTest.java`

**Work:**
1. **ProjectHandlerTest** — convert to `@ExtendWith(MockitoExtension.class)`, add `@Mock Project mockProject`, wire into constructor. Add 3 behavioral tests:
   - `unsoloAll_callsProjectUnsolo` — verify `mockProject.unsoloAll()` called
   - `unmuteAll_callsProjectUnmute` — verify `mockProject.unmuteAll()` called
   - `unarmAll_callsProjectUnarm` — verify `mockProject.unarmAll()` called
   - Keep existing registration + getState tests unchanged.

2. **MasterHandlerTest** — convert to `@ExtendWith(MockitoExtension.class)`, add `@Mock MasterTrack mockMasterTrack` + chain mocks. Add 5 behavioral tests:
   - `setVolume_callsMasterTrackVolume` — mock 3-level chain, verify `setImmediately(0.75)` called
   - `setPan_callsMasterTrackPan` — mock 3-level chain, verify `setImmediately(0.5)` called
   - `setMute_callsMasterTrackMute` — mock 2-level chain, verify `set(true)` called
   - `setSolo_callsMasterTrackSolo` — mock 2-level chain, verify `set(true)` called
   - `setColor_callsMasterTrackColor` — mock 2-level chain, verify `set(r, g, b)` called
   - Keep existing validation tests unchanged.

**Test criteria:** `./gradlew :gig-maestro:test` passes; ProjectHandlerTest has 5 tests; MasterHandlerTest has 9 tests.
**Acceptance:** All 8 new mock-based tests verify correct Bitwig API calls.

### Batch 5.3 — TrackHandler mock tests

**Delegation:** team (independent of 5.2)
**Decisions:** D-5.2, D-5.3, D-5.4
**Files:**
- Edit `gig-maestro/src/test/java/dev/gregross/gig/handlers/TrackHandlerTest.java`

**Work:**
1. Convert to `@ExtendWith(MockitoExtension.class)`, add `@Mock` fields for TrackBank, Application, CursorTrack, TrackBankManager, NoteInput + chain mocks.
2. In `@BeforeEach`: wire `trackBank.getSizeOfBank()` → 8, `trackBank.getItemAt(anyInt())` → mockTrack, keep real StateCache.
3. Add behavioral tests for all 16 methods needing mocks:
   - **3-level chains (2 tests):** setVolume, setPan — verify `.value().setImmediately()`
   - **2-level chains (5 tests):** setMute, setSolo, setArm, setColor, setCrossfade — verify `.set()`
   - **1-level Application calls (3 tests):** createAudio, createInstrument, createEffect
   - **1-level CursorTrack calls (5 tests):** rename, deleteSelected, duplicate, createGroup, setGroupExpanded (with expanded=true)
   - **1-level misc (1 test):** setMonitor — verify `monitorMode().set()`
   - Keep all existing validation + registration tests unchanged.

**Test criteria:** `./gradlew :gig-maestro:test` passes; TrackHandlerTest has 31 tests total (15 existing + 16 new).
**Acceptance:** All 16 new tests verify correct Bitwig API method calls with correct parameters.

### Batch 5.4 — Verify full build

**Delegation:** in-session (depends on 5.1, 5.2, 5.3)
**Decisions:** D-5.1
**Files:** None (verification only)

**Work:**
1. Run `./gradlew clean build` — full build.
2. Count total test methods and confirm increase from 343.
3. Verify Mockito is properly resolved in dependency tree.

**Test criteria:** Clean build succeeds; all tests pass; test count ≥ 367.
**Acceptance:** Mockito integration complete, no regressions.

**Phase Acceptance Criteria:**
- [ ] Mockito 5.x in version catalog and gig-maestro test dependencies
- [ ] ProjectHandlerTest has 3 new behavioral tests (unsolo, unmute, unarm)
- [ ] MasterHandlerTest has 5 new behavioral tests (volume, pan, mute, solo, color)
- [ ] TrackHandlerTest has 16 new behavioral tests covering all chain depths
- [ ] All existing validation tests still pass unchanged
- [ ] `./gradlew clean build` succeeds
- [ ] Total test count ≥ 367

**Completion triggers Phase 6 → version `0.6.0`**

---

## Plan Amendments

| Date | Version | Amendment | Reason |
|------|---------|-----------|--------|
| — | — | — | — |
