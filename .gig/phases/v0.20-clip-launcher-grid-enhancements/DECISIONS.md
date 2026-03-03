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
**Status:** ACTIVE | ACTIVE | AMENDED | REVISED
**ID:** D-{batch}.{num}
-->

## 2026-03-02 — Scope: What clip launcher grid enhancements to include?

**Decision:** Include clip color (set/get), scene color (set), clip loop/play boundary adjustment (setPlayStart, setPlayStop, setLoopStart, setLoopLength, setLoopEnabled), clip quantize, clip transpose, clip duplicateContent, alternative launch methods (launchAlt, launchRelease, launchReleaseAlt), and showInEditor. Exclude browseToInsertClip (already covered by BrowserHandler).
**Rationale:** All features are available in API v25 and fill gaps in the current clip launcher toolkit. Together they give agents full control over clip properties, note manipulation, and launch behavior. browseToInsertClip overlaps with existing browser/browseInsertDevice functionality.
**Alternatives considered:** (1) Only color + quantize — too narrow, misses natural companion features. (2) Include browseToInsertClip — rejected, already covered by browser handler.
**Status:** ACTIVE
**ID:** D-20.1

## 2026-03-02 — Clip Color: How to expose clip color setting?

**Decision:** Add `clip/setColor` to ClipHandler operating on slot-level via ClipLauncherSlot.color().set(r,g,b) with trackIndex + slotIndex params. Add `scene/setColor` to SceneHandler via Scene.color().set(r,g,b) with scene index param. Use float RGB (0.0–1.0) matching existing track/setColor and master/setColor patterns.
**Rationale:** Slot-level color setting works on the grid directly without requiring cursor selection. RGB float format is consistent with existing color methods (track/setColor uses r/g/b floats). Clip colors are already observed per-slot in StateCache (clipColors array). Scene colors need a new observer.
**Alternatives considered:** (1) Cursor clip color via cursorClip.color() — rejected, requires clip selection first and doesn't match grid-based workflow. (2) Integer 0–255 RGB — rejected, Bitwig API uses 0.0–1.0 floats.
**Status:** ACTIVE
**ID:** D-20.2

## 2026-03-02 — Clip Boundaries: How to expose loop and play boundary settings?

**Decision:** Add 6 cursor clip methods: `clip/setPlayStart`, `clip/setPlayStop`, `clip/setLoopStart`, `clip/setLoopLength`, `clip/setLoopEnabled`, and `clip/getPlaybackSettings` (getter returning all boundary + loop values). These operate on the cursor clip via SettableBeatTimeValue.set(double). Add loopStart and isLoopEnabled to the clip snapshot section (loopLength, playStart, playStop already observed).
**Rationale:** These are SettableBeatTimeValue properties on the Clip interface — the cursor clip already has observers for loopLength, playStart, playStop. Adding setters + the missing observer fields (loopStart, isLoopEnabled) completes the picture. A combined getter avoids multiple calls.
**Alternatives considered:** (1) Single `clip/setBoundaries` with all params — rejected, atomic setters match existing patterns and allow partial updates. (2) Skip loopStart observer (already in StateCache) — checked: loopStart is NOT observed yet, only loopLength is.
**Status:** ACTIVE
**ID:** D-20.3

## 2026-03-02 — Note Operations: How to expose quantize and transpose?

**Decision:** Add `clip/quantize` with `amount` param (double 0.0–1.0) and `clip/transpose` with `semitones` param (integer). Both operate on the cursor clip. These are simple fire-and-forget methods on the Clip interface.
**Rationale:** Quantize takes a morph factor (0.0 = no change, 1.0 = fully quantized) — this is Bitwig's built-in quantize, not a grid value. Transpose shifts all notes by semitones. Both are single-call operations with no observer state needed.
**Alternatives considered:** (1) Add grid-based quantize (e.g., quantize to 1/8) — not available in API, Bitwig's quantize() only takes an amount factor. (2) Skip transpose — rejected, it's a natural companion to quantize and essential for key changes.
**Status:** ACTIVE
**ID:** D-20.4

## 2026-03-02 — Launch Methods: How to expose alternative launch methods?

**Decision:** Add 3 new methods to ClipHandler: `clip/launchAlt` (trackIndex, slotIndex), `clip/launchRelease` (trackIndex, slotIndex), `clip/launchReleaseAlt` (trackIndex, slotIndex). Add `scene/launchAlt`, `scene/launchRelease`, `scene/launchReleaseAlt` with scene index. Also add `clip/duplicateContent` and `clip/showInEditor` on the cursor clip.
**Rationale:** Alternative launch methods (v18) enable pad-style performance workflows — launchAlt uses alternative clip settings, launchRelease signals pad release for momentary triggering. These work on slots (trackIndex + slotIndex) matching existing clip/launch. duplicateContent doubles clip content (useful for variation building), showInEditor opens the clip in the note/audio editor.
**Alternatives considered:** (1) Skip launchRelease — rejected, it's essential for momentary/gate launch modes. (2) Put duplicateContent in NoteHandler — rejected, it's a Clip method, not a note-level operation.
**Status:** ACTIVE
**ID:** D-20.5

## 2026-03-02 — Observers: What new snapshot fields to add?

**Decision:** Add 3 new clip cursor observers: `isLoopEnabled` (boolean), `loopStart` (double beats), and `clipColor` (r,g,b float array). Add scene color observers: `sceneColors` float[SCENE_COUNT][3] array in the scenes snapshot section. Clip color per-slot is already observed in StateCache (clipColors array).
**Rationale:** isLoopEnabled and loopStart are missing from the cursor clip observers despite loopLength/playStart/playStop being present. Clip cursor color enables reading the selected clip's color. Scene colors complete the grid visibility — tracks and clips already have colors.
**Alternatives considered:** (1) Skip scene colors — rejected, inconsistent with having track colors and clip colors. (2) Skip cursor clip color — rejected, needed for getLaunchSettings-style getters.
**Status:** ACTIVE
**ID:** D-20.6
