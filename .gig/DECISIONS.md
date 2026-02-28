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

## 2026-02-28 — Scope: What does Phase 7 cover?

**Decision:** Phase 7 is a system prompt + tool description overhaul. No new RPC methods. No new Java code (except possibly a lightweight timing delay in responses). The deliverable is a significantly better `tools/system-prompt.md` and refined `tools/claude-tools.json` descriptions. The agent becomes reliable and musical through better instructions, not more endpoints.
**Rationale:** The 55-method API surface is feature-complete for basic song creation. The gap is guidance — the agent can operate every knob but doesn't know *what* to do with them. System prompt changes are high-leverage (every future agent session benefits) and low-risk (no runtime code changes).
**Alternatives considered:** (a) Add new RPC methods (e.g., `session/snapshotLight`, `track/getDeviceChain`) — adds complexity, doesn't solve musicality. (b) Build an MCP wrapper that injects music theory — over-engineered, couples concerns.
**Status:** AMENDED
**ID:** D-7.1

## 2026-02-28 — Scope: What does Phase 7 cover? (amended)

**Decision:** Phase 7 is a system prompt + tool description overhaul. No new RPC methods or Java code. Two explicit exceptions are allowed: (1) doc-only aliases / rewording in tool descriptions (no new endpoints), and (2) adding a single "recommended call sequence" per domain in the system prompt. The deliverable is a significantly better `tools/system-prompt.md` and refined `tools/claude-tools.json` descriptions.
**Rationale:** The 55-method API surface is feature-complete. The gap is guidance, not endpoints. The exceptions ensure Phase 7 can fix UX without being blocked by a "no touching anything measurable" constraint — reworded descriptions and recommended sequences are doc changes with testable outcomes. Overridden by user — original: strict "no new RPC methods" with no explicit exceptions.
**Alternatives considered:** Same as D-7.1.
**Status:** ACTIVE
**ID:** D-7.1a

## 2026-02-28 — Reliability: How to handle async cursor lag?

**Decision:** Document the async lag in the system prompt as a "Known Behavior" section. After track creation or mutation, the agent should call `session_snapshot` and read `device.cursorTrackName` to get the authoritative cursor state — NOT trust the inline `cursorTrackName` in the response. Add a timing note: "State updates propagate within one flush cycle (~50ms). A brief pause before snapshot after mutations gives reliable reads."
**Rationale:** The lag is inherent to Bitwig's observer model — fixing it in code would require a blocking wait in the session thread, which could deadlock. Documenting it is the right approach. The agent already calls snapshot as part of the perception-action loop; this just makes the pattern explicit for mutation verification.
**Alternatives considered:** (a) Add `Thread.sleep(100)` before reading cursorTrackName in handlers — risky on the session thread, could cause stutter. (b) Remove cursorTrackName from responses entirely — loses the "quick feedback" benefit for cases where it IS current.
**Status:** ACTIVE
**ID:** D-7.2

## 2026-02-28 — Reliability: What error recovery patterns should the agent follow?

**Decision:** Add an "Error Recovery" section to the system prompt covering: (1) JSON-RPC error codes and what they mean (-32602 = bad params, -32601 = unknown method, -32603 = internal error), (2) common failure scenarios and recovery (out-of-range index → snapshot to find valid range; device not found → call listBitwigDevices; cursor lost after delete → select by index), (3) the rule "if an operation fails, snapshot before retrying — never retry blindly."
**Rationale:** Without recovery guidance, the agent loops on failures or gives up. Structured recovery patterns prevent the "keep inserting EQ-5 to wake up cursor" anti-pattern we observed in Phase 5 testing.
**Alternatives considered:** (a) Return structured error objects with recovery hints from the server — adds complexity to every handler, mixes concerns. (b) Just say "retry once" — doesn't help with systematic failures.
**Status:** ACTIVE
**ID:** D-7.3

## 2026-02-28 — Musicality: What music theory reference should the system prompt include?

**Decision:** Add a "Music Reference" section covering: (1) MIDI note table with octave labels and common instrument ranges, (2) scale formulas for major, natural minor, pentatonic major/minor, blues, (3) common chord progressions (I-IV-V-I, I-vi-IV-V, vi-IV-I-V, ii-V-I) with concrete MIDI note examples in C, (4) drum pattern templates (4-on-the-floor, breakbeat, hi-hat patterns), (5) velocity dynamics guide (accent patterns, ghost notes, humanization via ±0.05 velocity variation). Keep it concise — a reference card, not a textbook.
**Rationale:** The agent already knows music from training data, but needs a concrete reference anchored to Gig Maestro's coordinate system (steps, MIDI notes, velocity 0-1). Without this, every session the agent reinvents the mapping from "funk bassline" to actual step/note coordinates. A reference card eliminates that cognitive overhead.
**Alternatives considered:** (a) Embed full music theory — too long, burns context. (b) Link to external resources — agent can't fetch them during tool use. (c) Skip it and rely on training knowledge — works sometimes but produces inconsistent results across sessions.
**Status:** AMENDED
**ID:** D-7.4

## 2026-02-28 — Musicality: What music theory reference should the system prompt include? (amended)

**Decision:** Add an operational "Music Reference" section strictly tied to the step grid tools. Must include: (1) MIDI note ↔ name table (at least C1–C6, with octave boundaries), (2) scale formulas as semitone offset sets (e.g., major = [0,2,4,5,7,9,11], minor = [0,2,3,5,7,8,10], pentatonic major = [0,2,4,7,9], pentatonic minor = [0,3,5,7,10], blues = [0,3,5,6,7,10]), (3) chord templates as interval sets (major triad = [0,4,7], minor = [0,3,7], dom7 = [0,4,7,10], min7 = [0,3,7,10], maj7 = [0,4,7,11]), (4) GM drum map defaults (kick=36, snare=38, closed HH=42, open HH=46, ride=51, crash=49, tom-hi=48, tom-lo=45, clap=39), (5) velocity bands (ghost=0.2–0.35, soft=0.4–0.55, normal=0.6–0.75, accent=0.8–0.95). No general theory — everything expressed as numbers the agent can plug directly into `clip_setNotes`.
**Rationale:** Keeps it short and directly usable for setNotes / step grid coords. The agent doesn't need to know *why* a minor scale sounds sad — it needs `root + [0,2,3,5,7,8,10]` to generate the right MIDI note numbers. Overridden by user — original: broader reference card including progressions and humanization tips.
**Alternatives considered:** Same as D-7.4.
**Status:** ACTIVE
**ID:** D-7.4a

## 2026-02-28 — Musicality: Should the system prompt include song structure templates?

**Decision:** Add a "Song Building" section with: (1) a standard song structure template (intro 4-8 bars → verse 8-16 bars → chorus 8-16 bars → bridge 8 bars → outro 4-8 bars), (2) a step-by-step "build a song from scratch" workflow (set tempo → create drum track → create bass track → create lead → layer clips across scenes → arrange), (3) guidance on clip lengths (multiples of 4 bars in 4/4), (4) track ordering conventions (drums first, bass, harmony, melody, effects), (5) default assumptions (4/4 time, 120 BPM default, step size 0.25 for most work).
**Rationale:** The existing "Workflow for creating a song structure" in the Track Management section only covers creating one track. The agent needs a higher-level plan for assembling a multi-track arrangement. Song templates prevent the "64 random loops on 64 tracks" failure mode.
**Alternatives considered:** (a) Genre-specific templates (EDM, jazz, rock) — too many, too long. Start with one generic template and let the agent adapt. (b) No templates, just examples — examples are one-shot; templates are reusable.
**Status:** ACTIVE
**ID:** D-7.5

## 2026-02-28 — Reliability: Should tool descriptions be updated to warn about known limitations?

**Decision:** Update these tool descriptions in `claude-tools.json`: (1) `device_remove` — add warning that cursor device loses selection after removal; select another device or use snapshot to verify state. (2) `track_createAudio/Instrument/Effect` — note that `cursorTrackName` in response may lag; use snapshot to verify. (3) `cursor_selectTrack` — clarify this is sequential navigation only; use `track_select` for direct index access. (4) `clip_launch` — note that this starts the transport if stopped. Keep changes minimal — one sentence per tool, not paragraphs.
**Rationale:** Tool descriptions are the agent's first line of guidance. One-sentence warnings prevent known failure modes without bloating the tool schema. These specific tools caused failures in Phase 5 and 6 live testing.
**Alternatives considered:** (a) Don't touch tool descriptions, put everything in system prompt — agent may not re-read system prompt before every tool call. (b) Add detailed multi-paragraph descriptions — bloats the tool schema, increases token cost per call.
**Status:** ACTIVE
**ID:** D-7.6
