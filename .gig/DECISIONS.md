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

## 2026-03-03 — Architecture: How should song rebuild work?

**Decision:** Multi-step client-driven approach. The CLI sends a sequence of RPC calls orchestrated by a new `SongCommand` CLI class that reads the JSON file, parses sections, and issues RPC calls in the correct order with appropriate timing delays between phases. No new extension-side macro needed — the existing RPC methods + macros are sufficient. The CLI handles the orchestration (timing, sequencing, error recovery).

**Rationale:** The extension already has all the building blocks: `macro/setupScenes`, `macro/writeClip`, track/scene/clip operations, device insertion, cue markers, and mix controls. A single mega-macro on the extension side would be fragile (one failure = total rebuild failure, no visibility into progress). The CLI can log progress, retry on failure, and give the user control. This also avoids bloating the extension with song-specific logic.

**Alternatives considered:**
- (A) Single extension macro `macro/rebuildSong` — rejected: too complex for deferred scheduling, no error recovery, would be the largest single method by 10x
- (B) New extension-side rebuild handler with multi-phase scheduling — rejected: over-engineering, moves orchestration logic into the constrained extension environment

**Status:** ACTIVE
**ID:** D-21.1

---

## 2026-03-03 — Scope: What does the CLI rebuild command restore?

**Decision:** The rebuild restores in this order:
1. **Transport** — tempo, time signature
2. **Scenes** — create + rename + color (using `macro/setupScenes` + `scene/setColor`)
3. **Clips** — create clips and write notes per scene (using `macro/writeClip`)
4. **Clip colors** — set each clip's color to match scene color
5. **Track mix** — volume, pan, mute, solo, color per track
6. **Master mix** — volume, pan
7. **Cue markers** — position + name (using transport position + addAtPlayhead + rename)

**NOT restored** (manual steps):
- Instruments/presets (ISS-008 makes browser automation unreliable; user loads presets manually)
- Device chains and parameters (EQ, compressor settings — user rebuilds FX chain manually)
- Arrangement timeline clips (FLAG-3: no API for launcher→arranger copy)

**Rationale:** Notes + structure + mix is 90% of the rebuild value. Instruments require the user to load presets anyway (browser API is unreliable per ISS-008). Device parameters can be set after presets are loaded. This keeps the rebuild fast and reliable.

**Status:** ACTIVE
**ID:** D-21.2

---

## 2026-03-03 — CLI: SongCommand structure

**Decision:** Add `SongCommand` to the CLI as `gig song rebuild <file>`. It reads the JSON file, validates the structure, then executes the rebuild phases sequentially. Each phase logs progress to stdout. Uses `Thread.sleep()` between phases to allow Bitwig flush cycles to settle (same pattern as smoke tests).

Subcommands:
- `gig song rebuild <file>` — full rebuild from JSON
- `gig song dump` — export current session to JSON (calls snapshot + clip reads, mirrors the manual process we did)

**Rationale:** `dump` + `rebuild` are natural complements. The CLI already has the `RpcClient` infrastructure and Picocli subcommand pattern. `dump` automates the tedious manual export process (36 clip reads, device parameter capture, etc.).

**Status:** ACTIVE
**ID:** D-21.3

---

## 2026-03-03 — Timing: How to handle async settling between rebuild phases?

**Decision:** Use fixed delays between phases, calibrated from our real-world experience:
- Scene setup: 500ms after `macro/setupScenes` (need flush cycles for renames)
- Per-clip write: 200ms between `macro/writeClip` calls (each defers notes internally at 100ms)
- Clip color: 50ms between `clip/setColor` calls (synchronous operation)
- Cue markers: 200ms between add+rename cycles (transport position + addAtPlayhead is async)
- Track mix: no delay needed (synchronous parameter sets)

Total estimated rebuild time for a 36-clip song: ~15-20 seconds.

**Rationale:** Fixed delays are simple and reliable. The alternative (polling snapshot for confirmation) adds complexity and is fragile because snapshot changes are eventually consistent. Our real-world song composition session validated these timing patterns.

**Status:** ACTIVE
**ID:** D-21.4

---

## 2026-03-03 — JSON Format: Song file schema versioning

**Decision:** Add a `formatVersion` field to the `meta` section (value: `"1"` for current format). The rebuild command validates this field and rejects unknown versions. The existing `version` field in meta (`"gig-maestro-v0.20.8"`) is informational only — `formatVersion` is the contract.

Also normalize the JSON structure slightly:
- `clips[].lengthBeats` — add this field to each clip entry (currently missing, must be inferred from max note position). The `dump` command calculates it from the clip's actual length.
- `clips[].stepSize` — add this field (default 0.25 for all current clips). Needed by `macro/writeClip`.
- `clips[].color` — add per-clip color (currently stored on scenes, but clips may differ).
- `clips[].name` — add clip name if set.

**Rationale:** Without `lengthBeats` and `stepSize` in the JSON, the rebuild command has to guess or hardcode these values. Better to capture them at dump time. Format versioning prevents future incompatibilities.

**Status:** ACTIVE
**ID:** D-21.5

---

## 2026-03-03 — Extension: New RPC methods needed?

**Decision:** Add one new RPC method:
- `macro/dumpSong` — Returns the full song JSON (transport, tracks, scenes, clips with notes, mix settings, cue markers) in a single call. This replaces the manual 40+ RPC call sequence we used to build the JSON file. It iterates all tracks, selects each clip, reads notes, captures device info, and assembles the JSON — all server-side with proper flush cycle management.

No other new extension methods needed — the existing `macro/setupScenes`, `macro/writeClip`, `scene/setColor`, `clip/setColor`, track/master set methods, and cue marker methods cover the rebuild path.

**Rationale:** The dump side is the painful one — 36 individual clip selects + getNotes + device parameter reads. Doing this server-side avoids 40+ round-trips and handles cursor settle timing internally. The rebuild side already has adequate methods.

**Status:** ACTIVE
**ID:** D-21.6
