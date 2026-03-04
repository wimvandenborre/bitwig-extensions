# Decisions

> Append-only log. Never delete entries — amend or revise instead.
> Active decisions for the current phase live here.
> When a phase completes, its decisions are archived to `phases/`.

## 2026-03-04 — Scope: What does "sound design" mean for Phase 22?

**Decision:** Focus exclusively on **NoteStep expressive properties**. Expose all 11 missing per-note properties via RPC: pan, timbre, pressure, gain, transpose, releaseVelocity, velocitySpread, occurrence, recurrence, repeat (count + curve + velocityEnd + velocityCurve), and mute. This is the single highest-value gap — it transforms flat MIDI sequences into expressive performances.

**Rationale:** Currently only velocity, duration, and chance are exposed. The Bitwig NoteStep API has 14+ properties, all with getters and setters. These are synchronous operations on the CursorClip (no deferred scheduling needed). Arpeggiator, NoteInput, and modulation routing are larger features better suited for separate phases.

**Alternatives considered:**
- NoteInput/Arpeggiator — larger scope, requires new infrastructure (NoteInput creation, handler class), better as its own phase
- Device parameter modulation — API surface is limited, less immediate value
- All of the above — too large for one phase

**Status:** ACTIVE
**ID:** D-22.1

---

## 2026-03-04 — API Design: How to expose expressive properties?

**Decision:** Four new RPC methods:
1. `clip/setNoteExpressions` — batch-set scalar expressive properties on existing notes. Takes array of `{x, y, property, value}` where property is one of: `pan`, `timbre`, `pressure`, `gain`, `transpose`, `releaseVelocity`, `velocitySpread`, `mute`. Single method handles all scalar properties.
2. `clip/setNoteRepeat` — set repeat properties. Takes array of `{x, y, count, curve, velocityEnd, velocityCurve}` since repeat has 4 sub-properties set together.
3. `clip/setNoteOccurrence` — set occurrence condition. Takes array of `{x, y, condition, enabled}` where condition is a NoteOccurrence enum string.
4. `clip/setNoteRecurrence` — set recurrence pattern. Takes array of `{x, y, length, mask}` where length is 1-8 and mask is a bitmask.

Also extend `clip/getNotes` to return all expressive properties in the response (currently only returns x, y, velocity, duration, chance).

**Rationale:** Grouping scalar properties into one method reduces method count while keeping repeat/occurrence/recurrence separate since they have distinct parameter shapes. Extending getNotes ensures round-trip fidelity for the song dump/rebuild pipeline.

**Alternatives considered:**
- One method per property (14+ methods) — too many, clutters the API
- Single mega-method for everything — parameter shapes differ too much (scalar vs enum vs bitmask)
- Modify clip/setNotes to accept all properties inline — would change existing API contract

**Status:** ACTIVE
**ID:** D-22.2

---

## 2026-03-04 — Snapshot: Should expressive properties appear in the clip snapshot?

**Decision:** No. Expressive properties are per-note, not per-clip. They're returned by `clip/getNotes` (after this phase). The clip snapshot stays focused on clip-level metadata. Adding 11 properties × N notes to the snapshot would make it massive and slow.

**Rationale:** Snapshot is a quick state overview. Note-level detail belongs in `clip/getNotes`.

**Status:** ACTIVE
**ID:** D-22.3

---

## 2026-03-04 — Song Dump/Rebuild: How to handle expressive properties?

**Decision:** The dump side is automatic — `clip/getNotes` already returns whatever the extension provides, so adding properties to getNotes means the dump captures them. Extend `gig song rebuild` to write expressive properties after base notes: `macro/writeClip` → `clip/setNoteExpressions` → `clip/setNoteRepeat` → `clip/setNoteOccurrence` → `clip/setNoteRecurrence` (only if data present).

**Rationale:** Zero dump-side changes needed. Rebuild needs explicit calls for each expression type since `macro/writeClip` only handles base notes (velocity, duration).

**Status:** ACTIVE
**ID:** D-22.4

---

## 2026-03-04 — NoteOccurrence enum: How to handle?

**Decision:** Map NoteOccurrence enum values to string constants matching Bitwig's enum names. Use string matching (case-insensitive) in the RPC handler with a clear error listing valid values on invalid input.

**Rationale:** Enum values are stable across API versions. String matching is more ergonomic for RPC clients than numeric codes.

**Status:** ACTIVE
**ID:** D-22.5
