# Plan

> Living document — reflects what was done, not just what was intended.
> This file tracks the ACTIVE phase only. Completed phases are archived to `phases/`.

---

## Active Phase

### Phase 25 — Macro Composition Chains (v0.25.x)

> Combine track creation with sound design and section building into a single `macro/buildSong` call. Also add track color support to `macro/createTrack`. Reduces the "Build From Scratch" workflow from N+M+2 calls to 3 calls (tempo + buildSong + verify).

**Decisions:** D-25.1, D-25.2, D-25.3, D-25.4

| Batch | Version | Title | Delegation | Status |
|-------|---------|-------|------------|--------|
| 25.1 | `0.25.1` | Add color to macro/createTrack | in-session | pending |
| 25.2 | `0.25.2` | macro/buildSong implementation | in-session | pending |
| 25.3 | `0.25.3` | Unit tests | in-session | pending |
| 25.4 | `0.25.4` | Tool definition + system prompt + smoke tests | in-session | pending |
| 25.5 | `0.25.5` | Build verification | in-session | pending |

---

### Batch 25.1 — Add color to macro/createTrack

**Decisions:** D-25.3
**Files:** `MacroHandler.java`
**Work:**
- Add optional `color` object param (`r`, `g`, `b` floats) to `handleCreateTrack`
- Apply via `dispatcher.handleInternal("track/setColor", ...)` after rename, before device insertion
- Need to determine how `track/setColor` works — it takes `index`, but cursor track index may not be known. May need to use cursor track color API directly.
**Test criteria:** Covered in batch 25.3

---

### Batch 25.2 — macro/buildSong implementation

**Decisions:** D-25.1, D-25.2, D-25.4
**Files:** `MacroHandler.java`
**Work:**
- Register `macro/buildSong` method
- Accept `tracks[]` (each: `type`, `name?`, `device?`, `pages?`, `color?`) and `sections[]` (each: `sceneName`, `clips[]`, `sceneIndex?`)
- Calculate delay chain: each track gets cumulative delay based on prior tracks' page counts
- After all tracks complete, schedule sections sequentially
- Return `{trackCount, sectionCount}`
**Test criteria:** Covered in batch 25.3

---

### Batch 25.3 — Unit tests

**Decisions:** All
**Files:** `MacroHandlerTest.java`
**Work:**
- Test: `createTrack` with `color` sets track color after rename
- Test: `buildSong` with 2 tracks + 1 section logs correct call sequence
- Test: `buildSong` tracks-only (no sections) works
- Test: `buildSong` sections require tracks (empty tracks array → error)
- Test: `buildSong` return shape has trackCount + sectionCount
**Test criteria:** `./gradlew :gig-maestro:test` passes

---

### Batch 25.4 — Tool definition + system prompt + smoke tests

**Files:**
- `claude-tools.json` — add `color` to `macro_createTrack`, add `macro_buildSong` tool
- `system-prompt.md` — update Build From Scratch workflow, add buildSong example
- `offline-schemas.sh` — assertions for new schema fields and prompt content
**Test criteria:** `smoke-test.sh --offline` passes

---

### Batch 25.5 — Build verification

**Steps:**
- `./gradlew :gig-maestro:test` — all unit tests pass
- `./gradlew :gig-maestro:shadowJar` — extension builds
- `gig-maestro/scripts/smoke-test.sh --offline` — all offline smoke tests pass

---

**Phase Acceptance Criteria:**
- [ ] `macro/createTrack` accepts optional `color` param
- [ ] `macro/buildSong` creates tracks sequentially with proper delays
- [ ] `macro/buildSong` builds sections after all tracks complete
- [ ] Tool definitions and system prompt updated
- [ ] All tests pass

**Completion triggers Phase 26 → version `0.26.0`**
