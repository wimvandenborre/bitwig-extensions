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

## 2026-02-28 — Track Management: What RPC methods to expose?

**Decision:** Add 5 new RPC methods: `track/create` (polymorphic type dispatch), `track/delete` (cursor-based), `track/rename`, `track/select`, `track/duplicate`.
**Rationale:** Cover the CRUD lifecycle for tracks.
**Alternatives considered:** See D-6.1a.
**Status:** AMENDED
**ID:** D-6.1

## 2026-02-28 — Track Management: What RPC methods to expose? (amended)

**Decision:** Add 7 new RPC methods with explicit verbs:
1. `track/createAudio` — takes optional `position` (int, default -1 = append). Calls `Application.createAudioTrack(position)`.
2. `track/createInstrument` — takes optional `position` (int, default -1 = append). Calls `Application.createInstrumentTrack(position)`.
3. `track/createEffect` — takes optional `position` (int, default -1 = append). Calls `Application.createEffectTrack(position)`.
4. `track/deleteSelected` — deletes the cursor track via `cursorTrack.deleteObject()`. No params.
5. `track/rename` — takes `name` (string). Sets `cursorTrack.name().set(name)` on the cursor track.
6. `track/select` — takes `index` (int, 0-based). Selects a track by bank index. Uses `trackBank.getItemAt(index).selectInEditor()`.
7. `track/duplicate` — duplicates the cursor track via `cursorTrack.duplicate()`. No params.
**Rationale:** Explicit method names per track type (audio/instrument/effect) are clearer for LLM tool use than a polymorphic `track/create` with a type enum. LLMs do better with explicit verbs. `track/deleteSelected` is explicitly named to clarify it operates on the cursor track. Overridden by user — original: 5 methods with polymorphic `track/create` and ambiguous `track/delete`.
**Alternatives considered:** (a) Polymorphic `track/create` with type enum — ambiguous in Bitwig land where track types are fundamentally different. (b) `track/deleteByIndex` — TrackBank items don't reliably expose `deleteObject()` since `Track` from `getItemAt()` may not implement `DeleteableObject` the same way as CursorTrack; cursor-based deletion is the safe path.
**Status:** ACTIVE
**ID:** D-6.1a

## 2026-02-28 — Track Management: How to handle track selection by index?

**Decision:** Use `trackBank.getItemAt(index).selectInEditor()` with 0–63 range validation. No error data on failure.
**Rationale:** See D-6.2a.
**Status:** AMENDED
**ID:** D-6.2

## 2026-02-28 — Track Management: How to handle track selection by index? (amended)

**Decision:** Use `trackBank.getItemAt(index).selectInEditor()` to select a track by index. Validate index is within 0–63 range. On out-of-range, return a JSON-RPC error with code -32602 and message including current bank width (64) and the invalid index. The CursorTrack (`followSelection=true`) will follow the selection, making subsequent cursor-based operations (rename, delete, insert device) target the selected track.
**Rationale:** Guardrail error with bank context (width, invalid index) helps the LLM self-correct without burning tokens on a snapshot call. `selectInEditor()` updates Bitwig's UI selection, which is exactly the "select" semantic needed for cursor follow. Overridden by user — original: no structured error data.
**Alternatives considered:** (a) `selectInMixer()` — editor selection is more natural for clip launcher workflow. (b) Return bank offset in error — bank offset is always 0 in our fixed 64-track bank so it's redundant; width is sufficient.
**Status:** ACTIVE
**ID:** D-6.2a

## 2026-02-28 — Track Management: Where to put track management code?

**Decision:** Add track management methods to existing `TrackHandler`. Pass `Application` and `CursorTrack` as new constructor parameters. No separate utility class.
**Rationale:** See D-6.3a.
**Status:** AMENDED
**ID:** D-6.3

## 2026-02-28 — Track Management: Where to put track management code? (amended)

**Decision:** Add track management methods to existing `TrackHandler`. Pass `Application` and `CursorTrack` as new constructor parameters. Extract a `TrackBankManager` utility class that owns bank-level index validation and selection semantics (`selectByIndex`, `validateIndex`). TrackHandler delegates index operations to TrackBankManager.
**Rationale:** Keeps TrackHandler focused on RPC dispatch. TrackBankManager isolates paging/index concerns so future features (bank resize, select by name, track search) have a clean home. Small now but prevents handler bloat later. Overridden by user — original: no TrackBankManager extraction.
**Alternatives considered:** (a) Keep index logic inline in TrackHandler — works now but gets messy when bank operations grow. (b) Full TrackService abstraction — over-engineered for 7 methods.
**Status:** ACTIVE
**ID:** D-6.3a

## 2026-02-28 — Track Management: How to verify track creation worked?

**Decision:** Return `"ok"` only. LLM diffs snapshots.
**Rationale:** See D-6.4a.
**Status:** AMENDED
**ID:** D-6.4

## 2026-02-28 — Track Management: How to verify track creation worked? (amended)

**Decision:** Return a richer response from track creation and mutation methods: `{"ok": true, "cursorTrackName": "...", "cursorTrackIndex": N}`. The `cursorTrackName` is read from `cursorTrack.name().get()` and `cursorTrackIndex` from the cursor track's position in the bank (if determinable). For `track/select`, return the same shape. For `track/deleteSelected`, return `{"ok": true}` (no track context since the deleted track is gone). For `track/rename`, return `{"ok": true, "cursorTrackName": "..."}` with the new name.
**Rationale:** Richer responses reduce token burn — the LLM doesn't need to call `session/snapshot` (a huge payload) just to confirm a track was created or renamed. The cursor track name is a cheap read that provides immediate feedback. Overridden by user — original: return only "ok" for all methods.
**Alternatives considered:** (a) Monotonic bank version counter — adds state tracking complexity for marginal benefit vs. just returning the cursor name. (b) Full snapshot in response — way too heavy; cursor name is the right granularity.
**Status:** ACTIVE
**ID:** D-6.4a

## 2026-02-28 — Track Management: Should we expose the Action system?

**Decision:** Do NOT expose the Action system in Phase 6. Defer to a later phase. The 3 explicit track creation methods cover audio, instrument, and effect tracks. Group track creation deferred.
**Rationale:** The Action system is a general-purpose escape hatch with hundreds of undocumented action IDs. Exposing it prematurely adds surface area without clear value. Specific create methods are type-safe and sufficient.
**Alternatives considered:** (a) Expose `action/invoke` as generic RPC — too open-ended, could trigger destructive actions. (b) Add `track/createGroup` via actions now — action ID discovery needed, not guaranteed stable.
**Status:** ACTIVE
**ID:** D-6.5
