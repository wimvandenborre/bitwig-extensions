# Decisions

> Append-only log. Never delete entries — amend or revise instead.
> Active decisions for the current phase live here.
> When a phase completes, its decisions are archived to `phases/`.

---

## Phase 25 — Track Routing & Groups

### D-25.1 — Scope

**Status:** ACTIVE

**Decision:** Phase 25 adds group track detection, group fold/unfold control, group
navigation (into/out of groups), track type reporting in snapshot, and note routing
(addNoteSource/removeNoteSource). Audio I/O selection and SourceSelector write access
are NOT available in the API and are excluded.

**Rationale:** The Bitwig API v25 provides strong group track support (isGroup,
isGroupExpanded as SettableBooleanValue, Application.navigateIntoTrackGroup) and
note routing (Track.addNoteSource/removeNoteSource) but has no methods for audio
input/output selection — SourceSelector is read-only with only two boolean getters.
Focusing on what the API actually supports maximizes value.

**What's IN:** trackType + isGroup + isGroupExpanded in snapshot, track/setGroupExpanded
RPC, track/navigateInto + track/navigateToParent RPCs, track/addNoteSource +
track/removeNoteSource RPCs, track/createGroup RPC (via createParentTrack).

**What's OUT:** Audio I/O routing (no API), SourceSelector write (read-only),
send.sendMode() (already have isPreFader + send/setMode), siblingsTrackBank
(complex, limited value for LLM workflow).

---

### D-25.2 — Snapshot Enhancement

**Status:** ACTIVE

**Decision:** Add three new fields to each track in the snapshot: `trackType` (string),
`isGroup` (boolean), `isGroupExpanded` (boolean). These are observable via StateCache.

**Rationale:** The LLM needs to know which tracks are groups, what type each track is
(Group/Instrument/Audio/Hybrid/Effect/Master), and whether groups are expanded to make
informed decisions about track organization. All three are `Value` types with
`addValueObserver` support.

**Fields:**
- `trackType` — StringValue: "Group", "Instrument", "Audio", "Hybrid", "Effect", "Master"
- `isGroup` — BooleanValue (read-only)
- `isGroupExpanded` — SettableBooleanValue (read + write)

---

### D-25.3 — Group Control Methods

**Status:** ACTIVE

**Decision:** Add 3 RPC methods to TrackHandler for group management:
1. `track/setGroupExpanded` — params: `{expanded: boolean}` or `{toggle: true}` — uses
   `cursorTrack.isGroupExpanded().set(bool)` or `.toggle()`
2. `track/navigateInto` — no params — uses `application.navigateIntoTrackGroup(cursorTrack)`
   to make the group's children visible as top-level tracks
3. `track/navigateToParent` — no params — uses `application.navigateToParentTrackGroup()`
   to navigate back out of a group

**Rationale:** Group fold/unfold is essential for track organization. Navigate into/out
of groups is needed because TrackBank only shows tracks at the current group level when
not flattened. These methods let the LLM drill into group hierarchies.

---

### D-25.4 — Group Creation

**Status:** ACTIVE

**Decision:** Add `track/createGroup` RPC method that wraps the currently selected track
in a new group using `cursorTrack.createParentTrack(numSends, numScenes)`. Uses the
extension's existing bank dimensions (SendBank=4, SceneBank=5) for the parent track proxy.

**Rationale:** There is no dedicated `createGroup()` API method. The only way to create
a group is `Track.createParentTrack(numSends, numScenes)` which creates a parent group
above the current track. This is the standard Bitwig API pattern for grouping tracks.

---

### D-25.5 — Note Routing

**Status:** ACTIVE

**Decision:** Add 2 RPC methods:
1. `track/addNoteSource` — params: none — routes the extension's NoteInput to the
   current cursor track via `cursorTrack.addNoteSource(noteInput)`
2. `track/removeNoteSource` — params: none — removes the routing via
   `cursorTrack.removeNoteSource(noteInput)`

**Rationale:** `Track.addNoteSource(NoteInput)` and `removeNoteSource(NoteInput)` allow
routing the extension's MIDI note input directly to any track. Combined with Phase 23's
NoteInput handler (noteInput/sendNote, noteInput/sendMidi), this enables the LLM to
target note playback to specific tracks — essential for live performance and sound design
workflows.

---

### D-25.6 — API Structure

**Status:** ACTIVE

**Decision:** All new methods go in existing TrackHandler. No new handler class needed.
NoteInput reference is passed as an additional constructor parameter to TrackHandler.

**Rationale:** The 5 new methods (setGroupExpanded, navigateInto, navigateToParent,
createGroup, addNoteSource, removeNoteSource — 6 total) are all track-level operations
that naturally belong in TrackHandler. The Application instance is already available
in TrackHandler. NoteInput needs to be threaded through for the note routing methods.
