# Plan

> Living document — reflects what was done, not just what was intended.
> This file tracks the ACTIVE phase only. Completed phases are archived to `phases/`.

---

## Phase 24 — gig-maestro: Macro Improvements

**Goal:** Extend `macro/createTrack` to accept `pages` for combined track creation + sound design in a single call.

**Decisions:** D-24.1, D-24.2, D-24.3

---

### Batch 24.1 — Extend `macro/createTrack` with `pages` support

**Files:**
- `gig-maestro/src/main/java/dev/gregross/gig/handlers/MacroHandler.java`

**Changes:**
- Add `pages` parameter handling to `handleCreateTrack`
- Validate: if `pages` provided without `device`, throw `-32602`
- After device insertion, schedule `handleCreateSound` delegation via `dispatcher.handleInternal("macro/createSound", ...)` after `FLUSH_DELAY_MS`
- Pass `pages` (without `device`) so `createSound` applies to current device
- Add `paramCount` and `pageCount` to return when `pages` present

**Tests:** Unit tests in batch 24.2

---

### Batch 24.2 — Unit tests

**Files:**
- `gig-maestro/src/test/java/dev/gregross/gig/handlers/MacroHandlerTest.java`

**Changes:**
- Test: `createTrack` with `type` + `name` + `device` + `pages` → logs track creation, rename, device insert, and createSound call
- Test: `createTrack` with `pages` but no `device` → returns `-32602` error
- Test: `createTrack` with `device` but no `pages` → works as before (no createSound call)
- Test: return shape includes `paramCount` and `pageCount` when `pages` present

---

### Batch 24.3 — Tool definition + system prompt + smoke tests

**Files:**
- `gig-maestro/tools/claude-tools.json` — add `pages` property to `macro_createTrack`
- `gig-maestro/tools/system-prompt.md` — update macro usage guidance
- `gig-maestro/scripts/tests/offline-schemas.sh` — add assertions for new schema fields

---

### Batch 24.4 — Build verification

**Steps:**
- `./gradlew :gig-maestro:test` — all unit tests pass
- `./gradlew :gig-maestro:shadowJar` — extension builds
- `gig-maestro/scripts/smoke-test.sh --offline` — offline smoke tests pass
