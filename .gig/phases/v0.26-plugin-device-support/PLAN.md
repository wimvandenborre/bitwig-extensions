# Plan

> Living document — reflects what was done, not just what was intended.
> This file tracks the ACTIVE phase only. Completed phases are archived to `phases/`.

---

## Active Phase

### Phase 26 — Plugin Device Support (v0.26.x)

> Add plugin device support (VST2/VST3/CLAP) to macros. Currently `macro/createTrack`, `macro/createSound`, and `macro/buildSong` only support Bitwig built-in devices. Add a `plugin` parameter as an alternative to `device` for inserting third-party plugins with optional sound design via `pages`.

**Decisions:** D-26.1, D-26.2, D-26.3

| Batch | Version | Title | Delegation | Status |
|-------|---------|-------|------------|--------|
| 26.1 | `0.26.1` | Plugin support in MacroHandler | in-session | pending |
| 26.2 | `0.26.2` | Unit tests | in-session | pending |
| 26.3 | `0.26.3` | Tool definition + system prompt + smoke tests | in-session | pending |
| 26.4 | `0.26.4` | Build verification | in-session | pending |

---

### Batch 26.1 — Plugin support in MacroHandler

**Decisions:** D-26.1, D-26.3
**Files:** `MacroHandler.java`
**Work:**
- Extract device insertion logic into a helper method: `insertDevice(JsonObject params, JsonRpcDispatcher dispatcher)` that handles both `device` (Bitwig) and `plugin` ({type, id}) cases
- Update `handleCreateTrack`: use helper, validate mutual exclusivity, update `hasDevice` logic to include `plugin`
- Update `handleCreateSound`: same pattern — support `plugin` as alternative to `device`
- `handleBuildSong`: inherits support via `macro/createTrack` delegation (pass through `plugin` param)
- Validation: `device` + `plugin` together → IllegalArgumentException
- `pages` requires `device` OR `plugin` (update existing check)
**Test criteria:** Covered in batch 26.2

---

### Batch 26.2 — Unit tests

**Decisions:** All
**Files:** `MacroHandlerTest.java`
**Work:**
- Add `device/insertPluginDevice` stub to test setup
- Test: `createTrack` with `plugin` → calls `device/insertPluginDevice`
- Test: `createTrack` with `plugin` + `pages` → inserts plugin + sets params
- Test: `createTrack` with `device` + `plugin` → error
- Test: `createSound` with `plugin` → inserts plugin + sets params
- Test: `createSound` with `device` + `plugin` → error
- Test: `buildSong` with plugin track → delegates correctly
**Test criteria:** `./gradlew :gig-maestro:test` passes

---

### Batch 26.3 — Tool definition + system prompt + smoke tests

**Files:**
- `claude-tools.json` — add `plugin` property to `macro_createTrack`, `macro_createSound`, `macro_buildSong`
- `system-prompt.md` — document plugin support in macros, add examples
- `offline-schemas.sh` — assertions for new schema fields and prompt content
**Test criteria:** `smoke-test.sh --offline` passes

---

### Batch 26.4 — Build verification

**Steps:**
- `./gradlew :gig-maestro:test` — all unit tests pass
- `./gradlew :gig-maestro:shadowJar` — extension builds
- `gig-maestro/scripts/smoke-test.sh --offline` — all offline smoke tests pass

---

**Phase Acceptance Criteria:**
- [ ] `macro/createTrack` accepts `plugin` param (mutually exclusive with `device`)
- [ ] `macro/createSound` accepts `plugin` param
- [ ] `macro/buildSong` passes `plugin` through to track creation
- [ ] `pages` works with `plugin` (same as `device`)
- [ ] Mutual exclusivity validated with clear error
- [ ] Tool definitions and system prompt updated
- [ ] All tests pass

**Completion triggers Phase 27 → version `0.27.0`**
