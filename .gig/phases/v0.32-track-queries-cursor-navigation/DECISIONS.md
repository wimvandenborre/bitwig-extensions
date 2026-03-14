# Decisions

> Append-only log. Never delete entries — amend or revise instead.
> Active decisions for the current phase live here.
> When a phase completes, its decisions are archived to `phases/`.

## 2026-03-14 — Scope: What track query and cursor navigation methods to add?

**Decision:** Two concerns: (1) Add `canHoldNoteData` and `canHoldAudioData` to StateCache + snapshot for all 8 track bank tracks — these are the missing capability flags Claude needs for conditional logic. Skip `position` (already derivable from bank index + scroll position). (2) Add 5 CursorTrack navigation methods: `selectParent`, `selectFirstChild`, `isPinned`/`setPinned`, and a combined `cursorTrack/getInfo` that returns the cursor track's name, position, type, and pinned state.
**Rationale:** trackType/isGroup are already in the snapshot — no new RPC needed for those. The real gaps are canHoldNoteData/canHoldAudioData (Claude can't tell if a track accepts MIDI vs audio) and cursor navigation (can't traverse group hierarchies). The isPinned flag prevents cursor from jumping during batch operations.
**Alternatives considered:** Adding a standalone `track/getCapabilities` RPC (unnecessary — snapshot already delivers per-frame), adding all Cursor methods (selectFirst/Last/Next/Previous already exist as cursor_selectTrack with direction param).
**Status:** ACTIVE
**ID:** D-1.1

## 2026-03-14 — StateCache: How to add track capabilities?

**Decision:** Add two new boolean arrays to StateCache: `trackCanHoldNoteData[TRACK_COUNT]` and `trackCanHoldAudioData[TRACK_COUNT]`. Register observers in `registerGroupObservers()` alongside existing trackType/isGroup observers. Include in snapshot under each track object as `canHoldNoteData` and `canHoldAudioData`.
**Rationale:** Follows the exact pattern used for trackType, isGroup, and isGroupExpanded — same observer registration, same snapshot integration, same delta detection. The `canHoldNoteData()` and `canHoldAudioData()` methods return SettableBooleanValue (read-only in practice), so standard boolean observers work.
**Alternatives considered:** Lazy RPC query (breaks delta pattern — all track state should flow through StateCache), deriving from trackType (unreliable — Hybrid tracks can hold both).
**Status:** ACTIVE
**ID:** D-1.2

## 2026-03-14 — Cursor navigation: New handler or extend TrackHandler?

**Decision:** Add cursor navigation methods to TrackHandler since it already holds the CursorTrack reference. New methods: `cursor/selectParent` (no params), `cursor/selectFirstChild` (no params), `cursor/setPinned({pinned: boolean})`, `cursor/getInfo` (returns name, position, trackType, isPinned). Use the `cursor/` namespace prefix (already established by `cursor/selectTrack`).
**Rationale:** TrackHandler already has the CursorTrack object and uses the `cursor/` prefix for `cursor/selectTrack`. Adding 4 more cursor methods there is natural. A separate CursorHandler would be over-engineering for 4 methods.
**Alternatives considered:** New CursorHandler (unnecessary — TrackHandler already owns cursor), adding to ApplicationHandler (wrong — cursor is track-related).
**Status:** ACTIVE
**ID:** D-1.3

## 2026-03-14 — Delta: Should track capabilities trigger delta notifications?

**Decision:** Yes — include canHoldNoteData and canHoldAudioData in the tracks section hash computation. They will naturally flow through the existing delta mechanism since they're part of the track snapshot object.
**Rationale:** Follows established pattern. Track capabilities rarely change (only when track type changes), so delta overhead is negligible. Clients that monitor track state get the full picture.
**Alternatives considered:** Excluding from delta (inconsistent — all snapshot fields participate in delta).
**Status:** ACTIVE
**ID:** D-1.4
