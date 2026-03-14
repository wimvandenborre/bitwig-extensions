# Plan

> Living document — reflects what was done, not just what was intended.
> This file tracks the ACTIVE phase only. Completed phases are archived to `phases/`.

---

## Active Phase

### Phase 27 — Clip Expression Macros (v0.27.x)

> Extend macro note writing to support inline expression properties. Currently `macro/writeClip` and `macro/buildSection` only write basic notes (x, y, velocity, duration). Add support for chance, per-note expressions (pan, timbre, etc.), repeat/ratchet, occurrence, and recurrence — all inline on the note object.

**Decisions:** D-27.1, D-27.2, D-27.3

| Batch | Version | Title | Delegation | Status |
|-------|---------|-------|------------|--------|
| 27.1 | `0.27.1` | Expression support in writeNotesToCursor | in-session | pending |
| 27.2 | `0.27.2` | Unit tests | in-session | pending |
| 27.3 | `0.27.3` | Tool definition + system prompt + smoke tests | in-session | pending |
| 27.4 | `0.27.4` | Build verification | in-session | pending |

---

### Batch 27.1 — Expression support in writeNotesToCursor

**Decisions:** D-27.1, D-27.2
**Files:** `MacroHandler.java`
**Work:**
- After `clip/setNotes`, scan notes array for expression properties
- Collect notes with `chance` → batch into one `clip/setChance` call
- Collect notes with `expressions` → group by property name → one `clip/setNoteExpressions` call per property
- Collect notes with `repeat` → batch into one `clip/setNoteRepeat` call
- Collect notes with `occurrence` → batch into one `clip/setNoteOccurrence` call
- Collect notes with `recurrence` → batch into one `clip/setNoteRecurrence` call
- All expression calls scheduled after FLUSH_DELAY_MS (notes must exist first)
- Note: `writeNotesToCursor` is already called in a deferred context from `handleWriteClip` and `handleBuildSection`, so the expression scheduling adds another layer of delay
**Test criteria:** Covered in batch 27.2

---

### Batch 27.2 — Unit tests

**Decisions:** All
**Files:** `MacroHandlerTest.java`
**Work:**
- Add stubs for `clip/setChance`, `clip/setNoteExpressions`, `clip/setNoteRepeat`, `clip/setNoteOccurrence`, `clip/setNoteRecurrence`
- Test: `writeClip` with `chance` on notes → calls `clip/setChance`
- Test: `writeClip` with `expressions` (pan, timbre) → calls `clip/setNoteExpressions` per property
- Test: `writeClip` with `repeat` → calls `clip/setNoteRepeat`
- Test: `writeClip` with `occurrence` → calls `clip/setNoteOccurrence`
- Test: `writeClip` with basic notes only → no expression calls (backward compat)
- Test: `buildSection` with expression notes → expressions applied per clip
**Test criteria:** `./gradlew :gig-maestro:test` passes

---

### Batch 27.3 — Tool definition + system prompt + smoke tests

**Files:**
- `claude-tools.json` — extend note schema in `macro_writeClip` and `macro_buildSection` with expression properties
- `system-prompt.md` — update macro note format docs, add expression examples
- `offline-schemas.sh` — assertions for new schema fields and prompt content
**Test criteria:** `smoke-test.sh --offline` passes

---

### Batch 27.4 — Build verification

**Steps:**
- `./gradlew :gig-maestro:test` — all unit tests pass
- `./gradlew :gig-maestro:shadowJar` — extension builds
- `gig-maestro/scripts/smoke-test.sh --offline` — all offline smoke tests pass

---

**Phase Acceptance Criteria:**
- [ ] `macro/writeClip` supports inline expression properties on notes
- [ ] `macro/buildSection` inherits expression support
- [ ] Expressions applied after notes exist (deferred flush)
- [ ] Grouped by property type for efficiency
- [ ] Backward compatible — notes without expressions work unchanged
- [ ] Tool definitions and system prompt updated
- [ ] All tests pass

**Completion triggers Phase 28 → version `0.28.0`**
