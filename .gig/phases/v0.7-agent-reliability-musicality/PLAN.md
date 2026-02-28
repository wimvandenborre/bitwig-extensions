# Plan

> Living document — reflects what was done, not just what was intended.
> This file tracks the ACTIVE phase only. Completed phases are archived to `phases/`.

---

## Active Phase

### Phase 7 — Agent Reliability + Musicality (v0.7.x)

> Overhaul the system prompt and tool descriptions to make the LLM agent reliable (handles async lag, recovers from errors, verifies mutations) and musical (uses correct MIDI mappings, builds coherent songs from templates, follows operational music theory). No new RPC methods — all changes are in `tools/system-prompt.md` and `tools/claude-tools.json`.

**Decisions:** D-7.1a, D-7.2, D-7.3, D-7.4a, D-7.5, D-7.6

| Batch | Version | Title | Delegation | Status |
|-------|---------|-------|------------|--------|
| 7.1 | `0.7.1` | Known behaviors + error recovery sections | in-session | done |
| 7.2 | `0.7.2` | Operational music reference | in-session | done |
| 7.3 | `0.7.3` | Song building section + recommended call sequences | in-session | done |
| 7.4 | `0.7.4` | Tool description warnings + smoke tests | in-session | done |

### Batch 7.1 — Known Behaviors + Error Recovery Sections

**Delegation:** in-session
**Decisions:** D-7.2, D-7.3
**Files:** `tools/system-prompt.md`
**Work:**
- Add "Known Behaviors" section documenting: async cursor lag (cursorTrackName in responses may be stale; snapshot after mutations for authoritative state), flush cycle timing (~50ms), cursor device loss after `device_remove`
- Add "Error Recovery" section documenting: JSON-RPC error codes (-32602, -32601, -32603) and what they mean, common failure scenarios with recovery actions (out-of-range → snapshot; device not found → listBitwigDevices; cursor lost → select by index), the rule "snapshot before retry — never retry blindly"
**Test criteria:** System prompt contains "Known Behaviors" and "Error Recovery" sections. Grep for key phrases: "flush cycle", "-32602", "snapshot before retry".
**Acceptance:** Agent has clear guidance on when to trust inline responses vs. snapshot, and structured recovery patterns for every known failure mode.

### Batch 7.2 — Operational Music Reference

**Delegation:** in-session
**Decisions:** D-7.4a
**Files:** `tools/system-prompt.md`
**Work:**
- Add "Music Reference" section with:
  - MIDI note ↔ name table (C1=24 through C6=84, all naturals + sharps/flats, octave boundaries)
  - Scale formulas as semitone offset sets: major, natural minor, pentatonic major/minor, blues
  - Chord templates as interval sets: major/minor triads, dom7, min7, maj7, dim, aug
  - GM drum map: kick=36, snare=38, closed HH=42, open HH=46, ride=51, crash=49, tom-hi=48, tom-lo=45, clap=39
  - Velocity bands: ghost=0.2–0.35, soft=0.4–0.55, normal=0.6–0.75, accent=0.8–0.95
- All values must be directly usable in `clip_setNotes` — no prose theory, just numbers
**Test criteria:** System prompt contains "Music Reference" section. Grep for "semitone", "ghost", "kick=36", "[0,2,4,5,7,9,11]".
**Acceptance:** Every value in the reference can be plugged directly into a `clip_setNotes` call without conversion. No general music theory — strictly operational.

Depends on Batch 7.1 (section ordering).

### Batch 7.3 — Song Building Section + Recommended Call Sequences

**Delegation:** in-session
**Decisions:** D-7.1a, D-7.5
**Files:** `tools/system-prompt.md`
**Work:**
- Add "Song Building" section with:
  - Standard song structure template (intro → verse → chorus → bridge → outro with bar counts)
  - Step-by-step "build from scratch" workflow as a recommended call sequence: set tempo → create drum track → create bass → create lead → layer clips across scenes → launch
  - Clip length guidance (multiples of 4 bars in 4/4)
  - Track ordering conventions (drums, bass, harmony, melody, effects)
  - Default assumptions (4/4 time, 120 BPM, step size 0.25)
- One recommended call sequence per domain (track creation, note writing, device setup) per D-7.1a
- Replace the existing "Workflow for creating a song structure" in Track Management with a cross-reference to this new section
**Test criteria:** System prompt contains "Song Building" section. Grep for "intro", "verse", "chorus", "4/4", "120 BPM".
**Acceptance:** Agent has a concrete multi-track song building workflow. Each domain has one recommended call sequence. Default assumptions are explicit.

Depends on Batch 7.2 (music reference must exist for song building to reference).

### Batch 7.4 — Tool Description Warnings + Smoke Tests

**Delegation:** in-session
**Decisions:** D-7.6
**Files:** `tools/claude-tools.json`, `scripts/smoke-test.sh`
**Work:**
- Update tool descriptions (one sentence each):
  - `device_remove` — warn cursor device loses selection after removal
  - `track_createAudio`, `track_createInstrument`, `track_createEffect` — note cursorTrackName may lag; verify with snapshot
  - `cursor_selectTrack` — clarify sequential only; use `track_select` for direct index access
  - `clip_launch` — note this starts transport if stopped
- Update smoke tests:
  - Offline: verify system prompt contains "Known Behaviors", "Error Recovery", "Music Reference", "Song Building" sections
  - Offline: verify tool descriptions for device_remove contain "loses selection", track creates contain "snapshot to verify", cursor_selectTrack contains "track_select", clip_launch contains "transport"
  - Verify all 55 tools still valid JSON, no breaking changes
**Test criteria:** `./scripts/smoke-test.sh --offline` all pass. `./gradlew test` all pass. Tool JSON validates.
**Acceptance:** All 4 tool warnings present. All 4 new system prompt sections present. No existing tests broken. Smoke test count increases.

Depends on Batches 7.1, 7.2, 7.3 (needs final prompt to validate).

**Phase Acceptance Criteria:**
- [ ] System prompt has "Known Behaviors" section with async lag documentation
- [ ] System prompt has "Error Recovery" section with error codes and recovery patterns
- [ ] System prompt has "Music Reference" section with MIDI table, scales, chords, drums, velocity bands
- [ ] System prompt has "Song Building" section with structure template and call sequences
- [ ] All values in music reference are directly usable in clip_setNotes (no conversion needed)
- [ ] Tool descriptions updated: device_remove, track creates, cursor_selectTrack, clip_launch
- [ ] One recommended call sequence per domain (track, notes, devices)
- [ ] Default assumptions explicit (4/4, 120 BPM, step size 0.25)
- [ ] All existing tests still pass (51 unit + 101 offline smoke)
- [ ] New smoke tests validate all 4 system prompt sections and 4 tool description warnings
- [ ] No new RPC methods or Java code changes
- [ ] Tool JSON validates (>= 55 tools, no breaking changes)

**Completion triggers Phase 8 → version `0.8.0`**

---

## Plan Amendments

<!-- Log any changes to the plan after creation -->

| Date | Version | Amendment | Reason |
|------|---------|-----------|--------|
