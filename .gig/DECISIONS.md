# Decisions

> Append-only log. Never delete entries — amend or revise instead.
> Active decisions for the current phase live here.
> When a phase completes, its decisions are archived to `phases/`.

## 2026-03-04 — Scope: What does "NoteInput & Arpeggiator" mean for Phase 23?

**Decision:** Create a NoteInput in `init()`, then expose real-time MIDI injection (`sendRawMidiEvent`), arpeggiator control (all 12 properties), and note latch control (5 properties) via RPC. Also add arpeggiator and note latch state to the session snapshot. Exclude MPE/Expressive MIDI setup and key/velocity translation tables — they're niche features better suited for a future phase.

**Rationale:** NoteInput is the gateway to real-time note playback (vs. clip-based sequencing). The arpeggiator and note latch are the highest-value sub-components — they transform held notes into rhythmic patterns. MPE and translation tables require 128-entry arrays over JSON which is awkward, and few users need them. Keeping scope tight delivers the most useful features first.

**Alternatives considered:**
- Full NoteInput (MPE, translation tables, expression mapping) — too much JSON complexity for 128-entry arrays, niche use cases
- Arpeggiator only (no note injection) — misses the real-time playback use case entirely
- NoteInput + Arpeggiator + NoteLatch + translation tables — scope creep, tables can wait

**Status:** ACTIVE
**ID:** D-23.1

---

## 2026-03-04 — Infrastructure: How to create and manage the NoteInput?

**Decision:** Create a single NoteInput named "Gig Maestro" in `init()` with no MIDI filter mask (accepts all note messages). Store the reference alongside the Arpeggiator and NoteLatch accessors. Pass all three to a new `NoteInputHandler` class. The NoteInput is created once and persists for the extension lifetime — no dynamic creation/destruction. Requires declaring 1 MIDI input port in `GigMaestroDefinition.getNumMidiInPorts()`.

**Rationale:** `createNoteInput()` is on `MidiIn` (not `ControllerHost`) — obtained via `host.getMidiInPort(0)`. Must be called during `init()` — hard API constraint. Extension must declare at least 1 MIDI input port. A single NoteInput with no filter mask is the most flexible default. The handler pattern matches all other handlers (constructor takes API objects, `register(dispatcher)` adds methods). Dynamic NoteInput creation would require extension restart, which is impractical.

**Alternatives considered:**
- Multiple NoteInputs (e.g., per-channel) — unnecessary complexity, one input can send to any channel via raw MIDI
- Lazy creation via RPC — impossible, API requires init()-time creation
- No NoteInput, arpeggiator-only — arpeggiator requires a NoteInput to function

**Status:** ACTIVE
**ID:** D-23.2

---

## 2026-03-04 — API Design: How to structure the RPC methods?

**Decision:** 10 new RPC methods organized in 3 groups:

**NoteInput (2 methods):**
1. `noteInput/sendNote` — convenience method: `{note, velocity, channel?}`. velocity=0 sends note-off. Maps to `sendRawMidiEvent()` with proper status byte construction.
2. `noteInput/sendMidi` — raw MIDI: `{status, data0, data1}`. Direct passthrough to `sendRawMidiEvent()`.

**Arpeggiator (4 methods):**
3. `arpeggiator/configure` — batch-set properties: `{mode?, octaves?, rate?, gateLength?, shuffle?, humanize?, isFreeRunning?, enableOverlappingNotes?, usePressureToVelocity?, terminateNotesImmediately?}`. All optional — only set what's provided.
4. `arpeggiator/setEnabled` — `{enabled}`. Separate because enable/disable is the most common action.
5. `arpeggiator/releaseNotes` — no params. Release all arpeggiator notes.
6. `arpeggiator/getState` — no params. Returns current arpeggiator state (all properties).

**NoteLatch (4 methods):**
7. `noteLatch/configure` — batch-set: `{mode?, mono?, velocityThreshold?}`. All optional.
8. `noteLatch/setEnabled` — `{enabled}`.
9. `noteLatch/releaseNotes` — no params.
10. `noteLatch/getState` — no params. Returns current latch state.

**Rationale:** Batch configure methods reduce RPC round-trips for arpeggiator setup (commonly set mode+rate+octaves together). Separate enable/release methods are the most frequent operations and deserve their own endpoints. getState methods provide on-demand reads independent of the snapshot cycle. Two note input methods cover both convenient (sendNote) and advanced (sendMidi) use cases.

**Alternatives considered:**
- One method per property (12+ arpeggiator methods) — too many, clutters the API
- Single mega-configure for everything — NoteInput, arpeggiator, and latch have distinct concerns
- No getState, snapshot only — getState is useful for immediate reads without full snapshot overhead

**Status:** ACTIVE
**ID:** D-23.3

---

## 2026-03-04 — Snapshot: Should arpeggiator/noteLatch appear in the session snapshot?

**Decision:** Yes. Add an `arpeggiator` section and a `noteLatch` section to the session snapshot. The arpeggiator section includes: `isEnabled`, `mode`, `octaves`, `rate`, `gateLength`, `shuffle`, `humanize`, `isFreeRunning`, `enableOverlappingNotes`, `usePressureToVelocity`, `terminateNotesImmediately`. The noteLatch section includes: `isEnabled`, `mode`, `mono`, `velocityThreshold`, `activeNotes`.

**Rationale:** These are persistent settings (not transient like MIDI events). Including them in the snapshot lets the LLM see current arpeggiator/latch state alongside track and device state, enabling informed decisions about what to configure. The data is small (12 + 5 fields) and doesn't bloat the snapshot.

**Alternatives considered:**
- No snapshot, getState only — forces extra RPC calls to see arp/latch state
- Combined "noteInput" section — would mix NoteInput config (which has no observable state) with arp/latch

**Status:** ACTIVE
**ID:** D-23.4

---

## 2026-03-04 — Arpeggiator modes: How to handle the enum?

**Decision:** Accept mode strings case-insensitively and validate against the 16 known values. Return clear error listing all valid modes on invalid input. Mode strings: `all`, `up`, `up-down`, `up-then-down`, `down`, `down-up`, `down-then-up`, `flow`, `random`, `converge-up`, `converge-down`, `diverge-up`, `diverge-down`, `thumb-up`, `thumb-down`, `pinky-up`, `pinky-down`.

**Rationale:** Same pattern as NoteOccurrence in Phase 22 — string matching with case-insensitive comparison. Mode strings are stable API constants. A validation set with clear error messages is more ergonomic than letting Bitwig throw opaque errors.

**Alternatives considered:**
- Pass through without validation — Bitwig may silently ignore invalid modes
- Numeric mode IDs — less readable, error-prone

**Status:** ACTIVE
**ID:** D-23.5
