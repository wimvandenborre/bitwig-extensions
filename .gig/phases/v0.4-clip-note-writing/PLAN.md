# Plan

> Living document — reflects what was done, not just what was intended.
> This file tracks the ACTIVE phase only. Completed phases are archived to `phases/`.

---

## Active Phase

### Phase 4 — Clip Note Writing (v0.4.x)

> Expose Bitwig's CursorClip and NoteStep APIs over JSON-RPC so LLM agents can programmatically write, read, and clear MIDI notes in clip launcher slots. Batch operations (`setNotes`/`getNotes`) minimize round-trips — the LLM writes an entire drum pattern or bassline in a single call. Clip metadata (timing, selection, hasContent) is added to the session snapshot. Tool schemas, system prompt, CLI, and smoke tests are updated to match.

**Decisions:** D-4.1a, D-4.2a, D-4.3, D-4.4, D-4.5, D-4.6

| Batch | Version | Title | Delegation | Status |
|-------|---------|-------|------------|--------|
| 4.1 | `0.4.1` | CursorClip creation + clip observers + snapshot | in-session | done |
| 4.2 | `0.4.2` | Fix deprecated BeatTimeValue.addRawValueObserver | in-session | done |
| 4.3 | `0.4.3` | NoteHandler + clip/select + batch note operations | in-session | done |
| 4.4 | `0.4.4` | Tool schemas + system prompt update | in-session | done |
| 4.5 | `0.4.5` | CLI NoteCommand + smoke tests | in-session | done |
| 4.6 | `0.4.6` | Add clip/delete RPC method | in-session | done |

### Batch 4.1 — CursorClip creation + clip observers + snapshot

**Delegation:** in-session
**Decisions:** D-4.1a, D-4.5, D-4.3
**Files:**
- `src/main/java/dev/gregross/gig/extension/GigMaestroExtension.java` (modify — create CursorClip, pass to handlers/cache)
- `src/main/java/dev/gregross/gig/extension/StateCache.java` (modify — add clip section fields, observers, snapshot)

**Work:**
1. In `GigMaestroExtension.init()`, create `CursorClip` via `cursorTrack.createLauncherCursorClip("gig-clip", "Gig Clip", 64, 128)`. Store as field.
2. Add `StateCache.registerClipCursorObservers(Clip cursorClip)` method:
   - `cursorClip.playingStep().addValueObserver()` → `clipPlayingStep` (int)
   - `cursorClip.getLoopLength().addValueObserver()` → `clipLoopLength` (double)
   - `cursorClip.getPlayStart().addValueObserver()` → `clipPlayStart` (double)
   - `cursorClip.getPlayStop().addValueObserver()` → `clipPlayStop` (double)
   - `cursorClip.addStepDataObserver()` → `clipStepData` (boolean[][] or hasContent flag)
3. Add clip metadata fields to StateCache: `clipPlayingStep`, `clipLoopLength`, `clipPlayStart`, `clipPlayStop`, `clipHasNotes` (derived from StepDataObserver).
4. Add `getClipState()` method returning `{playingStep, loopLength, playStart, playStop, hasContent, trackName, stepSize}`.
5. Add `"clip"` to `getSnapshot()` and `getChangedSections()` (new hash field `prevClipHash`).
6. Call `stateCache.registerClipCursorObservers(cursorClip)` in `init()`.

**Test criteria:**
- Unit tests: `StateCacheDeltaTest` updated — `getChangedSections()` now reports 7 sections (add `"clip"`)
- `getSnapshot()` includes `"clip"` key with expected fields
- Builds without errors: `./gradlew build`

**Acceptance:**
- Snapshot includes `clip` section with metadata (playingStep, loopLength, playStart, playStop, hasContent)
- Delta detection covers 7 sections including `clip`
- CursorClip created during extension init

---

### Batch 4.2 — NoteHandler + clip/select + batch note operations

**Delegation:** in-session
**Decisions:** D-4.2a, D-4.4
**Depends on:** Batch 4.1

**Files:**
- `src/main/java/dev/gregross/gig/handlers/NoteHandler.java` (create)
- `src/main/java/dev/gregross/gig/handlers/ClipHandler.java` (modify — add `clip/select`)
- `src/main/java/dev/gregross/gig/extension/GigMaestroExtension.java` (modify — register NoteHandler)

**Work:**
1. Add `clip/select` to `ClipHandler`: takes `trackIndex` + `slotIndex`, calls `ClipLauncherSlot.select()` on the target slot. Follows existing `getSlotBank()` pattern.
2. Create `NoteHandler(Clip cursorClip)` with 6 RPC methods:
   - `clip/setNotes` — accepts `notes` array `[{x, y, velocity, duration}, ...]`. Loops calling `cursorClip.setStep(0, x, y, velocity, duration)` per entry. Returns `{count: N}`.
   - `clip/clearNote` — takes `x`, `y`. Calls `cursorClip.clearStep(0, x, y)`. Returns `"ok"`.
   - `clip/clearAllNotes` — no params. Calls `cursorClip.clearSteps()`. Returns `"ok"`.
   - `clip/getNotes` — scans grid (64 steps × 128 keys). For each cell, calls `cursorClip.getStep(0, x, y)`. If `NoteStep.state() != NoteStep.State.Empty`, adds to result array with `{x, y, velocity, duration}`. Returns sparse array.
   - `clip/setStepSize` — takes `size` (double, beat time). Calls `cursorClip.setStepSize(size)`. Returns `"ok"`.
   - `clip/scrollSteps` — takes `offset` (int). Calls `cursorClip.scrollToStep(offset)`. Returns `"ok"`.
3. Register `new NoteHandler(cursorClip).register(dispatcher)` in `GigMaestroExtension.init()`.

**Test criteria:**
- Builds without errors: `./gradlew build`
- Total RPC methods: 43 (36 existing + 7 new) — verify via `api/list` count in smoke test

**Acceptance:**
- `clip/select` selects a clip for editing
- `clip/setNotes` accepts an array and writes notes in one RPC call
- `clip/getNotes` returns sparse array of all notes in viewport
- `clip/clearNote`, `clip/clearAllNotes` clear notes
- `clip/setStepSize` and `clip/scrollSteps` control the grid viewport

---

### Batch 4.3 — Tool schemas + system prompt update

**Delegation:** in-session
**Decisions:** D-4.6
**Depends on:** Batch 4.2

**Files:**
- `tools/claude-tools.json` (modify — add 7 new tool definitions)
- `tools/system-prompt.md` (modify — add Note Editing section)

**Work:**
1. Add 7 tool definitions to `claude-tools.json`:
   - `clip_select` — `{trackIndex: integer, slotIndex: integer}`
   - `clip_set_notes` — `{notes: array of {x: integer, y: integer, velocity: number, duration: number}}`
   - `clip_clear_note` — `{x: integer, y: integer}`
   - `clip_clear_all_notes` — `{}`
   - `clip_get_notes` — `{}`
   - `clip_set_step_size` — `{size: number}`
   - `clip_scroll_steps` — `{offset: integer}`
2. Update `system-prompt.md`:
   - Add "Note Editing" section explaining step grid model
   - Document coordinate system: x = step position (0-63 in viewport), y = MIDI note (0-127, where 60 = C4)
   - Document batch note format for `setNotes`/`getNotes`
   - Document step size values: 0.25 = 1/16, 0.5 = 1/8, 1.0 = 1/4
   - Add example workflow: "Write a 4-beat kick pattern" using `clip_set_notes` with array
   - Update viewport model to include clip grid dimensions (64 steps × 128 keys)

**Test criteria:**
- `jq length tools/claude-tools.json` = 43
- All 7 new tool names present and unique
- System prompt contains "Note Editing" section

**Acceptance:**
- Tool schemas match RPC method signatures exactly
- System prompt gives LLM enough context to write notes without confusion

---

### Batch 4.4 — CLI NoteCommand + smoke tests

**Delegation:** in-session
**Decisions:** D-4.6
**Depends on:** Batch 4.3

**Files:**
- `src/cli/java/dev/gregross/gig/cli/NoteCommand.java` (create)
- `src/cli/java/dev/gregross/gig/cli/GigCli.java` (modify — add NoteCommand subcommand)
- `scripts/smoke-test.sh` (modify — add Phase 4 tests)
- `src/test/java/dev/gregross/gig/extension/StateCacheDeltaTest.java` (modify — update for 7 sections)

**Work:**
1. Create `NoteCommand` with subcommands:
   - `select` — `--track-index`, `--slot-index`
   - `set-notes` — `--json` (JSON array string) or `--note` (repeatable: `x,y,vel,dur`)
   - `clear-note` — `--x`, `--y`
   - `clear-all`
   - `get-notes`
   - `set-step-size` — `--size`
   - `scroll-steps` — `--offset`
2. Add `NoteCommand.class` to `GigCli` subcommands.
3. Update `StateCacheDeltaTest.firstCallReportsAllSectionsChanged()` to expect 7 sections (add `"clip"`).
4. Update `smoke-test.sh`:
   - Offline tests: 7 new tool schema validations, CLI `note --help` checks
   - Online tests: `clip/select`, `clip/setNotes` with small pattern, `clip/getNotes` readback, `clip/clearAllNotes`

**Test criteria:**
- `./gradlew test` — all unit tests pass (including updated delta test)
- `./gradlew cliShadowJar` — CLI builds
- `java -jar gig-cli.jar note --help` shows subcommands
- `scripts/smoke-test.sh --offline` — all offline tests pass

**Acceptance:**
- CLI note subcommands work end-to-end
- Smoke test coverage for all 7 new RPC methods
- Unit tests updated for 7-section delta detection
- Total tests: 29+ unit, 130+ smoke

---

**Phase Acceptance Criteria:**
- [ ] CursorClip created with 64×128 grid viewport
- [ ] 7 new RPC methods registered (43 total)
- [ ] `clip/setNotes` writes batch notes in one call
- [ ] `clip/getNotes` returns sparse array of all notes in viewport
- [ ] Snapshot includes `clip` section with metadata
- [ ] Delta detection covers 7 sections
- [ ] 7 new tool schemas in `claude-tools.json` (43 total)
- [ ] System prompt documents step grid model and note editing
- [ ] CLI `note` command with all subcommands
- [ ] All unit tests pass
- [ ] All smoke tests pass (offline + online)

**Completion triggers Phase 5 → version `0.5.0`**

---

## Plan Amendments

<!-- Log any changes to the plan after creation -->

| Date | Version | Amendment | Reason |
|------|---------|-----------|--------|
