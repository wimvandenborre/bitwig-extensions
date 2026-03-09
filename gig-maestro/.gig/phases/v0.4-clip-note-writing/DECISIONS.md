# Decisions

> Append-only log. Never delete entries — amend or revise instead.
> Active decisions for the current phase live here.
> When a phase completes, its decisions are archived to `phases/`.

<!-- Decision statuses:
  PROPOSED  — Claude's recommendation, awaiting user approval
  ACTIVE    — Approved and in effect
  AMENDED   — Overridden by user (original preserved, new entry appended)
  REVISED   — Claude revised based on new information (original preserved)
-->

<!-- Entry format:
## YYYY-MM-DD — Domain: Question

**Decision:** What was decided.
**Rationale:** Why this choice was made.
**Alternatives considered:** What else was evaluated.
**Status:** PROPOSED | ACTIVE | AMENDED | REVISED
**ID:** D-{batch}.{num}
-->

## 2026-02-28 — Clip API: How to create and manage the CursorClip?

**Decision:** Create a single `CursorClip` via `cursorTrack.createLauncherCursorClip(16, 128)` during `init()`. Grid width = 16 steps, grid height = 128 keys (full MIDI range). The cursor follows the selected clip on the selected track. To edit a specific clip, the LLM selects the slot first via a new `clip/select` RPC method, which calls `ClipLauncherSlot.select()`.
**Rationale:** The CursorClip follows selection, matching the existing cursor pattern (CursorTrack → CursorDevice). 16 steps is a reasonable default grid width (1 bar at 1/16 resolution). 128 covers the full MIDI range so no vertical scrolling is needed for most use cases. The LLM can scroll horizontally for clips longer than 1 bar using step scrolling.
**Alternatives considered:** (a) Creating per-track PinnableCursorClips — adds complexity and resource overhead for 64 tracks. (b) Smaller grid height (e.g., 24 keys) — would require vertical scrolling for basic workflows.
**Status:** AMENDED
**ID:** D-4.1

## 2026-02-28 — Clip API: How to create and manage the CursorClip? (amended)

**Decision:** Create a single `CursorClip` via `cursorTrack.createLauncherCursorClip(64, 128)` during `init()`. Grid width = 64 steps (4 bars at 1/16 resolution), grid height = 128 keys (full MIDI range). The 64-step grid is the CursorClip's **visible window** into the clip, not the clip's maximum length — clips can be any length, and the LLM scrolls the window via `clip/scrollSteps`. To edit a specific clip, the LLM selects the slot via `clip/select` (`trackIndex` + `slotIndex`), which calls `ClipLauncherSlot.select()`.
**Rationale:** 64 steps covers a 4-bar loop at 1/16 resolution without scrolling — the most common use case. The grid is a viewport: `scrollSteps` shifts it for longer clips, `setStepSize` changes resolution. 128 keys covers full MIDI range. Overridden by user — original: 16-step grid width.
**Alternatives considered:** (a) 128-step grid — larger memory footprint for diminishing returns; 4 bars handles most patterns. (b) 16 steps — too small, requires scrolling for even a 1-bar 1/16 pattern if combined with longer clips.
**Status:** ACTIVE
**ID:** D-4.1a

## 2026-02-28 — Clip API: What RPC methods to expose for note editing?

**Decision:** Add 7 new RPC methods in a new `NoteHandler`:
1. `clip/select` — select a clip slot for editing (`trackIndex`, `slotIndex`)
2. `clip/setNote` — write a note (`x`, `y`, `velocity`, `duration`)
3. `clip/clearNote` — clear a note at position (`x`, `y`)
4. `clip/clearAllNotes` — clear all notes in the clip
5. `clip/getNote` — read note data at position (`x`, `y`), returns NoteStep properties
6. `clip/setStepSize` — set step resolution in beat time (e.g., 0.25 = 1/16)
7. `clip/scrollSteps` — scroll the step grid viewport (`offset` in steps)
**Rationale:** This covers the full CRUD cycle: create notes, read them back, clear individual or all notes. Step size control lets the LLM choose resolution (1/4, 1/8, 1/16, 1/32). Step scrolling enables editing clips longer than the grid width. Each method maps cleanly to a single Bitwig API call.
**Alternatives considered:** (a) Bulk write method taking an array of notes — Bitwig has no bulk API, so it would just loop internally. Better to let the LLM issue individual calls for transparency. (b) Including `clip/setLoopLength` and `clip/setPlayStart` — useful but scope-creep; clip timing is already set via `clip/create`'s `lengthInBeats`. Can add in a later phase.
**Status:** AMENDED
**ID:** D-4.2

## 2026-02-28 — Clip API: What RPC methods to expose for note editing? (amended)

**Decision:** Add 7 new RPC methods:
1. `clip/select` — select a clip slot for editing (`trackIndex`, `slotIndex`) — added to `ClipHandler`
2. `clip/setNotes` — **batch write**: accepts an array of notes `[{x, y, velocity, duration}, ...]`, loops internally calling `Clip.setStep()` per note. One RPC call writes an entire drum pattern or bassline.
3. `clip/clearNote` — clear a single note at position (`x`, `y`)
4. `clip/clearAllNotes` — clear all notes in the clip
5. `clip/getNotes` — **batch read**: scans the current grid viewport and returns a sparse array of all notes with their properties (velocity, duration, etc.). One call reads the entire clip content.
6. `clip/setStepSize` — set step resolution in beat time (e.g., 0.25 = 1/16)
7. `clip/scrollSteps` — scroll the step grid viewport (`offset` in steps)
**Rationale:** Batch operations are critical for usability. Writing a 16-step drum pattern one note at a time = 16+ round-trips; with `setNotes` it's 1 call. `getNotes` returns a sparse array (only cells with notes), so the LLM gets a complete picture of clip content without N individual reads. The handler loops over the Bitwig note-by-note API internally — the LLM never needs to know. Overridden by user — original: single-note setNote/getNote.
**Alternatives considered:** (a) Single-note operations (original D-4.2) — dozens of round-trips for simple patterns. (b) Streaming/chunked responses — unnecessary complexity when batch arrays work fine.
**Status:** ACTIVE
**ID:** D-4.2a

## 2026-02-28 — Clip API: What note data to include in snapshot state?

**Decision:** Add a `clip` section to the snapshot with cursor clip metadata only: `hasContent` (boolean), `playStart`, `playStop`, `loopLength` (doubles in beats), `stepSize` (current grid resolution), and `trackName`/`slotIndex` (which clip is selected). Do NOT include a full note grid dump in the snapshot — the LLM reads notes via `clip/getNotes` on demand (batch read of the full grid viewport).
**Rationale:** Dumping a 64×128 grid on every snapshot would be expensive and wasteful. The snapshot tells the LLM "which clip is selected and how long it is." The LLM calls `getNotes` when it needs content. This matches the cursor model: snapshot gives metadata, actions operate on the cursor target.
**Alternatives considered:** (a) Including a sparse note list in snapshot — potentially large for note-dense clips, and changes with every edit. (b) No clip section at all — the LLM would be blind to which clip it's editing.
**Status:** ACTIVE
**ID:** D-4.3

## 2026-02-28 — Architecture: Where to put the note-editing handler code?

**Decision:** Create a new `NoteHandler` class in the `handlers/` package. It takes the `CursorClip` as a constructor parameter. Keep the existing `ClipHandler` for slot-level operations (launch, stop, record, create) — `NoteHandler` is for note-level operations inside a clip. Add `clip/select` to `ClipHandler` since it operates on slots.
**Rationale:** Separation of concerns: `ClipHandler` = session/slot operations, `NoteHandler` = note content operations. This follows the existing pattern where each handler owns a distinct Bitwig API surface. The `CursorClip` is a different API object from `ClipLauncherSlotBank`, so a separate handler is natural.
**Alternatives considered:** (a) Adding note methods to existing ClipHandler — mixes slot-level and note-level concerns, and ClipHandler doesn't hold a CursorClip reference. (b) A single "ClipContentHandler" that also does select — muddies the slot/content boundary.
**Status:** ACTIVE
**ID:** D-4.4

## 2026-02-28 — Observers: What CursorClip observers to register?

**Decision:** Register observers on the CursorClip for: `playingStep()` (int — which step is currently playing), `getLoopLength()` (double), `getPlayStart()` (double), `getPlayStop()` (double), and a `StepDataObserver` via `addStepDataObserver()` to track which cells have notes (for `hasContent` detection). Store these in StateCache under a new `clip` section.
**Rationale:** `playingStep` enables the LLM to know the current playback position within a clip. Loop/play boundaries let it understand clip timing. The StepDataObserver provides efficient change notification without polling. These are the minimum observers needed for the snapshot `clip` section from D-4.3.
**Alternatives considered:** (a) No StepDataObserver — would need to poll `getStep()` for hasContent. (b) Observer per note cell — way too many observers (2048+), and StepDataObserver already provides batch change tracking.
**Status:** ACTIVE
**ID:** D-4.5

## 2026-02-28 — Tooling: How to update tool schemas and CLI for Phase 4?

**Decision:** Add 7 new tool definitions to `tools/claude-tools.json` (matching D-4.2a methods, using batch `setNotes`/`getNotes`). Add `clip/select` to ClipHandler. Update `tools/system-prompt.md` with a "Note Editing" section covering the step grid model, coordinate system (x=step, y=MIDI note), batch note format, and an example workflow for writing a drum pattern in one call. Add a `NoteCommand` to the CLI with subcommands matching the 7 new RPC methods. Update smoke tests for the new methods.
**Rationale:** Maintains the 1:1 mapping between RPC methods, tool schemas, and CLI commands established in Phase 3. The system prompt update is critical — the step grid model is non-obvious and the LLM needs to understand coordinates, step sizes, MIDI note numbers, and the batch note array format.
**Alternatives considered:** (a) Skip CLI commands for note editing — but consistency with existing CLI surface is important for debugging and testing. (b) Skip system prompt update — the LLM would struggle with the x/y coordinate model without guidance.
**Status:** ACTIVE
**ID:** D-4.6
