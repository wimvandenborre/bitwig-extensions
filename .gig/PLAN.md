# Plan

> Living document — reflects what was done, not just what was intended.
> This file tracks the ACTIVE phase only. Completed phases are archived to `phases/`.

---

## Active Phase

### Phase 22 — NoteStep Expressive Properties (v0.22.x)

> Expose all 11 missing NoteStep expressive properties via RPC. Four new methods (`clip/setNoteExpressions`, `clip/setNoteRepeat`, `clip/setNoteOccurrence`, `clip/setNoteRecurrence`) plus extending `clip/getNotes` to return all properties. Also extend `gig song rebuild` to write expressions after base notes.

**Decisions:** D-22.1, D-22.2, D-22.3, D-22.4, D-22.5

| Batch | Version | Title | Delegation | Status |
|-------|---------|-------|------------|--------|
| 22.1 | `0.22.1` | Extend getNotes + setNoteExpressions | in-session | done |
| 22.2 | `0.22.2` | setNoteRepeat + setNoteOccurrence + setNoteRecurrence | in-session | done |
| 22.3 | `0.22.3` | Song rebuild expressive properties | in-session | pending |
| 22.4 | `0.22.4` | Unit tests | in-session | pending |
| 22.5 | `0.22.5` | Tool schemas + system prompt update | in-session | pending |
| 22.6 | `0.22.6` | Smoke tests | in-session | pending |

---

### Batch 22.1 — Extend getNotes + setNoteExpressions

**Delegation:** in-session
**Decisions:** D-22.2
**Files:** `src/main/java/dev/gregross/gig/handlers/NoteHandler.java`
**Work:**
1. Extend `clip/getNotes` to include all expressive properties on each note: `pan`, `timbre`, `pressure`, `gain`, `transpose`, `releaseVelocity`, `velocitySpread`, `mute`. Also include `occurrence` (string + enabled), `recurrence` (length + mask + enabled), and `repeat` (count + curve + velocityEnd + velocityCurve + enabled). Only include non-default values (0.0 for most, false for booleans, "ALWAYS" for occurrence) to keep responses compact.
2. Register `clip/setNoteExpressions` — takes `notes` array of `{x, y, property, value}` where property is one of: `pan`, `timbre`, `pressure`, `gain`, `transpose`, `releaseVelocity`, `velocitySpread`, `mute`. Validates ranges per property. Returns `{count}`.

**Test criteria:** `./gradlew shadowJar` compiles. Extension loads in Bitwig. `clip/getNotes` returns expressive fields. `clip/setNoteExpressions` sets scalar properties.
**Acceptance:** getNotes returns all 11+ property fields. setNoteExpressions handles all 8 scalar properties.

---

### Batch 22.2 — setNoteRepeat + setNoteOccurrence + setNoteRecurrence

**Delegation:** in-session (depends on 22.1 — same file)
**Decisions:** D-22.2, D-22.5
**Files:** `src/main/java/dev/gregross/gig/handlers/NoteHandler.java`
**Work:**
1. Register `clip/setNoteRepeat` — takes `notes` array of `{x, y, count, curve, velocityEnd, velocityCurve}`. All 4 sub-properties set together. Enables repeat on the note. Returns `{count}`.
2. Register `clip/setNoteOccurrence` — takes `notes` array of `{x, y, condition}`. Maps condition string (case-insensitive) to `NoteOccurrence` enum. Sets `setOccurrence()` + `setIsOccurrenceEnabled(true)`. Returns `{count}`. Invalid condition throws error listing valid values.
3. Register `clip/setNoteRecurrence` — takes `notes` array of `{x, y, length, mask}`. Validates length 1-8. Sets `setRecurrence(length, mask)` + `setIsRecurrenceEnabled(true)`. Returns `{count}`.

**Test criteria:** `./gradlew shadowJar` compiles. All 3 methods respond correctly in Bitwig.
**Acceptance:** 3 new RPC methods registered and functional.

---

### Batch 22.3 — Song rebuild expressive properties

**Delegation:** in-session (depends on 22.2 — needs RPC methods)
**Decisions:** D-22.4
**Files:** `src/cli/java/dev/gregross/gig/cli/SongCommand.java`
**Work:**
1. In `RebuildCommand`, after writing base notes via `macro/writeClip`, check each clip's notes for expressive properties.
2. If any notes have non-default expression values, call `clip/setNoteExpressions` with a batch of `{x, y, property, value}` entries.
3. If any notes have repeat data, call `clip/setNoteRepeat`.
4. If any notes have occurrence data, call `clip/setNoteOccurrence`.
5. If any notes have recurrence data, call `clip/setNoteRecurrence`.
6. Only make calls when data is present (skip if all defaults).

**Test criteria:** `./gradlew cliShadowJar` compiles. Rebuild of a song with expressive properties writes them correctly.
**Acceptance:** Round-trip fidelity: dump → rebuild preserves expressive properties.

---

### Batch 22.4 — Unit tests

**Delegation:** in-session (depends on 22.1, 22.2, 22.3)
**Decisions:** D-22.1, D-22.2
**Files:** `src/test/java/dev/gregross/gig/handlers/NoteHandlerTest.java`, `src/test/java/dev/gregross/gig/cli/SongCommandTest.java`
**Work:**
1. NoteHandlerTest: test registration of 4 new methods (setNoteExpressions, setNoteRepeat, setNoteOccurrence, setNoteRecurrence).
2. NoteHandlerTest: test that NoteOccurrence enum string mapping covers all 11 values.
3. SongCommandTest: test that rebuild handles expression data in JSON.

**Test criteria:** `./gradlew test` passes all new tests.
**Acceptance:** 8+ new unit tests passing.

---

### Batch 22.5 — Tool schemas + system prompt update

**Delegation:** in-session (depends on 22.1, 22.2)
**Decisions:** D-22.2
**Files:** `tools/claude-tools.json`, `tools/system-prompt.md`
**Work:**
1. Add 4 tool schemas: `clip_setNoteExpressions`, `clip_setNoteRepeat`, `clip_setNoteOccurrence`, `clip_setNoteRecurrence`.
2. Update `clip_getNotes` schema description to document new return fields.
3. Add "Expressive Note Properties" section to system prompt with property names, ranges, and usage guidance.

**Test criteria:** `jq . tools/claude-tools.json` validates. System prompt mentions all property names.
**Acceptance:** 4 new schemas + updated getNotes + system prompt section.

---

### Batch 22.6 — Smoke tests

**Delegation:** in-session (depends on 22.5)
**Decisions:** D-22.1, D-22.2
**Files:** `scripts/smoke-test.sh`
**Work:**
1. Schema validation: 4 new tool schemas present in claude-tools.json.
2. Schema validation: getNotes schema describes expressive properties.
3. System prompt: mentions expressive properties section.
4. CLI build: `./gradlew cliShadowJar` succeeds.

**Test criteria:** `./scripts/smoke-test.sh --offline` passes all new assertions.
**Acceptance:** 8+ new smoke assertions passing.

---

**Phase Acceptance Criteria:**
- [ ] `clip/getNotes` returns all expressive properties (pan, timbre, pressure, gain, transpose, releaseVelocity, velocitySpread, mute, occurrence, recurrence, repeat)
- [ ] `clip/setNoteExpressions` sets 8 scalar properties with range validation
- [ ] `clip/setNoteRepeat` sets repeat count, curve, velocityEnd, velocityCurve
- [ ] `clip/setNoteOccurrence` maps 11 NoteOccurrence enum values (case-insensitive)
- [ ] `clip/setNoteRecurrence` sets length (1-8) and bitmask
- [ ] `gig song rebuild` writes expressive properties when present in JSON
- [ ] All unit tests pass (`./gradlew test`)
- [ ] All smoke tests pass (`./scripts/smoke-test.sh --offline`)
- [ ] 4 new tool schemas in claude-tools.json
- [ ] System prompt documents expressive properties

**Completion triggers Phase 23 → version `0.23.0`**
